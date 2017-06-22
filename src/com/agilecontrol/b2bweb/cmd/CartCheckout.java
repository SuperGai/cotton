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
h1. ���ﳵ����

h2. ����

> {cmd:"b2b.cart.checkout",  pdts,allpdts}
����ѡ��Ĺ��ﳵ���ݣ�ȷ�����ɶ�������Ҫ�������˼��㶩���ܽ��
*pdts* - ��Ʒ���飬������ʽ��[$pdtid], �� [{pdtid, actid}] 
*$pdtid* - �����޹���Ŀ��ֱ�ӷ�pdtid����ʾ����ָ����Ʒ
*{pdt}* - {pdtid, actid} �ֱ�Ϊ��Ʒid�ͻid, ���лid����Ϊ�ջ�-1, ��ʾ�޻����Ʒ�����ﳵ�вμӻ����Ʒactidһ����Ϊ�գ�
*allpdts* - boolean �Ƿ�������ﳵ������Ʒ�����pdtsΪ�գ���ʶ��˲���

h2. ���

> {com_id,totAmt,totQty, pdts, sku_level, totDiscountOptions}

*comId* -- Number ���id
*totAmt* -- Number �ϼƼ۸�
*totQty* -- �����ϼ�
*pdts* -- array ÿ��Ԫ���ǵ�������µ���Ϣ��������{skupdt}, �����ǵ�pdt�������� {pdt}
*sku_level* -- boolean default false�Ƿ�sku����, ��ģ����sku������ʾ��sku���𣬷���pdt���𣬿ͻ�����Ҫͨ�� b2b.cart.pdt.get ����ȡ������
*totDiscountOptions* - jsonarray of jsonobj  [{name, discount}] , name - �������ƣ� discount - �����ۿ�

�����д洢����b_cart_checkout ���н�����֤�����е���Ҫ�����Ǹ���is_expire �ֶ�

h3. pdt

> {pdtid,actid, qty,amt,is_expire, st}

*pdtid* - {id,name,no,mainpic,...}
*actid* - -1 �� {id,name} ���ݸ���table:act:meta#cols��listview �ֶ�����
*qty* - int ������
*amt* - double ��Ʒ������ͼ۸����Ľ��
*is_expire* - string "Y"|"N"  �Ƿ�ʧЧ, ����Y��ʱ���ʾʧЧ�������κ�״̬������Ч��
*st* - status {c,m} c - code ���룬��0���Ǳ�ʾ����m - ������ʾ�� st �����ڱ�ʾ�޴��� 

����ad_sql#cart_ptds
> select m_product_id pdtid, B_PRMT_ID actid, sum(qty) qty, sum(price*qty) amt, max(modifieddate) mdate
>		from b_cart where xxx and user_id=$uid and is_order='Y' group by  m_product_id, B_PRMT_ID

h3. skupdt

> {pdtid,actid, qty,amt, colorsize,is_expire, st}
*colorsize - [{c, s, q, p,pp, st}] �ֱ��Ӧ c- color, s-size q-qty���ǵ�ǰɫ��ľ��嶩��
*is_expire* - string "Y"|"N"  �Ƿ�ʧЧ
*st* - status {c,m} c - code ���� int����0���Ǳ�ʾ����m - message ������ʾ string�� st �����ڱ�ʾ�޴���

����ad_sql#cart_ptds_sku
> select m_product_id pdtid, _PRMT_ID actid, xx color, xx size, qty from b_cart where xx and user_id=$uid and is_order='Y'

h3. �����ۿ���Ϣ

����ad_sql#cart_preorder

select xxx id,xxx name, xxx amt from xxx where $uid

id - �id
name - �����
amt - ��ǰ��Ʒ�ĺϼƽ��

 * @author yfzhu
 *
 */
@Admin(mail="yfzhu@lifecycle.cn")
public class CartCheckout extends CmdHandler {
	
	public CmdResult execute(JSONObject jo) throws Exception {
		
		//�������is_order������
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
				if(pdtId==-1) throw new NDSException("pdts��Ҫ������������������");
				engine.executeUpdate("update b_cart set is_order='Y' where user_id=? and m_product_id=?", new Object[]{usr.getId(), pdtId}, conn);
			}
		}else {
			if(jo.optBoolean("allpdts", false)){
				engine.executeUpdate("update b_cart set is_order='Y' where user_id=?", 
						new Object[]{usr.getId()}, conn);
			}else throw new NDSException("Select products first");
		}
		//����is_expire�ֶ�
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
		
		//��ȡ���ݿ������sql��䣬��ȡÿ����Ʒ��Ӧ��state�����磬BOTH��У����Ʒ�Ķ����Ƿ�Ϊż���Ҵ���5
		//sql�����ʽ: select pdtid, code, message from b_cart where xxx
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
			if(!"Y".equals(expire)){//��Ʒ�Ѿ�ʧЧ
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
		//�����ۿ�
		//select xxx actid, xxx amt from xxx where $uid
		rows= PhoneController.getInstance().getDataArrayByADSQL("cart_preorder", vc, conn, true);
		obj.put("totDiscountOptions", rows);
		
		JSONArray ar=new JSONArray();
		ar.put(obj);
		
		CmdResult res=new CmdResult(ar );
		return res;
		
	}
	/**
	 * ��ȡ���ﳵ��is_order����Ʒ����ϸ��Ϣ������ad_sql#cart_ptds_sku
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
