package com.agilecontrol.b2bweb.cmd;

import org.json.JSONArray;
import org.json.JSONObject;

import com.agilecontrol.b2b.cmd.ObjectGet;
import com.agilecontrol.b2b.cmd.Search;
import com.agilecontrol.b2b.schema.Table;
import com.agilecontrol.b2bweb.WebController;
import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.nea.util.Validator;
import com.agilecontrol.phone.Admin;
import com.agilecontrol.phone.CmdHandler;
import com.agilecontrol.phone.CmdResult;
import com.agilecontrol.phone.PhoneController;
import com.agilecontrol.phone.PhoneUtils;

/**

 输入

storeid - 所属门店id

输出
{pdts:[{pdt}]，销补商品的信息}



 * @author Supergai
 *
 */
@Admin(mail="xuwj@cottonshop.com")
public class IntelligentpdsGet extends ObjectGet {
	
	@Override
	public CmdResult execute(JSONObject jo) throws Exception {
		int storeid = jo.getInt("storeid");
		if(storeid==-1){
			throw new NDSException("storeid not found");
		}
		/**
		 * 逻辑为通过userid获得他的店仓获得销补的所有商品
		 */
	String sql = "select   b.*  from c_sale_replenishment a,m_product b  where a.m_product_id=b.id   and a.c_store_id=?";
		JSONArray pdtList = engine.doQueryObjectArray(sql, new Object[]{storeid},conn);
		if(pdtList==null){
			pdtList = new JSONArray();	    
		}
		return new CmdResult(pdtList);
	}

}
