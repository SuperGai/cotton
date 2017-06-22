package com.agilecontrol.phone.wechat.auth;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.velocity.VelocityContext;
import org.bouncycastle.util.encoders.UrlBase64;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;

import com.agilecontrol.b2b.schema.TableManager;
import com.agilecontrol.nea.core.control.web.WebUtils;
import com.agilecontrol.nea.core.query.QueryEngine;
import com.agilecontrol.nea.core.security.DESUtil;
import com.agilecontrol.nea.core.security.auth.AuthException;
import com.agilecontrol.nea.core.util.ConfigValues;
import com.agilecontrol.nea.core.util.CookieKeys;
import com.agilecontrol.nea.core.velocity.VelocityUtils;
import com.agilecontrol.nea.util.Base64;
import com.agilecontrol.nea.util.MD5Sum;
import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.nea.util.RandomGen;
import com.agilecontrol.nea.util.Tools;
import com.agilecontrol.nea.util.Validator;
import com.agilecontrol.phone.Admin;
import com.agilecontrol.phone.CmdResult;
import com.agilecontrol.phone.PhoneConfig;
import com.agilecontrol.phone.PhoneController;
import com.agilecontrol.phone.PhoneUtils;
import com.agilecontrol.phone.UUIDUtils;
import com.agilecontrol.phone.wechat.pay.util.EmojiFilter;
import com.qiniu.util.Json;

/**
 * 由于测试的需要，将微信认证和网络认证合在一个类中设置
 * 
 * @author yfzhu
 *
 */
@Admin(mail="wang.cun@lifecycle.cn")
public class AppBudingAuth {
	private static final Logger logger = LoggerFactory.getLogger(AppBudingAuth.class);
	private Connection conn;
	private HttpServletRequest req;
	private HttpServletResponse res;
	private Jedis jedis;//无需关闭
	public AppBudingAuth(Jedis jedis,Connection conn, HttpServletRequest req, HttpServletResponse res) throws Exception {
		this.jedis=jedis;
		this.conn=conn;
		this.req=req;
		this.res=res;
	}
	
