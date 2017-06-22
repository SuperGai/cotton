package com.agilecontrol.phone.impl.buding;

import org.json.JSONObject;

import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.phone.CmdHandler;
import com.agilecontrol.phone.CmdResult;
import com.agilecontrol.phone.PhoneController;
/**
 * 
 * 申请人收到Http MiscCmd反馈后，标记状态为“已选择店铺”，且storeid 为回传值，进入等待页面，下有刷新按钮（或app重新打开时），发起命令:
Http MiscCmd:

{
    cmd: "BGetRole", 
    userid: long,
    time: long,
    sign:String,
    obj:{storeid: long}
}

服务器读取当前用在store表里的角色，返回:
Resposne: {
code: int, message: String, 
obj: {
role: string none|admin|mgr| pur|emp 无权限|管理员|店长|采购|店员
}
}

 * @author yfzhu
 *
 */
public class BGetRole extends BudingHandler {

	@Override
	public CmdResult execute(JSONObject jo) throws Exception {
		long userId= jo.getLong("userid");
		
		long storeId=jo.getJSONObject("obj").getLong("storeid");
		
		JSONObject role=engine.doQueryObject("select priv role from d_store_emp where user_id=? and d_store_id=? ", new Object[]{userId,storeId} , conn);
		if(role==null) throw new NDSException("该店铺没有您的记录，请重新申请加入");
		return new CmdResult(role);
	}
	
	
}