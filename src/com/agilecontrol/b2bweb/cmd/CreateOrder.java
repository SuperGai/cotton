package com.agilecontrol.b2bweb.cmd;

import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONObject;

import com.agilecontrol.b2bweb.OrderSender;
import com.agilecontrol.nea.core.control.event.DefaultWebEvent;
import com.agilecontrol.nea.core.control.util.ValueHolder;
import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.nea.util.Tools;
import com.agilecontrol.nea.util.Validator;
import com.agilecontrol.phone.CmdHandler;
import com.agilecontrol.phone.CmdResult;
import com.agilecontrol.phone.PhoneConfig;

/**
h1. ���ﳵ���ɶ���

h2. ����

> {cmd:"b2b.cart.createorder",  addr_id,pdts,order_act_id,remarks, allpdts,delivery_terms_id}

����ѡ��Ĺ��ﳵ���ݣ�ȷ�����ɶ�������Ҫ�������˼��㶩���ܽ��

*addr_id* - int �û�ѡ��ĵ�ַid
*pdts* - jsonarray of jsonboj ��Ʒ {pdtid, actid}
*order_act_id* - int �����ۿ۵�id
*remark* - string ��ע
*allpdts* - boolean �Ƿ�������ﳵ������Ʒ�����pdtsΪ�գ���ʶ��˲���
*delivery_terms_id* - int ������ʽid

h2. ���

> {orderids}
*orderids* - JSONArray of int: b_bfo.id

h2. ����˵��

���¹��ﳵ��Ʒpdtid+actidָ�����У�����: b_favourite.is_order='Y', ���� 

> b_bfo_addorder(p_user_id in number,p_addr_id in number, p_order_act_id in number, p_remark in varchar2,r_orderid out varchar2 )
*p_user_id* users.id
*p_addr_id*  user_address.id
*p_order_act_id* �����ۿ۵�id,��У��˻�õ���Ч��
*p_remark_id* �򷽱�ע
*r_orderid* ���ɵĶ���id, ����ж�����ö��ŷָ�
 
�ͻ�ѡ�еĹ��ﳵ��Ʒ��������Ҫ���ɶ�������Ʒ����b_cart.is_order�ֶ�����Ϊ"Y"
��Ҫ�ڶ�������������ȷ�ϻ����Ч�ԣ�ʱ�䣬��棬��ǰ�û��ɲɹ��ȣ�
��ɶ�����ϸ��У�������ۿۻ�Ƿ����㡣
�ӹ��ﳵ��ɾ�������Ʒ������r_orderid���ͻ���

������Ҫ����֧�ַ��͵�����ϵͳb_bfo_addorder�������ܻ�𵥣����ɶ�ݶ�����

 * 
 * @author yfzhu
 *
 */
public class CreateOrder extends CartCheckout/*Ϊ�˸���getCartPdtStates*/ {
	
	
	public CmdResult execute(JSONObject jo) throws Exception {
		//user_address
		int addrId= this.getInt(jo, "addr_id");
		int  orderActId=jo.optInt("order_act_id",-1);
		String remark=jo.optString("remark");
		int delivery_terms_id = jo.optInt("delivery_terms_id", -1);
		JSONArray pdts=jo.optJSONArray("pdts");
		if(pdts!=null){
			//����ȫ������򹳵���Ʒ
			engine.executeUpdate("update b_cart set is_order='N' where user_id=?", new Object[]{usr.getId()}, conn);
			for(int i=0;i<pdts.length();i++){
				JSONObject pdt=pdts.getJSONObject(i);
				int pdtId=pdt.getInt("pdtid");
				int actId=pdt.getInt("actid");
				engine.executeUpdate("update b_cart set is_order='Y' where user_id=? and nvl(b_prmt_id,-1)=? and m_product_id=?", 
						new Object[]{usr.getId(), actId, pdtId}, conn);
			}
		}else {
			if(jo.optBoolean("allpdts", false)){
				engine.executeUpdate("update b_cart set is_order='Y' where user_id=?", 
						new Object[]{usr.getId()}, conn);
			}else throw new NDSException("Select products first");
		}
		
		vc.put("uid",usr.getId());
		vc.put("marketid", usr.getMarketId());
		//��ȡ���ݿ������sql��䣬��ȡÿ����Ʒ��Ӧ��state�����磬BOTH��У����Ʒ�Ķ����Ƿ�Ϊż���Ҵ���5
		//sql�����ʽ: select pdtid, code, message from b_cart where xxx
		//key: pdtid, value: {c,m}
		HashMap<Integer, JSONObject> pdtStates=getCartPdtStates();
		if(pdtStates.size()>0) throw new NDSException("@cart-pdt-not-valid@");//���ﳵ��Ʒ��������������ѡ��
		
		ArrayList params=new ArrayList();
		params.add(usr.getId());
		params.add(orderActId);
		params.add(addrId);
		params.add(usr.getMarketId());
		params.add(remark);
		if(delivery_terms_id != -1){
			params.add(delivery_terms_id);
		}
		params.add(String.class);//output
		/**
		 * CREATE OR REPLACE PROCEDURE b_bfo_addorder(v_user_id      NUMBER,
                                           v_b_prmt_id    NUMBER,
                                           v_address_id   NUMBER,
                                           v_b_market_id  NUMBER,
                                           v_remark       VARCHAR2,
                                           v_b_bfo_id_out OUT String)
		 */
		ArrayList result=engine.executeStoredProcedure("b_bfo_addorder",params, conn);
		
		String orderIds=result.get(0).toString();
		JSONArray ids= new JSONArray("["+orderIds+"]");

		//��¼��ǰ����Ϊ�ύ״̬
		if(PhoneConfig.SUBMIT_ORDER_AFTER_CREATE){
			for(int i=0;i<ids.length();i++){
				int orderId= ids.getInt(i);
				
				//ͨ��ƽ̨���ύ����
				DefaultWebEvent evt=new DefaultWebEvent("CommandEvent", event.getContext());
				evt.setParameter("command","ObjectSubmit");
				evt.setParameter("operatorid", String.valueOf(this.usr.getId()));
				evt.setParameter("table", "b_bfo");
				evt.setParameter("id", String.valueOf(orderId) ); 
				ValueHolder vh=helper.handleEvent(evt);
				
//				params=new ArrayList();
//				params.add(orderId);
//				engine.executeStoredProcedure("b_bfo_submit", params, true, conn);
			}
		}

		
		//��չ����������ҵ��Ҫ������Ƿ���Ҫ���Ͷ���
		String senderClass=PhoneConfig.ORDER_SENDER_CLASS;
				
		if(Validator.isNotNull(senderClass)){
			
			OrderSender sender=null;
			try{
				sender=(OrderSender)Class.forName(senderClass).newInstance();
				sender.init(false,conn);//async
			}catch(Throwable tx){
				logger.error("check param#phone.order_sender_class", tx);
			}
			for(int i=0;i<ids.length();i++){
				int orderId=ids.getInt(i);
				sender.send(orderId);
			}
		}
		
		JSONObject ret=new JSONObject();
		ret.put("orderids", ids);
		return new CmdResult(ret);
	}

}
