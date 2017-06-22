package com.agilecontrol.b2b.cmd;

import java.util.ArrayList;
import java.util.Enumeration;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.json.JSONArray;
import org.json.JSONObject;

import com.agilecontrol.nea.core.util.CookieKeys;
import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.phone.CmdHandler;
import com.agilecontrol.phone.CmdResult;
import com.agilecontrol.phone.PhoneController;
import com.agilecontrol.phone.PhoneUtils;
import com.agilecontrol.phone.UUIDUtils;
import com.agilecontrol.phone.YXController;
/**
 * 拿到我的相关信息
 * @author yfzhu
 *
 */
public class Whoami extends CmdHandler{

	@Override
	public CmdResult execute(JSONObject jo) throws Exception {
		JSONObject res=new JSONObject();
		
		res.put("usr", PhoneUtils.getRedisObj("usr", usr.getId(), conn, jedis));
		res.put("joinurl", getJoinURL());
		
		return new CmdResult(res);
	}
	
}