	/*** 
	 * 根据从微信获得信息，更新本地数据库，然后重新建立redis缓存，并刷新cookie的token信息
	 * 
	 * 将进行cookie的设置， cookie: "token=user:<userid>:<token>"
	 * @param json {
	 *     "openid": "OPENID",
	 *     "scope": "SCOPE",
	 *     "unionid": "UNIONID",
	 *     // If scope eq snsapi_userinfo, the following will be included
	 *     "lang": "LANG",
	 *     "nickname": "NICKNAME",
	 *     "sex": "SEX",
	 *     "province": "PROVINCE",
	 *     "city": "CITY",
	 *     "country": "COUNTRY",
	 *     "headimgurl": "HEADIMGURL"
	 *     "privilege": "PRIVILEGE"
	 *     joincode: querystring
	 * }
	 * 
	 * @return JSONObject 
	 * {id, nickname,imageurl, token, com:{id, name}, store:{id, name}, emp:{id,role}} M- Manager经理 D-Default店员
	 * @throws AuthException if register failed
	 */
	public JSONObject accept(JSONObject json) throws Exception {
		QueryEngine engine=QueryEngine.getInstance();
		String unionid=json.getString("unionid");
		JSONObject one=engine.doQueryObject("select * from usr where unionid=?", new Object[]{unionid}, conn);

		String token;
		long id; 
		
		
		
		//this is to save to db
		JSONObject obj=new JSONObject();
		obj.put("unionid", unionid);
			
		String nickName = json.optString("nickname");
		if(Validator.isNotNull(nickName)){
			//微信接口是不允许的，如果为空，表示是通过测试接口传过来的，不用更新现用户的内容
			obj.put("app_openid", json.optString("openid"));
			//wangcun 解决昵称乱码，暂时将乱码过滤，如果昵称全部有表情组成，将由系统自动生成一个昵称 2016-7-16 15:42:54
			//obj.put("nkname", nickName);
			obj.put("nkname", EmojiFilter.foFilter(nickName));
			obj.put("sex", json.optString("sex"));
			obj.put("prov", json.optString("province"));
			obj.put("city", json.optString("city"));
			obj.put("ctry", json.optString("country"));
			//用户微信如果没有头像，给客户加一个默认头像
			String headimgurl = json.optString("headimgurl");
			if(Validator.isNull(headimgurl)){
				obj.put("img", "http://img.1688mj.com/avatar"+RandomGen.getInt(1, 100) +".jpg");
			}
			obj.put("img", json.optString("headimgurl"));
			obj.put("name", EmojiFilter.foFilter(nickName));
			//fix by cmq  2016-5-25 11:57:11
			obj.put("nk_temp", URLEncoder.encode(nickName, "UTF-8"));
			
			String phone=json.optString("phone");
			if(Validator.isNotNull(phone)){
				obj.put("phone",phone);
			}
		}
		
		//读取用户的cookie，如果其中有join名的cookie，默认是DESUtil.encrypt($uid+","+$comid)格式，这里的uid和comid都是推荐人的，需要设置到潜在推荐人列
		addRecommender(json, obj);
		logger.debug("obj:"+ obj+", one:"+ one+", json:"+ json);
		if(one!=null){
			//判断是否绑定手机号
			Cookie cookie=CookieKeys.getCookieObj(req, "phone");
			String phoneIn = null;
			if(cookie!=null){
				phoneIn=cookie.getValue();
				if((!one.optString("phone").equals(phoneIn) && one.optString("phone").length()==11 )&& !"".equals(phoneIn)){
					//判断完成直接将cookie删除
					Cookie cookiePhone=new Cookie("phone","");
					CookieKeys.addCookie(req, res, cookiePhone, false, 1);
					throw new NDSException("该微信已绑定手机");
				}
			}
			
			id=one.getLong("id");
			obj.put("id", id);
			//wangcun 2016-4-24 10:52:55  第一次从App进需要更新app_openid并清除redis
			if(null == one.getString("app_openid") || "undefined".equals(one.getString("app_openid"))){
				PhoneUtils.modifyTable("usr", obj, conn);
				jedis.del("usr:"+id);
			}
			//yfzhu 2016.3.31不再更新
			//PhoneUtils.modifyTable("usr", obj, conn);
			//yfzhu 2016.5.11 需要更新潜在推荐人，如果附带的话
			//wangcun 2016-7-14 16:07:40 只有在推荐人为空的时候更新，潜在推荐人，如果已经有了推荐人，那么潜在推荐不在更新
			long ptre= obj.optLong("pt_recom_u_id", -1);
			if(one.optLong("pt_recom_u_id", -1)!= ptre && ptre>0 && -1 == obj.optLong("recom_u_id", -1)){
				JSONObject recobj=new JSONObject();
				recobj.put("id", id);
				recobj.put("pt_recom_u_id",ptre );
				//更新潜在推荐人的时候一起更新mtime
				recobj.put("mtime", new Date());
				PhoneUtils.modifyTable("usr", recobj, conn);
			}
		}else{
			//create a new user
			/*id=PhoneController.getInstance().getNextId("usr",conn);
			obj.put("id", id);
			obj.put("cusr", id);
			
			*//**
			 * 没有意义，所以注释掉 2016-4-14 21:43:15 wang.cun
			 *//*
			if(Validator.isNull(nickName)){
				obj.put("nkname", openid);
			}
			//创建用户
			PhoneUtils.insertTable("usr", obj, conn);*/
			
			//判断该手机是否创建用户，如果已经创建，那么就完善个人信息，如果没有创建，设置cookie
			Cookie cookiePhone=CookieKeys.getCookieObj(req, "phone");
			Cookie cookieToken=CookieKeys.getCookieObj(req, "token");
			String phoneIn = null;
			if(cookiePhone!=null &&cookieToken!=null){
				phoneIn=cookiePhone.getValue();
				JSONObject two=engine.doQueryObject("select * from usr where phone=?", new Object[]{phoneIn}, conn);
				if(two !=null){
					id=two.getLong("id");
					obj.put("id", id);
					obj.put("phone", phoneIn);
					PhoneUtils.modifyTable("usr", obj, conn);
					jedis.del("usr:"+id);
				}else{
					return BoundPhone(obj);
				}											
			}else{
				return BoundPhone(obj);
			}	
		}
		
		JSONObject usr=constructUserObj(id);
		String key=null;
		while(true){
			token=UUIDUtils.compressedUuid();//   PhoneUtils.createDistinctUUID("usr:"+id+":{0}");
			//从redis获取用户信息
			key="usr:"+id+":"+token;
			if(jedis.hsetnx(key, "remoteip", req.getRemoteAddr())==0)continue;
			jedis.hsetnx(key, "agent", req.getHeader("User-Agent"));
			break;
		}
		jedis.expire(key,  PhoneConfig.COOKIE_TIMEOUT);
		
		
		//设置cookie
		Cookie cookie=new Cookie("token", id+":"+token);
		CookieKeys.addCookie(req, res, cookie, false, PhoneConfig.COOKIE_TIMEOUT);
		
		usr.put("token", id+":"+token);
		JSONObject jse = PhoneUtils.toPrimeType(TableManager.getInstance().getTable("usr"),  usr);
		
		/*return PhoneUtils.toPrimeType(TableManager.getInstance().getTable("usr"),  usr);*/
		return jse;
	}
	
