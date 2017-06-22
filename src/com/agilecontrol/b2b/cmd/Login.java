package com.agilecontrol.b2b.cmd;

import java.util.Calendar;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.nea.util.Validator;
import com.agilecontrol.phone.CmdHandler;
import com.agilecontrol.phone.CmdResult;
import com.agilecontrol.phone.PhoneController;
import com.agilecontrol.phone.wechat.auth.AppBudingAuth;
import com.agilecontrol.phone.wechat.auth.BudingAuth;

/**
 * ��������ҳ���ԣ����ָ��΢�źŵĵ�½
req
{unionid:"randomstr", demo: true}
randomstr - ���ֻ��ĵ�һ���̶�uuid
deom - true ����true�ͱ���true��ʱ��ϵͳ�������demo�˺��з����һ������ID����1Сʱ�ڲ���������ʹ��

 

res:
	 * @return {id, nickname,imageur, recom_u_id, token,  com:{id, name}, store:{id, name}, recom_u:{unkname --user nick name--, id, name(com attributes)}}
	 * ���com_id��0����ʾ��ǰ�û���δ����/�����̼ң���Ҫ�����Ƽ��˵��̼���ʾ, ���recom_u_idΪ�գ���ʾû���Ƽ���

 * @author yfzhu
 *
 */
public class Login extends CmdHandler {
	/**
	 * Guest can execute this task, default to false
	 * @return
	 */
	public boolean allowGuest(){
		return true;
	}
	/**
	 * ��ȡ���ñ� ad_sql#demo:unionids, ������redis���ҵ�һ�����õ�unionid��ռ�ò�����Ϊ�Լ��� 
	 * redis key demo:$deviceid, value: unionid, timeout: �ڶ����賿12:00������ʱ�ߵ���) 
	 * redis �ж�ȡ������ demo:unionids list type�� ����ù��ˣ��ͻ��ٴδ����ݿ���ȡȫ��������ʱ���ֿͻ��˾�������unionid��
	 *  
	 * @param deviceId
	 * @return 
	 * @throws Exception
	 */
	private String getPreparedUnionId(String deviceId ) throws Exception{
		String key= "demo:"+ deviceId;
		String value= jedis.get(key);
		if(Validator.isNotNull(value)) return value;
		long cnt=jedis.llen("demo:unionids");
		if(cnt==0){
			//reload all from db
			JSONArray ids= (JSONArray) PhoneController.getInstance().getValueFromADSQLAsJSON("demo:unionids");
			String[] sids=new String[ids.length()];
			for(int i=0;i<ids.length();i++){
				sids[i]=ids.getString(i);
			}
			logger.debug("add "+ ids + " to demo:unionids");
			jedis.lpush("demo:unionids", sids);
		}
		String unionId= jedis.rpop("demo:unionids");
		
		Calendar time=Calendar.getInstance();
		time.setTimeInMillis(new java.util.Date().getTime());
		time.add(Calendar.DATE, 1);
		time.set(Calendar.HOUR_OF_DAY, 0);
		time.set(Calendar.MINUTE, 0);
		time.set(Calendar.SECOND, 0);
		int seconds=(int) ((time.getTimeInMillis() - System.currentTimeMillis())/1000.0);
		
		jedis.set(key, unionId);
		jedis.expire(key, seconds);//����24�����
		
		return unionId;
		
	}
	
	@Override
	public CmdResult execute(JSONObject jo) throws Exception {
		
		String deviceId= jo.optString("deviceid");
		String unionId=jo.optString("unionid");
		if(Validator.isNull(deviceId) && Validator.isNull(unionId)) throw new NDSException("ר����ʾ�ã����ṩdeviceid|unionid");
		if(Validator.isNull(unionId)){
			unionId=getPreparedUnionId(deviceId);
		}
		
		jo= new JSONObject().put("unionid",unionId );
		
		AppBudingAuth ba=new AppBudingAuth(jedis, conn, this.event.getContext().getHttpServletRequest(), this.event.getContext().getHttpServletResponse());
		
		JSONObject ret=ba.accept(jo);
		
		CmdResult res=new CmdResult(ret);
		return res;
	}

}
