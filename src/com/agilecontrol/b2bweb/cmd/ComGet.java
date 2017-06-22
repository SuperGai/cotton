package com.agilecontrol.b2bweb.cmd;

import org.json.JSONArray;
import org.json.JSONObject;

import com.agilecontrol.b2b.cmd.ObjectGet;
import com.agilecontrol.b2b.cmd.Search;
import com.agilecontrol.b2b.schema.Table;
import com.agilecontrol.b2bweb.WebController;
import com.agilecontrol.nea.core.schema.ClientManager;
import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.nea.util.Tools;
import com.agilecontrol.nea.util.Validator;
import com.agilecontrol.phone.Admin;
import com.agilecontrol.phone.CmdHandler;
import com.agilecontrol.phone.CmdResult;
import com.agilecontrol.phone.PhoneController;
import com.agilecontrol.phone.PhoneUtils;

/**

> {cmd:"b2b.com.get"}

{
    name,description,id,logo    
}

都是ad_client表的字段
 
 * @author yfzhu
 *
 */
@Admin(mail="yfzhu@lifecycle.cn")
public class ComGet extends ObjectGet {
	
	/**
	 * 
	 */
	public CmdResult execute(JSONObject jo) throws Exception {
		
		JSONObject obj=new JSONObject();
		com.agilecontrol.nea.core.schema.Client client=ClientManager.getInstance().getDefaultClient();
		obj.put("id", client.getId());
		obj.put("name", client.getName());
		obj.put("description", client.getDescription());
		obj.put("logo", client.getLogoURL());
		
		return new CmdResult(obj);
	}
}













