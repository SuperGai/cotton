package com.agilecontrol.b2bweb.cmd;

import java.util.ArrayList;

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
h1. user_address 用户地址修改

> {cmd:"b2b.addr.modify.both",columns}

params :[company_name,billing_address,client_city,client_zipcode,client_country,shipping_address,shipping_city,shipping_zipcpde,shipping_country,buyer_tel,accountant_contacter,
             *                accountant_tel,accountant_email,shipping_name,shipping_tel,shipping_email,bank_name,account_name,account_no,swift_code,b_currency,eori_code,sales_contact]

both定制接口 要求修改地址即输即保存
 
 * @author wu.qiong
 *
 */
public class AddrModifyByBoth extends CmdHandler {
	
	public CmdResult execute(JSONObject jo) throws Exception {
		
		JSONArray ja = jo.optJSONArray("columns");
		int id = jo.optInt("addrId", -1);
		if(id == -1){
			throw new NDSException("未找到相应的addrId");
		}
		
		if(ja == null || ja.length() == 0){
			return new CmdResult();
		}else{
			ArrayList params=new ArrayList();
			params.add(usr.getId());
			for(int i = 0;i < ja.length();i++){
			if(ja.get(i) == null){
				params.add("");
			}else{
				params.add(ja.get(i));
			}	
			}
			engine.executeStoredProcedure("merge_c_store_info", params, conn);
			jedis.del("addr:"+id);
		}
		return new CmdResult();
	}
}













