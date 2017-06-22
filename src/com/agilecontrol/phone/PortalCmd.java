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
 * 订货会相关命令，与FairCmd不同的是：客户端不是买手，不需要FairContext
 * 
 * 关于web来的cmd命令，转换为class的定位读取方式
 *  首先读取配置文件 /META-INF/conf/portalcmdmap.properties，格式 webcmd=javaclass
 *  如:
 *  portal.getmenus=com.agilecontrol.portal.cmd.GetMenus
 *  如果没有定位到，将默认传人的cmd就是class名称直接定位，如果class名称没有"."，将默认在 ad_param#phone.cmd.packages 指定的参数中寻找相应class
 * 
 */
@Admin(mail="sun.tao@lifecycle.cn")
public class PortalCmd extends Command implements PluginLifecycleListener{
	/**
	 * 为提高cmdhanlder class 检索性能，设置此缓存：
	 * key: cmdHandler name，就是少而来Package 的名称, class 对应task的名称
	 */
	private Hashtable<String, Class> cmdHandlerClasses= new Hashtable();
	/**
	 *  /META-INF/conf/portalcmdmap.properties
	 */
	private Properties cmdMapping;
	
	/**
	 * 根据/META-INF/conf/portalcmdmap.properties进行cmd映射，如果没有定义映射，返回原值
	 * @param cmdFromWeb portalcmdmap.properties的key的部分
	 * @return 如果映射未定义，将返回原值
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
				throw new NDSException("未定义cmd:"+cmdHandlerName );
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
		//将userObj对象定义才这里,以便在处理异常中获取该用户的相关信息  原位置:line:134
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
			//将userObj 对象的定义调到本函数的前部分 line:92
			//UserObj userObj=null;
			
//			logger.debug("cmdHandler.allowGuest()="+cmdHandler.allowGuest());
			if(cmdHandler.allowGuest()){
				needLogin = false;
			}else{
				UserWebImpl userWeb =event.getContext().getUserWeb();
				boolean isSessionValid=!userWeb.isGuest();
				/**
				 *  下面的代码是测试用途，从jo中获取token
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
							//这是从portal页面登陆了系统，没有token, 需要构造一个
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
						//已经被禁用了
						logger.debug("usr:"+uid_token+" has been disabled");
						jedis.del(key);
						return needLogin(event);
					}
					
					
					//根据当前usr的配置识别debug状态，将有nginx redirect到特别的portal上去监控
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
	                        cookie.setMaxAge(0);// 立即销毁cookie
	                        cookie.setPath("/");
	                        event.getContext().getHttpServletResponse().addCookie(cookie);
						}
					}
					//add userObj to request "usr"
					userObj=new UserObj(usrjo); // this is from db redis cache
					userObj.setToken(uid_token);
					//lang_id 需要从session里获取，session的langid设置在 jedis.hget(key, "lang_id"), 如果用户不退出，界面上仍然保持当前语言
					int langId=Tools.getInt( jedis.hget(key, "lang_id"),  userObj.getLangId());
					if(langId!=userObj.getLangId()){
						userObj.setLangId(langId); 
						Locale newLocale=LanguageManager.getInstance().getLocale(langId);
						event.getContext().getHttpServletRequest().getSession().setAttribute(org.apache.struts.Globals.LOCALE_KEY,newLocale);
						if(userWeb!=null) userWeb.setLocale(newLocale);// @这样的翻译需要@
						logger.debug("change locale to "+ langId);
					}
					
					event.getContext().getHttpServletRequest().setAttribute("usr", userObj);
				}catch(Exception ex){
					logger.error("fail to parse cookie "+uid_token+" from req:"+ jo, ex);
					return needLogin(event);
				}
				}
				if(!isSessionValid && userObj.getId()!=UserWebImpl.GUEST_ID){
					//如果后台session无效，而cookie中的用户不是guest，将初始化后台
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
			//定义参数对象 params，此对象可以添加任何需要的信息，最终将该对象传递到定义的  获取异常信息 的函数中(getExceptionMessage)，以便进行其他操作
			JSONObject params = new JSONObject();
			
			try{
				params.put( "server",server);
				params.put( "servername", serverName);
				params.put( "port", port);
				if(null != userObj){
					//stao   UserObj.java 中含有对象转json方法，调用方法 toJSON() 获得该对象的json对象，并放入参数 params中
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
			//如果是测试环境，将不做处理
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
	 * 用户必须属于指定用户组才能访问B2B，默认用户组：B2B
	 * @param event
	 * @return
	 * @throws Exception
	 */
	private ValueHolder noPermission(DefaultWebEvent event) throws Exception{
		CmdResult result=new CmdResult( Tools.getInt(SipStatus.AUTH_FAILD.getCode(),1010), "无权限",new JSONObject());
		ValueHolder holder = new ValueHolder();
		holder.put("message",  "无权限");
		holder.put("code",  result.getCode());
		holder.put("restResult",  result.getRestResult());
		
		
		Cookie cookie=CookieKeys.getCookieObj(event.getContext().getHttpServletRequest(), "token");
		if(cookie!=null){
			cookie.setValue(null);
            cookie.setMaxAge(0);// 立即销毁cookie
            cookie.setPath("/");
            event.getContext().getHttpServletResponse().addCookie(cookie);
		}
		return holder;
	}
	
