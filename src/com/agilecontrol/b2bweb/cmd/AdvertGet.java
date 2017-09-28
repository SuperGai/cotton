package com.agilecontrol.b2bweb.cmd;

import java.io.StringWriter;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.json.JSONArray;
import org.json.JSONObject;

import com.agilecontrol.b2b.cmd.ObjectGet;
import com.agilecontrol.nea.core.control.event.NDSEventException;
import com.agilecontrol.nea.core.velocity.VelocityUtils;
import com.agilecontrol.nea.util.Validator;
import com.agilecontrol.phone.CmdResult;
import com.agilecontrol.phone.PhoneController;

/**

 输入
{cmd:"b2b.advert.get", method,name}

method -- getGroup|getPosition|getPostionNameByGroup 获取广告组|获取广告位|获取广告组中的广告位的名称
name -- 广告组或广告位的名称


method为getGroup的输出
[{text,image,target,cat_id,items:[{item}]}]

text - string 广告位的描述
image - string 广告位图片
target - string 广告位的跳转方式 href|cat|act|pdt   target为什么就有相对的跳转值

item 每个广告的信息
{text,image,target,cat_id}
	text - string 广告的描述
	image - string 广告图片
	target - string 广告的跳转方式 href|cat|act|pdt   target为什么就有相对的跳转值
	
method为getPosition的输出
{items:{text,image,target,cat_id}}
{text,image,target,cat_id}
	text - string 广告的描述
	image - string 广告图片
	target - string 广告的跳转方式 href|cat|act|pdt   target为什么就有相对的跳转值

 * @author wu.qiong
 */
public class AdvertGet extends ObjectGet {
	
	public CmdResult execute(JSONObject jo) throws Exception {
		
		String method = jo.getString("method");
		String name = jo.getString("name"); 
		
		vc.put("marketid", usr.getMarketId());
		vc.put("uid", usr.getId());
		vc.put("langid", usr.getLangId());
		
		if(method.equals("getGroup")){
			
			JSONArray res = getGroup(name);
			CmdResult result=new CmdResult(res);
			return result;
			
		}else if(method.equals("getPosition")){
			
			JSONObject res = getPosition(name);
			CmdResult result=new CmdResult(res);
			return result;
			
		}else if(method.equals("getPostionNameByGroup")){
			
			JSONArray res = getPostionNameByGroup(name);
			CmdResult result=new CmdResult(res);
			return result;
			
		}
			
		return new CmdResult();
		
	}
	
	/**
	 * @Title:AdvertGet
	 * @Description:获取广告组的信息
	 * @param name  广告组的名字
	 * @return
	 * [{text,image,target,cat_id,items:[{item}]}]

		text - string 广告位的描述
		image - string 广告位图片
		target - string 广告位的跳转方式 href|cat|act|pdt   target为什么就有相对的跳转值

		item 每个广告的信息
		{text,image,target,cat_id}
			text - string 广告的描述
			image - string 广告图片
			target - string 广告的跳转方式 href|cat|act|pdt   target为什么就有相对的跳转值
	 * @throws Exception JSONObject
	 * @throws
	 */
	public JSONArray getGroup(String name) throws Exception {
		
		JSONArray adPositions = engine.doQueryJSONArray("select ap.name from b_adpos ap,b_adgrp ag where ap.grpid = ag.id and ag.name = ?", new Object[]{name}, conn);
		JSONArray res = new JSONArray();
		
		if(adPositions != null){
			for(int i = 0; i < adPositions.length(); i++){
				String positionName = adPositions.getString(i);
				res.put(getPosition(positionName));
			}	
		}
		
		return res;
	}
	
	public JSONObject getPosition(String name) throws Exception {
		
		vc.put("name", name);
		
		String adPosition = jedis.hget("advert:" + usr.getLangId(), "advertPosition:"+name);
		if(Validator.isNull(adPosition)){
			JSONObject jo = PhoneController.getInstance().getObjectByADSQL("b2b:advert:position", vc, conn);
			if(jo != null){
				jo = parseAdvertText(jo,"position");//解析text
			}
			
			JSONArray items = PhoneController.getInstance().getDataArrayByADSQL("b2b:advert:position:items", vc, conn, true);
			JSONArray temp = new JSONArray();
			if(items != null){
				for(int i = 0;i < items.length();i++){
					temp.put(parseAdvertText(items.getJSONObject(i),"item"));
				}
			}
			if(jo == null){
				jo = new JSONObject();
			}
			jo.put("items", temp);
			
			adPosition= jo.toString();
			jedis.hset("advert:" + usr.getLangId(), "advertPosition:"+name,adPosition);
		}
		
		return new JSONObject(adPosition);
	}
	
