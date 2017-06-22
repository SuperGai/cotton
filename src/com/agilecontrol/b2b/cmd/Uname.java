package com.agilecontrol.b2b.cmd;

import java.util.ArrayList;
import java.util.Enumeration;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.json.JSONArray;
import org.json.JSONObject;

import com.agilecontrol.nea.core.util.ConfigValues;
import com.agilecontrol.nea.core.util.CookieKeys;
import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.phone.CmdHandler;
import com.agilecontrol.phone.CmdResult;
import com.agilecontrol.phone.PhoneController;
import com.agilecontrol.phone.PhoneUtils;
import com.agilecontrol.phone.UUIDUtils;
import com.agilecontrol.phone.YXController;
/**
 * 拿到服务器的相关信息
 * @author yfzhu
 *
 */
public class Uname extends CmdHandler{

	@Override
	public CmdResult execute(JSONObject jo) throws Exception {
		JSONObject res=new JSONObject();
		
		JSONObject session=res;//new JSONObject();
		String uid_token=null;
		Cookie cookie=CookieKeys.getCookieObj(event.getContext().getHttpServletRequest(), "token");
		if(cookie!=null)uid_token=cookie.getValue();

		HttpServletRequest request=event.getContext().getHttpServletRequest();

		session.put("cookie",uid_token==null?"":uid_token);
		session.put("id", event.getContext().getSession().getId());
		session.put("duration",  event.getContext().getSession().getMaxInactiveInterval()+" s");
		session.put("svraddr",  request.getLocalAddr());
		session.put("svrname",  request.getLocalName());
		session.put("svrport",  request.getLocalPort());
		session.put("server.id", ConfigValues.get("server.id", ""));
		
		JSONObject header=new JSONObject();
		Enumeration enu=request.getHeaderNames();
        while( enu.hasMoreElements()) {
            String param= (String)enu.nextElement();
            header.put(param, request.getHeader(param));
        }
        session.put("header", header);
        
		
		return new CmdResult(res);
	}
	
}
