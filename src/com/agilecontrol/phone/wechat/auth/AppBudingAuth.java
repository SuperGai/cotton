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
 * ���ڲ��Ե���Ҫ����΢����֤��������֤����һ����������
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
	private Jedis jedis;//����ر�
	public AppBudingAuth(Jedis jedis,Connection conn, HttpServletRequest req, HttpServletResponse res) throws Exception {
		this.jedis=jedis;
		this.conn=conn;
		this.req=req;
		this.res=res;
	}
	
	/*** 
	 * ���ݴ�΢�Ż����Ϣ�����±������ݿ⣬Ȼ�����½���redis���棬��ˢ��cookie��token��Ϣ
	 * 
	 * ������cookie�����ã� cookie: "token=user:<userid>:<token>"
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
	 * {id, nickname,imageurl, token, com:{id, name}, store:{id, name}, emp:{id,role}} M- Manager���� D-Default��Ա
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
			//΢�Žӿ��ǲ�����ģ����Ϊ�գ���ʾ��ͨ�����Խӿڴ������ģ����ø������û�������
			obj.put("app_openid", json.optString("openid"));
			//wangcun ����ǳ����룬��ʱ��������ˣ�����ǳ�ȫ���б�����ɣ�����ϵͳ�Զ�����һ���ǳ� 2016-7-16 15:42:54
			//obj.put("nkname", nickName);
			obj.put("nkname", EmojiFilter.foFilter(nickName));
			obj.put("sex", json.optString("sex"));
			obj.put("prov", json.optString("province"));
			obj.put("city", json.optString("city"));
			obj.put("ctry", json.optString("country"));
			//�û�΢�����û��ͷ�񣬸��ͻ���һ��Ĭ��ͷ��
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
		
		//��ȡ�û���cookie�����������join����cookie��Ĭ����DESUtil.encrypt($uid+","+$comid)��ʽ�������uid��comid�����Ƽ��˵ģ���Ҫ���õ�Ǳ���Ƽ�����
		addRecommender(json, obj);
		logger.debug("obj:"+ obj+", one:"+ one+", json:"+ json);
		if(one!=null){
			//�ж��Ƿ���ֻ���
			Cookie cookie=CookieKeys.getCookieObj(req, "phone");
			String phoneIn = null;
			if(cookie!=null){
				phoneIn=cookie.getValue();
				if((!one.optString("phone").equals(phoneIn) && one.optString("phone").length()==11 )&& !"".equals(phoneIn)){
					//�ж����ֱ�ӽ�cookieɾ��
					Cookie cookiePhone=new Cookie("phone","");
					CookieKeys.addCookie(req, res, cookiePhone, false, 1);
					throw new NDSException("��΢���Ѱ��ֻ�");
				}
			}
			
			id=one.getLong("id");
			obj.put("id", id);
			//wangcun 2016-4-24 10:52:55  ��һ�δ�App����Ҫ����app_openid�����redis
			if(null == one.getString("app_openid") || "undefined".equals(one.getString("app_openid"))){
				PhoneUtils.modifyTable("usr", obj, conn);
				jedis.del("usr:"+id);
			}
			//yfzhu 2016.3.31���ٸ���
			//PhoneUtils.modifyTable("usr", obj, conn);
			//yfzhu 2016.5.11 ��Ҫ����Ǳ���Ƽ��ˣ���������Ļ�
			//wangcun 2016-7-14 16:07:40 ֻ�����Ƽ���Ϊ�յ�ʱ����£�Ǳ���Ƽ��ˣ�����Ѿ������Ƽ��ˣ���ôǱ���Ƽ����ڸ���
			long ptre= obj.optLong("pt_recom_u_id", -1);
			if(one.optLong("pt_recom_u_id", -1)!= ptre && ptre>0 && -1 == obj.optLong("recom_u_id", -1)){
				JSONObject recobj=new JSONObject();
				recobj.put("id", id);
				recobj.put("pt_recom_u_id",ptre );
				//����Ǳ���Ƽ��˵�ʱ��һ�����mtime
				recobj.put("mtime", new Date());
				PhoneUtils.modifyTable("usr", recobj, conn);
			}
		}else{
			//create a new user
			/*id=PhoneController.getInstance().getNextId("usr",conn);
			obj.put("id", id);
			obj.put("cusr", id);
			
			*//**
			 * û�����壬����ע�͵� 2016-4-14 21:43:15 wang.cun
			 *//*
			if(Validator.isNull(nickName)){
				obj.put("nkname", openid);
			}
			//�����û�
			PhoneUtils.insertTable("usr", obj, conn);*/
			
			//�жϸ��ֻ��Ƿ񴴽��û�������Ѿ���������ô�����Ƹ�����Ϣ�����û�д���������cookie
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
			//��redis��ȡ�û���Ϣ
			key="usr:"+id+":"+token;
			if(jedis.hsetnx(key, "remoteip", req.getRemoteAddr())==0)continue;
			jedis.hsetnx(key, "agent", req.getHeader("User-Agent"));
			break;
		}
		jedis.expire(key,  PhoneConfig.COOKIE_TIMEOUT);
		
		
		//����cookie
		Cookie cookie=new Cookie("token", id+":"+token);
		CookieKeys.addCookie(req, res, cookie, false, PhoneConfig.COOKIE_TIMEOUT);
		
		usr.put("token", id+":"+token);
		JSONObject jse = PhoneUtils.toPrimeType(TableManager.getInstance().getTable("usr"),  usr);
		
		/*return PhoneUtils.toPrimeType(TableManager.getInstance().getTable("usr"),  usr);*/
		return jse;
	}
	
	/**
	 * �����û���Ҫ���ֻ������ô˷���
	 * @throws UnsupportedEncodingException 
	 * @throws JSONException 
	 */
	private JSONObject  BoundPhone(JSONObject obj) throws Exception{
		Cookie cookie=new Cookie("unionid",URLEncoder.encode(obj.toString(), "UTF-8"));
		logger.debug("�ҵ�cookie"+URLEncoder.encode(obj.toString(), "UTF-8"));
		CookieKeys.addCookie(req, res, cookie, false, PhoneConfig.COOKIE_TIMEOUT);
		JSONObject jse = new JSONObject();
		jse.put("usr", "");
		return jse;
	}
	
	/**
	 * ��ȡjoin name cookie��������ڣ�Ĭ��������: 
	 * DESUtil.encrypt($uid+","+$comid)��ʽ�������uid��comid�����Ƽ��˵ģ���Ҫ���õ�Ǳ���Ƽ�����
	 * @param json
	 * @throws Exception
	 */
	private void addRecommender(JSONObject json, JSONObject obj) throws Exception{
		String query=null;
		Cookie cookie=CookieKeys.getCookieObj(req, "join");
		if(cookie!=null)query=cookie.getValue();
		logger.debug("addRecommender cookie:"+ query+", obj="+ json);
		if(Validator.isNull(query) ){
			//���Դ�obj��joincode��ȡ
			query=json.optString("joincode");
		}
		if(Validator.isNull(query) ){
			return;
		}
		int idx=query.indexOf("&");if(idx>0)query=query.substring(0, idx);//remove �����Ĳ���
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
	 * �ͻ�������Ҫ�����ݣ���ʽ: {id, nickname,imageurl, token, com:{id, name}, store:{id, name}}
	 * @param userId
	 * @return {id, nickname,imageurl, token, role, com:{id, name}, store:{id, name}, recom_u_id:{unkname --user nick name--, id, name(com attributes)}}
	 * ���com_id��0����ʾ��ǰ�û���δ����/�����̼ң���Ҫ�����Ƽ��˵��̼���ʾ, ���recom_u_idΪ�գ���ʾû���Ƽ���
	 * @throws Exception
	 */
	private JSONObject constructUserObj(long userId) throws Exception{
		VelocityContext vc = VelocityUtils.createContext();
		vc.put("uid", userId);
		
		JSONObject jo=PhoneUtils.getRedisObj("usr", userId, conn, jedis);
		long comId=jo.optLong("com_id",0);
		if(comId>0){
			//�ͻ����ܻ�û�ж�Ӧcom
			vc.put("comid", comId);
			long storeId=jo.optLong("st_id", 0);
			if(storeId==0) throw new NDSException("�쳣:comId="+comId+"��storeId=0(usrid="+userId);
			long empId=jo.optLong("emp_id",0);
			if(empId==0) throw new NDSException("�쳣:comId="+comId+"��empId=0(usrid="+userId);
			
			vc.put("stid", storeId);
			jo.put("com", PhoneUtils.getRedisObj("com", comId, conn, jedis));
			jo.put("st",PhoneUtils.getRedisObj("st", storeId,  conn, jedis));
			//emp
			jo.put("emp", PhoneUtils.getRedisObj("emp", empId,  conn, jedis));
		}else{
			//�Ƽ���
			long recom_u_id=jo.optLong("pt_recom_u_id",0);
			if(recom_u_id>0){
				//load from cache
				JSONObject recomUser=PhoneUtils.getRedisObj("usr", recom_u_id, conn, jedis);
				//�ض���com
				long  recom_com_id=recomUser.optLong("com_id",0);
				if(recom_com_id==0) throw new NDSException("�Ƽ��˵��̼Ҳ����ڣ���������");
				JSONObject recomUserCom=PhoneUtils.getRedisObj("com", recom_com_id, conn, jedis);
				recomUserCom.put("unkname", recomUser.optString("nkname"));
				jo.put("pt_recom_u_id", recomUserCom);
			}
		}
		
		return jo;
	}
	
	
}
