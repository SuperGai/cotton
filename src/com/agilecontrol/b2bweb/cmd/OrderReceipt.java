package com.agilecontrol.b2bweb.cmd;

import java.util.ArrayList;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.json.JSONArray;
import org.json.JSONObject;

import com.agilecontrol.b2bweb.OrderSender;
import com.agilecontrol.nea.core.velocity.VelocityUtils;
import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.nea.util.Tools;
import com.agilecontrol.nea.util.Validator;
import com.agilecontrol.phone.CmdHandler;
import com.agilecontrol.phone.CmdResult;
import com.agilecontrol.phone.PhoneConfig;
import com.agilecontrol.phone.PhoneController;

/**
h1. 确认收货

h2. 输入

> {cmd:"b2b.order.receipt",  id}

*id* - int  b_bfo.id


 * 
 * @author wu.qiong
 *
 */
public class OrderReceipt extends CmdHandler {
	
	public CmdResult execute(JSONObject jo) throws Exception {
		int orderId= this.getInt(jo, "id");
		jedis.del("bfo:"+orderId);
		vc.put("bfoid", orderId);
		vc.put("userid", usr.getId());
		vc.put("type", "cfmr");
		
		PhoneController.getInstance().executeProcedure("b_bfo_sub", vc, conn);
		
		return new CmdResult();
	}

}
