package com.agilecontrol.phone;
import java.util.*;
import java.io.ByteArrayInputStream;
import java.io.StringBufferInputStream;
import java.rmi.RemoteException;
import java.sql.Connection;

import javax.servlet.http.Cookie;

import com.agilecontrol.b2b.schema.TableManager;
import com.agilecontrol.nea.core.control.ejb.Command;
import com.agilecontrol.nea.core.control.event.DefaultWebEvent;
import com.agilecontrol.nea.core.control.util.ValueHolder;
import com.agilecontrol.nea.core.control.web.UserWebImpl;
import com.agilecontrol.nea.core.control.web.WebUtils;
import com.agilecontrol.nea.core.io.PluginLifecycleListener;
import com.agilecontrol.nea.core.query.QueryEngine;
import com.agilecontrol.nea.core.query.QueryUtils;
import com.agilecontrol.phone.UserObj;
import com.agilecontrol.nea.core.rest.SipStatus;
import com.agilecontrol.nea.core.util.CookieKeys;
import com.agilecontrol.nea.core.util.MessagesHolder;
import com.agilecontrol.nea.core.velocity.VelocityUtils;
import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.nea.util.NDSRuntimeException;
import com.agilecontrol.nea.util.Tools;
import com.agilecontrol.nea.util.Validator;

import org.apache.axis.utils.NetworkUtils;
import org.apache.velocity.VelocityContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.python.modules.thread.thread;

import redis.clients.jedis.Jedis;

/**
 * ��������������FairCmd��ͬ���ǣ��ͻ��˲������֣�����ҪFairContext
 * 
 * ����web����cmd���ת��Ϊclass�Ķ�λ��ȡ��ʽ
 *  ���ȶ�ȡ�����ļ� /META-INF/conf/portalcmdmap.properties����ʽ webcmd=javaclass
 *  ��:
 *  portal.getmenus=com.agilecontrol.portal.cmd.GetMenus
 *  ���û�ж�λ������Ĭ�ϴ��˵�cmd����class����ֱ�Ӷ�λ�����class����û��"."����Ĭ���� ad_param#phone.cmd.packages ָ���Ĳ�����Ѱ����Ӧclass
 * 
 */
@Admin(mail="sun.tao@lifecycle.cn")
public class PortalCmd extends Command implements PluginLifecycleListener{
	/**
	 * Ϊ���cmdhanlder class �������ܣ����ô˻��棺
	 * key: cmdHandler name�������ٶ���Package ������, class ��Ӧtask������
	 */
	private Hashtable<String, Class> cmdHandlerClasses= new Hashtable();
	/**
	 *  /META-INF/conf/portalcmdmap.properties
	 */
	private Properties cmdMapping;
	
	/**
	 * ����/META-INF/conf/portalcmdmap.properties����cmdӳ�䣬���û�ж���ӳ�䣬����ԭֵ
	 * @param cmdFromWeb portalcmdmap.properties��key�Ĳ���
	 * @return ���ӳ��δ���壬������ԭֵ
	 */
	private String findCmdByMapping(String cmdFromWeb) throws Exception{
		if(cmdMapping==null){
			String props=PhoneController.getInstance().getResourceAsString("META-INF/conf/portalcmdmap.properties");
			cmdMapping=new Properties();
			ByteArrayInputStream inStream= new ByteArrayInputStream( props.getBytes("iso8859-1"));
			cmdMapping.load(inStream);
		}
		String value=cmdMapping.getProperty(cmdFromWeb);
		if(value==null) value=cmdFromWeb;
		return value;
	}
	
