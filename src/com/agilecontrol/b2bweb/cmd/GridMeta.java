package com.agilecontrol.b2bweb.cmd;

import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONObject;

import com.agilecontrol.b2b.cmd.ObjectGet;
import com.agilecontrol.b2b.cmd.Search;
import com.agilecontrol.b2b.schema.Table;
import com.agilecontrol.b2bweb.WebController;
import com.agilecontrol.nea.core.schema.ClientManager;
import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.nea.util.Tools;
import com.agilecontrol.nea.util.Validator;
import com.agilecontrol.phone.Admin;
import com.agilecontrol.phone.CmdHandler;
import com.agilecontrol.phone.CmdResult;
import com.agilecontrol.phone.PhoneController;
import com.agilecontrol.phone.PhoneUtils;
import com.agilecontrol.phone.PhoneController.SQLWithParams;

/**

h1. 获取在线编辑矩阵的定义

h2. 输入

> {cmd:"b2b.grid.meta", key, canchange}

*canchange* 是否允许修改key对应的选项值，全局控制
*key*  是在ad_sql#online_edit_kvs定义的key，可以有多个，对应的默认值可以在这里传输过去。如需要默认设置活动id=101，且不允许修改：
> {cmd:"b2b.grid.meta", "acts": 101, canchange: false}

h2. 输出

>{kvs: [{key, desc, values, type, default }]}

*key* - 客户端和服务器表示的关键字
*desc* - 客户端界面显示名称
*values* - array of array,  【【v1,v2】】 其中第一列是界面可见的描述，第二列是后台接受的value, 对于acts，首列是活动名称，第二列是act.id
*type* - string: 可选："checkbox"|"select"
*default* - 缺省值，如果设置，客户端应该将对应值默认选中，对应checkbox类，default值是boolean或0,1
*canchange* - boolean 是否允许修改，如果false，表示当前选项不能修改

h2. 定义

kvs的定义从ad_sql#online_edit_kvs 读取

kv配置项目定义: ad_sql#online_edit_kvs, kv 用于界面过滤选项，有两种形式：checkbox，和select, checkbox表示选中或不选中。select是类似活动选择的模式，select形式必须配置values对应sql

[{key, desc, filtersql, valuesql, default}]

*key* - 客户端和服务器表示的关键字
*desc* - 客户端界面显示名称
*filtersql* - 将拼接到商品过滤语句中的sql部分 ad_sql的name，指定对应sql语句，sql语句的格式： select 1 from xxx where xxx.pdtid=b_mk_pdt.id and xxx.ownerid=?, 支持的？对应的变量：

对应B2B项目
$usrid

对于订货会项目
$funitid
$fairid
$usrid

*valuesql* - 生成界面选项的sql，如果设置，界面必使用select模式，sql格式规范：select description,value from xxxx, 其中第一列是界面可见的描述，第二列是后台接受的value, 对于acts，首列是活动名称，第二列是act.id
*default* - 缺省值，可以不设置，对应checkbox类，default值是boolean或0,1



 * @author yfzhu
 *
 */
@Admin(mail="yfzhu@lifecycle.cn")
public class GridMeta extends ObjectGet {
	
	/**
	 * 
	 */
	public CmdResult execute(JSONObject jo) throws Exception {
		
		boolean canChange=jo.optBoolean("canchange", true);
		vc.put("marketid", usr.getMarketId());
		
		//2016-12-9 lsh
		if(Validator.isNull(jo.optString("table"))) throw new NDSException("@b2bedit-config@"+"ad_sql#grid:"+jo.optString("table")+":online_edit_kvs"+"@b2bedit-found@");
		
		JSONArray kvDefs=(JSONArray) PhoneController.getInstance().getValueFromADSQLAsJSON("grid:"+jo.optString("table")+":online_edit_kvs", conn);
		JSONArray kvs=new JSONArray();
		for(int i=0;i<kvDefs.length();i++){
			//{key, desc, filtersql, valuesql, default}
			JSONObject kvDef=kvDefs.getJSONObject(i);
			//{key, desc, values, type, default }
			JSONObject kv=new JSONObject();
			String key=kvDef.getString("key");
			kv.put("key", key);
			kv.put("desc", kvDef.getString("desc"));
			String defaultValue= jo.optString(key);
			String valueSQL=kvDef.optString("valuesql");//ad_sql#name
			if(Validator.isNull(valueSQL)){
				kv.put("type", "checkbox");
				
			}else{
				//其中第一列是界面可见的描述，第二列是后台接受的value
				vc.put("uid", usr.getId());
				vc.put("marketid", usr.getMarketId());
				JSONArray values=(JSONArray)PhoneController.getInstance().getDataArrayByADSQL(valueSQL, vc, conn, false);
				kv.put("type", "select");
				kv.put("values", values);
			}
			//取默认值
			if(Validator.isNotNull(defaultValue))kv.put("default", defaultValue);
			if(!canChange) kv.put("canchange", false);
			kvs.put(kv);
		}
		
		
		JSONObject obj=new JSONObject();
		obj.put("kvs", kvs);
		
		return new CmdResult(obj);
	}
}













