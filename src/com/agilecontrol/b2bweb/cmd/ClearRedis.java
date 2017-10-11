package com.agilecontrol.b2bweb.cmd;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONArray;
import org.json.JSONObject;

import com.agilecontrol.b2b.cmd.ObjectGet;
import com.agilecontrol.b2b.cmd.Search;
import com.agilecontrol.b2bweb.DimDefTranslator;
import com.agilecontrol.b2bweb.DimTranslator;
import com.agilecontrol.b2bweb.WebController;
import com.agilecontrol.nea.core.control.web.SessionContextManager;
import com.agilecontrol.nea.core.control.web.UserWebImpl;
import com.agilecontrol.nea.core.control.web.WebUtils;
import com.agilecontrol.nea.core.db.oracle.QDate;
import com.agilecontrol.nea.core.schema.TableManager;
import com.agilecontrol.nea.core.security.Directory;
import com.agilecontrol.nea.core.security.SecurityUtils;
import com.agilecontrol.nea.core.security.User;
import com.agilecontrol.nea.core.security.UserManager;
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

h1. Clear 清除Redis缓存

h2. web命令

在登陆后的左下角web命令行，支持以下关键命令:

> bb -c clear [key]

key可以含有通配符，如：act:* 表示所有的活动，b2b:* 表示所有的分类树，举例:

> bb -c clear pdt:*

如果不提供key参数，将读取ad_sql#clear_redis_keys 的配置，其中放置的是jsonarray of string，
 每个string 都是含通配符的key, 默认配置为:
>["b2b:*", "a*", "b*", "c*", "p*", "sys*"]
表示几乎清除了所有的redis key（排除user:*)

h2. 市场表上的列表按钮

上述命令行业配置为 市场表(b_market)的列表按钮 《清除基础资料缓存》，要求操作用户是root或对b_market表有修改权限的人

h2. 自动的缓存清除

另外，在framework5中，通过注入SchemaActionListener，监听了所有的表的修改和删除提交动作，
将定位redis中以表的别名+":*" 的key，全部清除。注意，不是所有的表都有完全对应的redis key，
比如 b_market就没有。而商品分类树也不是标准的key，而是以b2b:起头

 * @author yfzhu
 *
 */
@Admin(mail="yfzhu@lifecycle.cn")
public class ClearRedis extends CmdHandler {
	/**
	 * 
	 */
	public CmdResult execute(JSONObject jo) throws Exception {
		
		HttpServletRequest req = event.getContext().getHttpServletRequest();
		UserWebImpl userWeb =event.getContext().getUserWeb();

		if(!userWeb.getUserName().equals("root")  && !usr.getName().equals("root")){
			//user should has permission to change b_market
			userWeb.checkPermission(TableManager.getInstance().getTable("b_market").getSecurityDirectory()	,Directory.WRITE);
		}

		String key=jo.optString("args");
		long cnt=0;
		if(Validator.isNotNull(key)){
			if(key.equals("*")) throw new NDSException("不允许全清");
			LanguageManager.getInstance().clear(conn);
			cnt+=delKeyPattern(key);
		}else{
			JSONArray keys=(JSONArray) PhoneController.getInstance().getValueFromADSQLAsJSON("clear_redis_keys", conn, false);
			for(int i=0;i<keys.length();i++){
				key= keys.getString(i);
				cnt+=delKeyPattern(key);
			}
			//还包括dim,lang
			LanguageManager.getInstance().clear(conn);
			DimTranslator.getInstance().clear(conn);
			DimDefTranslator.getInstance().clear(conn);
			
			PhoneController.getInstance().clear();
			com.agilecontrol.b2b.schema.TableManager.getInstance().reload();
            
		}
		MarketManager.getInstance().clear(conn);
		JSONObject ret=new JSONObject();
		String message="清除redis key合计"+ cnt+"个";
		ret.put("message", message);//这是为shell脚本能看到提示
		CmdResult cr=new CmdResult(0, message/*这是为了ad_action按钮能看到提示*/,ret);
		
		return cr;
	}
	
	private long delKeyPattern(String keyPatter) throws Exception{
		Set<String> set=jedis.keys(keyPatter);
		if(set.size()>0){
			String[] keyall=new String[set.size()];
			set.toArray(keyall);
			long cnt=jedis.del(keyall);
			logger.debug("delete jedis keys:"+ Tools.toString(keyall)+", cnt="+ cnt);
			return cnt;
		}
		return 0;
	}
}













