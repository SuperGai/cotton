package com.agilecontrol.phone.wechat.pay;

import org.json.JSONObject;

import com.agilecontrol.phone.CmdHandler;
import com.agilecontrol.phone.CmdResult;
/**
 * 获取当前用户的penId
 * URL: /servlets/binserv/Phone
 * MiscCmd: {
 *     "cmd": "com.agilecontrol.phone.wechat.auth.GetOpenId"
 * }
 * Result: {
 *     "openId": "OPENID"
 * }
 * 
 * @author cw 
 * 
 */

public class GetOpenId extends CmdHandler
{

	@Override
	public CmdResult execute(JSONObject jo) throws Exception {
		String openId = usr.getName();
		JSONObject json = new JSONObject();
		json.put("openId", openId);
		
		return new CmdResult(json);
	}

}