	private CmdHandler newInstance(String cmdHandlerName) throws Exception{
		CmdHandler cmdHandler=null;
		Class clazz= cmdHandlerClasses.get(cmdHandlerName);
		if(clazz!=null){
			cmdHandler=(CmdHandler) clazz.newInstance();
		}else{
		
			for(String packageName: PhoneController.getInstance().getCmdHandlerPackages()){
				String className=packageName+"."+cmdHandlerName;
				try{
					clazz =Class.forName(className);
					cmdHandler=(CmdHandler) clazz.newInstance();
					break;
				}catch(ClassNotFoundException cnfe){
					
				}
			}
			if(cmdHandler == null){
				logger.error("Fail to find cmdhandler "+ cmdHandlerName+ " in packages: "+ 
						PhoneController.getInstance().getCmdHandlerPackages());
				throw new NDSException("δ����cmd:"+cmdHandlerName );
			}else{
				cmdHandlerClasses.put(cmdHandlerName, clazz);
			}
		}
		return cmdHandler;
	}
	/**
	 * 
	 * @param jsonObject
	 *            - cmd* - command, in folder: com.agilecontrol.fair.cmd
	 * 
	 * @return "result" will be jsonObject of ResultSet
	 * 
	 * 
	 */
	public ValueHolder execute(DefaultWebEvent event) throws RemoteException,
			NDSException {
		
		long startTime= System.currentTimeMillis();
		MessagesHolder mh = MessagesHolder.getInstance();
		Connection conn = null;
		String cmd=null;
		CmdHandler cmdHandler=null;
		Jedis jedis=null;
		String uid_token=null;
		String requestResult = "REQUEST_SUCCESS";
		
		//stao @time:2016/7/20 
		//��userObj�����������,�Ա��ڴ����쳣�л�ȡ���û��������Ϣ  ԭλ��:line:134
		UserObj userObj=null;

		try {
			JSONObject jo = (JSONObject) event.getParameterValue("jsonObject");
			
			logger.debug(jo.toString());
			
			try {
				jo = jo.getJSONArray("transactions").getJSONObject(0).getJSONObject("params");
			} catch (Exception e) {}
			
			jedis=WebUtils.getJedis();
			conn = QueryEngine.getInstance().getConnection();
			cmd = jo.getString("cmd").trim();
			cmd=findCmdByMapping(cmd);
			logger.debug("find mapping cmd:"+cmd);
			String className;
			
			//load task according to cmd param
			if(cmd.indexOf(".")>0){
				//
				className=cmd;
				cmdHandler=(CmdHandler) Class.forName(className).newInstance();
				logger.debug("new instance "+ cmdHandler.getClass().getName());
			}else{
				//default package, loop over cmdhandler packages
				String cmdHandlerName= capitalizeFirstLetter(cmd);
				try{
					cmdHandler= newInstance(cmdHandlerName);
				}catch(Exception tx){
					if(cmdHandlerName.endsWith("Search")) cmdHandler= newInstance("Search");
					else if(cmdHandlerName.endsWith("Get")) cmdHandler= newInstance("ObjectGet");
					else if(cmdHandlerName.endsWith("Modify")) cmdHandler= newInstance("ObjectModify");
					else if(cmdHandlerName.endsWith("Add")) cmdHandler= newInstance("ObjectAdd");
					else if(cmdHandlerName.endsWith("Void")) cmdHandler= newInstance("ObjectVoid");
					else throw tx;
				}
				
			}
			
			
			CmdResult result;
			
			boolean needLogin = true;
			//stao @time:2016/7/20
			//��userObj ����Ķ��������������ǰ���� line:92
			//UserObj userObj=null;
			
//			logger.debug("cmdHandler.allowGuest()="+cmdHandler.allowGuest());
			if(cmdHandler.allowGuest()){
				needLogin = false;
			}else{
				UserWebImpl userWeb =event.getContext().getUserWeb();
				boolean isSessionValid=!userWeb.isGuest();
				/**
				 *  ����Ĵ����ǲ�����;����jo�л�ȡtoken
				 */
				if(jo!=null){
					Object tk=jo.opt("token");
					if(tk!=null && tk.toString().contains(":")){
						uid_token=tk.toString();
					}
				}
				//read cookie for user information, token cookie value: "$uid:$token"
				if(uid_token==null){
					Cookie cookie=CookieKeys.getCookieObj(event.getContext().getHttpServletRequest(), "token");
					if(cookie!=null)uid_token=cookie.getValue();
					if(Validator.isNull(uid_token) ){
						if(!isSessionValid)return needLogin(event);
						else{
							//���Ǵ�portalҳ���½��ϵͳ��û��token, ��Ҫ����һ��
							uid_token=PhoneUtils.setupCookie(userWeb.getUserId(),jo, 
									event.getContext().getHttpServletRequest(), event.getContext().getHttpServletResponse(), jedis,conn);
							logger.debug("create a new uid token="+ uid_token);
						}
					}
				}
				if(!Validator.isNull(uid_token) ){
				//usr:<usr_id>:<token>
				String key="usr:"+uid_token;
				JSONObject jousr=null;
				String value=null;
				try{
					if(!jedis.exists(key)){
						logger.debug("not found key: "+ key);
						return needLogin(event);
					}
					long userId=Tools.getLong(uid_token.split(":")[0], 0);
					if(userId==0){
						logger.warn("Strange: uid_token not valid:"+ uid_token+" with uid parsed to 0");
						return needLogin(event);
					}
					VelocityContext vc = VelocityUtils.createContext();
					vc.put("uid", userId);
					JSONObject usrjo=PhoneUtils.getRedisObj("usr", userId, conn, jedis);
					if(Tools.getYesNo(usrjo.opt("en"), true)==false){
						//�Ѿ���������
						logger.debug("usr:"+uid_token+" has been disabled");
						jedis.del(key);
						return needLogin(event);
					}
					
					
					//���ݵ�ǰusr������ʶ��debug״̬������nginx redirect���ر��portal��ȥ���
					Cookie cookie=CookieKeys.getCookieObj(event.getContext().getHttpServletRequest(), "debug");
					String usrDebug=  jedis.get("debug:"+userId);
					if(Validator.isNotNull(usrDebug)){
						//will set cookie so nginx will redirect this user to a special portal for debuging
						if(cookie==null || !usrDebug.equals(cookie.getValue())){
							cookie=new Cookie("debug",usrDebug);
							CookieKeys.addCookie(event.getContext().getHttpServletRequest(), event.getContext().getHttpServletResponse(), cookie, false,  PhoneConfig.COOKIE_TIMEOUT);
						}
					}else{
						if(cookie!=null){
							cookie.setValue(null);
	                        cookie.setMaxAge(0);// ��������cookie
	                        cookie.setPath("/");
	                        event.getContext().getHttpServletResponse().addCookie(cookie);
						}
					}
					//add userObj to request "usr"
					userObj=new UserObj(usrjo); // this is from db redis cache
					userObj.setToken(uid_token);
					//lang_id ��Ҫ��session���ȡ��session��langid������ jedis.hget(key, "lang_id"), ����û����˳�����������Ȼ���ֵ�ǰ����
					int langId=Tools.getInt( jedis.hget(key, "lang_id"),  userObj.getLangId());
					if(langId!=userObj.getLangId()){
						userObj.setLangId(langId); 
						Locale newLocale=LanguageManager.getInstance().getLocale(langId);
						event.getContext().getHttpServletRequest().getSession().setAttribute(org.apache.struts.Globals.LOCALE_KEY,newLocale);
						if(userWeb!=null) userWeb.setLocale(newLocale);// @�����ķ�����Ҫ@
						logger.debug("change locale to "+ langId);
					}
					
					event.getContext().getHttpServletRequest().setAttribute("usr", userObj);
				}catch(Exception ex){
					logger.error("fail to parse cookie "+uid_token+" from req:"+ jo, ex);
					return needLogin(event);
				}
				}
				if(!isSessionValid && userObj.getId()!=UserWebImpl.GUEST_ID){
					//�����̨session��Ч����cookie�е��û�����guest������ʼ����̨
					WebUtils.getSessionContextManager(event.getContext().getHttpServletRequest().getSession(true));
					event.getContext().getHttpServletRequest().getSession().setAttribute("USER_ID", userObj.getName());
					
				}
			}
						
			logger.debug("setContext "+ userObj+", event="+ event.toDetailString());
			cmdHandler.setContext( event, conn,jedis, userObj);
			cmdHandler.prepare(jo);
			cmdHandler.setDefaultWebEventHelper(helper);
			result=cmdHandler.execute(jo);
			logger.debug("result:"+ result.getMessage());
			ValueHolder holder = new ValueHolder();
			if (result == null) {
				result = new CmdResult();
			}
			holder.put("message", MessagesHolder.getInstance().translateMessage(result.getMessage(), event.getLocale()));
			holder.put("code",  result.getCode());
			holder.put("restResult",  result.getRestResult());
			holder.put("data", result.getObject());//2016.9.8 yfzhu for webaction (object button)
			
			if(cmdHandler.isDebugResult())
				logger.debug("token="+uid_token+": "+result.toString());
			return holder;
			
		} catch (Throwable t) {
			requestResult = "REQUEST_FAIL";
			logger.debug("token="+uid_token+": "+event.toDetailString());
			logger.error("exception", t);
			String server =event.getContext().getHttpServletRequest().getLocalAddr();
			String serverName =event.getContext().getHttpServletRequest().getLocalName();
			long port=event.getContext().getHttpServletRequest().getLocalPort();
			//stao @time:2016/7/20
			//����������� params���˶����������κ���Ҫ����Ϣ�����ս��ö��󴫵ݵ������  ��ȡ�쳣��Ϣ �ĺ�����(getExceptionMessage)���Ա������������
			JSONObject params = new JSONObject();
			
			try{
				params.put( "server",server);
				params.put( "servername", serverName);
				params.put( "port", port);
				if(null != userObj){
					//stao   UserObj.java �к��ж���תjson���������÷��� toJSON() ��øö����json���󣬲�������� params��
					//JSONObject jsonObj =userObj.toJSON();
					JSONObject jsonObj =new JSONObject();
					jsonObj.put( "id",0== userObj.getId()? "null":userObj.getId());
					jsonObj.put( "name", userObj.getName());
					jsonObj.put( "truename", userObj.getTrueName());
					params.put( "userObj", jsonObj);			
				}
			}
			catch( JSONException e){
				logger.error( e.getLocalizedMessage(), e);
			}
			//stao 2016/08/03
			//����ǲ��Ի���������������
			if(PhoneConfig.IS_DEVELOP_ENV){
				throw new NDSException(PhoneUtils.getExceptionMessage(t, Locale.SIMPLIFIED_CHINESE));
			}
			else{
				throw new NDSException(PhoneUtils.getExceptionMessage(params,conn, jedis,t, Locale.SIMPLIFIED_CHINESE));				
			}
//			if (t instanceof NDSException)
//				throw (NDSException) t;
//			else
				
		} finally {
			try{if(cmdHandler!=null) cmdHandler.distroy();}catch(Throwable tx){}
			try {if (jedis != null)jedis.close();} catch (Throwable e) {}
			try {if (conn != null)conn.close();} catch (Throwable e) {}
			logger.debug(requestResult+":["+cmd+"] takes "+ ((System.currentTimeMillis()-startTime)/1000.0)+ " seconds");
		}
	}
	/**
	 * �û���������ָ���û�����ܷ���B2B��Ĭ���û��飺B2B
	 * @param event
	 * @return
	 * @throws Exception
	 */
	private ValueHolder noPermission(DefaultWebEvent event) throws Exception{
		CmdResult result=new CmdResult( Tools.getInt(SipStatus.AUTH_FAILD.getCode(),1010), "��Ȩ��",new JSONObject());
		ValueHolder holder = new ValueHolder();
		holder.put("message",  "��Ȩ��");
		holder.put("code",  result.getCode());
		holder.put("restResult",  result.getRestResult());
		
		
		Cookie cookie=CookieKeys.getCookieObj(event.getContext().getHttpServletRequest(), "token");
		if(cookie!=null){
			cookie.setValue(null);
            cookie.setMaxAge(0);// ��������cookie
            cookie.setPath("/");
            event.getContext().getHttpServletResponse().addCookie(cookie);
		}
		return holder;
	}
	
