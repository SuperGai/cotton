package com.agilecontrol.phone;

import java.io.StringWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.agilecontrol.nea.core.control.web.WebUtils;
import com.agilecontrol.nea.core.query.QueryEngine;
import com.agilecontrol.phone.UserObj;
import com.agilecontrol.nea.core.velocity.VelocityUtils;
import com.agilecontrol.nea.util.Validator;

import redis.clients.jedis.Jedis;

/**
 *
 *������Ϣ����
 *��Ҫ����:ʹ���߳̽��м���������ȡ���͵����� sys:exception �е���Ϣ�������ʼ� 
 *��������sys:exception,��������,��PhoneConfig.ERROR_MJ_QUEUE
 *
 * ����mailto ���ԣ����notnull, ����Ϊ�ռ��˵�ַ
 *
 * @author stao
 * 
 */
@Admin(mail="sun.tao@lifecycle.cn")
public class ExceptionListener implements Runnable {
	private static final Logger		 logger	= LoggerFactory.getLogger( ExceptionListener.class);
	private static ExceptionListener listener;
	private Jedis					 jedis;
	private String					 queue	= PhoneConfig.ERROR_MJ_QUEUE;
	private boolean					 isRun	= true;
	private String					 modelTemp;
	
	private ExceptionListener(){
		//��ȡ jedis ����
		jedis = WebUtils.getJedis();
		
		//��ȡģ��
		QueryEngine engine = null;
		Connection conn = null;
		try{
			engine = QueryEngine.getInstance();
			conn = engine.getConnection();
			modelTemp = PhoneController.getInstance().getValueFromADSQL( "error_mail", conn);
		}
		catch( Exception e){
			logger.error( e.getLocalizedMessage(), e);
		}
		finally{
			try{
				engine.closeConnection( conn);
			}
			catch( SQLException e){
				logger.error( e.getLocalizedMessage(), e);
			}
		}
	}
	
	/**
	 * ��ȡ������
	 * @return listener
	 */
	public static ExceptionListener getInstance(){
		if( null == listener) {
			listener = new ExceptionListener();
		}
		return listener;
	}
	
	/**
	 * ��ȡjidis ʵ��
	 * @return
	 */
	private Jedis getJedis(){
		return jedis;
	}
	/**
	 * ȷ���Ƿ����߳���������
	 * @return true ����߳�������
	 */
	public boolean isRunning(){
		return isRun;
	}
	/**
	 * ������ǰ�߳�
	 * ͨ�����������߳��Ƿ����
	 */
	public void stopProcess(){
		isRun = false;
	}
	
	/**
	 * ��װģ������
	 * @param jsonStr json,����Ϣ�����л�ã�����key��Ӧ��ֵ����ƥ��ģ����Ϣ
	 * @return ��װ��ɵ�ģ����Ϣ
	 */
	private String assembleModelContent(JSONObject jsonObj){
		String msg = null;
		if(Validator.isNull( modelTemp)){
			return null;
		}
		try{
			VelocityContext velocity = null;
			velocity = VelocityUtils.createContext();
			JSONObject user = (JSONObject)jsonObj.opt( "userObj");
			if( null != user) {
				velocity.put( "userObj", user);
			}
			velocity.put( "server", jsonObj.opt( "server"));
			velocity.put( "servername", jsonObj.opt( "servername"));
			velocity.put( "port", jsonObj.opt( "port"));
			velocity.put( "fileName", jsonObj.opt( "fileName"));
			velocity.put( "errorid", jsonObj.opt( "errorid"));
			velocity.put( "exception", jsonObj.opt( "exception"));
			velocity.put( "time", jsonObj.opt( "time"));
			velocity.put( "mailto", jsonObj.opt( "mailto"));
			
			StringWriter output = new StringWriter();
			Velocity.evaluate( velocity, output, VelocityUtils.class.getName(), modelTemp);
			msg = output.toString();
		}
		catch( Exception e){
			logger.error( e.getLocalizedMessage(), e);
		}
		return msg;
	}
	
	/**
	 * 
	 * @param jsonStr ����Ϣ�����л�õ�json�ַ���
	 * @return ��ȡtitle(json ��fileName��Ӧ�ֶ�)
	 */
	private String getTitle(String jsonStr){
		String title = null;
		JSONObject obj = null;;
		try{
			obj = new JSONObject( jsonStr);
			title = obj.optString( "fileName", "");
		}
		catch( JSONException e){
			logger.error( e.getLocalizedMessage(), e);
		}
		return title;
	}
	
	/**
	 *�����ʼ�
	 *�ʼ������� #ad_param#error_mailbox;��Ϊ��ʱ���Żᷢ���ʼ�
	 * @param title �ʼ�����
	 * @param msg �ʼ����� 
	 * @param emailStr ���Ϊnull����Ĭ��ʹ��PhoneConfig.ERROR_MAILBOX
	 */
	private void handleException(String title, String msg, String emailStr){
		//��ȡerror_mailbox
		if(Validator.isNull(emailStr))emailStr = PhoneConfig.ERROR_MAILBOX;
		//emailStr = "sun.tao@lifecycle.cn"; //���������ַ
		//��email��Ϊ��ʱ�����ʼ�����
		if( Validator.isNotNull( emailStr)&& Validator.isNotNull( msg)) {
			JSONArray emailsArr = new JSONArray().put(emailStr );
			title = "�쳣�����ʼ�" + title;
			//�����ʼ�
			MailUtil mailUtil = new MailUtil();
			try{
				mailUtil.sendEmail( emailsArr, title, msg);//�ռ���
			}
			catch( Exception e){
				logger.error( e.getLocalizedMessage(), e);
			}
		}
	}
	
	/**
	 * �����߳�ִ�ж��м����������õ�����Ϣ��ģ��������ã�ͨ���ʼ����ͳ�ȥ
	 * �˴�ʹ����jedis�� blpop()����
	 * 
	 * ע:����blpop()����ֵ����һ����������Ԫ�ص��б���һ��Ԫ���Ǳ�����Ԫ�������� key ���ڶ���Ԫ���Ǳ�����Ԫ�ص�ֵ��
	 * ����ڶ���Ԫ�ؾ����ʼ�����
	 */
	@Override
	public void run(){
		listener = ExceptionListener.getInstance();
		logger.info( "������Ϣ���м���");
		if(Validator.isNotNull(queue)){
			while (isRun){
				List<String> vals = jedis.blpop( 0, queue);
				if( (vals != null) && (vals.size() != 0)) {
					if( vals.size() == 2) {
						//{ mailto, time, fileName, excetpion}
						String jsonStr = vals.get( 1);
						try{
							JSONObject jo=new JSONObject(jsonStr);
							String title = jo.optString("fileName", "");
							String msg = assembleModelContent( jo);
							handleException( title, msg, jo.optString("mailto"));
						}catch(Throwable tx){
							logger.error("Fail to handle:"+ jsonStr, tx);
						}
					}
				}
			}
		}
		
	}
}