	private ValueHolder needLogin(DefaultWebEvent event) throws Exception{
		CmdResult result=new CmdResult( Tools.getInt(SipStatus.AUTH_FAILD.getCode(),1009), "需要重新登录",new JSONObject());
		ValueHolder holder = new ValueHolder();
		holder.put("message",  "需要重新登录");
		holder.put("code",  result.getCode());
		holder.put("restResult",  result.getRestResult());
		
		
		Cookie cookie=CookieKeys.getCookieObj(event.getContext().getHttpServletRequest(), "token");
		if(cookie!=null){
			cookie.setValue(null);
            cookie.setMaxAge(0);// 立即销毁cookie
            cookie.setPath("/");
            event.getContext().getHttpServletResponse().addCookie(cookie);
		}
		return holder;
	}
	
	@Override
	public void pluginDestroyed() {
		try{
			
//			PhoneController.getInstance().shutdown();
			//结束队列 监听线程
			/*ExceptionListener  listener = ExceptionListener.getInstance();
			listener.stopProcess();*/
			
			logger.debug("portalPluginDestroyed");
			
		}catch(Throwable tx){
			logger.error("Fail to destroy fair controller", tx);
		}
		
	}
	/**
	 * 此方法调用的时候，还不能呼叫 FairController.getInstance(), 因为，FairController.getInstance() 需要schema全部ok，然后FairConfig里会初始化FindForm，里面需要
	 * 根据多语言创建column.descripiton，其中会使用到MessagesHolder, 而MessagesHolder的初始化是在MainServlet里，晚于StartupEngine!
	 * PluginLoaded调用是在StartupEngine里就启动了
	 * 
	 */
	public void pluginLoaded() {
		//判定phone.id_worker不能配置在数据中
		try{
			/*
			 *@stao
			 *启动消息队列接收线程，用于接收报错消息
			 *这里使用线程操作队列消息的处理过程
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
			//定时任务 ，自动识别客户端版本
			//register some actions to monitor, after plugin finished setup

		}catch(Throwable tx){
			logger.error("Fail to init phone plugin", tx);
		}
	}	
	/**
	 * 将字符串最后1一个点后面的首字母大写，如
	 * 	“b2b.check” -> "b2b.Check",
	 *  "login" -> "Login"
	 *   
	 * @param data
	 * @return
	 */
	private  String capitalizeFirstLetter ( String data ){
		int lastIdx=data.lastIndexOf('.');
		if(lastIdx>0 ){
			if( lastIdx<data.length() -1){
				//点号是最后一个字符，不正确的data
				throw new NDSRuntimeException("命令格式不正确:"+ data);
			}
			//dot不是最后一个字母
			return data.substring(0,lastIdx+1)+ data.substring(lastIdx+1, lastIdx+2).toUpperCase()+data.substring(lastIdx+2);
		}
		//没点号，将第一个字符大写
		String firstLetter = data.substring(0,1).toUpperCase();
		String restLetters = data.substring(1);
		return firstLetter + restLetters;
	}

	

}