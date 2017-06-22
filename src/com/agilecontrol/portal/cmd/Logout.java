package com.agilecontrol.portal.cmd;

import javax.servlet.http.Cookie;

import org.json.JSONArray;
import org.json.JSONObject;

import com.agilecontrol.b2b.cmd.ObjectGet;
import com.agilecontrol.b2b.cmd.Search;
import com.agilecontrol.b2b.schema.Table;
import com.agilecontrol.nea.core.control.web.WebUtils;
import com.agilecontrol.nea.core.query.QueryEngine;
import com.agilecontrol.nea.core.schema.TableManager;
import com.agilecontrol.nea.core.security.User;
import com.agilecontrol.nea.core.security.UserManager;
import com.agilecontrol.nea.core.security.pwd.PwdEncryptor;
import com.agilecontrol.nea.core.util.CookieKeys;
import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.phone.Admin;
import com.agilecontrol.phone.CmdHandler;
import com.agilecontrol.phone.CmdResult;
import com.agilecontrol.phone.PhoneController;
import com.agilecontrol.phone.PhoneUtils;

/**

h1. 退出系统

>{cmd:"b2b.logout"}

返回 无

 * @author stao
 *
 */
@Admin(mail="sun.tao@lifecycle.cn")
public class Logout extends com.agilecontrol.b2b.cmd.Logout {
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
