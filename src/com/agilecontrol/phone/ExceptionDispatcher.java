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
 * ���������쳣�Ĵ���ӿ�
 * 
 * redis push ������Ϣ ������ sys:exception��
 * ��ϢƵ��:sys:exception
 * ��Ϣģ��:ad_sql#error_mail
 * ����ģ����Ϣ����ͨ��jedis������Ϣ������ȥ
 * @author stao
 */
class ExceptionDispatcher {
	private static final Logger		   logger = LoggerFactory.getLogger( ExceptionDispatcher.class);
	private static ExceptionDispatcher instance;
	
	/**
	 * ��ȡ��ǰʵ��
	 * @return
	 */
	public static ExceptionDispatcher getInstance(){
		if( null == instance) {
			instance = new ExceptionDispatcher();
		}
		return instance;
	}
	
	/**
	 * ��Ϣ���� redis ������
	 * ��һ�����ģ�����ã���ģ����Ϣ�ƽ�����
	 * ���ģ����Ϣ������
	  * ��Ϣģ��:ad_sql#error_mail Ϊ��ʱ�������ᷢ����Ϣ��Ƶ����
	  * @param throwable - ��ǰ�쳣
	  * @param params - ���ٺ����������ԣ�
	  * {
	  *		userid:$usrObj.id
	  *     �쳣:$exception} ��Ϊ��չ���㣬ʹ��jsonobj
	  * @throws JSONException
	  */
	public void notifyException(Throwable throwable, JSONObject params, Jedis jedis){
		//��ȡexception��Ϣ,����װ��params������
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
			//��ȡ��Ϣ��������,���󱨸潫���͵�����Ϣ������
			String queue = PhoneConfig.ERROR_MJ_QUEUE;
			//String queue="sys:exception";
			//��paramsת�����ַ����������͵���Ϣ������
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
