package com.agilecontrol.b2bweb.cmd;

import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONObject;

import com.agilecontrol.b2b.cmd.ObjectGet;
import com.agilecontrol.b2b.cmd.Search;
import com.agilecontrol.b2b.schema.Table;
import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.nea.util.Tools;
import com.agilecontrol.phone.Admin;
import com.agilecontrol.phone.CmdHandler;
import com.agilecontrol.phone.CmdResult;
import com.agilecontrol.phone.PhoneConfig;
import com.agilecontrol.phone.PhoneController;
import com.agilecontrol.phone.PhoneUtils;

/**
h1. 销补表移除单个

h2. 输入


> *id* - 商品id


h2. 输出

{code,message}



 * @author supergai
 *
 */
@Admin(mail="xuwj@cottonshop.com")
public class IntelligentpdsDeletet extends CmdHandler {	
	public CmdResult execute(JSONObject jo) throws Exception {		
		int storeid = jo.getInt("storeid");
		int pdtid = jo.getInt("pdtid");
        engine.executeUpdate("delete from c_sale_replenishment where c_store_id=? and m_product_id=?", new Object[]{storeid,pdtid}, conn);	
		return CmdResult.SUCCESS;
		
	}
	
}
