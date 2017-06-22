package com.agilecontrol.b2bweb.cmd;

import org.json.JSONArray;
import org.json.JSONObject;

import com.agilecontrol.b2b.cmd.ObjectGet;
import com.agilecontrol.b2b.cmd.Search;
import com.agilecontrol.b2b.schema.Table;
import com.agilecontrol.nea.core.control.web.WebUtils;
import com.agilecontrol.nea.core.query.QueryEngine;
import com.agilecontrol.nea.core.schema.TableManager;
import com.agilecontrol.nea.core.security.SecurityUtils;
import com.agilecontrol.nea.core.security.User;
import com.agilecontrol.nea.core.security.UserManager;
import com.agilecontrol.nea.core.security.pwd.PwdEncryptor;
import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.phone.Admin;
import com.agilecontrol.phone.CmdHandler;
import com.agilecontrol.phone.CmdResult;
import com.agilecontrol.phone.PhoneController;
import com.agilecontrol.phone.PhoneUtils;

/**

输入
{cmd:"b2b.user.modifypassword", oldpassword,newpassword}

oldpassword - string 旧密码
newpassword - string 新密码

客户端应该判断2次新密码一致

目前服务器端没有错误次数，校验码等的控制

Edit
输出
{
    code,message
}

 * @author yfzhu
 *
 */
@Admin(mail="yfzhu@lifecycle.cn")
public class ModifyPassword extends CmdHandler {
	/**
	 * @param jo - {oldpassword, newpassword}
	 */
	public CmdResult execute(JSONObject jo) throws Exception {
		String oldpasswd=this.getString(jo, "oldpassword");
		String newpassword=this.getString(jo, "newpassword");
		
	  	//这里不能用session/event中的user,因为tomcat session 可能不在登陆状态（cookie登陆）
		User user=SecurityUtils.getUser((int)usr.getId()); 
	  	if(!user.isValidPassword(oldpasswd)){
	  		throw new NDSException("原始密码不正确");
	  	}
			
		//密码保存方法：md5(10位随机数+password), 10位随机数保存在users.pwdrand里
		String rand=com.agilecontrol.nea.core.util.PwdGenerator.getPassword(10);
		String pwdHash=PwdEncryptor.encrypt(newpassword, rand);
		
		//如果要求明码保存，就设置password字段内容
		String planPasswd="true".equals(WebUtils.getProperty("passwords.plain", "true"))? newpassword:"";
		QueryEngine.getInstance().executeUpdate("update users set passwordhash=?,pwdrand=?,password=?,FailedLoginAttempts=0 where id=?", 
				new Object[]{pwdHash,rand,planPasswd,usr.getId()}, conn);
		//remove login lock
		String userName=(String) QueryEngine.getInstance().doQueryOne("select name from users where id=?", new Object[]{usr.getId()}, conn);
		UserManager.getInstance().removeUser(userName);
	  	
		CmdResult res=CmdResult.SUCCESS;
		return res;
		
	}

}
