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

h1. ��Ʒ��ͬ���۵�ͳ����

h2. ����

> {cmd:"b2b.mark.badge", pdtid, actid}

�����ְ�1-2 �3 �У�4-5 �õ�ͳ��ֵ�����磺{"bad": 5, "good": 50, "neutral":3} , ����sql��� mark_badge 

h2. ���

<pre>
{ k:v }
</pre>

*k* - bad|good|neutral
*v* - ����

����
> {"bad": 5, "good": 50, "neutral":3} 

 * @author yfzhu
 *
 */
@Admin(mail="yfzhu@lifecycle.cn")
public class MarkBadge extends CmdHandler {
	
	/**
	 * 
	 */
	public CmdResult execute(JSONObject jo) throws Exception {
		int pdtId= this.getInt(jo, "pdtid");
		int actId= jo.optInt("actid",-1);
		//����pprice
		vc.put("pdtid", pdtId);
		vc.put("actid", actId);
		vc.put("marketid", usr.getMarketId());
		int customerId= getCustomerIdByActOrMarket(actId,usr.getMarketId()) ;
		vc.put("customerid",customerId);
		vc.put("uid", usr.getId());
		
		JSONArray rows=PhoneController.getInstance().getDataArrayByADSQL("mark_badge", vc, conn, true/*return obj*/);
		
		JSONObject obj=toKVObject(rows);
		
		return new CmdResult(obj);
	}

}













