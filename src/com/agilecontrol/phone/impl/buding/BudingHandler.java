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
 * �ӿ���չCmdHandler������������������״̬����ģʽ��������Ϣ���ж�Ӧ�ú�����֤�ӿ�
signkey: �û�����Ϣǩ��key�����û���¼��ʱ����ͻ��˴洢�����ڷ���������״̬���ͻ�������Ҳ�����¼��֤��ÿ������������������
����Ҫ�Բ��ֹؼ���Ϣ����ǩ�����㷨��
sign=md5(userid+signkey+timestamp)��timestamp - long, etc time�����롣
name���û���¼��, sign ��Ҫ������ÿ��msg��
 * 
 * @author yfzhu
 *
 */
public abstract class BudingHandler extends CmdHandler {
	
	/**
	 * �Ƿ�����sign����Ч��У��
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
	 * ����ָ����ļ�¼����, ���Զ�����msgid, ����nextid ����
	 * @param topic ��Ҫ���͵�topic
	 * @param tableName
	 * @param action "add","modify","delete"
	 * @param obj ָ����������ֶ�ֵ
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
	 * ִ�д洢����������RunJSON��ģʽ
	 * �洢����:  proc(long userId, jo in clob, jo out clob)  as 
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
	 * ��֤
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
	  		throw new NDSException("�ֻ�ϵͳʱ�䲻�ԣ��ͱ�׼ʱ�����10��������");
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
			throw new NDSException("ǩ����Ч");
		}
		
	}
	/**
	 * �����ݿ��ȡ�û�signkey
	 * @param userId
	 * @return
	 * @throws Exception
	 */
	private String loadSignKey(long userId) throws Exception{
		return (String)engine.doQueryOne("select passwordhash from users where id=?", new Object[]{userId}, conn);
	}
}