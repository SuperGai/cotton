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
 * APP ���½��û�
 * ����ҳ�洫�������ֻ��ţ��жϵ�ǰ���ݿ����Ƿ��Ѿ����ڴ��û�
 * 1.������ڣ��жϵ�ǰ�û���û��unionid
 * 	    (1)����unionid,ֱ������token
 * 		(2)������unionid���ж���û��cookie��"unionid"
 * 			 �ٴ���cookie����cookie�����ݽ��а󶨴洢������token
 * 			 �ڲ�����cookie��ֱ������token
 * 2.��������ڣ��ж���û��cookie��"unionid"
 * 	    (1)����cookie����cookie�����ݽ��а󶨴洢���������û�������token
 * 	    (2)������cookie���������û���ֱ������token
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
		logger.debug(one+"���ڵ�����");
		if(one != null){
			id = one.getLong("id");
			if(one.optString("unionid")==null && (userinfo != null && userinfo != "")){
				JSONObject jso = new JSONObject(userinfo);
				jso.put("phone", phone);
				logger.debug("��cookie�еõ����û���Ϣ"+jso);
				//��¼ע���޸ģ�������tmp_openid�л��openid
				PhoneUtils.modifyTable("usr", jso, conn);
			}
		}else{
			//�����û�
			if(userinfo != null && userinfo != ""){
				JSONObject jso = new JSONObject(userinfo);
				jso.put("nkname", jso.getString("nkname"));
				jso.put("phone", phone);
				id=PhoneController.getInstance().getNextId("usr",conn);
				jso.put("id", id);
				jso.put("cusr", id);
				PhoneUtils.insertTable("usr", jso, conn);
				logger.debug("�����ɹ�����΢��");
			}else{
				id=PhoneController.getInstance().getNextId("usr",conn);
				JSONObject jso = new JSONObject();
				jso.put("id", id);
				jso.put("phone", phone);
				jso.put("cusr", id);
				jso.put("img","http://img.1688mj.com/logo"+RandomGen.getInt(1, 100)+".jpg");
				jso.put("name", "�û�"+phone);
				jso.put("city", " ");
				jso.put("prov", " ");
				jso.put("nkname", "�û�"+phone);
				PhoneUtils.insertTable("usr", jso, conn);
				logger.debug("�����û��ɹ�");
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
		Cookie cookie1=new Cookie("token", id+":"+token);
		CookieKeys.addCookie(req, res, cookie1, false, PhoneConfig.COOKIE_TIMEOUT);
		//��unionid������������
		Cookie cookie2=new Cookie("unionid", "");
		CookieKeys.addCookie(req, res, cookie2, false, 0);
		//�����ֻ���cookie
		Cookie cookie3=new Cookie("phone", phone);
		CookieKeys.addCookie(req, res, cookie3, false, PhoneConfig.COOKIE_TIMEOUT);
		
		usr.put("token", id+":"+token);
		JSONObject jse = PhoneUtils.toPrimeType(TableManager.getInstance().getTable("usr"),  usr);
		logger.debug("jse="+jse);
		/*return PhoneUtils.toPrimeType(TableManager.getInstance().getTable("usr"),  usr);*/
		return new CmdResult(jse);
		
	}
	
	
	/**
	 * �ж��Ƿ����cookie:"unionid"
	 * @throws UnsupportedEncodingException 
	 */
	private String isExist() throws UnsupportedEncodingException{
		String userinfo = null;
		Cookie cookie=CookieKeys.getCookieObj(req, "unionid");
		if(cookie!=null)userinfo= URLDecoder.decode(cookie.getValue(), "utf-8");  
		logger.debug("��cookie�еõ�����Ϣ��"+userinfo);
		return userinfo;
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
		
		JSONObject jo = PhoneUtils.getRedisObj("usr", userId,  conn, jedis);
		logger.debug(jo+"�����ݿ�ȵ�������");
		if(null == jo ){
			return null;
		}
		long comId = jo.optLong("com_id",0);
		if(comId>0){
			//�ͻ����ܻ�û�ж�Ӧcom
			vc.put("comid", comId);
			long storeId=jo.optLong("st_id", 0);
			if(storeId==0) throw new NDSException("�쳣:comId="+comId+"��storeId=0(usrid="+userId);
			long empId=jo.optLong("emp_id",0);
			if(empId==0) throw new NDSException("�쳣:comId="+comId+"��empId=0(usrid="+userId);
			
			vc.put("stid", storeId);
			jo.put("com", PhoneUtils.getRedisObj("com", comId,  conn, jedis));
			jo.put("st",PhoneUtils.getRedisObj("st", storeId,  conn, jedis));
			//emp
			jo.put("emp", PhoneUtils.getRedisObj("emp", empId,  conn, jedis));
		}else{
			//�Ƽ���
			long recom_u_id=jo.optLong("pt_recom_u_id",0);
			if(recom_u_id>0){
				//load from cache
				JSONObject recomUser=PhoneUtils.getRedisObj("usr", recom_u_id,  conn, jedis);
				//�ض���com
				long  recom_com_id=recomUser.optLong("com_id",0);
				if(recom_com_id==0) throw new NDSException("�Ƽ��˵��̼Ҳ����ڣ���������");
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