	/**
	 * 发现用户需要绑定手机，调用此方法
	 * @throws UnsupportedEncodingException 
	 * @throws JSONException 
	 */
	private JSONObject  BoundPhone(JSONObject obj) throws Exception{
		Cookie cookie=new Cookie("unionid",URLEncoder.encode(obj.toString(), "UTF-8"));
		logger.debug("我的cookie"+URLEncoder.encode(obj.toString(), "UTF-8"));
		CookieKeys.addCookie(req, res, cookie, false, PhoneConfig.COOKIE_TIMEOUT);
		JSONObject jse = new JSONObject();
		jse.put("usr", "");
		return jse;
	}
	
	/**
	 * 读取join name cookie，如果存在，默认内容是: 
	 * DESUtil.encrypt($uid+","+$comid)格式，这里的uid和comid都是推荐人的，需要设置到潜在推荐人列
	 * @param json
	 * @throws Exception
	 */
	private void addRecommender(JSONObject json, JSONObject obj) throws Exception{
		String query=null;
		Cookie cookie=CookieKeys.getCookieObj(req, "join");
		if(cookie!=null)query=cookie.getValue();
		logger.debug("addRecommender cookie:"+ query+", obj="+ json);
		if(Validator.isNull(query) ){
			//尝试从obj的joincode获取
			query=json.optString("joincode");
		}
		if(Validator.isNull(query) ){
			return;
		}
		int idx=query.indexOf("&");if(idx>0)query=query.substring(0, idx);//remove 其他的部分
		try{
			String[] val=DESUtil.decrypt(query, ConfigValues.SECURITY_SIMPLE_ENCRYPT_KEY).split(",");
			long ptrecId= Tools.getLong(val[0], -1);
			if(ptrecId>0){
				obj.put("pt_recom_u_id", ptrecId);
				logger.debug("add pt_recom_u_id="+ ptrecId+" to usr id="+ json.optLong("id"));
			}
		}catch(Throwable tx){
			logger.error("Fail to decrypt join cookie:"+ query, tx);
			
		}

	}
	/**
	 * 客户端所需要的内容，格式: {id, nickname,imageurl, token, com:{id, name}, store:{id, name}}
	 * @param userId
	 * @return {id, nickname,imageurl, token, role, com:{id, name}, store:{id, name}, recom_u_id:{unkname --user nick name--, id, name(com attributes)}}
	 * 如果com_id是0，表示当前用户尚未创建/加入商家，需要考虑推荐人的商家显示, 如果recom_u_id为空，表示没有推荐人
	 * @throws Exception
	 */
	private JSONObject constructUserObj(long userId) throws Exception{
		VelocityContext vc = VelocityUtils.createContext();
		vc.put("uid", userId);
		
		JSONObject jo=PhoneUtils.getRedisObj("usr", userId, conn, jedis);
		long comId=jo.optLong("com_id",0);
		if(comId>0){
			//客户可能还没有对应com
			vc.put("comid", comId);
			long storeId=jo.optLong("st_id", 0);
			if(storeId==0) throw new NDSException("异常:comId="+comId+"但storeId=0(usrid="+userId);
			long empId=jo.optLong("emp_id",0);
			if(empId==0) throw new NDSException("异常:comId="+comId+"但empId=0(usrid="+userId);
			
			vc.put("stid", storeId);
			jo.put("com", PhoneUtils.getRedisObj("com", comId, conn, jedis));
			jo.put("st",PhoneUtils.getRedisObj("st", storeId,  conn, jedis));
			//emp
			jo.put("emp", PhoneUtils.getRedisObj("emp", empId,  conn, jedis));
		}else{
			//推荐人
			long recom_u_id=jo.optLong("pt_recom_u_id",0);
			if(recom_u_id>0){
				//load from cache
				JSONObject recomUser=PhoneUtils.getRedisObj("usr", recom_u_id, conn, jedis);
				//必定有com
				long  recom_com_id=recomUser.optLong("com_id",0);
				if(recom_com_id==0) throw new NDSException("推荐人的商家不存在，数据有误！");
				JSONObject recomUserCom=PhoneUtils.getRedisObj("com", recom_com_id, conn, jedis);
				recomUserCom.put("unkname", recomUser.optString("nkname"));
				jo.put("pt_recom_u_id", recomUserCom);
			}
		}
		
		return jo;
	}
	
	
}
