package com.agilecontrol.b2b.cmd;

import java.net.URLDecoder;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.velocity.VelocityContext;
import org.json.JSONObject;

import com.agilecontrol.b2b.schema.TableManager;
import com.agilecontrol.nea.core.util.CookieKeys;
import com.agilecontrol.nea.core.velocity.VelocityUtils;
import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.nea.util.Validator;
import com.agilecontrol.phone.Admin;
import com.agilecontrol.phone.CmdHandler;
import com.agilecontrol.phone.CmdResult;
import com.agilecontrol.phone.LanguageManager;
import com.agilecontrol.phone.PhoneConfig;
import com.agilecontrol.phone.PhoneController;
import com.agilecontrol.phone.PhoneUtils;
import com.agilecontrol.phone.UUIDUtils;
import com.agilecontrol.phone.wechat.pay.util.EmojiFilter;
/**
 * 微信端登录/创建用户
 * 1.判断当前手机是否已经存在用户，如果已经存在判断unionid是否存在，如果同时存在 ，登录成功，如果不存在则进行更新操作
 * 2.如果不存在用户，则直接创建用户
 */
@Admin(mail="wang.cun@lifecycle.cn")
public class UsrAdd extends CmdHandler{
	String token;
	long id;
	private HttpServletRequest req;
	private HttpServletResponse res;
	@Override
	public CmdResult execute(JSONObject jo) throws Exception {
		req = this.event.getContext().getHttpServletRequest();
		res = this.event.getContext().getHttpServletResponse();
		String userinfo = null;
		Cookie cookie=CookieKeys.getCookieObj(req, "unionid");
		JSONObject jso = null;
		if(cookie!=null){
			userinfo=URLDecoder.decode(cookie.getValue(), "utf-8");
			jso = new JSONObject(userinfo);
			logger.debug("从cookie中得到的用户信息"+jso);
		}
			
		String phone = jo.optString("phone");
		jso.put("phone", phone);
		JSONObject one=engine.doQueryObject("select * from usr where phone=?", new Object[]{phone}, conn);
		if(one != null){
			if(one.optString("unionid")!= null){
				throw new NDSException("该手机号已绑定微信");
			}else{
				id=one.getLong("id");
				jso.put("id", id);
				jso.put("phone", phone);
				//wangcun 2016-4-24 10:52:55  第一次从微信进需要更新openid并清除redis
				if(Validator.isNull(one.optString("openid")) || "undefined".equals(one.getString("openid"))){
					PhoneUtils.modifyTable("usr", jso, conn);
					jedis.del("usr:"+id);
				}
				//yfzhu 2016.3.31不再更新
				//PhoneUtils.modifyTable("usr", obj, conn);
				//yfzhu 2016.5.11 需要更新潜在推荐人，如果附带的话
				long ptre= jso.optLong("pt_recom_u_id", -1);
				if(one.optLong("pt_recom_u_id", -1)!= ptre && ptre>0){
					JSONObject recobj=new JSONObject();
					recobj.put("id", id);
					recobj.put("pt_recom_u_id",ptre );
					PhoneUtils.modifyTable("usr", recobj, conn);
				}
			}
		}else{
			//创建用户
			id=PhoneController.getInstance().getNextId("usr",conn);
			jso.put("id", id);
			jso.put("cusr", id);
			PhoneUtils.insertTable("usr", jso, conn);
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
		Cookie cookie1=new Cookie("token", id+":"+token);
		CookieKeys.addCookie(req, res, cookie1, false, PhoneConfig.COOKIE_TIMEOUT);
		//将unionid里面的数据清空
		Cookie cookie2=new Cookie("unionid", "");
		CookieKeys.addCookie(req, res, cookie2, false, PhoneConfig.COOKIE_TIMEOUT);
		
		usr.put("token", id+":"+token);
		JSONObject jse = PhoneUtils.toPrimeType(TableManager.getInstance().getTable("usr"),  usr);
		logger.debug("jse="+jse);
		/*return PhoneUtils.toPrimeType(TableManager.getInstance().getTable("usr"),  usr);*/
		return new CmdResult(jse);
		
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
		
		JSONObject jo=PhoneUtils.getRedisObj("usr", userId,  conn, jedis);
		long comId=jo.optLong("com_id",0);
		if(comId>0){
			//客户可能还没有对应com
			vc.put("comid", comId);
			long storeId=jo.optLong("st_id", 0);
			if(storeId==0) throw new NDSException("异常:comId="+comId+"但storeId=0(usrid="+userId);
			long empId=jo.optLong("emp_id",0);
			if(empId==0) throw new NDSException("异常:comId="+comId+"但empId=0(usrid="+userId);
			
			vc.put("stid", storeId);
			jo.put("com", PhoneUtils.getRedisObj("com", comId,  conn, jedis));
			jo.put("st",PhoneUtils.getRedisObj("st", storeId,  conn, jedis));
			//emp
			jo.put("emp", PhoneUtils.getRedisObj("emp", empId,  conn, jedis));
		}else{
			//推荐人
			long recom_u_id=jo.optLong("pt_recom_u_id",0);
			if(recom_u_id>0){
				//load from cache
				JSONObject recomUser=PhoneUtils.getRedisObj("usr", recom_u_id,  conn, jedis);
				//必定有com
				long  recom_com_id=recomUser.optLong("com_id",0);
				if(recom_com_id==0) throw new NDSException("推荐人的商家不存在，数据有误！");
				JSONObject recomUserCom=PhoneUtils.getRedisObj("com", recom_com_id,  conn, jedis);
				recomUserCom.put("unkname", recomUser.optString("nkname"));
				jo.put("pt_recom_u_id", recomUserCom);
			}
		}
		
		return jo;
	}

	@Override
	public boolean allowGuest() {
		return true;
	}
}
