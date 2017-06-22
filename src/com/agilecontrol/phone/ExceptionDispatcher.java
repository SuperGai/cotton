package com.agilecontrol.phone;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.agilecontrol.nea.core.control.web.WebUtils;
import com.agilecontrol.nea.core.query.QueryEngine;
import com.agilecontrol.nea.core.velocity.VelocityUtils;
import com.agilecontrol.nea.util.Validator;

import redis.clients.jedis.Jedis;

/**
 * 对异其他异常的处理接口
 * 
 * redis push 队列消息 到队列 sys:exception中
 * 消息频道:sys:exception
 * 消息模板:ad_sql#error_mail
 * 配置模板消息，并通过jedis对象将消息发布出去
 * @author stao
 */
class ExceptionDispatcher {
	private static final Logger		   logger = LoggerFactory.getLogger( ExceptionDispatcher.class);
	private static ExceptionDispatcher instance;
	
	/**
	 * 获取当前实例
	 * @return
	 */
	public static ExceptionDispatcher getInstance(){
		if( null == instance) {
			instance = new ExceptionDispatcher();
		}
		return instance;
	}
	
	/**
	 * 消息推送 redis 队列中
	 * 进一步完成模板配置，将模板消息推进队列
	 * 完成模板消息的配置
	  * 消息模板:ad_sql#error_mail 为空时，将不会发布消息到频道上
	  * @param throwable - 当前异常
	  * @param params - 至少含有以下属性：
	  * {
	  *		userid:$usrObj.id
	  *     异常:$exception} ，为扩展方便，使用jsonobj
	  * @throws JSONException
	  */
	public void notifyException(Throwable throwable, JSONObject params, Jedis jedis){
		//获取exception信息,并组装到params对象中
		Writer result = new StringWriter();
		PrintWriter printWriter = new PrintWriter( result);
		throwable.printStackTrace( printWriter);
		String excepStr = result.toString();
		SimpleDateFormat smf = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss");
		Date date = new Date();
		String timeStr = smf.format( date);
		
		String mailTo=PhoneUtils.getAdminMailFromFirstAnnotation(throwable);
		try{
			params.put( "fileName", throwable.getStackTrace()[0].getClassName());
			params.put( "time", timeStr);
			params.put( "exception", excepStr);
			if(mailTo!=null)params.put( "mailto", mailTo);
			//获取消息队列名称,错误报告将发送到该消息队列中
			String queue = PhoneConfig.ERROR_MJ_QUEUE;
			//String queue="sys:exception";
			//将params转换成字符串，并推送到消息队列中
			String str = params.toString();
			if(Validator.isNotNull(queue)){
				jedis.rpush( queue, str);				
			}
		}
		catch( Exception e){
			logger.error( e.getLocalizedMessage(), e);
		}
	}
}
