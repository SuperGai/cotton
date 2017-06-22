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
h1. 重新下单

h2. 输入

> {cmd:"b2b.order.reOrder",  id,is_clearcart}

*id* - int  b_bfo.id
*is_clearcart - true | false 是否清除购物车


 * 
 * @author wu.qiong
 *
 */
public class ReOrder extends CmdHandler {
	
	public CmdResult execute(JSONObject jo) throws Exception {
		int orderId= jo.optInt("id",-1);
		String is_clearcart = jo.optString("is_clearcart","N");
		
		if(orderId == -1){
			throw new NDSException("没有传入订单id!");
		}
		ArrayList al = new ArrayList();
		al.add(orderId);
		al.add(usr.getId());
		al.add(is_clearcart);
	    ArrayList res = engine.executeStoredProcedure("b_bfo_reorder", al, conn);
		
		return new CmdResult(res);
	}

}
