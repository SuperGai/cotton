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
 * 仅用于网页测试，完成指定微信号的登陆
req
{unionid:"randomstr", demo: true}
randomstr - 是手机的的一个固定uuid
deom - true 不是true就报错，true的时候系统将从随机demo账号中分配出一个独用ID，在1小时内不给其他人使用

 

res:
	 * @return {id, nickname,imageur, recom_u_id, token,  com:{id, name}, store:{id, name}, recom_u:{unkname --user nick name--, id, name(com attributes)}}
	 * 如果com_id是0，表示当前用户尚未创建/加入商家，需要考虑推荐人的商家显示, 如果recom_u_id为空，表示没有推荐人

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
	 * 读取配置表 ad_sql#demo:unionids, 尝试在redis中找到一个闲置的unionid，占用并设置为自己， 
	 * redis key demo:$deviceid, value: unionid, timeout: 第二天凌晨12:00（并到时踢掉他) 
	 * redis 中读取并缓存 demo:unionids list type， 如果用光了，就会再次从数据库中取全再来（这时部分客户端就在重用unionid）
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
		jedis.expire(key, seconds);//当天24点清除
		
		return unionId;
		
	}
	
	@Override
	public CmdResult execute(JSONObject jo) throws Exception {
		
		String deviceId= jo.optString("deviceid");
		String unionId=jo.optString("unionid");
		if(Validator.isNull(deviceId) && Validator.isNull(unionId)) throw new NDSException("专供演示用，请提供deviceid|unionid");
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
