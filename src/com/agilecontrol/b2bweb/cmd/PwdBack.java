package com.agilecontrol.b2bweb.cmd;

import java.sql.Connection;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.agilecontrol.b2b.cmd.ObjectGet;
import com.agilecontrol.b2b.cmd.Search;
import com.agilecontrol.b2b.schema.Table;
import com.agilecontrol.nea.core.control.ejb.command.PasswordBack;
import com.agilecontrol.nea.core.control.event.DefaultEventContext;
import com.agilecontrol.nea.core.control.event.DefaultWebEvent;
import com.agilecontrol.nea.core.control.event.EventContext;
import com.agilecontrol.nea.core.control.util.ValueHolder;
import com.agilecontrol.nea.core.control.web.WebUtils;
import com.agilecontrol.nea.core.query.QueryEngine;
import com.agilecontrol.nea.core.schema.TableManager;
import com.agilecontrol.nea.core.security.SecurityUtils;
import com.agilecontrol.nea.core.security.User;
import com.agilecontrol.nea.core.security.UserManager;
import com.agilecontrol.nea.core.security.pwd.PwdEncryptor;
import com.agilecontrol.nea.core.security.pwd.PwdStrength;
import com.agilecontrol.nea.core.util.ConfigValues;
import com.agilecontrol.nea.core.util.PwdGenerator;
import com.agilecontrol.nea.util.MD5Sum;
import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.nea.util.Tools;
import com.agilecontrol.nea.util.Validator;
import com.agilecontrol.phone.Admin;
import com.agilecontrol.phone.CmdHandler;
import com.agilecontrol.phone.CmdResult;
import com.agilecontrol.phone.PhoneController;
import com.agilecontrol.phone.PhoneUtils;

/**

输入
{cmd:"b2b.password.back", email}

email - string 注册时登记的邮箱

通过email找到用户信息

Edit
输出
{
    code,message
}

 * @author wu.qiong
 *
 */
public class PwdBack extends CmdHandler {
	private static Logger logger=LoggerFactory.getLogger(PwdBack.class);
	private EventContext rootUserContext;
	
	/**
	 * Guest can execute this task, default to false
	 * 
	 * @return
	 */
	public boolean allowGuest() {
		return true;
	}
	
	/**
	 * 系统管理员的环境
	 * @return
	 */
	private static EventContext createRootUserContext(){
		EventContext rootUserContext=null;
		try{
			int rootUserId=Tools.getInt(QueryEngine.getInstance().doQueryOne("select id from users where name='root'"),893);
			User user=SecurityUtils.getUser(rootUserId);
			rootUserContext=new DefaultEventContext(user);
		}catch(Throwable t){
			logger.error("Fail to load root id", t);
			
		}
		return rootUserContext;
	}
	public PwdBack(){
		rootUserContext =createRootUserContext();
	}
	
	 /**
	 
	 * 
     *  0 密码重置邮件（唯一标识符）已发送到邮箱 (mailsent) , 输入参数为 email
     *  1.判断邮箱是否存在 
     *  1 显示邮箱输入页面  fail 无任何输入参数
     *  2 提示邮箱未找到 fail 输入参数为 email 
     *  3 发生异常，稍后再试 fail
     *  4 重置链接已经过期，请重新申请 fail 输入参数为 ck + uid
     *  0 确认重置链接有效，提示密码设置页面(唯一标识符) (enterpwd) 输入参数为 ck+uid
     *  0 新密码保存成功 (success) 输入参数为ck 和 pwd/pwdnew
     *  5 密码不一致或太简单  fail 输入参数为ck 和 pwd/pwdnew enterpwd
     *  6 邮件已经在15分钟内发出，请在邮箱里查找，或稍后再试
     *
	 * 
	 */
	public CmdResult execute(JSONObject jo) throws Exception {
		
		String email = jo.optString("email", "");
		String ck = jo.optString("ck","");
		int uid = jo.optInt("uid", -1);
		
		JSONObject res = new JSONObject();
		if(email != ""){
			res = checkEmail(jo);
		}else if(ck != "" && uid != -1){
			res = checkCode(jo);
		}else{
			throw new NDSException("不支持该模式!");
		}
		
		return new CmdResult(res);
		
	}
	
	/**
	 * 
	 * @Title:PwdBack 认证邮箱
	 * @Description:TODO
	 * @param jo - {email}
	 * @return {code,message}
	 * code -    int      0 - success    	|   1 -fail			|	2 fail						|	3 fail
	 * message - string   "邮箱认证成功"  	|   "该邮箱未注册!"	|	"邮件已经发出,请到邮箱中查看!"	|	"业务回滚的错误"
	 * @throws Exception JSONObject
	 * @throws
	 */
	public JSONObject checkEmail(JSONObject jo) throws Exception{
		JSONObject res = new JSONObject();
		try{
			String email = jo.optString("email");
			List al=QueryEngine.getInstance().doQueryList("select id, name, truename from users where email=?",new Object[]{email},conn);
			
			if(al.size()==0){
				res.put("code", 1);
				res.put("message", "该邮箱未注册!");
				return res;
			}
			// just one:
			al=(List) al.get(0);
			int userId=Tools.getInt( al.get(0),-1);
			String userName= (String)al.get(1);
			String trueName= (String)al.get(2);
			// 如果邮件在15分钟内已经发出过，禁止再次发送
			int cnt=Tools.getInt(QueryEngine.getInstance().doQueryOne("select count(*) from u_pwdback where user_id=? and creationdate>sysdate - 1/96",new Object[]{userId},conn),0);
			if(cnt>0){
				res.put("code", 2);//too frequent
				res.put("message", "邮件已经发出,请到邮箱中查看!");
				return res;
			}
			sendMail(userId, userName,conn);
			res.put("code", 0);
			res.put("message", "密码重置邮件已发送到邮箱");
		}catch(Throwable t){
    		logger.debug(event.toDetailString());
    		logger.error("Fail",t);
    		//会导致这里的业务全部回滚
    		res.put("code", 3);
    		res.put("message", t.getMessage());
    	}finally{
    		helper.closeConnection(conn, event);
    	}
		return res;
	}
	
