package com.agilecontrol.b2bweb.cmd;

import java.util.HashMap;

import org.json.JSONObject;

import com.agilecontrol.b2b.cmd.Search;
import com.agilecontrol.phone.Admin;
import com.agilecontrol.phone.CmdHandler;
import com.agilecontrol.phone.CmdResult;

/**
 参照Search

cmd: "b2b.act.search"

 * @author yfzhu
 *
 */
@Admin(mail="yfzhu@lifecycle.cn")
public class ActSearch extends Search {
	/**
	 * 对输入的查询条件进行重构，以支持特殊的查询请求
	 * @param jo 原始的查询条件，有可能在此地被重构
	 * @return 额外的查询条件，将增加到查询where语句中
	 * key: 是要补充到sql where 部分的clause语句内容，比如 "emp_id=?"
	 * value 是问号对应的实际值，目前key仅支持一个问号比如对应上面的value= 当前emp的id
	 * 
	 */
	protected HashMap<String, Object> reviseSearchCondition(JSONObject jo) throws Exception{
		HashMap<String,Object> data=new HashMap();
		data.put("b_market_id=?", usr.getMarketId());
		
		return data;
	}

}
