package com.agilecontrol.b2b.cmd;

import javax.servlet.http.Cookie;

import org.json.JSONObject;

import com.agilecontrol.nea.core.util.CookieKeys;
import com.agilecontrol.nea.util.Validator;
import com.agilecontrol.phone.CmdHandler;
import com.agilecontrol.phone.CmdResult;
import com.agilecontrol.phone.PhoneConfig;
import com.agilecontrol.phone.wechat.auth.BudingAuth;

/**
 * 
 h1. Logout �����˳�ϵͳ
 
 h3. ����
 
 �����ý������˳���ť
 
 h3. ����
 
 * �Ƴ� jedis: uid:$token
 * invalidate��ǰ��session
 * ɾ��cookie
 
 h3. ����
 
 >{keepsession}
 
 *keepsession* - boolean  �Ƿ���session�����ڵ�portal���logout���ǰ�����ʱ����Ϊportal�����ȥ��
 invalidate session�Ĳ������μ�com.agilecontrol.nea.core.action.LogoutAction
 
 h3. ���
 
 >{msg}
 * msg - ��Ӧ����Ϣ

 * @author yfzhu
 *
 */
public class Logout extends CmdHandler {
	
	@Override
	public CmdResult execute(JSONObject jo) throws Exception {
		try{
			
			Cookie cookie=CookieKeys.getCookieObj(event.getContext().getHttpServletRequest(), "token");
			if(cookie!=null){
				cookie.setValue(null);
	            cookie.setMaxAge(0);// ��������cookie
	            cookie.setPath("/");
	            event.getContext().getHttpServletResponse().addCookie(cookie);
			}
			if(!jo.optBoolean("keepsession", false))
				this.event.getContext().getSession().invalidate();
			String token="usr:"+usr.getToken();
			long cnt=jedis.del(token);
			logger.debug("del jedis token:"+ token+"(cnt="+ cnt+")");
		}catch(Throwable tx){
			logger.error("fail to logout", tx);
		}
		CmdResult res=new CmdResult(new JSONObject().put("msg", "�Ѿ��˳�"));
		return res;
	}

}
