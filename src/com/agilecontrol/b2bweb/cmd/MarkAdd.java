package com.agilecontrol.b2bweb.cmd;

import org.json.JSONArray;
import org.json.JSONObject;

import com.agilecontrol.b2b.cmd.ObjectAdd;
import com.agilecontrol.b2b.cmd.Search;
import com.agilecontrol.b2b.schema.Table;
import com.agilecontrol.b2bweb.WebController;
import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.nea.util.Tools;
import com.agilecontrol.nea.util.Validator;
import com.agilecontrol.phone.Admin;
import com.agilecontrol.phone.CmdHandler;
import com.agilecontrol.phone.CmdResult;
import com.agilecontrol.phone.PhoneController;
import com.agilecontrol.phone.PhoneUtils;

/**
h1. 新增评论

> {cmd:"b2b.mark.add", obj}

目前的评论可以随意新增，不做控制

obj：{pdtid,actid,orderid,score,comments,url}

*pdtid* - 必填
*actid* - 如实填，因为不同活动对应的经销商可能不同，发货人就不同
*orderid* - 当前订单的id，系统将进行订单明细校验，要求必须有对应订单明细才能评论
*score* - 用户打分
*comments* - 评论
*url* - 图片
 
 * @author yfzhu
 *
 */
@Admin(mail="yfzhu@lifecycle.cn")
public class MarkAdd extends CmdHandler {
	
	public CmdResult execute(JSONObject obj) throws Exception {
		JSONObject jo=obj.getJSONObject("obj");
		int pdtId=this.getInt(jo, "pdtid");
		int actId=jo.optInt( "actid",-1);
		int orderId=this.getInt(jo, "orderid");
		vc.put("marketid", usr.getMarketId());
		vc.put("actid", actId);
		vc.put("pdtid", pdtId);
		vc.put("uid", usr.getId());
		int customerId= getCustomerIdByActOrMarket(actId,usr.getMarketId()) ;
		vc.put("customerid",customerId);
		
		//check order id should contain this product/act
		int cnt=engine.doQueryInt("select count(*) from b_bfoitem where b_bfo_id=? and m_product_id=? and ((b_prmt_id=? and b_prmt_id>0 ) or (b_prmt_id is null and -1=?))",
				new Object[]{orderId,pdtId,actId,actId},conn);
		if(cnt==0) throw new NDSException("@no-permission@");
		
		double score=jo.optDouble("score",0);
		if(score==0)throw new NDSException("需要填写评分");
		
		String comments=jo.optString("comments");
		if(Validator.isNull(comments))comments="";
		
		String url=jo.optString("url");
		if(Validator.isNull(url))url="";
		
		int objId=engine.getSequence("b_pdt_fav", conn);
		engine.executeUpdate("insert into b_pdt_fav(id,c_customer_id,user_id,m_product_id,score,comments,url,ownerid,creationdate) values(?,?,?,?,?,?,?,?,sysdate)",
				new Object[]{objId,customerId, usr.getId(), pdtId,score,comments,url, usr.getId()}, conn);

		JSONObject one= PhoneUtils.fetchObject(manager.getTable("mark"), objId, conn, jedis);
		return new CmdResult(one);
	}

}