	private ValueHolder needLogin(DefaultWebEvent event) throws Exception{
		CmdResult result=new CmdResult( Tools.getInt(SipStatus.AUTH_FAILD.getCode(),1009), "��Ҫ���µ�¼",new JSONObject());
		ValueHolder holder = new ValueHolder();
		holder.put("message",  "��Ҫ���µ�¼");
		holder.put("code",  result.getCode());
		holder.put("restResult",  result.getRestResult());
		
		
		Cookie cookie=CookieKeys.getCookieObj(event.getContext().getHttpServletRequest(), "token");
		if(cookie!=null){
			cookie.setValue(null);
            cookie.setMaxAge(0);// ��������cookie
            cookie.setPath("/");
            event.getContext().getHttpServletResponse().addCookie(cookie);
		}
		return holder;
	}
	
	@Override
	public void pluginDestroyed() {
		try{
			
//			PhoneController.getInstance().shutdown();
			//�������� �����߳�
			/*ExceptionListener  listener = ExceptionListener.getInstance();
			listener.stopProcess();*/
			
			logger.debug("portalPluginDestroyed");
			
		}catch(Throwable tx){
			logger.error("Fail to destroy fair controller", tx);
		}
		
	}
	/**
	 * �˷������õ�ʱ�򣬻����ܺ��� FairController.getInstance(), ��Ϊ��FairController.getInstance() ��Ҫschemaȫ��ok��Ȼ��FairConfig����ʼ��FindForm��������Ҫ
	 * ���ݶ����Դ���column.descripiton�����л�ʹ�õ�MessagesHolder, ��MessagesHolder�ĳ�ʼ������MainServlet�����StartupEngine!
	 * PluginLoaded��������StartupEngine���������
	 * 
	 */
	public void pluginLoaded() {
		//�ж�phone.id_worker����������������
		try{
			/*
			 *@stao
			 *������Ϣ���н����̣߳����ڽ��ձ�����Ϣ
			 *����ʹ���̲߳���������Ϣ�Ĵ������
			 */
			ExceptionListener listener = ExceptionListener.getInstance();
			if(!listener.isRunning()){
				Thread thread = new Thread( listener);
				thread.start();
				logger.debug("ExceptionListener started");
			}else{
				logger.debug("ExceptionListener has already been started");
			}
			
//			if(Validator.isNotNull(PhoneConfig.MQ_BROKER_URL) && PhoneConfig.MQ_CLIENT_ID>=0){
//				PhoneController.getInstance().getMQClient();
//			}
			TableManager.getInstance();
			
			logger.debug("B2BPluginLoaded");
			//��ʱ���� ���Զ�ʶ��ͻ��˰汾
			//register some actions to monitor, after plugin finished setup

		}catch(Throwable tx){
			logger.error("Fail to init phone plugin", tx);
		}
	}	
	/**
	 * ���ַ������1һ������������ĸ��д����
	 * 	��b2b.check�� -> "b2b.Check",
	 *  "login" -> "Login"
	 *   
	 * @param data
	 * @return
	 */
	private  String capitalizeFirstLetter ( String data ){
		int lastIdx=data.lastIndexOf('.');
		if(lastIdx>0 ){
			if( lastIdx<data.length() -1){
				//��������һ���ַ�������ȷ��data
				throw new NDSRuntimeException("�����ʽ����ȷ:"+ data);
			}
			//dot�������һ����ĸ
			return data.substring(0,lastIdx+1)+ data.substring(lastIdx+1, lastIdx+2).toUpperCase()+data.substring(lastIdx+2);
		}
		//û��ţ�����һ���ַ���д
		String firstLetter = data.substring(0,1).toUpperCase();
		String restLetters = data.substring(1);
		return firstLetter + restLetters;
	}

	

}