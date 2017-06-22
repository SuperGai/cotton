package com.agilecontrol.b2bweb.cmd;

import java.util.ArrayList;
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
h1. 购物车结算

h2. 输入

> {cmd:"b2b.cart.checkout",  pdts,allpdts}
根据选择的购物车内容，确认生成订单，需要服务器端计算订单总金额
*pdts* - 商品数组，两种形式：[$pdtid], 或 [{pdtid, actid}] 
*$pdtid* - 对于棉购项目，直接放pdtid，表示所有指定商品
*{pdt}* - {pdtid, actid} 分别为商品id和活动id, 其中活动id可以为空或-1, 表示无活动的商品（购物车中参加活动的商品actid一定不为空）
*allpdts* - boolean 是否包含购物车所有商品，如果pdts为空，将识别此参数

h2. 输出

> {com_id,totAmt,totQty, pdts, sku_level, totDiscountOptions}

*comId* -- Number 店家id
*totAmt* -- Number 合计价格
*totQty* -- 数量合计
*pdts* -- array 每个元素是到尺码的下单信息，见下文{skupdt}, 或者是到pdt级别，下文 {pdt}
*sku_level* -- boolean default false是否到sku级别, 如活动模型是sku，将显示到sku级别，否则到pdt级别，客户端需要通过 b2b.cart.pdt.get 来获取矩阵订量
*totDiscountOptions* - jsonarray of jsonobj  [{name, discount}] , name - 促销名称， discount - 促销折扣

将运行存储过程b_cart_checkout 进行结算验证，其中的重要操作是更新is_expire 字段

h3. pdt

> {pdtid,actid, qty,amt,is_expire, st}

*pdtid* - {id,name,no,mainpic,...}
*actid* - -1 或 {id,name} 内容根据table:act:meta#cols的listview 字段配置
*qty* - int 订单量
*amt* - double 商品金额，按最低价格计算的结果
*is_expire* - string "Y"|"N"  是否失效, 仅当Y的时候表示失效，其他任何状态都是有效！
*st* - status {c,m} c - code 代码，非0都是表示错误，m - 错误提示， st 不存在表示无错误 

基于ad_sql#cart_ptds
> select m_product_id pdtid, B_PRMT_ID actid, sum(qty) qty, sum(price*qty) amt, max(modifieddate) mdate
>		from b_cart where xxx and user_id=$uid and is_order='Y' group by  m_product_id, B_PRMT_ID

h3. skupdt

> {pdtid,actid, qty,amt, colorsize,is_expire, st}
*colorsize - [{c, s, q, p,pp, st}] 分别对应 c- color, s-size q-qty，是当前色码的具体订量
*is_expire* - string "Y"|"N"  是否失效
*st* - status {c,m} c - code 代码 int，非0都是表示错误，m - message 错误提示 string， st 不存在表示无错误

基于ad_sql#cart_ptds_sku
> select m_product_id pdtid, _PRMT_ID actid, xx color, xx size, qty from b_cart where xx and user_id=$uid and is_order='Y'

h3. 整单折扣信息

调用ad_sql#cart_preorder

select xxx id,xxx name, xxx amt from xxx where $uid

id - 活动id
name - 活动名称
amt - 当前商品的合计金额

 * @author yfzhu
 *
 */
@Admin(mail="yfzhu@lifecycle.cn")
public class CartCheckout extends CmdHandler {
	
