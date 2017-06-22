package com.agilecontrol.phone;

import org.json.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.agilecontrol.nea.core.schema.ClientManager;
import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.nea.util.Validator;

/**
 * 存储redis的"usr:<usr_id>:<token>" 的内容，String->Json
 * 
 * {id,phone,nickname,openid,headimgurl}
 * 
 * name 目前对于openid
 * @author yfzhu
 *
 */
@Admin(mail="yfzhu@lifecycle.cn")
public class UserObj {
	private static final Logger logger = LoggerFactory.getLogger(UserObj.class);	
	
	private long id;
	private String name;
	private String truename;
	/**
	 * 微信的id
	 */
	private String openid;
	/**
	 * 微信的unionid
	 */
	private String unionid;
	
	/**
	 * 部分类需要通过token来清除redis的临时数据，比如usr:$token
	 */
	private String token;
	/**
	 * 当前所用语言, 
	 */
	private int langId;
	/**
	 * 当前所在市场
	 */
	private int marketId;
	
	/**
	 * 
	 * @param jo {id, phone, nkname, openid, st_id,com_id,emp_id, lang_id,mkt_id}
	 */
	public UserObj(JSONObject jo) throws NDSException{
		id=jo.optLong("id",0);
		name=jo.optString("name");
		truename=jo.optString("truename");
		openid=jo.optString("openid");
		unionid=jo.optString("unionid");
		
		if(id==0) throw new NDSException("UserObj的id未设置");
		
		langId=jo.optInt("lang_id", LanguageManager.getInstance().getDefaultLangId());
		marketId=jo.optInt("mkt_id", 0);
		
	}
	
	
	/**
	 * login 生成的token
	 * @return
	 */
	public String getToken(){
		return token;
	}
	/**
	 * 设置登录的cookie token, format: "$id:$randomuuid", 举例 "893:QRaK34c4QS-xnxCpzUCTAg"
	 * @param token
	 */
	public void setToken(String token){
		this.token=token;
	}
	
	/**
	 * 
	 * @return {id, phone, nkname, openid, st_id,com_id,emp_id}
	 * @throws JSONException
	 */
	public JSONObject toJSON() throws JSONException{
		JSONObject jo=new JSONObject();
		jo.put("id", id);
		jo.put("name", name);
		jo.put("truename",truename);
		jo.put("openid", openid);
		jo.put("unionid", unionid);

		jo.put("lang_id", this.langId);
		jo.put("mkt_id", this.marketId);
		
		return jo;
	}
	
	public String toString(){
		try{
			JSONObject jo=toJSON();
			return jo.toString();
		}catch(Exception ex){
			logger.error("Fail to parse jo:", ex);
			return "";
		}
	}
	/**
	 * b_market.id
	 * @return
	 */
	public int getMarketId(){
		return marketId;
	}
	
	/**
	 * 设置b_market.id
	 * @param mktId
	 */
	public void setMarketId(int mktId){
		this.marketId=mktId;
	}
	/**
	 * b_language.id
	 * @return
	 */
	public int getLangId(){
		return langId;
	}
	/**
	 * 设置 b_language.id
	 * @param langId
	 */
	public void setLangId(int langId){
		this.langId=langId;
	}
	
	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	/**
	 * @return
	 */
	public String getTrueName() {
		return truename;
	}
	/**
	 * @return
	 */
	public void setTrueName(String s) {
		truename=s;
	}


	public String getOpenId() {
		return openid;
	}

	public void setOpenId(String openid) {
		this.openid = openid;
	}
	
	public String getUnionid() {
		return unionid;
	}
	
	public void setUnionid(String unionid) {
		this.unionid = unionid;
	}


	/**
	 * always ClientManager.getInstance().getDefaultClientId()
	 * @return the comId
	 */
	public long getComId() {
		return ClientManager.getInstance().getDefaultClientId();
	}

	/**
	 * @param comId the comId to set
	 */
	public void setComId(long comId) {
		//null
		
	}


}
