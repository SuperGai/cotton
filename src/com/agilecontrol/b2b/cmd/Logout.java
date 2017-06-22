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
 h1. Logout 主动退出系统
 
 h3. 条件
 
 在设置界面有退出按钮
 
 h3. 操作
 
 * 移除 jedis: uid:$token
 * invalidate当前的session
 * 删除cookie
 
 h3. 输入
 
 >{keepsession}
 
 *keepsession* - boolean  是否保留session，用于当portal里的logout激活当前命令的时候。因为portal本身会去做
 invalidate session的操作，参见com.agilecontrol.nea.core.action.LogoutAction
 
 h3. 输出
 
 >{msg}
 * msg - 对应的消息

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
	            cookie.setMaxAge(0);// 立即销毁cookie
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
		CmdResult res=new CmdResult(new JSONObject().put("msg", "已经退出"));
		return res;
	}

}
