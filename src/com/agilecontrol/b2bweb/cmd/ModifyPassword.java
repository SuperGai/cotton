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

����
{cmd:"b2b.user.modifypassword", oldpassword,newpassword}

oldpassword - string ������
newpassword - string ������

�ͻ���Ӧ���ж�2��������һ��

Ŀǰ��������û�д��������У����ȵĿ���

Edit
���
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
		
	  	//���ﲻ����session/event�е�user,��Ϊtomcat session ���ܲ��ڵ�½״̬��cookie��½��
		User user=SecurityUtils.getUser((int)usr.getId()); 
	  	if(!user.isValidPassword(oldpasswd)){
	  		throw new NDSException("ԭʼ���벻��ȷ");
	  	}
			
		//���뱣�淽����md5(10λ�����+password), 10λ�����������users.pwdrand��
		String rand=com.agilecontrol.nea.core.util.PwdGenerator.getPassword(10);
		String pwdHash=PwdEncryptor.encrypt(newpassword, rand);
		
		//���Ҫ�����뱣�棬������password�ֶ�����
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