	public JSONArray getPostionNameByGroup(String name) throws Exception {
	
		JSONArray adPositions = engine.doQueryObjectArray("select ap.name from b_adpos ap,b_adgrp ag where ap.grpid = ag.id and ap.ISACTIVE='Y' and ag.name = ?  order by ap.ORDERNO  ", new Object[]{name}, conn);
		
		return adPositions;
		
	}
	/**
	 * @Title:parseAdvertText
	 * @Description:用velocity 解析 广告位 获取广告图的 text
	 * @param jo,type
	 * jo:{text,image,target,cat_id}
	 * type:position 广告位|item 广告图  
	 * 
	 * @return 解析好的广告的text
	 * @throws Exception String
	 * @throws
	 */
	private JSONObject parseAdvertText(JSONObject jo,String type) throws Exception {
		
		
		String target = jo.getString("target");
		int id = jo.getInt("id");
		String text = jo.getString("text");
		
		int langObjId;
		if(type.equalsIgnoreCase("position")){

			langObjId = engine.doQueryInt("select id from b_adpos_trans where posid = ? and langid = ? ", new Object[]{id,usr.getLangId() }, conn);
			
			if(langObjId != -1){
				text = engine.doQueryString("select text from b_adpos_trans where posid = ? and landid = ? ", new Object[]{id,usr.getLangId() }, conn);
			}
			
		}else if(type.equalsIgnoreCase("item")){
			
			langObjId = engine.doQueryInt("select id from b_aditem_trans where itemid =? and langid = ? ", new Object[]{id,usr.getLangId() }, conn);
			
			if(langObjId != -1){
				text = engine.doQueryString("select text from b_aditem_trans where itemid =? and langid = ? ", new Object[]{id,usr.getLangId() }, conn);
			}
		}
		
		VelocityContext parseVC = VelocityUtils.createContext();
		
		if(target.equalsIgnoreCase("pdt")){
			
			int pdtid = jo.getInt("pdt_id");
			
			
			String sql = "select value note,name no,pdtdtls dtls,imageurl mainpic,pricelist price,fabcode tags from m_product where id = ?"; 
			JSONObject pdtInfo = engine.doQueryObject(sql, new Object[]{pdtid}, conn);
			
			String note = pdtInfo.optString("note","");
			String no = pdtInfo.optString("no","");
			String mainpic = pdtInfo.optString("mainpic","");
			String price = pdtInfo.optString("price","");
			String tags = pdtInfo.optString("tags","");
			String dtls = pdtInfo.optString("dtls","");
			
			langObjId=engine.doQueryInt("select id from m_product_trans where m_product_id=? and B_LANGUAGE_ID=?", new Object[]{pdtid,usr.getLangId() }, conn);
			
			if(langObjId != -1){
				
				JSONObject pdtTrans = engine.doQueryObject("select value note,pdtdtls dtls,fabcode tags from m_product_trans where m_product_id=? and B_LANGUAGE_ID=?", new Object[]{pdtid,usr.getLangId() }, conn);
				
				note = pdtTrans.optString("note","");
				dtls = pdtTrans.optString("dtls","");
				tags = pdtTrans.optString("tags","");
				
			}
			
			parseVC.put("pdtid", pdtid);
			parseVC.put("note", note);
			parseVC.put("no", no);
			sql = "select bc.symbol from B_CURRENCY bc,b_market bm  where bm.b_currency_id = bc.id and bm.id = ? ";
			String currency = engine.doQueryString(sql, new Object[]{usr.getMarketId()}, conn);//获取货币符号 ￥ | $
			parseVC.put("currency",currency);
			parseVC.put("mainpic", mainpic);
			parseVC.put("price", price);
			parseVC.put("tags", tags);
			parseVC.put("dtls",dtls);
			
			StringWriter desc = new StringWriter();
		    Velocity.evaluate(parseVC,desc,VelocityUtils.class.getName(),text);
		    
		    jo.put("text", desc.toString());
			
		}else if(target.equalsIgnoreCase("act")){
			
			int actid = jo.getInt("act_id");
			
			String sql = "select name,description,begindate,enddate,b_market_id marketid,ad_type type,pic_url img,prmt_type prmttype from b_prmt where id = ?"; 
			JSONObject actInfo = engine.doQueryObject(sql, new Object[]{actid}, conn);
			
			parseVC.put("actid", actid);
			parseVC.put("name", actInfo.optString("name",""));
			parseVC.put("description", actInfo.optString("description",""));
			parseVC.put("begindate", actInfo.optString("begindate",""));
			parseVC.put("enddate", actInfo.optString("enddate",""));
			parseVC.put("marketid", actInfo.optString("marketid",""));
			parseVC.put("type", actInfo.optString("type",""));
			parseVC.put("img", actInfo.optString("img",""));
			parseVC.put("prmttype", actInfo.optString("prmttype",""));
			
			StringWriter desc = new StringWriter();
		    Velocity.evaluate(parseVC,desc,VelocityUtils.class.getName(),text);
		    
		    jo.put("text", desc.toString());
			
		}else if(target.equalsIgnoreCase("cat")){
			
			int catid = jo.getInt("cat_id");
			String sql = "select attribname name,imgurl pic from m_dim where id = ?";
			
			JSONObject catInfo = engine.doQueryObject(sql, new Object[]{catid}, conn);
			
			String name = catInfo.optString("name","");
			String pic = catInfo.optString("pic","");
			
			langObjId=engine.doQueryInt("select id from m_dim_trans where m_dim_id  = ? and B_LANGUAGE_ID = ?", new Object[]{catid,usr.getLangId() }, conn);
			if(langObjId != -1){
				name = engine.doQueryString("select attribname from m_dim_trans where m_dim_id = ? and b_language_id = ?", new Object[]{catid,usr.getLangId() }, conn);
			}
			
			parseVC.put("name", name);
			parseVC.put("pic", pic);
			
			StringWriter desc = new StringWriter();
		    Velocity.evaluate(parseVC,desc,VelocityUtils.class.getName(),text);
		    
		    jo.put("text", desc.toString());
		}
		return jo;
	}
	

}
