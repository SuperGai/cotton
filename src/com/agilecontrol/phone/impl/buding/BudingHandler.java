package com.agilecontrol.phone.impl.buding;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;

import org.json.JSONObject;

import com.agilecontrol.nea.core.util.ConfigValues;
import com.agilecontrol.nea.util.MD5Sum;
import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.phone.CmdHandler;
import com.agilecontrol.phone.CmdResult;
import com.agilecontrol.phone.PhoneController;
/**
 * 
 * 接口扩展CmdHandler。布丁服务器端是无状态运行模式，所有消息体中都应该含有认证接口
signkey: 用户级消息签名key，在用户登录的时候给客户端存储。由于服务器端无状态，客户端连接也无需登录验证，每个发给服务器的请求，
都需要对部分关键信息进行签名。算法：
sign=md5(userid+signkey+timestamp)，timestamp - long, etc time到毫秒。
name即用户登录名, sign 需要附着在每个msg中
 * 
 * @author yfzhu
 *
 */
public abstract class BudingHandler extends CmdHandler {
	
	/**
	 * 是否启动sign的有效性校验
	 */
	private static boolean ENABLE_SIGN_CHECK=false;
	private static long NETWORK_DELAY_SECONDS=1000*60*10;// 10 mininutes 
	/**
	 * Guest can execute this task, default to false
	 * @return
	 */
	public boolean allowGuest(){
		return true;
	}
	/**
	 * 发送指定表的记录动作, 将自动创建msgid, 利用nextid 技术
	 * @param topic 需要发送的topic
	 * @param tableName
	 * @param action "add","modify","delete"
	 * @param obj 指定表的所有字段值
	 * @throws Exception
	 */
	protected void publish(String topic, String tableName, String action,JSONObject obj) throws Exception{
		JSONObject payload=new JSONObject();
		payload.put("from", 0);
//		payload.put("msgid", PhoneController.getInstance().getDistributedNextId("msgid"));
		JSONObject msg=new JSONObject();
		msg.put("table", tableName.toLowerCase());
		msg.put("action", action);
		msg.put("obj", obj);
		payload.put("msg", msg);
		//PhoneController.getInstance().getMQClient().publish(topic,  payload.toString().getBytes());
	}
	
	/**
	 * 执行存储函数，类似RunJSON的模式
	 * 存储函数:  proc(long userId, jo in clob, jo out clob)  as 
	 * @param function
	 * @param userId
	 * @param param
	 * @return
	 * @throws Exception
	 */
	protected String execDbClobProc(String function, long userId, JSONObject param)throws Exception{
		ArrayList params=new ArrayList();
		params.add( userId);
		params.add(new StringBuilder(param.toString()));
		params.add(StringBuilder.class);//this is out
		
		ArrayList res=engine.executeStoredProcedure(function, params, conn);
		return (String)res.get(0);
		
	}
	/**
	 * 验证
	 * @param jo
	 * @return null if valid 
	 */
	public void prepare(JSONObject jo) throws Exception{
		if(!ENABLE_SIGN_CHECK) return;
		long userId= jo.optLong("userid");
		long d= jo.optLong("time");//milliseconds to etc
	  	  // range
	  	if( System.currentTimeMillis()- d < -NETWORK_DELAY_SECONDS || System.currentTimeMillis()-d >NETWORK_DELAY_SECONDS){
	  		logger.debug(" range test:"+(System.currentTimeMillis()- d));
	  		throw new NDSException("手机系统时间不对，和标准时间相差10分钟以上");
	  	}
	  	
	  	//load signkey from user
	  	String signKey=loadSignKey(userId);//(String)CacheManager2.getInstance().get("key."+ userId);
//	  	if(signKey==null){
//	  		
//	  		CacheManager2.getInstance().put("key."+userId, signKey);
//	  	}
	  	String sign=MD5Sum.toCheckSumStr(userId+signKey+d );
		String joSign=jo.optString("sign");
		if(!sign.equals(joSign)){
			throw new NDSException("签名无效");
		}
		
	}
	/**
	 * 从数据库获取用户signkey
	 * @param userId
	 * @return
	 * @throws Exception
	 */
	private String loadSignKey(long userId) throws Exception{
		return (String)engine.doQueryOne("select passwordhash from users where id=?", new Object[]{userId}, conn);
	}
}