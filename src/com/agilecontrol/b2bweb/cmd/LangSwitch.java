package com.agilecontrol.b2bweb.cmd;

import java.util.ArrayList;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONArray;
import org.json.JSONObject;

import com.agilecontrol.b2b.cmd.ObjectGet;
import com.agilecontrol.b2b.cmd.Search;
import com.agilecontrol.b2b.schema.Table;
import com.agilecontrol.b2b.schema.TableManager;
import com.agilecontrol.b2bweb.WebController;
import com.agilecontrol.nea.core.control.web.SessionContextManager;
import com.agilecontrol.nea.core.control.web.UserWebImpl;
import com.agilecontrol.nea.core.control.web.WebUtils;
import com.agilecontrol.nea.core.db.oracle.QDate;
import com.agilecontrol.nea.core.util.ConfigValues;
import com.agilecontrol.nea.core.util.WebKeys;
import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.nea.util.Tools;
import com.agilecontrol.nea.util.Validator;
import com.agilecontrol.phone.Admin;
import com.agilecontrol.phone.CmdHandler;
import com.agilecontrol.phone.CmdResult;
import com.agilecontrol.phone.LanguageManager;
import com.agilecontrol.phone.MarketManager;
import com.agilecontrol.phone.PhoneController;
import com.agilecontrol.phone.PhoneUtils;

/**

h1. 切换语言

h2. 输入

> {cmd:"b2b.user.switchlang", code,langid}

*code* 要切换的语言的code, b_language.name, 优先于langid
*langid* id 

h2. 输出

code, message

 * @author yfzhu
 *
 */
@Admin(mail="yfzhu@lifecycle.cn")
public class LangSwitch extends CmdHandler {
	/**
	 * 
	 */
	public CmdResult execute(JSONObject jo) throws Exception {
		HttpServletRequest req = event.getContext().getHttpServletRequest();
		Locale locale=null;
		locale=req.getLocale();
		
		int newLangId=-1;
		String code=jo.optString("code");
		if(Validator.isNotNull(code)){
			newLangId=LanguageManager.getInstance().getLanguageId(code);
		}else{
			newLangId=jo.optInt( "langid",-1);
		}
		if(newLangId==-1) throw new NDSException("Need code/langid");
		Locale newLocale=LanguageManager.getInstance().getLocale(newLangId);
		
		
		engine.executeUpdate("update users set b_language_id=? where id=?", new Object[]{newLangId, usr.getId()}, conn);
		
		//web session
		WebUtils.getSessionContextManager(req.getSession(true));
		req.getSession().setAttribute(org.apache.struts.Globals.LOCALE_KEY,newLocale);
		SessionContextManager manager= (SessionContextManager)req.getSession().getAttribute(WebKeys.SESSION_CONTEXT_MANAGER);
		UserWebImpl user=((UserWebImpl) manager.getActor(com.agilecontrol.nea.core.util.WebKeys.USER));
		Locale userOldLocale=user.getLocale();
		user.setLocale(newLocale);
		
		//remove userobj from jedis, so will reload lang to cache
		jedis.del("usr:"+ usr.getId());
		
		//update token session redis obj
		String key= "usr:"+usr.getToken();
		logger.debug("langswitch user id="+usr.getId()+" locale from "+ locale.toString()+"(userweb:"+userOldLocale+",hashcode="+ user.hashCode()+") to new "+ newLocale+", and "+key+".lang_id="+ newLangId);
		jedis.hset(key, "lang_id",String.valueOf( newLangId));
		
		return CmdResult.SUCCESS;
	}

}













