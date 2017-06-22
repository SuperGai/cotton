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
 *监听消息队列
 *主要功能:使用线程进行监听，将获取推送到队列 sys:exception 中的消息，并发邮件 
 *队列名：sys:exception,已做配置,在PhoneConfig.ERROR_MJ_QUEUE
 *
 * 增加mailto 属性，如果notnull, 将作为收件人地址
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
		//获取 jedis 对象
		jedis = WebUtils.getJedis();
		
		//获取模板
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
	 * 获取单对象
	 * @return listener
	 */
	public static ExceptionListener getInstance(){
		if( null == listener) {
			listener = new ExceptionListener();
		}
		return listener;
	}
	
	/**
	 * 获取jidis 实例
	 * @return
	 */
	private Jedis getJedis(){
		return jedis;
	}
	/**
	 * 确认是否有线程正在运行
	 * @return true 如果线程运行中
	 */
	public boolean isRunning(){
		return isRun;
	}
	/**
	 * 结束当前线程
	 * 通过变量控制线程是否结束
	 */
	public void stopProcess(){
		isRun = false;
	}
	
	/**
	 * 组装模板内容
	 * @param jsonStr json,从消息队列中获得，其中key对应的值用于匹配模板消息
	 * @return 组装完成的模板信息
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
	 * @param jsonStr 从消息队列中获得的json字符串
	 * @return 获取title(json 中fileName对应字段)
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
	 *发送邮件
	 *邮件接收人 #ad_param#error_mailbox;不为空时，才会发送邮件
	 * @param title 邮件标题
	 * @param msg 邮件内容 
	 * @param emailStr 如果为null，将默认使用PhoneConfig.ERROR_MAILBOX
	 */
	private void handleException(String title, String msg, String emailStr){
		//获取error_mailbox
		if(Validator.isNull(emailStr))emailStr = PhoneConfig.ERROR_MAILBOX;
		//emailStr = "sun.tao@lifecycle.cn"; //设置邮箱地址
		//当email不为空时，则邮件发送
		if( Validator.isNotNull( emailStr)&& Validator.isNotNull( msg)) {
			JSONArray emailsArr = new JSONArray().put(emailStr );
			title = "异常提醒邮件" + title;
			//发送邮件
			MailUtil mailUtil = new MailUtil();
			try{
				mailUtil.sendEmail( emailsArr, title, msg);//收件人
			}
			catch( Exception e){
				logger.error( e.getLocalizedMessage(), e);
			}
		}
	}
	
	/**
	 * 开启线程执行队列监听，并将得到的消息与模板进行配置，通过邮件发送出去
	 * 此处使用了jedis的 blpop()方法
	 * 
	 * 注:关于blpop()返回值返回一个含有两个元素的列表，第一个元素是被弹出元素所属的 key ，第二个元素是被弹出元素的值。
	 * 这里第二个元素就是邮件内容
	 */
	@Override
	public void run(){
		listener = ExceptionListener.getInstance();
		logger.info( "启动消息队列监听");
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
