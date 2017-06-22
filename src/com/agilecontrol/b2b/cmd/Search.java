package com.agilecontrol.b2b.cmd;

import org.json.*;

import java.util.*;

import com.agilecontrol.b2b.query.SearchRequest;
import com.agilecontrol.b2b.query.SearchResult;
import com.agilecontrol.b2b.schema.*;
import com.agilecontrol.phone.UserObj;
import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.nea.util.StringUtils;
import com.agilecontrol.nea.util.Validator;
import com.agilecontrol.phone.CmdHandler;
import com.agilecontrol.phone.CmdResult;
import com.agilecontrol.phone.PhoneConfig;
import com.agilecontrol.phone.PhoneController;
import com.agilecontrol.phone.PhoneUtils;
import com.agilecontrol.phone.UUIDUtils;

/**
 * 
条件
列表结果都可以通过Search来返回

输入
{
    table, exprs, querystr,  maxcnt, mask, usepref, orderby,pagesize
}
table - String 搜索的表名，如: emp
exprs - Expression 精确到字段的过滤条件，元素key-value为 主表字段名和对应值, 多个条件表示并且and
querystr - String 通用查询字符串，与exprs为and关系
maxcnt - int 最大查询结果数，有时候客户端控制1，后台最大为2000，前台设置的值不能超过2000，默认2000
pagesize - 页面数, 将对应返回的cnt
mask - string 字段取值， 可选"list"|"obj", 默认为"list", 有时客户端以卡片形式显示结果，就需要配置为 obj，以便获取更多显示字段数据（见TableManager#Column#mask)
usepref - boolean，是否使用常用查询条件，默认为true，在部分表上有设置常用查询条件，将结合此条件进行过滤
orderby - string, 部分定制页面上有orderby选项，以关键字进行匹配
举例:

   { 
        table:"spo", exprs: {st_id: 33, emp_id: 20, state: ["V","O", "S"], price: "12~20"}, 
        querystr: "13918891588", usepref: false, orderby: "stocktime_first"}
    }  

Expression
格式

   {key: value}
key - 字段名称，需要在当前主表上
value - 字段的值，支持数组或单值，数组表示任意一个匹配
举例: {"st_id": 13} 表示要求st_id=13


返回
{
    total, start, cnt, cachekey, $table+"_s" 
}
total - 当前查询结果计数，最大为2000
start - 起始行,0 开始计数
cnt - 当前结果的行数
cachekey - 格式"list:$table:$uid:$uuid"，是指在redis缓存的查询结果的key，redisk中存放List of Id， redis的一个列表查询结果最多缓存30分钟(inactive时间)，timeout后客户端将报错
$table+"_s" - 是返回的对象类别的json key名
举例:

{
    total:1000, start:0, cnt: 20, cachekey: "list:pdt:2:241p4iafiaf", spo_s:[{id,name,value,img}]}}
}
分页访问结果
当前查询结果是个分页列表，当客户端需要获取列表中某一段数据的时候，可以发起以下请求

  {cmd, cachekey, start, cnt}
cmd - 固定为"Search"
cachekey - 是在Seach返回的cachekey, 这是区分首次search和后续search的关键属性，如果识别，就作为获取列表的方式
start - 起始id， start from 0
cnt - 需要的记录数，最后一页不一定有这么多，以返回的结果为准
举例

{cmd: “SearchResult”, cachekey:"list:$table:$uid:$uuid", start: 60, cnt: 20 }
返回的结果:

{
    total, start, cnt, cachekey, $table+"_s" 
}   
 * @author yfzhu
 *
 */
public class Search extends CmdHandler {
	/**
	 * 对搜索的SearchResult.toJSON对象进行改装，以符合客户端要求
	 * @param ret redis SearchResult.toJSON，直接再次编辑
	 * @throws Exception
	 */
	protected void postAction(JSONObject ret) throws Exception{
		
	}
	
	/**
	 * 对输入的查询条件进行重构，以支持特殊的查询请求
	 * @param jo 原始的查询条件，有可能在此地被重构
	 * @return 额外的查询条件，将增加到查询where语句中
	 * key: 是要补充到sql where 部分的clause语句内容，比如 "emp_id=?"
	 * value 是问号对应的实际值，目前key仅支持一个问号比如对应上面的value= 当前emp的id
	 * 
	 */
	protected HashMap<String, Object> reviseSearchCondition(JSONObject jo) throws Exception{
		return null;
	}
	
	@Override
	public CmdResult execute(JSONObject jo) throws Exception {
		SearchResult sr=null;
		
		//如果没有定义table, 将使用class名称中的search前的部分
		Table table=findTable(jo,"Search");
		if(table!=null && Validator.isNull( jo.optString("table"))) 
			jo.put("table", table.getName());
		String cacheKey=jo.optString("cachekey");
		boolean idOnly=jo.optBoolean("idonly",false);
		 
		if(Validator.isNull(cacheKey) || !jedis.exists(cacheKey) /*需求：若key timeout也要重新查*/) {
			//进行搜索构造
			HashMap<String, Object> addParams=reviseSearchCondition(jo);
			sr=this.search(jo, addParams);
		}else{
			
			//验证cacheKey和用户一致
			String[] keyParts=cacheKey.split(":");
			if(keyParts.length!=6) throw new NDSException("错误的cachekey");
			
			if(!"list".equals(keyParts[0]))throw new NDSException("错误的cachekey(list)");
			
			if(!keyParts[2].equals(String.valueOf(usr.getId()))) throw new NDSException("错误的cachekey(uid)");
			
			int start= jo.optInt("start",0);
			int pageSize= jo.optInt("pagesize", 20);
			sr=this.searchByCache(cacheKey, start, pageSize, table, idOnly);
		}
		JSONObject ret= sr.toJSONObject();
		
		//根据配置决定是否组装对象为复杂结构
		JSONArray ja=sr.getData();
		if(!idOnly){
			this.assembleArrayByConfName(ja, "table:"+table.getName() +":assemble:list", table);
			
			//针对每个元素，修改tagcolumn的值, 转string为jsonarray
			for(int i=0;i<ja.length();i++){
				JSONObject one=ja.getJSONObject(i);
				this.reviseColumnsOfJSONTypeValue(one, table);
			}
		}
		postAction(ret);
		CmdResult res=new CmdResult(ret );

		return res;
	}
	
	
	
	
}
