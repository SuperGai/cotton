package com.agilecontrol.b2bweb.cmd;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONObject;

import com.agilecontrol.b2b.cmd.Search;
import com.agilecontrol.b2b.query.SearchResult;
import com.agilecontrol.b2b.schema.Column;
import com.agilecontrol.b2b.schema.Table;
import com.agilecontrol.b2bweb.WebController;
import com.agilecontrol.b2bweb.cmd.DimList.DimValue;
import com.agilecontrol.nea.core.util.MessagesHolder;
import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.nea.util.Tools;
import com.agilecontrol.nea.util.Validator;
import com.agilecontrol.phone.Admin;
import com.agilecontrol.phone.CmdHandler;
import com.agilecontrol.phone.CmdResult;
import com.agilecontrol.phone.LanguageManager;
import com.agilecontrol.phone.PhoneConfig;
import com.agilecontrol.phone.PhoneController;
import com.agilecontrol.phone.PhoneUtils;

/**
 参照Search, 每个商品数据需要读取翻译表M_PRODUCT_TRANS
 进行翻译

> {cmd:"b2b.news.search", 常规搜索条件, doctype}

*doctype* - string, 新闻内部类型 rule 规章制度|notes 内部通知|company 公司新闻|industry 行业新闻|hotspot 主页动态热点|declaration 集团公告|latest 最新动态|other 其他

 对比标准的search方法，
 {
    table, exprs, querystr,  maxcnt,pagesize, mask, usepref, orderby
 }
 * @author wu.qiong
 *
 */
public class NewsSearch extends Search {
	
	private String doctype;
	public CmdResult execute(JSONObject jo) throws Exception {
		doctype = jo.optString("doctype","");
		String cacheKey=jo.optString("cachekey");
		if(Validator.isNotNull(cacheKey) && jedis.exists(cacheKey) /*需求：若key timeout也要重新查*/){
			jo.put("table","news");//cache key wil reload from this table's pk records
		}
		return super.execute(jo);
	}
	/**
	 * 处理以下过滤条件：新闻类型
	 * @param jo 原始的查询条件，有可能在此地被重构
	 * @return 额外的查询条件，将增加到查询where语句中
	 * key: 是要补充到sql where 部分的clause语句内容，比如 "emp_id=?"
	 * value 是问号对应的实际值，如果value是java.util.List，将允许多值代替？号
	 */
	protected HashMap<String, Object> reviseSearchCondition(JSONObject jo) throws Exception{
		HashMap<String, Object> map=new HashMap<String, Object>();
		StringBuilder sb=new StringBuilder();
		Table table=manager.getTable("news");
		ArrayList params=new ArrayList();
		String scs = (String) table.getJSONProp("search_on");
			if (Validator.isNotNull(scs)) {
				String[] scss = scs.split(",");
				Column cl = table.getColumn(scss[0]);
				if (cl == null){
					throw new NDSException(table + ".search_on扩展属性中的字段未定义:" + scss[0]);
				}else{
					sb=new StringBuilder(" lower( ");
					sb.append(cl.getRealName()).append(") = ?");
				}
				params.add(doctype);

			} else{
				throw new NDSException("需要配置" + table + "的search_on扩展属性");
			}
			map.put(sb.toString(), params);
		return map;
	}
}
