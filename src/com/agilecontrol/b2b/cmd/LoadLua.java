package com.agilecontrol.b2b.cmd;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;

import com.agilecontrol.phone.CmdResult;
import com.agilecontrol.phone.CmdHandler;
import com.agilecontrol.phone.PhoneController;
import com.agilecontrol.phone.PhoneUtils;
import com.agilecontrol.nea.core.control.web.WebUtils;
import com.agilecontrol.nea.core.query.QueryEngine;
import com.agilecontrol.nea.core.util.ConfigValues;
import com.agilecontrol.nea.core.util.WebKeys;
import com.agilecontrol.nea.util.Configurations;
import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.nea.util.Tools;
import com.agilecontrol.nea.util.Validator;
/**
 * 将ad_sql#name 指定的script 加载到redis 并返回sha1 值, 强制重载
 * 参数:
 * {
 * name  -- ad_sql的name, 如果用逗号分隔，将返回多个sql name对应的sha1
 *
 * } 
 * return {sha1}
 * @author yfzhu
 *
 */
public class LoadLua extends CmdHandler {
	
	@Override
	public CmdResult execute(JSONObject jo) throws Exception {
		checkIsLifecycleManager();	
		String[] names=getString(jo,"name").split("[, ]");
		JSONArray ja=new JSONArray();
		for(String name:names){
			if(Validator.isNotNull(name)){
			String sha1=PhoneController.getInstance().getRedisScript(name, conn, jedis,true);
			ja.put(name+": "+ sha1);
			}
		}
		
		CmdResult ret= new CmdResult(ja);
		return ret;
	}
	
	
}
