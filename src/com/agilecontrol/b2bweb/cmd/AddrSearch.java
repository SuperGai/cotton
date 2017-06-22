package com.agilecontrol.b2bweb.cmd;

import java.util.HashMap;

import org.json.JSONObject;

import com.agilecontrol.b2b.cmd.Search;
import com.agilecontrol.phone.Admin;
import com.agilecontrol.phone.CmdHandler;
import com.agilecontrol.phone.CmdResult;

/**
 h1. user_address 用户地址搜索

> {cmd:"b2b.addr.search"}

强制添加条件: isactive='Y' and user_id=$uid

 * @author yfzhu
 *
 */
@Admin(mail="yfzhu@lifecycle.cn")
public class AddrSearch extends Search {
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
		data.put("isactive=?", "Y");
		data.put("user_id=?", usr.getId());
		
		return data;
	}

}
