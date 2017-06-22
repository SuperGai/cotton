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

h1. 语言列表

无需登录

h2. 输入

> {cmd:"b2b.user.langlist"}

h2. 输出

> [{lang}]
*lang* - jsonobj {id,code, desc} 当前用户可选择语言, id需要在langswitch的时候提供


 * @author yfzhu
 *
 */
@Admin(mail="yfzhu@lifecycle.cn")
public class LangList extends CmdHandler {
	/**
	 * Guest can execute this task, default to false
	 * 
	 * @return
	 */
	public boolean allowGuest() {
		return true;
	}

	/**
	 * 
	 */
	public CmdResult execute(JSONObject jo) throws Exception {
		HttpServletRequest req = event.getContext().getHttpServletRequest();
		JSONArray langs=new JSONArray();
		for(int langid:LanguageManager.getInstance().getAllLanguageIds()){
	 		JSONObject lang=new JSONObject();
	 		lang.put("id", langid);
	 		lang.put("code", LanguageManager.getInstance().getLanguageName(langid));
	 		lang.put("desc",LanguageManager.getInstance().getLanguageDesc(langid));
	 		langs.put(lang);
		}
		
		return new CmdResult(langs);
	}

}