	/**
	 * @Title:PwdBack 验证check code
	 * @Description:TODO
	 * @param jo {ck,uid,loginpass,verifypass}
	 * @return {code,message}
	 * code -    int      0 - success    	|   4 -fail				|	5 fail						
	 * message - string   "认证成功"  		|   "重置链接已经过期"	|	"密码太简单"	
	 * @throws Exception JSONObject
	 * @throws
	 */
	public JSONObject checkCode(JSONObject jo) throws Exception{
		JSONObject res = new JSONObject();
		String ck = jo.optString("ck");
		int uid = jo.optInt("uid");
		try{
			// 校验ck 的有效性
			String name=findUser(ck,uid,conn);
			if(name!=null){
				// 设置了密码吗
				String password= jo.optString("loginpass");
				String password2= jo.optString("verifypass");
				if(Validator.isNull(password)){
					res.put("code", 0);
    	    		res.put("message", "请输入新密码");
    	    		res.put("username",name);
				}else{
					if(PwdStrength.check(password)>1){
						saveNewPassword(uid,name, password,ck, conn,event);
						res.put("code", 0);
        	    		res.put("message", "密码保存成功");
        	    		res.put("username",name);
					}else{
						res.put("code", 5);
        	    		res.put("message", "密码太简单");
        	    		res.put("username",name);
					}
				}
			}else{
				res.put("code", 4);
	    		res.put("message", "重置链接已经过期");
			}
		}catch(Throwable t){
    		logger.debug(event.toDetailString());
    		logger.error("Fail",t);
    		//会导致这里的业务全部回滚
    		res.put("code", 3);
    		res.put("message", t.getMessage());
    	}finally{
    		helper.closeConnection(conn, event);
    	}
		return res;
	}
	/**
     * 发送重置邮件给指定用户
     * @param userId
     * @param name users.name
     * @throws Exception
     */
	private void sendMail(int userId, String name, Connection conn) throws Exception{
		DefaultWebEvent evt=new DefaultWebEvent("CommandEvent",rootUserContext);
		evt.put(helper.PARAM_SQL_CONNECTION, conn);
		evt.setEventName("CommandEvent");
		
		// 创建记录即会通过监控器发出邮件
		evt.setParameter("command", "ObjectCreate");
		evt.setParameter("table", "u_pwdback");
		evt.setParameter("nds.control.ejb.UserTransaction", "N");
		//evt.setParameter("directory", TableManager.getInstance().getTable("USERS").getSecurityDirectory());
		evt.setParameter("user_id__name", name);
		evt.setParameter("chkcode", createCheckCode());
		evt.setParameter("isused", "N");
		
		ValueHolder holder=helper.handleEvent(evt);
		logger.debug("send mail:"+holder.get("message"));
		/*if( Tools.getInt(holder.get("code"),-1)!=0){
			throw new NDSException( (String)holder.get("message"));
		}*/
	}
	 /**
     * 随机字符串的MD5码
     * @return
     */
    private String createCheckCode(){
    	String r=PwdGenerator.getPassword(32);
    	try{
    		r= MD5Sum.toCheckSumStr(r );
    	}catch(Throwable t){
    		logger.error("Fail to do md5", r);
    	}
		return r;
    }
    /**
	 * 检查是否为有效的重置标识
	 * @param ck
	 * @param uid users.id
	 * @return null if not found valid one
	 * @throws Exception
	 */
	private String findUser(String ck, int uid, Connection conn) throws Exception{
		return (String)QueryEngine.getInstance().doQueryOne(
				"select u.name from u_pwdback p, users u where p.user_id=? and p.chkcode=? and p.isused='N' and p.creationdate>sysdate- 1/48 and u.id=p.user_id",
				new Object[]{uid, ck}, conn);
	}
	/**
	 * 更新指定用户的密码
	 * @param ck 可寻找到用户
	 * @param passwd
	 * @throws Exception
	 */
	private void saveNewPassword(int userId,String userName, String passwd, String chkcode, Connection conn,DefaultWebEvent event) throws Exception{
		
		DefaultWebEvent evt=new DefaultWebEvent("CommandEvent",rootUserContext);
		evt.put(helper.PARAM_SQL_CONNECTION, conn);
		evt.setEventName("CommandEvent");
		
		evt.setParameter("command", "ChangePassword");
		evt.setParameter("userid", String.valueOf(userId));
		evt.setParameter("password1", passwd);
		evt.setParameter("password2", passwd);
		
		ValueHolder holder=helper.handleEvent(evt);
		logger.debug("change password:"+holder.get("message"));

		QueryEngine.getInstance().executeUpdate("update u_pwdback set isused='Y' where user_id=? and chkcode=?", 
				new Object[]{userId, chkcode}, conn);
		
		User user= SecurityUtils.getUser(userId);
		// 保存到COOKIE里
		SecurityUtils.addCookies(event.getContext().getHttpServletRequest(), 
				event.getContext().getHttpServletResponse(), userName, passwd,
				ConfigValues.SECURITY_REMEMBER_ME);
	}

}
