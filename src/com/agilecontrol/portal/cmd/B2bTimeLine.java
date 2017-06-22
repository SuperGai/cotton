package com.agilecontrol.portal.cmd;

import org.apache.commons.lang.Validate;
import org.json.JSONArray;
import org.json.JSONObject;

import com.agilecontrol.nea.core.control.event.NDSEventException;
import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.nea.util.Validator;
import com.agilecontrol.phone.CmdHandler;
import com.agilecontrol.phone.CmdResult;
import com.agilecontrol.phone.PhoneController;

/**
 * ������b2bʱ����
 * 
 * ���룺
 * 
 * �����
 * 
 [
 {
     id      : 1,
     btime   : "1999-10-11",
     title   : "ʱ����",
     subtitle: "�޹�ʱ����",
     content : "�޹�ʱ�����޹�ʱ�����޹�ʱ�����޹�ʱ�����޹�ʱ����",
     img     : "img/timelineimg1.png",
     icon    : "img/timelineicon1.png"
 }
 ];
 * 
 * @author sun.yifan
 *
 */
public class B2bTimeLine extends CmdHandler {

	@Override
	public CmdResult execute(JSONObject jo) throws Exception {
		String pdtsql = PhoneController.getInstance().getValueFromADSQL("b2b:timeline",conn);
		if(Validator.isNull(pdtsql)){
			throw new NDSException("ad_sql#b2b:timeline not found");
		} 
		JSONArray pdtdesc = engine.doQueryObjectArray(pdtsql, new Object[]{},conn);
		if(pdtdesc==null){
			pdtdesc = new JSONArray();
		}
		return new CmdResult(pdtdesc);
	}

	@Override
	public boolean allowGuest() {
		return true;
	}
	

}
