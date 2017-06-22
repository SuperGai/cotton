package com.agilecontrol.b2b.cmd;

import java.util.ArrayList;
import java.util.Enumeration;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.velocity.VelocityContext;
import org.json.JSONArray;
import org.json.JSONObject;

import com.agilecontrol.nea.core.util.ConfigValues;
import com.agilecontrol.nea.core.util.CookieKeys;
import com.agilecontrol.nea.core.velocity.VelocityUtils;
import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.nea.util.Tools;
import com.agilecontrol.nea.util.Validator;
import com.agilecontrol.phone.Admin;
import com.agilecontrol.phone.CmdHandler;
import com.agilecontrol.phone.CmdResult;
import com.agilecontrol.phone.PhoneConfig;
import com.agilecontrol.phone.PhoneController;
import com.agilecontrol.phone.PhoneUtils;
import com.agilecontrol.phone.UUIDUtils;
import com.agilecontrol.phone.YXController;
/**
 * …Ë÷√‘∆–≈µƒcookie£∫
 * 
uid - yxid.tolowercase()
sdktoken - yxpwd 
 * @author yfzhu
 *
 */
@Admin(mail="chen.mengqi@lifecycle.cn")
public class YunxinCookieSet extends CmdHandler{
	/**
	 * Guest can execute this task, default to false
	 * 
	 * @return
	 */
	public boolean allowGuest() {
		return true;
	}
	@Override
	public CmdResult execute(JSONObject jo) throws Exception {
		String uid_token=null;
		Cookie cookie=CookieKeys.getCookieObj(event.getContext().getHttpServletRequest(), "token");
		if(cookie!=null)uid_token=cookie.getValue();
		else {
			logger.warn("not found token cookie in YunxinCookieSet");
			return CmdResult.SUCCESS;
		
		}
		long userId=Tools.getLong(uid_token.split(":")[0], 0);
		

		VelocityContext vc = VelocityUtils.createContext();
		vc.put("uid", userId);
		JSONObject usrjo=PhoneUtils.getRedisObj("usr", userId,  conn, jedis);
		long empId=usrjo.optLong("emp_id", -1);
		if(empId<=0){
			logger.warn("not found emp_id of token "+ uid_token +" in YunxinCookieSet");
			return CmdResult.SUCCESS;
		}
		JSONObject empObj=PhoneUtils.getRedisObj("emp", empId,  conn, jedis);
		String yxid= empObj.optString("yxid");
		String yxpwd=empObj.optString("yxpwd");
		
		if(Validator.isNotNull(yxid) && Validator.isNotNull(yxpwd)){
			HttpServletRequest request=event.getContext().getHttpServletRequest();
			HttpServletResponse response=event.getContext().getHttpServletResponse();
			//long timeout= ConfigValues.get("phone.yunxin_cookie_timeout", 7*24*3600);
			cookie=new Cookie("uid",yxid.toLowerCase());
			CookieKeys.addCookie(request, response, cookie, false,  PhoneConfig.COOKIE_TIMEOUT);
			cookie=new Cookie("sdktoken",yxpwd);
			CookieKeys.addCookie(request, response, cookie, false,  PhoneConfig.COOKIE_TIMEOUT);
			logger.debug("set cookie of uid="+ yxid.toLowerCase()+", and sdktoke="+yxpwd);
		}
		
		return CmdResult.SUCCESS;
	}
	
}
