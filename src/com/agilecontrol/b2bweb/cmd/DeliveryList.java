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
 * ��ȡ������ʽ
 * 
 * �����[{id,payment_terms, deposit_deadline, delivery_window}]
 * 
 * id -- ������ʽid
 * payment_terms -- string �������
 * deposit_deadline -- string ����֧����ֹ����
 * delivery_window -- string ��������
 *
 */
public class DeliveryList extends CmdHandler {

	@Override
	public CmdResult execute(JSONObject jo) throws Exception {
		
		
		JSONArray ja = PhoneController.getInstance().getDataArrayByADSQL("get_delivery_terms", vc, conn, true);
	
		return new CmdResult(ja);
	}

}
