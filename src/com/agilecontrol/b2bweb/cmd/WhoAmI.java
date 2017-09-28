package com.agilecontrol.b2bweb.cmd;

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

h1. 用户信息

h2. 输入

> {cmd:"b2b.user.whoami"}

h2. 输出

<pre>
{
	name, truename,token, lang_id, langs, market
}
</pre>

*name* - string 系统账号名
*truename* - string 姓名
*token* - string 登陆token
*lang_id* - int 当前语言id
*lang* - jsonobj {id,code, desc} 当前用户可选择语言, id需要在langswitch的时候提供
*market - jsonobj {id,desc,code currency}
> *id* - int 当前所在市场id，b_market
> *desc* - string 当前所在市场名称  
> *currency* - String 市场货币符号，如￥，$
* balance  经销商余额  




 * @author yfzhu
 *
 */
@Admin(mail="yfzhu@lifecycle.cn")
public class WhoAmI extends CmdHandler {
	/**
	 * 
	 */
	public CmdResult execute(JSONObject jo) throws Exception {
		Table table=TableManager.getInstance().getTable("usr");
		
		JSONObject user=PhoneUtils.fetchObject(table, usr.getId(), conn, jedis);
		JSONObject market=new JSONObject();
		market.put("desc",  MarketManager.getInstance().getMarketDesc(usr.getMarketId()));
		market.put("currency",  MarketManager.getInstance().getCurrency(usr.getMarketId()));
		market.put("id", usr.getMarketId());
		market.put("code", MarketManager.getInstance().getMarketName(usr.getMarketId()));
		
		user.put("market", market);
		JSONObject lang=new JSONObject();
		int langid=usr.getLangId();
		lang.put("id", langid);
		lang.put("code", LanguageManager.getInstance().getLanguageName(langid));
		lang.put("desc",LanguageManager.getInstance().getLanguageDesc(langid));
		
		user.put("lang", lang);
		user.put("lang_id", langid);//保证都是从usr当前对象，这是session级的对象，中获取，而不是 user:$id, 对应的是db级的id 
		
		user.put("token", usr.getToken());
		user.put("session", event.getContext().getSession().getId());
		user.put("logintime", new QDate(event.getContext().getSession().getCreationTime()));
	    
		
		String sql="select  fc.feeremain  from users, FA_CUSTOMER fc where users.c_customer_id=fc.c_customer_id and users.id="+usr.getId();
		String balance=engine.doQueryString(sql, new Object[]{}, conn);
		user.put("balance", balance);
		
		return new CmdResult(user);
	}

}













