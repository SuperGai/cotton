package com.agilecontrol.portal.cmd;

import java.util.HashMap;

import javax.servlet.http.HttpSession;

import org.json.JSONArray;
import org.json.JSONObject;

import com.agilecontrol.nea.core.control.event.DefaultEventContext;
import com.agilecontrol.nea.core.control.event.DefaultWebEvent;
import com.agilecontrol.nea.core.control.util.ValueHolder;
import com.agilecontrol.nea.core.security.SecurityUtils;
import com.agilecontrol.nea.core.security.auth.AuthException;
import com.agilecontrol.nea.core.security.auth.Authenticator;
import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.nea.util.Validator;
import com.agilecontrol.phone.CmdHandler;
import com.agilecontrol.phone.CmdResult;
import com.agilecontrol.phone.PhoneUtils;

/**
 * 发送短信验证码-验证码存放redis
 * @author stao
 *
 */  
public class CheckTelCode extends CmdHandler {
	@Override
	public boolean allowGuest() {
		return true;
	}
	
	@Override
	public CmdResult execute(JSONObject jo) throws Exception {
		String phoneNum = jo.optString("telphone", "");
		String tpcode = jo.optString("tpcode","");
		String code =jo.optString("code", "");
		
		//校验 redis中该手机验证码
		String key="portalregcheckcode:"+ phoneNum;
		String checkCode= jedis.hget(key,"checkcode");
		//logger.debug("----checkeCode" + checkCode);
		if(null == checkCode || "".equals(checkCode)){
			throw new NDSException("验证码失效");
		};
		if(!checkCode.equals(code)){
			throw new NDSException("验证码错误");
		};
		
		//获取session中的 serverValidCode 值
		HttpSession session = event.getContext().getSession();
		String serverValidCode=(String)session.getAttribute("com.agilecontrol.nea.core.control.web.ValidateMServlet");
		//logger.debug("----------------tpcode:"+tpcode);
		//logger.debug("----------------serverValidCode:"+serverValidCode);
		if(Validator.isNull(serverValidCode)){
			throw new NDSException("图片验证码已失效");
		}
		if(!serverValidCode.equalsIgnoreCase(tpcode)){
			throw new NDSException("图片验证码错误");
		}
		//使用完后删除
		jedis.del(key);
		return new CmdResult();
	}

}
