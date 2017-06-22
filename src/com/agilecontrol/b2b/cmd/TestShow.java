package com.agilecontrol.b2b.cmd;

import org.json.JSONArray;
import org.json.JSONObject;

import com.agilecontrol.nea.core.query.QueryEngine;
import com.agilecontrol.phone.CmdHandler;
import com.agilecontrol.phone.CmdResult;

public class TestShow extends CmdHandler {

	@Override
	public CmdResult execute(JSONObject jo) throws Exception {
		// TODO Auto-generated method stub
		logger.debug("--------------------------------testshow==="+jo.toString());
		String tableName = jo.getString("table");
		long id = QueryEngine.getInstance().getSequence(tableName, conn);
		return new CmdResult(id);
	}

}
