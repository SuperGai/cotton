package com.agilecontrol.portal.cmd;

import org.json.JSONArray;
import org.json.JSONObject;

import com.agilecontrol.nea.util.Validator;
import com.agilecontrol.phone.Admin;
import com.agilecontrol.phone.CmdHandler;
import com.agilecontrol.phone.CmdResult;
import com.agilecontrol.phone.PhoneController;

/**
输入

request 
参数 :
mail:帐户名
truename:真实姓名
password：密码
job：申请岗位
telphone：手机号
remark：备注

输出

{
	success:true/false 成功时为 true 否则为 false
	id:用户id (success 为true 时，存在此属性，其他情况不存在)
}

当 success 为  true 时,会返回 id

 * 
 * @author stao
 *
 */
@Admin(mail="sun.tao@lifecycle.cn")
public class NormalRegister extends CmdHandler {
	@Override
	public CmdResult execute(JSONObject jo) throws Exception{
		JSONObject obj = new JSONObject().put( "success", false);
		
		String mail = jo.optString( "mail");
		String truename = jo.optString( "truename");
		String password = jo.optString( "password");
		String job = jo.optString( "job");
		String telphone = jo.optString( "telphone");
		telphone = telphone+ "(str)";
		String remark = jo.optString( "remark");
		
		try{
			String adSql = "users_insert";
			vc.put( "EMAIL", mail);
			vc.put( "TRUENAME", truename);
			vc.put( "PASSWORD", password);
			vc.put( "DESCRIPTION", job);
			vc.put( "PHONE2", telphone);
			vc.put( "COMMENTS", remark);
			String id = PhoneController.getInstance().executeFunction( adSql, vc, conn);
			obj.put( "success", true);
			obj.put( "id", id);
		}
		finally{
		};
		
		return new CmdResult( obj);
		
	}
	
	/**
	 * Guest can execute this task, default to false
	 * @return
	 */
	public boolean allowGuest(){
		return true;
	}
	
}
