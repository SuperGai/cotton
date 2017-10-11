package com.agilecontrol.b2bweb.cmd;

import java.util.ArrayList;
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
h1. �Ƴ�������������Ʒ

h2. ����

> {cmd:"b2b.cart.delqtynotcan"}


> *userid* - 

h2. ���

{code,message}



 * @author yfzhu
 *
 */
@Admin(mail="yfzhu@lifecycle.cn")
public class CartDelQtynotcan extends CmdHandler {
	
	public CmdResult execute(JSONObject jo) throws Exception {
		ArrayList params=new ArrayList();
		params.add(usr.getId());   
		ArrayList result=engine.executeStoredProcedure("b_cart_qtycan",params, conn);
		JSONObject ret=new JSONObject();
	    ret.put("success", "ɾ���ɹ�");
		return new CmdResult(ret);		
	}	
}
