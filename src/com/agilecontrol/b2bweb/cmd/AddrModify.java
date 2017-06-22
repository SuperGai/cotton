package com.agilecontrol.b2bweb.cmd;

import org.json.JSONArray;
import org.json.JSONObject;

import com.agilecontrol.b2b.cmd.ObjectAdd;
import com.agilecontrol.b2b.cmd.Search;
import com.agilecontrol.b2b.schema.Table;
import com.agilecontrol.b2bweb.WebController;
import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.nea.util.Tools;
import com.agilecontrol.nea.util.Validator;
import com.agilecontrol.phone.Admin;
import com.agilecontrol.phone.CmdHandler;
import com.agilecontrol.phone.CmdResult;
import com.agilecontrol.phone.PhoneController;
import com.agilecontrol.phone.PhoneUtils;

/**
h1. user_address 用户地址修改

> {cmd:"b2b.addr.add",obj}

 这里需要注意的是，修改方法是：将原对象设置为isactive='N', 然后复制一条新记录，返回新记录给客户端
 
 * @author yfzhu
 *
 */
@Admin(mail="yfzhu@lifecycle.cn")
public class AddrModify extends ObjectAdd {
	
	public CmdResult execute(JSONObject jo) throws Exception {
		JSONObject obj= getObject(jo,"obj");
		obj.put("user_id", usr.getId());
		
		int id= getInt(obj, "id");
		engine.executeUpdate("update user_address set isactive='N' where id=?", new Object[]{id}, conn);
		jedis.del("addr:"+id);
		
		obj.put("table", "addr");
		obj.remove("id");
		
		obj.put("modifierid", usr.getId());
		obj.put("isactive","Y");
		return super.execute(jo);
	}
}













