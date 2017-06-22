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

h1. 单个订单的执行

h2. 输入

> {cmd:"b2b.order.exec",  id, action}

*id* - int 当前订单的id
*action*- string 动作，可选: "cancel"(创建人), "signoff"（接受人） 

h2. 输出

<pre>
{
	code,message
}
</pre>

*code* - int 0表示正常，其他都是错误
*message* - string 提示的消息



 * @author yfzhu
 *
 */
@Admin(mail="yfzhu@lifecycle.cn")
public class BfoExec extends CmdHandler {
	/**
	 * 
	 */
	public CmdResult execute(JSONObject jo) throws Exception {
		int objectId=this.getInt(jo, "id");
		
		//check permission
		
		String action=this.getString(jo, "action");
		if("cancel".equals(action)){
			int cnt=engine.doQueryInt("select count(*) from b_bfo where id=? and ownerid=?", new Object[]{objectId, usr.getId()}, conn);
			if(cnt==0) throw new NDSException("@no-permission@");
			ArrayList params=new ArrayList();
			params.add(objectId);
			engine.executeStoredProcedure("b_bfo_cancel", params, false, conn);
		}else if("signoff".equals(action)){
			int cnt=engine.doQueryInt("select count(*) from b_bfo where id=? and ownerid=?", new Object[]{objectId, usr.getId()}, conn);
			if(cnt==0) throw new NDSException("@no-permission@");
			
			cnt=engine.doQueryInt("select count(*) from b_bfo where id=? and send_status=3", new Object[]{objectId}, conn);
			if(cnt==0) throw new NDSException("订单需要在待收货状态才能签收");
			
			ArrayList params=new ArrayList();
			params.add(objectId);
			engine.executeStoredProcedure("b_bfo_signoff", params, false, conn);
			
		}else throw new NDSException("不支持的action参数");
		return CmdResult.SUCCESS;
	}

}













