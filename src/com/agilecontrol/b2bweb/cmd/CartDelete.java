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
h1. ���ﳵ��Ʒ�Ƴ�

h2. ����

> {cmd:"b2b.cart.delete", pdts}
*pdts* - jsonarrary of {pdtid, actid} 
> *pdtid* - ��Ʒid
> *actid* - �id����������-1����ʾ���μӻ����Ʒ

h2. ���

{code,message}



 * @author yfzhu
 *
 */
@Admin(mail="yfzhu@lifecycle.cn")
public class CartDelete extends CmdHandler {
	
	public CmdResult execute(JSONObject jo) throws Exception {
		
		JSONArray pdts=jo.getJSONArray("pdts");
		for(int i=0;i<pdts.length();i++){
			JSONObject pdt=pdts.getJSONObject(i);
			int pdtId= pdt.getInt("pdtid");
			int actId= pdt.optInt("actid", -1);
			if(actId==-1){
				engine.executeUpdate("delete from b_cart where user_id=? and m_product_id=?", new Object[]{usr.getId(), pdtId}, conn);
			}else{
				engine.executeUpdate("delete from b_cart where user_id=? and m_product_id=? and b_prmt_id=?",
						new Object[]{usr.getId(), pdtId,actId}, conn);
			}
		}
		
		return CmdResult.SUCCESS;
		
	}
	
}
