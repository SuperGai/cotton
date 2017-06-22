package com.agilecontrol.portal.cmd;

import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONObject;

import com.agilecontrol.b2b.cmd.Search;
import com.agilecontrol.b2b.schema.Column;
import com.agilecontrol.b2b.schema.Table;
import com.agilecontrol.nea.core.query.QueryEngine;
import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.nea.util.Tools;
import com.agilecontrol.nea.util.Validator;
import com.agilecontrol.phone.CmdResult;
import com.agilecontrol.phone.PhoneUtils;
import com.agilecontrol.phone.UserObj;

/**
 参照Search, 每个商品数据需要读取翻译表M_PRODUCT_TRANS
 进行翻译

> {cmd:"b2b.province.search", 常规搜索条件, c_country_id}

*c_country_id* - number,国家id

 对比标准的search方法，
 {
    table, exprs, querystr,  maxcnt,pagesize, mask, usepref, orderby
 }
 * @author stao
 *
 */
public class ProvinceSearch extends Search {
	
	private int c_country_id;
	public CmdResult execute(JSONObject jo) throws Exception {
		if (null == usr || null == usr.getName()) {
			int rootUserId=Tools.getInt(QueryEngine.getInstance().doQueryOne("select id from users where name='root'"),893);
			JSONObject usrjo = PhoneUtils.getRedisObj("usr", rootUserId, conn, jedis);
			usr=new UserObj(usrjo);
		}
		c_country_id = jo.optInt("c_country_id",-1);
		String cacheKey=jo.optString("cachekey");
		if(Validator.isNotNull(cacheKey) && jedis.exists(cacheKey) /*需求：若key timeout也要重新查*/){
			jo.put("table","province");//cache key wil reload from this table's pk records
		}
		return super.execute(jo);
	}
	/**
	 * 处理以下过滤条件：国家id
	 * @param jo 原始的查询条件，有可能在此地被重构
	 * @return 额外的查询条件，将增加到查询where语句中
	 * key: 是要补充到sql where 部分的clause语句内容，比如 "c_country_id=?"
	 * value 是问号对应的实际值，如果value是java.util.List，将允许多值代替？号
	 */
	protected HashMap<String, Object> reviseSearchCondition(JSONObject jo) throws Exception{
		HashMap<String, Object> map=new HashMap<String, Object>();
		StringBuilder sb=new StringBuilder();
		Table table=manager.getTable("province");
		ArrayList params=new ArrayList();
		
		String scs = (String) table.getJSONProp("search_on");
		if (Validator.isNotNull(scs)) {
			String[] scss = scs.split(",");
			Column cl = table.getColumn(scss[0]);
			if (cl == null){
				throw new NDSException(table + ".search_on扩展属性中的字段未定义:" + scss[0]);
			}else{
				
				if( c_country_id != -1){
					sb.append(cl.getRealName() + " = ?");
					params.add(c_country_id);
				}
			}

		} else{
			throw new NDSException("需要配置" + table + "的search_on扩展属性");
		}
			map.put(sb.toString(), params);
		return map;
	}
	
	/* (non-Javadoc)
	 * @see com.agilecontrol.phone.CmdHandler#allowGuest()
	 */
	@Override
	public boolean allowGuest() {
		return true;
	}
}
