package com.agilecontrol.b2b.query;

import org.json.*;

import java.util.*;

import com.agilecontrol.b2b.schema.*;
import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.nea.util.StringUtils;
import com.agilecontrol.nea.util.Validator;
import com.agilecontrol.phone.Admin;
import com.agilecontrol.phone.CmdHandler;
import com.agilecontrol.phone.CmdResult;
import com.agilecontrol.phone.PhoneConfig;
import com.agilecontrol.phone.PhoneController;
import com.agilecontrol.phone.PhoneUtils;
import com.agilecontrol.phone.UUIDUtils;

/**
 * 封装搜索对象

用法：显示当前商家的员工列表, 名称含有“朱叶峰"的

 SearchRequest req=new SearchRequest("emp");
 req.addParam("com_id", usr.getComId()); //当前商家
 req.setQueryString( "朱叶峰");
 SearchResult res=cmd.search(req);
 return res.toJSONObject();

条件
列表结果都可以通过Search来返回

输入
{
    table, exprs, querystr,  maxcnt, mask, usepref, orderby
}
table - String 搜索的表名，如: emp
exprs - Expression 精确到字段的过滤条件，元素key-value为 主表字段名和对应值, 多个条件表示并且and
querystr - String 通用查询字符串，与exprs为and关系
maxcnt - int 最大查询结果数，有时候客户端控制1，后台最大为2000，前台设置的值不能超过2000，默认2000
mask - string 字段取值， 可选"list"|"obj", 默认为"list", 有时客户端以卡片形式显示结果，就需要配置为 obj，以便获取更多显示字段数据（见TableManager#Column#mask)
usepref - boolean，是否使用常用查询条件，默认为true，在部分表上有设置常用查询条件，将结合此条件进行过滤
orderby - string, 部分定制页面上有orderby选项，以关键字进行匹配
举例:

   { 
        table:"spo", expres: {st_id: 33, emp_id: 20, state: ["V","O", "S"], price: "12~20"}, 
        querystr: "13918891588", usepref: false, orderby: "stocktime_first"}
    }  

Expression
格式

   {key: value}
key - 字段名称，需要在当前主表上
value - 字段的值，支持数组或单值，数组表示任意一个匹配
举例: {"st_id": 13} 表示要求st_id=13

 * 
 * 
 * @author yfzhu
 *
 */
@Admin(mail="yfzhu@lifecycle.cn")
public class SearchRequest {
	
	private JSONObject jo=null;
	private JSONObject orderbyDef=null;
	/**
	 * 在什么表上做查询
	 * @param table
	 */
	public SearchRequest(String table)throws JSONException {
		jo=new JSONObject();
		setTable(table);
	}
	/**
	 *@param jo  {
    table, exprs, querystr,  maxcnt, mask, usepref, orderby
}
table - String 搜索的表名，如: emp
exprs - Expression 精确到字段的过滤条件，元素key-value为 主表字段名和对应值, 多个条件表示并且and
querystr - String 通用查询字符串，与exprs为and关系
maxcnt - int 最大查询结果数，有时候客户端控制1，后台最大为2000，前台设置的值不能超过2000，默认2000
mask - string 字段取值， 可选"list"|"obj", 默认为"list", 有时客户端以卡片形式显示结果，就需要配置为 obj，以便获取更多显示字段数据（见TableManager#Column#mask)
usepref - boolean，是否使用常用查询条件，默认为true，在部分表上有设置常用查询条件，将结合此条件进行过滤
orderby - string, 部分定制页面上有orderby选项，以关键字进行匹配
举例:

   { 
        table:"spo", expres: {st_id: 33, emp_id: 20, state: ["V","O", "S"], price: "12~20"}, 
        querystr: "13918891588", usepref: false, orderby: "stocktime_first"}
    }  

	 * 
	 * @throws Exception
	 */
	public SearchRequest(JSONObject jo){
		this.jo=jo;
	}
	/**
	 * 转换为JSON对象
	 * @return
	 */
	public JSONObject toJSONObject(){
		return jo;
	}
	/**
	 * 添加过滤条件
	 * @param column 在查询表上的字段
	 * @param condition 支持类型:
	 *   column 是number类型的，输入 "a~b" 表示最大b（包含)，最小a(包含), a和b可以只有1个，如果没有"~", 只要给数字就可以，不要给=号
	 *   column 是date的，可以输入"now-3" 表示>=大前天，系统总是默认用 >=，now表示当前时间
	 *   column 是str的，默认强匹配，用=号
	 *   column 的扩展属性
	 * @throws JSONException 
	 */
	public void addParam(String column, Object condition) throws JSONException {
		JSONObject expr=(JSONObject)jo.optJSONObject("expr");
		if(expr==null)expr=new JSONObject();
		expr.put(column, condition);
	}
	/**
	 * 界面的搜索框
	 * @param s 作为模糊搜索条件
	 */
	public void setQueryString(String s )throws JSONException {
		jo.put("querystr", s);
	}
	/**
	 * 添加所有列表界面字段，即mask[1]==true的字段
	 */
	public void addColumnsForListView()throws JSONException {
		jo.put("mask", "list");
	}
	/**
	 * 添加所有列表界面字段，即mask[0]==true的字段
	 */
	public void addColumnsForObjectView()throws JSONException {
		jo.put("mask", "obj");
	}
	/**
	 * 设置用户的编好作为查询条件，注意和addParam的顺序，总是后调用的相同字段覆盖前面的
	 * @throws Exception
	 */
	public void setUserPreferenceAsParam() throws JSONException {
		jo.put("usepref", true);
	}
	/**
	 * @param orderByDef 结构: {table: string, column: string, asc: boolean, join: string, param:[] } 
		 * 指定是基于什么表的字段做排序,  join指定和主表之间连接的关系，param是在join中出现的？的替代变量，支持$stid,$uid, $empid,$comid
		 * 目前只支持查询模式
		 * 举例:
		 * 本店热销商品在前
		 * {table:"stg", column:"stg.samt", asc: false, join: "stg.pdtid=pdt.id and stg.st_id=?", param:["$stid"]}
	 * 
	 */
	public void setOrderBy(JSONObject orderByDef)throws JSONException {
		jo.put("orderby",orderByDef);
	}
	/**
	 * 设置查询主表
	 * @param table
	 * @throws Exception
	 */
	public void setTable(String table) throws JSONException {
		jo.put("table", table);
	}
	/**
	 * 记录的最大读取数量，不能大于2000
	 * @param cnt
	 */
	public void setMaxCount(int cnt)throws JSONException {
		jo.put("maxcnt", cnt);
	}
	
	
}
