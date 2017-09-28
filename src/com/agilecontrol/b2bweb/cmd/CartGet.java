package com.agilecontrol.b2bweb.cmd;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONObject;

import com.agilecontrol.b2b.cmd.ObjectGet;
import com.agilecontrol.b2b.cmd.Search;
import com.agilecontrol.b2b.schema.Table;
import com.agilecontrol.b2bweb.WebController;
import com.agilecontrol.phone.Admin;
import com.agilecontrol.phone.CmdHandler;
import com.agilecontrol.phone.CmdResult;
import com.agilecontrol.phone.PhoneController;
import com.agilecontrol.phone.PhoneUtils;

/**

> {cmd:"b2b.cart.get", qtyonly}

*qtyonly* - 是否只要合计订量，显示在购物车badge上

根据当前已登陆用户信息，获取购物车内容，目前的购物车是按供应商分开的，默认1个供应商一个购物车，类似于麦加模式。

现有的B2B开发，购物车为一个，但仍然jsonarray个格式

h2. 输出

> [{cart}] or {qty}

qty -  针对qtyonly的时候显示

cart结构

> {com_id,com_name,pdts:[ {pdtid:{no,note,mainpic,price}, actid:{id,name}, qty, amt, mdate, st }]}

*com_id* - int 当前b2b项目为 ad_client_id
*com_name* - string 当前b2b项目为 ad_client.name
*pdts* - 商品列表  [ {pdtid:{no,note,mainpic,price}, actid:{id,name}, qty, amt, mdate, st:{m,c} }]
*st* - status {c,m} c - code 代码 int，非0都是表示错误，m - message 错误提示 string， st 不存在表示无错误
	
 * @author yfzhu
 *
 */
@Admin(mail="yfzhu@lifecycle.cn")
public class CartGet extends CmdHandler {
	
	public CmdResult execute(JSONObject jo) throws Exception {
		
		boolean qtyOnly=jo.optBoolean("qtyonly", false);
	 if(qtyOnly){
			JSONObject qtyObj=new JSONObject();
			//数据库会在下架商品的时候维护购物车，会在点结算的时候更新购物车
			int qty=engine.doQueryInt("select nvl(sum(qty),0) from b_cart where user_id=? and b_market_id=? and isactive='Y' and is_expire='N'",
					new Object[]{usr.getId(), usr.getMarketId()}, conn);
			qtyObj.put("qty", qty);
			return new CmdResult(qtyObj);
		}
		
		vc.put("uid",usr.getId());
		//select m_product_id pdtid, B_PRMT_ID actid, sum(qty) qty, sum(price*qty) amt, max(modifieddate) mdate
		JSONArray rows= PhoneController.getInstance().getDataArrayByADSQL("cart_ptds", vc, conn, true);
		//读取数据库的配置sql语句，获取每个商品对应的state，例如，BOTH将校验商品的订量是否为偶数且大于5
		//sql语句样式: select pdtid, code, message from b_cart where xxx
		//key: pdtid, value: {c,m}
		HashMap<Integer, JSONObject> pdtStates=getCartPdtStates();
		
		Table pdtTable=manager.getTable("pdt");
		Table actTable=manager.getTable("act");
		for(int i=0;i<rows.length();i++){
			JSONObject row=rows.getJSONObject(i);
			long pdtId=row.getInt("pdtid");
			int actId=row.optInt("actid", -1);
			
			JSONObject pdtObj=this.fetchObject(pdtId, "pdt", pdtTable.getColumnsInListView(),null);
			WebController.getInstance().replacePdtValues(pdtObj, usr, vc, jedis, conn);
			
			row.put("pdtid", pdtObj);
			if(actId!=-1)row.put("actid", this.fetchObject(actId, "act", actTable.getColumnsInListView(),null));
			JSONObject state=pdtStates.get(pdtId);
			if(state!=null) row.put("st",state); 		
		}
		int storeid=engine.doQueryInt("select c_store_id from users where id=?",new Object[]{usr.getId()});
		vc.put("storeid", storeid);
		vc.put("yearmonth", gettodaytime());
		String tot_advise_amt="";
		JSONArray advitems= PhoneController.getInstance().getDataArrayByADSQL("b2b:advise:get",vc, conn, true);
	    JSONObject adviceAmt=PhoneController.getInstance().getObjectByADSQL("b2b:adviseamt:get", vc, conn);
	    if(adviceAmt!=null){
			tot_advise_amt=adviceAmt.getString("tot_advise_amt");
	    }
		JSONObject obj=new JSONObject();
		obj.put("com_id", 0);
		obj.put("com_name", "");
		obj.put("pdts", rows);
		obj.put("advitems", advitems);
		obj.put("adviceAmt", adviceAmt);
		obj.put("tot_advise_amt", tot_advise_amt);
		JSONArray ar=new JSONArray();
		ar.put(obj);		
		CmdResult res=new CmdResult(ar);
		
		return res;
		
	}

	public String gettodaytime(){
	     Date today=new Date();
		 SimpleDateFormat f=new SimpleDateFormat("yyyyMM");
		 String time=f.format(today);
		 return time;
	}
	
}
