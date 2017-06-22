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
h1. b_bfo订单

> {cmd:"b2b.order.get"}

按cartcheckout一致的结构输出 pdts

 
 * @author yfzhu
 *
 */
@Admin(mail="yfzhu@lifecycle.cn")
public class BfoGet extends ObjectGet {
	
	/**
	 * 单层简单对象获取后，方便开发校验权限
	 * @param table
	 * @param fetchObj 可以理解就是当前记录展开 
	 * @throws Exception
	 */
	protected void checkPermission(Table table, JSONObject fetchObj) throws Exception{
		//用户需要是当前订单的创建人, 或管理员
		int ownerId=fetchObj.optInt("ownerid", -2);
		if(ownerId==-2){
			throw new NDSException("请在ad_sql#table:bfo:meta中配置ownerid字段");
		}
		if(usr.getId()!=ownerId){
			//是否管理员
			if(!usr.getName().equals("root")) throw new NDSException("@no-permission@");
		}
	}
	/**
	 * 获取购物车中is_order的商品的详细信息，基于ad_sql#cart_ptds_sku
	 * 
	 * cart_ptds_sku:
	 * select m_product_id pdtid, _PRMT_ID actid, xx color, xx size, qty from b_cart where xx and user_id=$uid and is_order='Y'
	 * @return key: pdtid+"."+ actid, value: [{c,s,q}]
	 * @throws Exception
	 */
	private HashMap<String,JSONArray> getCartOrderSKUs() throws Exception{
		HashMap<String,JSONArray> pdts=new HashMap<String,JSONArray>();
		JSONArray rows=PhoneController.getInstance().getDataArrayByADSQL("bfo_ptds_sku", vc, conn, true);
		for(int i=0;i<rows.length();i++){
			JSONObject row=rows.getJSONObject(i);
			int pdtId=row.getInt("pdtid");
			int actId=row.optInt("actid", -1);
			String key=pdtId+"."+actId;
			JSONArray pdt=pdts.get(key);
			if(pdt==null){
				pdt=new JSONArray();
				pdts.put(key, pdt);
			}
//			JSONObject one=new JSONObject();
//			one.put("c", row.getString("color"));
//			one.put("s", row.getString("size"));
//			one.put("q", row.getInt("qty"));
//			one.put("p", row.optDouble("price"));
//			one.put("pp", row.optDouble("pprice"));
//			one.put("amt", row.optDouble("amt"));
			pdt.put(row);
		}
		
		return pdts;
	}
	/**
	 * redis完成缓存更新后的处理
	 * @param table
	 * @param retObj 正常处理完的，将返回客户端的对象，可以重构, 这里已经是装配完的对象
	 * @throws Exception
	 */
	protected void postAction(Table bfoTable, JSONObject retObj) throws Exception{
		vc.put("uid",usr.getId());
		vc.put("marketid", usr.getMarketId());
		vc.put("id", retObj.getInt("id"));
		
		boolean skuLevel=PhoneConfig.CART_SKU_LEVEL;
		double totAmt=0;
		int totQty=0;
		//key: pdtid+"."+actid, value: colorsize
		HashMap<String, JSONArray> pdtSKUs=null;
		if(skuLevel) pdtSKUs=getCartOrderSKUs();
		//select m_product_id pdtid, B_PRMT_ID actid, sum(qty) qty, sum(price*qty) amt, max(modifieddate) mdate
		//from b_cart where xxx and user_id=$uid and is_order='Y' group by  m_product_id, B_PRMT_ID
		JSONArray rows= PhoneController.getInstance().getDataArrayByADSQL("bfo_checkout_ptds", vc, conn, true);
		Table pdtTable=manager.getTable("pdt");
		Table actTable=manager.getTable("act");
		for(int i=0;i<rows.length();i++){
			JSONObject row=rows.getJSONObject(i);
			long pdtId=row.getInt("pdtid");
			int actId=row.optInt("actid", -1);
			JSONObject pdt=this.fetchObject(pdtId, "pdt", pdtTable.getColumnsInListView(),null);
			WebController.getInstance().replacePdtValues(pdt, usr.getLangId(), usr.getMarketId(), vc, jedis, conn);
			row.put("pdtid", pdt);
			
			if(actId!=-1)row.put("actid", this.fetchObject(actId, "act", actTable.getColumnsInListView(),null));
			if(skuLevel) row.put("colorsize", pdtSKUs.get(pdtId+"."+actId));
			totAmt+= row.getDouble("amt");
			totQty+=row.getInt("qty");
		}
		retObj.put("pdts", rows);
		retObj.put("sku_level", skuLevel);
	}

}













