package com.agilecontrol.portal.cmd;

import java.util.ArrayList;

import javax.servlet.http.Cookie;

import org.json.JSONArray;
import org.json.JSONObject;

import com.agilecontrol.b2b.cmd.ObjectGet;
import com.agilecontrol.b2b.cmd.Search;
import com.agilecontrol.b2b.schema.Table;
import com.agilecontrol.b2b.schema.TableManager;
import com.agilecontrol.b2bweb.WebController;
import com.agilecontrol.nea.core.control.event.DefaultWebEvent;
import com.agilecontrol.nea.core.control.util.ValueHolder;
import com.agilecontrol.nea.core.db.oracle.QDate;
import com.agilecontrol.nea.core.rest.SipStatus;
import com.agilecontrol.nea.core.util.CookieKeys;
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

h1. �û���Ϣ

h2. ����

> {cmd:"portal.user.whoami"}

h2. ���

<pre>
{
	name, truename,token, lang_id, langs, market
}
</pre>

*name* - string ϵͳ�˺���
*truename* - string ����
*token* - string ��½token
*lang_id* - int ��ǰ����id
*lang* - jsonobj {id,code, desc} ��ǰ�û���ѡ������, id��Ҫ��langswitch��ʱ���ṩ
> *id* - int ��ǰ�����г�id��b_market
> *desc* - string ��ǰ�����г�����  
> *currency* - String �г����ҷ��ţ��磤��$




 * @author stao
 *
 */
@Admin(mail="sun.tao@lifecycle.cn")
public class WhoAmI extends CmdHandler {
	/**
	 * 
	 */
	public CmdResult execute(JSONObject jo) throws Exception {
		Table table=TableManager.getInstance().getTable("usr");
		
		JSONObject user=PhoneUtils.fetchObject(table, usr.getId(), conn, jedis);		
		JSONObject lang=new JSONObject();
		int langid=usr.getLangId();
		lang.put("id", langid);
		lang.put("code", LanguageManager.getInstance().getLanguageName(langid));
		lang.put("desc",LanguageManager.getInstance().getLanguageDesc(langid));
		
		user.put("lang", lang);
		user.put("lang_id", langid);//��֤���Ǵ�usr��ǰ��������session���Ķ����л�ȡ�������� user:$id, ��Ӧ����db����id 
		
		user.put("token", usr.getToken());
		user.put("session", event.getContext().getSession().getId());
		user.put("logintime", new QDate(event.getContext().getSession().getCreationTime()));
		
		
		return new CmdResult(user);
	}

}













