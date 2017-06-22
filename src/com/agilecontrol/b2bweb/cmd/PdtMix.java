package com.agilecontrol.b2bweb.cmd;

import org.json.JSONArray;
import org.json.JSONObject;

import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.nea.util.Validator;
import com.agilecontrol.phone.CmdResult;
import com.agilecontrol.phone.PhoneController;

/**
 * 根据both要求，前台需要商品详情，根据PdtGet和PdtDescription拿到商品详情
 * 
 * 
 * h1. 商品详情界面

h2. 场景

单品界面,获取商品详情

h2. 输入

> {cmd:"b2b.pdt.mix",id }

*id* - int pdt.id

h2. 输出

> {pdtid, note,no,mainpic,price,tags,allpic,isfav,dtls}

*pdtid* - int pdtid
*note* - string 商品备注 
*no* - string 商品编号
*mainpic* -string 商品图片
*price* - double 显示的价格
*tags* - jsonarray of string 商品标签，如: ["爆款","范冰冰同款"]
*allpic* - jsonarray of string 商品图片, 来自于b_img/b_pdt_img
*isfav* -1/0 状态 是否收藏，来自于 B_FAVOURITE 	
*dtls* - string 商品详情, 来自 M_PRODUCT_TRANS
 [
 	{
 		"title":"@m_dim1_id@","value":"棉购"
 	},
 	{
 		"title":"@m_dim1_id@","value":"棉购"
 	},
 	{
 		"title":"@m_dim1_id@","value":"棉购"
 	}
 ]
 
 场景：both商品详情
 * @author lsh
 *
 */
public class PdtMix extends PdtGet {

	@Override
	public CmdResult execute(JSONObject jo) throws Exception {
		
		return super.execute(jo);
	}
	@Override
	protected void postAction(JSONObject retObj) throws Exception {
		long pdtid = retObj.optLong("id",-1);
		if(pdtid==-1){
			throw new NDSException("pdtid not found");
		}
		String pdtsql = PhoneController.getInstance().getValueFromADSQL("b2b:pdt:descget",conn);
		if(Validator.isNull(pdtsql)){
			throw new NDSException("ad_sql#b2b:pdt:desc not found");
		} 
		JSONArray pdtdesc = engine.doQueryObjectArray(pdtsql, new Object[]{pdtid,usr.getLangId(),usr.getLangId()},conn);
		if(pdtdesc==null){
			pdtdesc = new JSONArray();
		}
		retObj.put("desc", pdtdesc);
		super.postAction(retObj);
	}
	
}
