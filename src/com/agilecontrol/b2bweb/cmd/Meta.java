package com.agilecontrol.b2bweb.cmd;

import org.json.JSONArray;
import org.json.JSONObject;

import com.agilecontrol.b2b.cmd.ObjectGet;
import com.agilecontrol.b2b.cmd.Search;
import com.agilecontrol.nea.core.schema.*;
import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.phone.Admin;
import com.agilecontrol.phone.CmdHandler;
import com.agilecontrol.phone.CmdResult;
import com.agilecontrol.phone.PhoneController;
import com.agilecontrol.phone.PhoneUtils;

/**

> {cmd:"b2b.meta"}

 获取ad_table元数据的定义，可以用于生产 ad_sql#table:$table:meta

 * @author yfzhu
 *
 */
@Admin(mail="yfzhu@lifecycle.cn")
public class Meta extends ObjectGet {
	/**
	 * @param jo - {table - string ad_table.name }
	 */
	public CmdResult execute(JSONObject jo) throws Exception {
		String tname= this.getString(jo, "table");
		TableImpl table=(TableImpl)TableManager.getInstance().getTable(tname);
		if(table==null) throw new NDSException("table "+ tname+" not found in TableManager");
		JSONObject tbJSONObj=(table).toMaiJiaObject();
				
		CmdResult res=new CmdResult( tbJSONObj );
		return res;
		
	}

}
