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
h1. user_address �û���ַ�޸�

> {cmd:"b2b.addr.add",obj}

 ������Ҫע����ǣ��޸ķ����ǣ���ԭ��������Ϊisactive='N', Ȼ����һ���¼�¼�������¼�¼���ͻ���
 
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













