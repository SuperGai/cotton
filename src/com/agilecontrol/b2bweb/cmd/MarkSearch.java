package com.agilecontrol.b2bweb.cmd;

import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONObject;

import com.agilecontrol.b2b.cmd.Search;
import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.phone.Admin;
import com.agilecontrol.phone.CmdHandler;
import com.agilecontrol.phone.CmdResult;
import com.agilecontrol.phone.PhoneController;

/**
 参照Search, 面向"mark", 即: b_pdt_fav

> {cmd:"b2b.mark.search", xx}


 * @author yfzhu
 *
 */
@Admin(mail="yfzhu@lifecycle.cn")
public class MarkSearch extends Search {
	
	
	/**
	 * 对输入的查询条件进行重构，以支持特殊的查询请求
	 * @param jo 原始的查询条件，有可能在此地被重构
	 * @return 额外的查询条件，将增加到查询where语句中
	 * key: 是要补充到sql where 部分的clause语句内容，比如 "emp_id=?"
	 * value 是问号对应的实际值，目前key仅支持一个问号比如对应上面的value= 当前emp的id
	 * 
	 */
	protected HashMap<String, Object> reviseSearchCondition(JSONObject jo) throws Exception{
		int pdtId=this.getInt(jo, "pdtid");
		int actId=jo.optInt( "actid",-1);
		int customerId= getCustomerIdByActOrMarket(actId,usr.getMarketId()) ;
		
		HashMap<String,Object> data=new HashMap();
		data.put("c_customer_id=?", customerId);
		data.put("m_product_id=?", pdtId);
		
		return data;
	}

}
