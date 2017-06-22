package com.agilecontrol.portal.cmd;

import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONObject;

import com.agilecontrol.nea.core.control.event.DefaultEventContext;
import com.agilecontrol.nea.core.control.event.DefaultWebEvent;
import com.agilecontrol.nea.core.control.util.ValueHolder;
import com.agilecontrol.nea.core.security.SecurityUtils;
import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.phone.CmdHandler;
import com.agilecontrol.phone.CmdResult;
import com.agilecontrol.phone.PhoneUtils;

/**
 * 发送短信验证码-验证码存放redis
 * @author stao
 *
 */  
public class GetTelCode extends CmdHandler {
	@Override
	public boolean allowGuest() {
		return true;
	}
	
	@Override
	public CmdResult execute(JSONObject jo) throws Exception {
		String phoneNum = jo.optString("telphone", "");
		String checkCode=PhoneUtils.createCheckCode();

		
		
		/*//容联云 那一套
		int templateId = PhoneConfig.SMS_TEMPLATEID;
		JSONArray data = new JSONArray();
		data.put( checkCode);
		data.put(PhoneConfig.SMS_READTIMEOUT+"秒");
		//存redis
		JSONObject robj = PhoneUtils.sendSMS(phoneNum,checkCode, templateId,jedis,data);*/
		
		
		
		//通用
		/*JSONArray idArray= engine.doQueryObjectArray("select id from P_CU_APPLY_INFO where mobil=?",new Object[]{phoneNum}, conn);
		if(null != idArray && idArray.length()>0){
			throw new NDSException("该手机号已被使用！");
		}*/
		//使用root账号发送命令
		String name ="root";
		DefaultEventContext dec = new DefaultEventContext(SecurityUtils.getUser(name));
		dec.setConnection(conn);
		DefaultWebEvent evt = new DefaultWebEvent("CommandEvent", dec);
		evt.setParameter("command", "com.agilecontrol.nea.monitor.SendSMS");
		JSONObject rs = new JSONObject();
		rs.put("phone", phoneNum);
		//短信模板建议配置成ad_sql 存volicity中，可以更改短信的内容
		String before =engine.doQueryString("select value from ad_sql where name =?",new Object[]{"portal:register:msg:content:before"}, conn);
		String after =engine.doQueryString("select value from ad_sql where name =?",new Object[]{"portal:register:msg:content:after"}, conn);
		int time =Integer.parseInt(engine.doQueryString("select value from ad_sql where name =?",new Object[]{"portal:register:msg:time"}, conn));
		String msgcontent =before +checkCode +after;
		logger.debug("---msg:"+msgcontent);
		logger.debug("------------time:"+time);
		rs.put("content",msgcontent);
		rs.put("onlyMsg", true);
		
		evt.put("jsonObject", rs);
		
		ValueHolder vh = helper.handleEvent(evt);
		JSONObject res=(JSONObject)vh.get("restResult");
		logger.debug("\n ***********************res:"+res);

		//写入redis
				String key="portalregcheckcode:"+ phoneNum;
				HashMap<String, String> hash=new HashMap();
				hash.put("checkcode", checkCode	);
				hash.put("fail","0");
				jedis.hmset(key, hash);
				jedis.expire(key, time * 60);
		return new CmdResult(res);
	}

}
