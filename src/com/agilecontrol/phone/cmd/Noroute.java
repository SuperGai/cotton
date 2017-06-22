package com.agilecontrol.phone.cmd;

import org.json.JSONObject;

import com.agilecontrol.phone.*;

/**
 * @author yfzhu
 */
public class Noroute extends CmdHandler {

	public boolean allowGuest(){
		return true;
	}
	
	@Override
	public CmdResult execute(JSONObject jo) throws Exception {
		CmdResult cr=new CmdResult(-100,"√¸¡ÓŒ¥∂®“Â:"+jo.optString("cmd"),new JSONObject());
		return cr;
	}
}
