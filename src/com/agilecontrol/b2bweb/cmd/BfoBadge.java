package com.agilecontrol.b2bweb.cmd;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;

import com.agilecontrol.b2b.cmd.ObjectGet;
import com.agilecontrol.b2b.cmd.Search;
import com.agilecontrol.b2b.schema.Table;
import com.agilecontrol.b2bweb.WebController;
import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.nea.util.Tools;
import com.agilecontrol.nea.util.Validator;
import com.agilecontrol.phone.Admin;
import com.agilecontrol.phone.CmdHandler;
import com.agilecontrol.phone.CmdResult;
import com.agilecontrol.phone.PhoneController;
import com.agilecontrol.phone.PhoneUtils;

/**
h1. 订单不同发货状态的统计数

h2. 输入

> {cmd:"b2b.order.badge"}

将发货状态send_status的统计值带上，比如：{"1": 5, "3": 5} 表示 已确认有5张单，发货中有3张单

h2. 输出

<pre>
{ k:v }
</pre>

*k* - 订单状态值
*v* - 状态对应订单数

举例
> {"1": 5, "3": 5}

 * @author yfzhu
 *
 */
@Admin(mail="yfzhu@lifecycle.cn")
public class BfoBadge extends CmdHandler {
	
	/**
	 * 
	 */
	public CmdResult execute(JSONObject jo) throws Exception {
		
		JSONObject obj=toKVObject(engine.doQueryJSONArray(
				"select send_status, count(*) from b_bfo where DEST_USER_ID=? and send_status in(2,3) group by send_status",
				new Object[]{usr.getId()}, conn));
		
		return new CmdResult(obj);
	}

}













