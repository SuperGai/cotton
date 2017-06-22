package com.agilecontrol.phone.cmd;

import java.io.StringWriter;

import javax.servlet.http.HttpServletRequest;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.json.*;

import com.agilecontrol.nea.core.schema.Table;
import com.agilecontrol.nea.core.schema.TableManager;
import com.agilecontrol.nea.core.security.SecurityUtils;
import com.agilecontrol.nea.core.util.MessagesHolder;
import com.agilecontrol.nea.core.velocity.VelocityUtils;
import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.nea.util.Validator;
import com.agilecontrol.phone.CmdHandler;
import com.agilecontrol.phone.CmdResult;
import com.agilecontrol.phone.PhoneController;
import com.agilecontrol.phone.util.UserSchema;

import java.util.*;
/**
 支持ad_sql语句执行
 ad_sql的value部分是含有?的sql语句，params里写的是$xxx 等velocity变量，
 runsql 需要指定以下输入参数:
 {
  type: QUERY | UPDATE | PROCEDURE | PROCEDURE_WITH_RESULT | FUNCTION | JSON | TEXT, // Optional, default is 'QUERY'
  name: ad_sql#name
  var: {"key":"value"}
 }
 type:
   QUERY: 执行查询
   UPDATE: 执行insert、update、delete、ddl，返回影响行数，int
   PROCEDURE: 执行DB存储过程，无返回
   PROCEDURE_WITH_RESULT: 执行DB存储过程，返回code和message
   FUNCTION: 执行DB函数，唯一返回，String
   JSON: 返回ad_sql文本所配置的json对象或json数组
   TEXT: 直接返回ad_sql文本
 name - ad_sql#name 当前用户必须具有读取到ad_sql当前记录的权限
 var: 变量集合，key是变量的名称，value是变量的值。注意key不要写$符号
 
 row_is_obj: true|false, 返回的数组中的行是对象还是数组
 
 java会根据当前强制写入$userid, $username， 以及其他的用户环境变量，一律小写（详细见表AD_USER_ATTR），防止客户端篡改
 
 返回: 
 [[col1,col2],..] 按sql的约定来
 * @author yfzhu
 *
 */
public class RunSQL extends CmdHandler {
	
	public static final String TYPE_QUERY = "QUERY";
	public static final String TYPE_UPDATE = "UPDATE";
	public static final String TYPE_PROCEDURE = "PROCEDURE";
	public static final String TYPE_PROCEDURE_WITH_RESULT = "PROCEDURE_WITH_RESULT";
	public static final String TYPE_FUNCTION = "FUNCTION";
	public static final String TYPE_JSON = "JSON";
	public static final String TYPE_TEXT = "TEXT";

	@Override
	public CmdResult execute(JSONObject jo) throws Exception {
		
		String name=jo.getString("name");
		
		boolean rowIsObj=jo.optBoolean("row_is_obj", false);
		
		int sqlId= engine.doQueryInt("select id from ad_sql where name=?", new Object[]{name}, conn);
		
		VelocityContext vc=VelocityUtils.createContext();

		JSONObject var= jo.optJSONObject("var");
		if(var!=null){
			for(Iterator it=var.keys();it.hasNext();){
				String key=(String) it.next();
				vc.put(key, var.get(key));
			}
		}
		vc.put("conn",conn);
		vc.put("c", this);
		vc.put("userid", usr.getId());
		vc.put("username",usr.getName());
		//用户环境变量
		//todo
		
		
		PhoneController ctrl = PhoneController.getInstance();
		Object o = new JSONObject();
		String type = jo.optString("type", TYPE_QUERY);
		if (TYPE_QUERY.equalsIgnoreCase(type)) {
			o = ctrl.getDataArrayByADSQL(name, vc, conn, rowIsObj);
		} else if (TYPE_UPDATE.equalsIgnoreCase(type)) {
			o = ctrl.executeUpdate(name, vc, conn);
		} else if (TYPE_PROCEDURE.equalsIgnoreCase(type)) {
			ctrl.executeProcedure(name, vc, conn);
		} else if (TYPE_PROCEDURE_WITH_RESULT.equalsIgnoreCase(type)) {
			o = ctrl.executeProcedure(name, vc, conn, true);
		} else if (TYPE_FUNCTION.equalsIgnoreCase(type)) {
			o = ctrl.executeFunction(name, vc, conn);
		} else if (TYPE_JSON.equalsIgnoreCase(type)) {
			//o = ctrl.getValueFromADSQLAsJSON(name, conn);
			/*
			 * 对@tranlation-key@做翻译
			 * modified on 20161028
			 */
			String data = ctrl.getValueFromADSQL(name, conn);
			data=MessagesHolder.getInstance().translateMessage(data, this.locale);
			
			if( data.trim().startsWith("{")){
				o=new JSONObject(data);
			}else{
				JSONArray ja=new JSONArray(data);
				o=ja;
			}
		} else if (TYPE_TEXT.equalsIgnoreCase(type)) {
			o = ctrl.getValueFromADSQL(name, conn);
		}
		
		CmdResult cr=new CmdResult();
		cr.setObject(o);
		return cr;
	}
}