package com.agilecontrol.phone.cmd;

import java.util.ArrayList;
import java.util.Collection;

import org.json.JSONArray;
import org.json.JSONObject;

import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.nea.util.Validator;
import com.agilecontrol.phone.CmdHandler;
import com.agilecontrol.phone.CmdResult;

/**
 * ִ�����ݿ�ĺ����������ݿ�Ľ�����ؿͻ���
 * �洢������: jo.getString("func")
 * �洢����:  func(jo in clob) return clob as 
 * 
 * @author yfzhu
 *
 */
public class RunJSON extends CmdHandler {
	
	@Override
	public CmdResult execute(JSONObject jo) throws Exception {
		
		String func=jo.optString("func");
		if(Validator.isNull(func)) throw new NDSException("δ�������func");
		
		ArrayList params=new ArrayList();
		params.add(new StringBuilder(jo.toString()));
		
		ArrayList results=new ArrayList();
		results.add(java.sql.Clob.class);
		
		Collection ret=engine.executeFunction(func, params, results, conn);
		String res=(String) ret.iterator().next();
		
		
		return new CmdResult(res);
	}

}
