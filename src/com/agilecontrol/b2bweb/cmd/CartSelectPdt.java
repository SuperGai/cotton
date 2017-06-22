package com.agilecontrol.b2bweb.cmd;

import java.util.ArrayList;
import java.util.Collection;
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
h1. 购物车选中商品后显示折扣

h2. 输入

> {cmd:"b2b.cart.selectpdt",  pdts}
根据选择的购物车内容，计算当前的折扣（不含整单折扣），有些商品组合后，就有折扣产生，需要显示总折扣在购物车最低下
这是一个即时计算，每次勾选新的商品，都需要调用此后台方法重新计算

*pdts* - 商品数组，两种形式：[$pdtid], 或 [{pdtid, actid}] 
*$pdtid* - 对于棉购项目，直接放pdtid，表示所有指定商品
*{pdt}* - {pdtid, actid} 分别为商品id和活动id, 其中活动id可以为空或-1, 表示无活动的商品（购物车中参加活动的商品actid一定不为空）

h2. 输出

> {totAmt}

*totAmt* -- Number 折后金额，注意不是单品金额的合计，也不含整单折扣。而是

h2. 操作

更新b_cart.is_order字段，更新当前选中商品，然后调用b_cart_selectpdt(p_user_id in number，r_amt out number)) 存储过程，返回选中商品的折后金额

 * @author yfzhu
 *
 */
@Admin(mail="yfzhu@lifecycle.cn")
public class CartSelectPdt extends CmdHandler {
	
	public CmdResult execute(JSONObject jo) throws Exception {
		
		//清空所有is_order的设置
		engine.executeUpdate("update b_cart set is_order='N' where user_id=?", new Object[]{usr.getId()}, conn);
		JSONArray pdts=jo.getJSONArray("pdts");
		for(int i=0;i<pdts.length();i++){
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
		}
		
		ArrayList params=new ArrayList();
		params.add(usr.getId());
		ArrayList resultsClass=new ArrayList();
		resultsClass.add(Double.class);
		
		Collection spres=engine.executeFunction("b_cart_selectpdt", params, resultsClass, conn);
		double amt= Double.parseDouble( String.valueOf( spres.iterator().next()));
		
		JSONObject ret=new JSONObject();
		ret.put("totAmt", amt);
		
		CmdResult res=new CmdResult(ret);
		return res;
		
	}
	
}
