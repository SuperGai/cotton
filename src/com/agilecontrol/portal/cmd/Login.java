package com.agilecontrol.portal.cmd;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.velocity.VelocityContext;
import org.json.JSONObject;

import redis.clients.jedis.Jedis;

import com.agilecontrol.phone.*;
import com.agilecontrol.b2b.schema.TableManager;
import com.agilecontrol.nea.core.control.web.SessionContextManager;
import com.agilecontrol.nea.core.control.web.UserWebImpl;
import com.agilecontrol.nea.core.control.web.WebUtils;
import com.agilecontrol.nea.core.query.QueryEngine;
import com.agilecontrol.nea.core.query.SPResult;
import com.agilecontrol.nea.core.security.SecurityUtils;
import com.agilecontrol.nea.core.security.auth.Authenticator;
import com.agilecontrol.nea.core.util.ConfigValues;
import com.agilecontrol.nea.core.util.CookieKeys;
import com.agilecontrol.nea.core.util.MessagesHolder;
import com.agilecontrol.nea.core.util.WebKeys;
import com.agilecontrol.nea.core.velocity.VelocityUtils;
import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.nea.util.Tools;
import com.agilecontrol.nea.util.Validator;
/**
输入

request 参数

{cmd:"b2b.login", username, password,redirect, verifyCode}

username - 用户名
password - 密码
verifyCode - 校验码, 原始校验码位置: /servlets/vms, 返回的是一张4位字母的图片，调用此地址的时候，
将在session的attribute “com.agilecontrol.nea.core.control.web.ValidateMServlet” 中缓存此4位字母，以便在后台进行对比
redirect - 成功后去哪个页面

目前暂不实现verifyCode

输出
如果没有设置redirect，将返回 /nea/core/portal
 * 
 * @author stao
 *
 */
@Admin(mail="sun.tao@lifecycle.cn")
public class Login  extends CmdHandler {
	
	
	
	@Override
	public CmdResult execute(JSONObject jo) throws Exception {
		
		HttpServletRequest req = event.getContext().getHttpServletRequest();
		
		
		String userName = jo.optString("username");
		String password = jo.optString("password");
		
		if(Validator.isNotNull(userName)){
			userName= userName.trim();
		}else throw new NDSException("需要username");
		if(Validator.isNotNull(password)){
			password= password.trim();
		}else throw new NDSException("需要password");

		Locale locale=null;
		locale=req.getLocale();
		String lang=locale.getLanguage();
		logger.debug("lang="+ lang);
		
		
		Map headerMap = new HashMap();
		Enumeration enu1 = req.getHeaderNames();
		while (enu1.hasMoreElements()) {
			String name = (String) enu1.nextElement();
			Enumeration enu2 = req.getHeaders(name);
			List headers = new ArrayList();
			while (enu2.hasMoreElements()) {
				String value = (String) enu2.nextElement();
				headers.add(value);
			}
			headerMap.put(name, (String[]) headers.toArray(new String[0]));
		}
		Map parameterMap = req.getParameterMap();
		// web 登录验证时需要强制使用verifyCode
		HttpSession ses = req.getSession();
		headerMap.put("remote_address", req.getRemoteAddr());
		headerMap.put("remote_host", req.getRemoteHost());
		

		JSONObject loginres = new JSONObject();

		if (SecurityUtils.authenticate(userName, password, headerMap,
				parameterMap) == Authenticator.SUCCESS) {
			
			WebUtils.getSessionContextManager(req.getSession(true));
			req.getSession().setAttribute(org.apache.struts.Globals.LOCALE_KEY,
					locale);
			// will trigger com.agilecontrol.nea.core.web.SessionAttributeController#attributeReplaced
			// that will setup UserWebImpl in session
			req.getSession().setAttribute("USER_ID", userName);
			SessionContextManager manager= (SessionContextManager)req.getSession().getAttribute(WebKeys.SESSION_CONTEXT_MANAGER);
			UserWebImpl user=((UserWebImpl) manager.getActor(com.agilecontrol.nea.core.util.WebKeys.USER));
			req.getSession().setMaxInactiveInterval(
					ConfigValues.get("fair.imgsvr.timeout", 4 * 60 * 60));// seconds

//			loginres.put("isok", 1);

			String token=PhoneUtils.setupCookie(user.getUserId(),jo, event.getContext().getHttpServletRequest(), event.getContext().getHttpServletResponse(), jedis,conn);
			loginres.put("token", token);
			
		} else {
//			logger.info("Error login, username="+ userName+ " from "+ req.getRemoteHost());
//			loginres.put("isok", 0);
//			loginres.put("message", MessagesHolder.getInstance().getMessage(locale, "sipstatus.authFaild"));
			throw new NDSException(MessagesHolder.getInstance().getMessage(locale, "sipstatus.authFaild"));
		}
		String url=jo.optString("redirect");
		if(url!=null) loginres.put("redirect", url);
		return new CmdResult(loginres);		
	}
	
	/**
	 * Guest can execute this task, default to false
	 * @return
	 */
	public boolean allowGuest(){
		return true;
	}
	

}
