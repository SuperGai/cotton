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
h1. broItems 退货单明细

> {cmd:"b2b.bro.modify",id,remarks}

id -- 退货单明细 id

type -- remark | others  remark -- string 修改备注 others 修改退货原因

remarks -- 退货单明细备注
reason_id -- 退货原因id

 
 * @author wu.qiong
 *
 */
public class BroModify extends CmdHandler {
	
	public CmdResult execute(JSONObject jo) throws Exception {
		
		int bro_item_id = jo.optInt("id", -1);
		String remark = jo.optString("remark", "");
		int reason_id = jo.optInt("reason_id",-1);
		String type = jo.optString("type", "remark");
		
		if(bro_item_id == -1){
			throw new NDSException("未找到 id 为"+bro_item_id+" 退货单 ");
		}
		
		if(type.toLowerCase() == "remark" ){
			engine.executeUpdate("update b_broitem set remark=? where id=?", new Object[]{remark,bro_item_id}, conn);
		}else {
			engine.executeUpdate("update b_broitem set b_bro_reason_id=? where id=?", new Object[]{reason_id,bro_item_id}, conn);
		}
		return new CmdResult();
	}
}













