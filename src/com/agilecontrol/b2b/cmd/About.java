package com.agilecontrol.b2b.cmd;

import java.util.Enumeration;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONArray;
import org.json.JSONObject;

import com.agilecontrol.nea.util.Validator;
import com.agilecontrol.phone.Admin;
import com.agilecontrol.phone.CmdHandler;
import com.agilecontrol.phone.CmdResult;
import com.agilecontrol.phone.PhoneUtils;

/**
 * 
 * @author stao
 * 
 * 返回结构 {"result":{ "session":[ "调试信息", "svraddr:127.0.0.1", "svrname:mjsvr001", "svrport:8083", "cookie:c", "uid:18"
 * ]}, "code":0, "message":"@complete@" }
 * 
 */
@Admin(mail="sun.tao@lifecycle.cn")
public class About extends CmdHandler {
	
	@Override
	public CmdResult execute(JSONObject jo) throws Exception{
		JSONObject res = new JSONObject();
		
		JSONArray session = new JSONArray();
		session.put( "调试信息");
		
		HttpServletRequest request = event.getContext().getHttpServletRequest();
		
		//获取服务端服务器名，地址，端口，和 当前用户所在的  debug 中
		session.put( "svrname:" + request.getLocalName());
		session.put( "svrport:" + request.getLocalPort());
		//logger.debug( "-----------------");
		String debug = null;
		
		Enumeration cooEnum = request.getHeaders( "cookie"); // support multiple values
		if( null != cooEnum) {
			while (cooEnum.hasMoreElements()){
				String cooStr = (String) cooEnum.nextElement();
				logger.debug( "cooStr:" + cooStr);
				String[] arrStr = cooStr.split( ";");
				for( int i = 0; i < arrStr.length; i++){
					if( arrStr[i].contains( "debug")) {
						String debugStr = arrStr[i].trim();
						String[] debArr = debugStr.split( "=");
						if( 2 == debArr.length) {
							debug = debArr[1];
							logger.debug( "debug:" + debug);
						}
					}
				}
			}
		}
		//logger.debug( "-------------------");
		
		if( Validator.isNull( debug)) {
			debug = "默认";
		}
		
		boolean isdebug = false;
		if( debug.contains( "t")) {
			isdebug = true;
		}
		
		session.put( "cookie:" + debug);
		JSONObject debugModel = new JSONObject();
		debugModel.put( "isdebug", isdebug);
		debugModel.put( "cookie", debug);
		res.put( "debugModel", debugModel);
		
		//获取redis中当前用户的信息,并塞进session数组中
		JSONObject usrObj = PhoneUtils.getRedisObj( "usr", usr.getId(),  conn, jedis);
		long uId = usrObj.getLong( "id");
		session.put( "uid:" + uId);
		res.put( "session", session);
		return new CmdResult( res);
	}
}
