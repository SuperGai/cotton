package com.agilecontrol.b2bweb.cmd;

import org.json.JSONArray;
import org.json.JSONObject;

import com.agilecontrol.nea.core.control.event.NDSEventException;
import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.nea.util.Validator;
import com.agilecontrol.phone.CmdHandler;
import com.agilecontrol.phone.CmdResult;
import com.agilecontrol.phone.PhoneController;

/**
 * {cmd:"b2b.delivery.list"} 
 * 
 * 获取交货方式
 * 
 * 输出：[{id,payment_terms, deposit_deadline, delivery_window}]
 * 
 * id -- 交货方式id
 * payment_terms -- string 定金比例
 * deposit_deadline -- string 定金支付截止日期
 * delivery_window -- string 交货日期
 *
 */
public class DeliveryList extends CmdHandler {

	@Override
	public CmdResult execute(JSONObject jo) throws Exception {
		
		
		JSONArray ja = PhoneController.getInstance().getDataArrayByADSQL("get_delivery_terms", vc, conn, true);
	
		return new CmdResult(ja);
	}

}
