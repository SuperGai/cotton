package com.agilecontrol.phone.impl.buding;

import org.json.JSONObject;

import com.agilecontrol.phone.CmdHandler;
import com.agilecontrol.phone.CmdResult;

public class BLogout extends CmdHandler {

	@Override
	public CmdResult execute(JSONObject jo) throws Exception {
		/*
		 *  TODO Empty cmd for front-end to prevent command not found exception.
		 *  Replace it with actual code later. Created by zhangbh on 2016-01-28
		 */
		return CmdResult.SUCCESS;
	}

	@Override
	public boolean allowGuest() {
		// TODO Test only
		return true;
	}

}
