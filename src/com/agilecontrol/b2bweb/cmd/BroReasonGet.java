package com.agilecontrol.b2bweb.cmd;

import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONObject;

import com.agilecontrol.b2b.cmd.ObjectGet;
import com.agilecontrol.b2b.cmd.Search;
import com.agilecontrol.b2b.schema.Table;
import com.agilecontrol.b2bweb.WebController;
import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.nea.util.Tools;
import com.agilecontrol.nea.util.Validator;
import com.agilecontrol.phone.Admin;
import com.agilecontrol.phone.CmdHandler;
import com.agilecontrol.phone.CmdResult;
import com.agilecontrol.phone.PhoneConfig;
import com.agilecontrol.phone.PhoneController;
import com.agilecontrol.phone.PhoneUtils;

/**
 h1. b_bro�˻�ԭ��������ѡ��

> {cmd:"b2b.bro.reason.get"}

     [{id,desc}]
     
	   id -- int �˻�ԭ��id
	   desc -- string �˻�ԭ������
	   
	   
 * @author wu.qiong
 *
 */
public class BroReasonGet extends CmdHandler{
	
	   public CmdResult execute(JSONObject jo) throws Exception {
		
		vc.put("uid",usr.getId());
		vc.put("marketid", usr.getMarketId());
		
		String sql = "select id,description note from b_bro_reason order by orderno";
		
		JSONArray ja = engine.doQueryObjectArray(sql, new Object[]{}, conn);
		
		return new CmdResult(ja);
	}

}













