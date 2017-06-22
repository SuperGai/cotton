package com.agilecontrol.b2bweb.cmd;

import java.util.ArrayList;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.json.JSONArray;
import org.json.JSONObject;

import com.agilecontrol.b2bweb.OrderSender;
import com.agilecontrol.nea.core.velocity.VelocityUtils;
import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.nea.util.Tools;
import com.agilecontrol.nea.util.Validator;
import com.agilecontrol.phone.CmdHandler;
import com.agilecontrol.phone.CmdResult;
import com.agilecontrol.phone.PhoneConfig;
import com.agilecontrol.phone.PhoneController;

/**
h1. �����µ�

h2. ����

> {cmd:"b2b.order.reOrder",  id,is_clearcart}

*id* - int  b_bfo.id
*is_clearcart - true | false �Ƿ�������ﳵ


 * 
 * @author wu.qiong
 *
 */
public class GetBroId extends CmdHandler {
	
	public CmdResult execute(JSONObject jo) throws Exception {
		
		int bfo_id= jo.optInt("bfo_id",-1);
		
		if(bfo_id == -1){
			throw new NDSException("û�д��붩��id!");
		}
		ArrayList al = new ArrayList<>();
		al.add(bfo_id);
		al.add(Integer.class);//output
		al.add(String.class);
		ArrayList res = engine.executeStoredProcedure("b_bro_addorder", al, conn);
		
		return new CmdResult(new JSONObject().put("id",res.get(1)));
	}

}
