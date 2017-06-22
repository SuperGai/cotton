package com.agilecontrol.b2b.schema;

import java.text.SimpleDateFormat;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.nea.util.Validator;
import com.agilecontrol.phone.Admin;
@Admin(mail="yfzhu@lifecycle.cn")
public class SchemaUtils {
	private static final Logger logger = LoggerFactory.getLogger(SchemaUtils.class);

	/**
	 * 格式: "yyyy-MM-dd HH:mm:ss"
	 */
	public static ThreadLocal<SimpleDateFormat> dateTimeSecondsFormatter=new ThreadLocal(){
		protected synchronized Object initialValue() {
	    	SimpleDateFormat a=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	    	a.setLenient(false);
	    	return a;
	    }
	};
	
	
	public static long getLong(JSONObject jo, String name) throws Exception {
		long value = jo.optLong(name, -1);
		if (value == -1){
			logger.error("Fail to find "+name+" from jo:"+ jo);
			throw new NDSException(name + "属性未定义");
		}
		return value;
	}
	
	public static int getInt(JSONObject jo, String name) throws Exception {
		int value = jo.optInt(name, -1);
		if (value == -1){
			logger.error("Fail to find "+name+" from jo:"+ jo);
			throw new NDSException(name + "属性未定义");
		}
		return value;
	}

	public static String getString(JSONObject jo, String name) throws Exception {
		String value = jo.optString(name);
		if (Validator.isNull(value)){
			logger.error("Fail to find "+name+" from jo:"+ jo);
			throw new NDSException(name + "属性未定义");
		}
		return value;
	}	
	
	/**
	 * 
	 * @param jo yyyy-MM-dd kk:mm:ss
	 * @param name
	 * @return
	 * @throws Exception
	 */
	public static java.util.Date getDate(JSONObject jo, String name) throws Exception{
		String value=jo.optString(name);
		if (Validator.isNull(value)){
			logger.error("Fail to find "+name+" from jo:"+ jo);
			throw new NDSException(name + "属性未定义");
		}
		return dateTimeSecondsFormatter.get().parse(value);
	}
}
