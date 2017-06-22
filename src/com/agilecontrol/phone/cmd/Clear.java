package com.agilecontrol.phone.cmd;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONArray;
import org.json.JSONObject;

import com.agilecontrol.phone.CmdResult;
import com.agilecontrol.phone.CmdHandler;
import com.agilecontrol.phone.PhoneController;
import com.agilecontrol.phone.PhoneUtils;
import com.agilecontrol.phone.util.*;
import com.agilecontrol.nea.core.query.QueryEngine;
import com.agilecontrol.nea.core.schema.TableManager;
import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.nea.util.Tools;
/**
 * 清除缓存，比如qlc
 * @author yfzhu
 *
 */
public class Clear extends CmdHandler {

	@Override
	public CmdResult execute(JSONObject jo) throws Exception {
		
		PhoneController.getInstance().clear();
		
		String message="缓存被清除";
		
		CmdResult cr=new CmdResult();
		JSONObject cro= new JSONObject();
		cro.put("message", message);
		cr.setObject(cro);
		
		return cr;
	}
	
}

