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
����

request ����

{cmd:"b2b.login", username, password,redirect, verifyCode}

username - �û���
password - ����
verifyCode - У����, ԭʼУ����λ��: /servlets/vms, ���ص���һ��4λ��ĸ��ͼƬ�����ô˵�ַ��ʱ��
����session��attribute ��com.agilecontrol.nea.core.control.web.ValidateMServlet�� �л����4λ��ĸ���Ա��ں�̨���жԱ�
redirect - �ɹ���ȥ�ĸ�ҳ��

Ŀǰ�ݲ�ʵ��verifyCode

���
���û������redirect�������� /nea/core/portal
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
		}else throw new NDSException("��Ҫusername");
		if(Validator.isNotNull(password)){
			password= password.trim();
		}else throw new NDSException("��Ҫpassword");

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
		// web ��¼��֤ʱ��Ҫǿ��ʹ��verifyCode
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
