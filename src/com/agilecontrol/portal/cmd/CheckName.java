package com.agilecontrol.portal.cmd;

import org.json.JSONArray;
import org.json.JSONObject;

import com.agilecontrol.nea.util.Validator;
import com.agilecontrol.phone.Admin;
import com.agilecontrol.phone.CmdHandler;
import com.agilecontrol.phone.CmdResult;

/**
输入

request 参数  name:用户名（ email形式）

输出

{
	exist:true/false
}

用户名已存在时，exist 为  true，否则为 false

 * 
 * @author stao
 *
 */
@Admin(mail="sun.tao@lifecycle.cn")
public class CheckName extends CmdHandler {
	@Override
	
	public CmdResult execute(JSONObject jo) throws Exception{
		String name = jo.optString( "name");
		JSONArray nameArray = null;
		JSONObject obj = new JSONObject();
		obj.put( "exist", false);
		if( Validator.isNotNull( name)) {
			nameArray = engine.doQueryObjectArray( "select name from users where name=?", new Object[]{ name }, conn);
			if( null != nameArray && 0 != nameArray.length()) {
				obj.put( "exist", true);
			}
		}
		
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
