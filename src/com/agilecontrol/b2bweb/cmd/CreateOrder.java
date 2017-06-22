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
h1. 购物车生成订单

h2. 输入

> {cmd:"b2b.cart.createorder",  addr_id,pdts,order_act_id,remarks, allpdts,delivery_terms_id}

根据选择的购物车内容，确认生成订单，需要服务器端计算订单总金额

*addr_id* - int 用户选择的地址id
*pdts* - jsonarray of jsonboj 商品 {pdtid, actid}
*order_act_id* - int 整单折扣的id
*remark* - string 备注
*allpdts* - boolean 是否包含购物车所有商品，如果pdts为空，将识别此参数
*delivery_terms_id* - int 交货方式id

h2. 输出

> {orderids}
*orderids* - JSONArray of int: b_bfo.id

h2. 操作说明

更新购物车商品pdtid+actid指定的行，设置: b_favourite.is_order='Y', 调用 

> b_bfo_addorder(p_user_id in number,p_addr_id in number, p_order_act_id in number, p_remark in varchar2,r_orderid out varchar2 )
*p_user_id* users.id
*p_addr_id*  user_address.id
*p_order_act_id* 整单折扣的id,需校验此获得的有效性
*p_remark_id* 买方备注
*r_orderid* 生成的订单id, 如果有多个，用逗号分隔
 
客户选中的购物车商品，即本次要生成订单的商品，在b_cart.is_order字段设置为"Y"
需要在订单创建过程中确认活动的有效性（时间，库存，当前用户可采购等）
完成订单明细后，校验整单折扣活动是否满足。
从购物车中删除相关商品，返回r_orderid给客户端

订单需要考虑支持发送到其他系统b_bfo_addorder方法可能会拆单，生成多份订单，

 * 
 * @author yfzhu
 *
 */
public class CreateOrder extends CartCheckout/*为了复用getCartPdtStates*/ {
	
	
	public CmdResult execute(JSONObject jo) throws Exception {
		//user_address
		int addrId= this.getInt(jo, "addr_id");
		int  orderActId=jo.optInt("order_act_id",-1);
		String remark=jo.optString("remark");
		int delivery_terms_id = jo.optInt("delivery_terms_id", -1);
		JSONArray pdts=jo.optJSONArray("pdts");
		if(pdts!=null){
			//首先全部清除打钩的商品
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
		//读取数据库的配置sql语句，获取每个商品对应的state，例如，BOTH将校验商品的订量是否为偶数且大于5
		//sql语句样式: select pdtid, code, message from b_cart where xxx
		//key: pdtid, value: {c,m}
		HashMap<Integer, JSONObject> pdtStates=getCartPdtStates();
		if(pdtStates.size()>0) throw new NDSException("@cart-pdt-not-valid@");//购物车商品订量有误，请重新选择
		
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

		//纪录当前订单为提交状态
		if(PhoneConfig.SUBMIT_ORDER_AFTER_CREATE){
			for(int i=0;i<ids.length();i++){
				int orderId= ids.getInt(i);
				
				//通过平台的提交方法
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

		
		//扩展订单，根据业务要求决定是否需要传送订单
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
