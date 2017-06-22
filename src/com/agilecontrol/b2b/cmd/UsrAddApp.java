package com.agilecontrol.b2b.cmd;

import java.io.UnsupportedEncodingException;
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
import com.agilecontrol.nea.util.RandomGen;
import com.agilecontrol.phone.Admin;
import com.agilecontrol.phone.CmdHandler;
import com.agilecontrol.phone.CmdResult;
import com.agilecontrol.phone.PhoneConfig;
import com.agilecontrol.phone.PhoneController;
import com.agilecontrol.phone.PhoneUtils;
import com.agilecontrol.phone.UUIDUtils;
import com.agilecontrol.phone.wechat.pay.util.EmojiFilter;
/**
 * APP 端新建用户
 * 根据页面传过来的手机号，判断当前数据库中是否已经存在此用户
 * 1.如果存在，判断当前用户有没有unionid
 * 	    (1)存在unionid,直接生成token
 * 		(2)不存在unionid，判断有没有cookie："unionid"
 * 			 ①存在cookie，对cookie内数据进行绑定存储，生成token
 * 			 ②不存在cookie，直接生成token
 * 2.如果不存在，判断有没有cookie："unionid"
 * 	    (1)存在cookie，对cookie内数据进行绑定存储，创建新用户和生成token
 * 	    (2)不存在cookie，创建新用户，直接生成token
 * 		
 * @author wangcun
 *
 */
@Admin(mail="wang.cun@lifecycle.cn")
public class UsrAddApp extends CmdHandler{
	String token;
	long id;
	private HttpServletRequest req;
	private HttpServletResponse res;
	@Override
	public CmdResult execute(JSONObject jo) throws Exception {
		
		req = this.event.getContext().getHttpServletRequest();
		res = this.event.getContext().getHttpServletResponse();
		String phone = jo.optString("phone");
		String userinfo = isExist();
		JSONObject one=engine.doQueryObject("select * from usr where phone=?", new Object[]{phone}, conn);
		logger.debug(one+"现在的数据");
		if(one != null){
			id = one.getLong("id");
			if(one.optString("unionid")==null && (userinfo != null && userinfo != "")){
				JSONObject jso = new JSONObject(userinfo);
				jso.put("phone", phone);
				logger.debug("从cookie中得到的用户信息"+jso);
				//登录注册修改，可以在tmp_openid中获得openid
				PhoneUtils.modifyTable("usr", jso, conn);
			}
		}else{
			//创建用户
			if(userinfo != null && userinfo != ""){
				JSONObject jso = new JSONObject(userinfo);
				jso.put("nkname", jso.getString("nkname"));
				jso.put("phone", phone);
				id=PhoneController.getInstance().getNextId("usr",conn);
				jso.put("id", id);
				jso.put("cusr", id);
				PhoneUtils.insertTable("usr", jso, conn);
				logger.debug("创建成功并绑定微信");
			}else{
				id=PhoneController.getInstance().getNextId("usr",conn);
				JSONObject jso = new JSONObject();
				jso.put("id", id);
				jso.put("phone", phone);
				jso.put("cusr", id);
				jso.put("img","http://img.1688mj.com/logo"+RandomGen.getInt(1, 100)+".jpg");
				jso.put("name", "用户"+phone);
				jso.put("city", " ");
				jso.put("prov", " ");
				jso.put("nkname", "用户"+phone);
				PhoneUtils.insertTable("usr", jso, conn);
				logger.debug("创建用户成功");
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
		Cookie cookie1=new Cookie("token", id+":"+token);
		CookieKeys.addCookie(req, res, cookie1, false, PhoneConfig.COOKIE_TIMEOUT);
		//将unionid里面的数据清空
		Cookie cookie2=new Cookie("unionid", "");
		CookieKeys.addCookie(req, res, cookie2, false, 0);
		//设置手机号cookie
		Cookie cookie3=new Cookie("phone", phone);
		CookieKeys.addCookie(req, res, cookie3, false, PhoneConfig.COOKIE_TIMEOUT);
		
		usr.put("token", id+":"+token);
		JSONObject jse = PhoneUtils.toPrimeType(TableManager.getInstance().getTable("usr"),  usr);
		logger.debug("jse="+jse);
		/*return PhoneUtils.toPrimeType(TableManager.getInstance().getTable("usr"),  usr);*/
		return new CmdResult(jse);
		
	}
	
	
	/**
	 * 判断是否存在cookie:"unionid"
	 * @throws UnsupportedEncodingException 
	 */
	private String isExist() throws UnsupportedEncodingException{
		String userinfo = null;
		Cookie cookie=CookieKeys.getCookieObj(req, "unionid");
		if(cookie!=null)userinfo= URLDecoder.decode(cookie.getValue(), "utf-8");  
		logger.debug("从cookie中得到的信息："+userinfo);
		return userinfo;
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
		
		JSONObject jo = PhoneUtils.getRedisObj("usr", userId,  conn, jedis);
		logger.debug(jo+"从数据库等到的数据");
		if(null == jo ){
			return null;
		}
		long comId = jo.optLong("com_id",0);
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
