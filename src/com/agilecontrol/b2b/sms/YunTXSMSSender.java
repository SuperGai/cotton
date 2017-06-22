package com.agilecontrol.b2b.sms;


import org.json.*;

import com.agilecontrol.nea.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.agilecontrol.nea.util.MD5Sum;
import com.agilecontrol.phone.Admin;
import com.agilecontrol.phone.PhoneConfig;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Pattern;
/**
 * Send sms messages
 * 短信接口
 *接口文档参照供应商官网www.yuntongxun.com
 * @author gutt
 *
 */
@Admin(mail="sun.yifan@lifecycle.cn")
public class YunTXSMSSender {
	/**
	 * Timeout in seconds for read sms result from http connection
	 */
	protected int readTimeout;
	/**
	 * Timeout in seconds for connect to sms service provider 
	 */
	protected int connectTimeout;
	/**
	 * Some words are prohibited during sending, read from db ad_param.
	 * words should seperated by comma
	 */
	protected String forbidWords;
	protected Pattern forbidWordsPattern=null;
	/**
	 * length of max sms content(without postfix)
	 */
	protected int smsLength=-1;
	/**
	 * Words that will append to sms content
	 */
	protected String postfix;
	
	/**
	 * "" or "2" 
	 */
	protected String channelPostfix="";
	
	private Logger logger= LoggerFactory.getLogger(getClass());
	private String accountsid;//账户Id
	private String authtoken;//账户授权令牌
	private String serverURL;
	private String appid;
	private String date;//时间戳
	/**
	 * 
	 * @param context should contain following parameters:
	 * 	"connection" - db connection this connection has transaction maintained by outside 
	 * 	do not close or commit transaction when sending
	 * 	
	 * @throws Exception
	 */
	public void init() throws Exception{
	    readTimeout=PhoneConfig.SMS_READTIMEOUT;
		connectTimeout=PhoneConfig.SMS_CONNECTTIMEOUT;
		
	    //corpId= conf.getProperty("sms.corpid");
	    accountsid = PhoneConfig.SMS_ACCOUNTSID;
	    authtoken = PhoneConfig.SMS_AUTHTOKEN;
	    appid = PhoneConfig.SMS_APPID;
	    serverURL = PhoneConfig.SMS_SERVERURL;
	    SimpleDateFormat df = new SimpleDateFormat("yyyyMMddHHmmss");//设置日期格式
	    date= df.format(new Date());
	    serverURL=serverURL.replace("{accountSid}", accountsid);
	    serverURL=serverURL.replace("{SigParameter}", MD5Sum.toCheckSumStr(accountsid+authtoken+date).toUpperCase());
	}
	/**
	 * 设置包头相关信息
	 * Authorization 为.使用Base64编码（账户Id + 冒号 + 时间戳）
	 */
	protected HttpURLConnection openHttpConnection(String url, String method) throws MalformedURLException, IOException{
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestProperty("Accept", "application/json");   
        conn.setRequestProperty("Content-Type", "application/json;charset=utf-8");   
        conn.setRequestProperty("Content-length", "256");   
        conn.setRequestProperty("Authorization", String.valueOf(Base64.encode((accountsid+":"+date).getBytes())));// 维持长连接   
		conn.setRequestMethod(method);
		conn.setDoOutput(true);
		conn.setConnectTimeout(this.connectTimeout*1000);
		conn.setReadTimeout(this.readTimeout*1000);
		conn.setRequestProperty("Connection", "close"); // keep alive=false
		return conn;
	}
	private String sendRequest(String apiURL, JSONObject params, String method) throws Exception {

        //String queryString = (null == params) ? "" : RestUtils.delimit(params.entrySet(),true	);
        HttpURLConnection conn =openHttpConnection(apiURL, method);
		
		conn.connect();
		conn.getOutputStream().write(params.toString().getBytes("UTF-8"));
		BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"));
		StringBuffer buffer = new StringBuffer();
		String line;
		while ((line = reader.readLine()) != null) {
			buffer.append(line);
		}
		//logger.debug(Tools.toString( conn.getHeaderFields()));
		reader.close();
        conn.disconnect();
		String msg= buffer.toString();
		return msg;
	}	
	/**
	 * 
	 * @param content
	 * @param phones
	 * @return 0 for ok, others for error
	 */
	public JSONObject sendBatchMessage(String content, String phones){
		JSONObject ret = new JSONObject();
		try{
			init();
			String url= serverURL;
			System.out.println(url);
			JSONObject contents = new JSONObject(content);
			contents.put("to", phones);	
			contents.put("appId", this.appid);
			String msg=null;
			msg=sendRequest(url,contents, "POST" );
			JSONObject jo= new JSONObject(msg);
			String retmsg=jo.optString("statusCode");
			ret=getBackInfo(jo);
		}catch(Throwable tx){
			logger.error("Fail to process  content="+content+", phones="+phones, tx);
		}
		return ret;
	}
	private JSONObject getBackInfo(JSONObject jo) throws Exception{
		JSONObject ret  = new JSONObject();
		String retCode = jo.optString("statusCode");
		String retMsg =  jo.optString("statusMsg");
		if(!retCode.equals("000000")){
			logger.error("错误码=" + retCode +" 错误信息= "+retMsg);
		}
		if(retCode.equals("000000")){
			ret.put("code", 0);
			ret.put("msg","发送成功");
		}else if (retCode.equals("160050")) {
			ret.put("code",1);
			ret.put("msg","短信发送失败，请重试");
		}else if (retCode.equals("160039") || retCode.equals("160040") || retCode.equals("160041")) {
			ret.put("code",2);
			ret.put("msg","您今天获取验证码的次数过多，请隔天再试");
		}else if (retCode.equals("160038")) {
			ret.put("code",2);
			ret.put("msg","验证码发送过频繁，请稍后再试");
		}else if (retCode.equals("160042")) {
			ret.put("code",2);
			ret.put("msg","号码格式有误，请检查输入");
		}else if (retCode.equals("160000")) {
			ret.put("code",2);
			ret.put("msg","运营商忙碌中，请稍后再试");
		}else {
			ret.put("code",2);
			ret.put("msg","系统忙碌中，请稍后再试");
		}
		return ret;
	}
	
	/**
	 * Status of the sender
	 * @return
	 */
	public String getStatus(){
		try{
			return "";
		}catch(Throwable t){
			logger.error("Fail to check connection status of transport of mail session", t);
			return "Fail to get connection status:"+ t;
		}
	}
	
	public static void main(String[] args) throws Exception{
		YunTXSMSSender y=new YunTXSMSSender();
		y.init();
		y.sendBatchMessage("{templateId:66870,datas:['5050',10]}","18103888786");
	}
	
}