	public CmdResult execute(JSONObject jo) throws Exception {
		
		//清空所有is_order的设置
		engine.executeUpdate("update b_cart set is_order='N' where user_id=?", new Object[]{usr.getId()}, conn);
		JSONArray pdts=jo.optJSONArray("pdts");
		
		if(pdts!=null)for(int i=0;i<pdts.length();i++){
			Object one= pdts.get(i);
			if(one instanceof JSONObject){
				//{pdtid, actid}
				int pdtId= ((JSONObject)one).getInt("pdtid");
				int actId= ((JSONObject)one).optInt("actid",-1);
				if(actId==-1){
					engine.executeUpdate("update b_cart set is_order='Y' where user_id=? and m_product_id=? and B_PRMT_ID is null", new Object[]{usr.getId(), pdtId}, conn);
				}else{
					engine.executeUpdate("update b_cart set is_order='Y' where user_id=? and m_product_id=? and B_PRMT_ID=?", new Object[]{usr.getId(), pdtId, actId}, conn);
				}
			}else{
				int pdtId=Tools.getInt(one, -1);
				if(pdtId==-1) throw new NDSException("pdts需要是数字数组或对象数组");
				engine.executeUpdate("update b_cart set is_order='Y' where user_id=? and m_product_id=?", new Object[]{usr.getId(), pdtId}, conn);
			}
		}else {
			if(jo.optBoolean("allpdts", false)){
				engine.executeUpdate("update b_cart set is_order='Y' where user_id=?", 
						new Object[]{usr.getId()}, conn);
			}else throw new NDSException("Select products first");
		}
		//更新is_expire字段
		ArrayList params=new ArrayList();
		params.add(usr.getId());
		engine.executeStoredProcedure("b_cart_checkout", params, false, conn);
		
		vc.put("uid",usr.getId());
		vc.put("marketid", usr.getMarketId());
		
		
		boolean skuLevel=PhoneConfig.CART_SKU_LEVEL;
		double totAmt=0;
		int totQty=0;
		//key: pdtid+"."+actid, value: colorsize
		HashMap<String, JSONArray> pdtSKUs=null;
		if(skuLevel) pdtSKUs=getCartOrderSKUs();
		//select m_product_id pdtid, B_PRMT_ID actid, sum(qty) qty, sum(price*qty) amt, max(modifieddate) mdate, is_expire
		//from b_cart where xxx and user_id=$uid and is_order='Y' group by  m_product_id, B_PRMT_ID
		JSONArray rows= PhoneController.getInstance().getDataArrayByADSQL("cart_checkout_ptds", vc, conn, true);
		
		//读取数据库的配置sql语句，获取每个商品对应的state，例如，BOTH将校验商品的订量是否为偶数且大于5
		//sql语句样式: select pdtid, code, message from b_cart where xxx
		//key: pdtid, value: {c,m}
		HashMap<Integer, JSONObject> pdtStates=getCartPdtStates();
		
		Table table=manager.getTable("pdt");
		for(int i=0;i<rows.length();i++){
			JSONObject row=rows.getJSONObject(i);
			int pdtId=row.getInt("pdtid");
			int actId=row.optInt("actid", -1);
			JSONObject pdtObj=this.fetchObject(pdtId, "pdt", table.getColumnsInListView(),null);
			WebController.getInstance().replacePdtValues(pdtObj, usr.getLangId(), usr.getMarketId(), vc, jedis, conn);
			row.put("pdtid", pdtObj);
			if(actId!=-1)row.put("actid", this.fetchObject(actId, "act", table.getColumnsInListView(),null));
			if(skuLevel) row.put("colorsize", pdtSKUs.get(pdtId+"."+actId));
			//will set st(state) here 
			JSONObject state=pdtStates.get(pdtId);
			if(state!=null) row.put("st",state); 
			String expire= row.optString("is_expire");
			if(!"Y".equals(expire)){//商品已经失效
				totAmt+= row.getDouble("amt");
				totQty+=row.getInt("qty");
			}
		}
		JSONObject obj=new JSONObject();
		obj.put("com_id", 0);
		obj.put("com_name", "");
		obj.put("pdts", rows);
		obj.put("sku_level", skuLevel);
		obj.put("totAmt", totAmt);
		obj.put("totQty", totQty);
		//整单折扣
		//select xxx actid, xxx amt from xxx where $uid
		rows= PhoneController.getInstance().getDataArrayByADSQL("cart_preorder", vc, conn, true);
		obj.put("totDiscountOptions", rows);
		
		JSONArray ar=new JSONArray();
		ar.put(obj);
		
		CmdResult res=new CmdResult(ar );
		return res;
		
	}
	/**
	 * 获取购物车中is_order的商品的详细信息，基于ad_sql#cart_ptds_sku
	 * 
	 * cart_ptds_sku:
	 * select m_product_id pdtid, _PRMT_ID actid, xx color, xx size, qty from b_cart where xx and user_id=$uid and is_order='Y'
	 * @return key: pdtid+"."+ actid, value: [{c,s,q,p,p,amt,is_expire}]
	 * @throws Exception
	 */
	private HashMap<String,JSONArray> getCartOrderSKUs() throws Exception{
		HashMap<String,JSONArray> pdts=new HashMap<String,JSONArray>();
		JSONArray rows=PhoneController.getInstance().getDataArrayByADSQL("cart_ptds_sku", vc, conn, true);
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
			JSONObject one=new JSONObject();
			one.put("c", row.getString("color"));
			one.put("s", row.getString("size"));
			one.put("q", row.getInt("qty"));
			one.put("p", row.optDouble("price",0));
			one.put("pp", row.optDouble("pprice",0));
			one.put("amt", row.optDouble("amt",0));
			one.put("is_expire",row.optString("is_expire"));
			pdt.put(one);
		}
		
		return pdts;
	}
}
