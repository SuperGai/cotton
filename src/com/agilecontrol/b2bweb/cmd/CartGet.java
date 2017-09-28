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

*qtyonly* - �Ƿ�ֻҪ�ϼƶ�������ʾ�ڹ��ﳵbadge��

���ݵ�ǰ�ѵ�½�û���Ϣ����ȡ���ﳵ���ݣ�Ŀǰ�Ĺ��ﳵ�ǰ���Ӧ�̷ֿ��ģ�Ĭ��1����Ӧ��һ�����ﳵ�����������ģʽ��

���е�B2B���������ﳵΪһ��������Ȼjsonarray����ʽ

h2. ���

> [{cart}] or {qty}

qty -  ���qtyonly��ʱ����ʾ

cart�ṹ

> {com_id,com_name,pdts:[ {pdtid:{no,note,mainpic,price}, actid:{id,name}, qty, amt, mdate, st }]}

*com_id* - int ��ǰb2b��ĿΪ ad_client_id
*com_name* - string ��ǰb2b��ĿΪ ad_client.name
*pdts* - ��Ʒ�б�  [ {pdtid:{no,note,mainpic,price}, actid:{id,name}, qty, amt, mdate, st:{m,c} }]
*st* - status {c,m} c - code ���� int����0���Ǳ�ʾ����m - message ������ʾ string�� st �����ڱ�ʾ�޴���
	
 * @author yfzhu
 *
 */
@Admin(mail="yfzhu@lifecycle.cn")
public class CartGet extends CmdHandler {
	
	public CmdResult execute(JSONObject jo) throws Exception {
		
		boolean qtyOnly=jo.optBoolean("qtyonly", false);
	 if(qtyOnly){
			JSONObject qtyObj=new JSONObject();
			//���ݿ�����¼���Ʒ��ʱ��ά�����ﳵ�����ڵ�����ʱ����¹��ﳵ
			int qty=engine.doQueryInt("select nvl(sum(qty),0) from b_cart where user_id=? and b_market_id=? and isactive='Y' and is_expire='N'",
					new Object[]{usr.getId(), usr.getMarketId()}, conn);
			qtyObj.put("qty", qty);
			return new CmdResult(qtyObj);
		}
		
		vc.put("uid",usr.getId());
		//select m_product_id pdtid, B_PRMT_ID actid, sum(qty) qty, sum(price*qty) amt, max(modifieddate) mdate
		JSONArray rows= PhoneController.getInstance().getDataArrayByADSQL("cart_ptds", vc, conn, true);
		//��ȡ���ݿ������sql��䣬��ȡÿ����Ʒ��Ӧ��state�����磬BOTH��У����Ʒ�Ķ����Ƿ�Ϊż���Ҵ���5
		//sql�����ʽ: select pdtid, code, message from b_cart where xxx
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
