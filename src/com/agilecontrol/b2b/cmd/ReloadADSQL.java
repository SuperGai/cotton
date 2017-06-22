package com.agilecontrol.b2b.cmd;

import java.io.StringReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.HashSet;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;

import org.json.JSONObject;

import com.agilecontrol.b2b.schema.SyncADSQL;
import com.agilecontrol.b2b.schema.TableManager;
import com.agilecontrol.nea.core.query.QueryEngine;
import com.agilecontrol.nea.core.util.ConfigValues;
import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.nea.util.Validator;
import com.agilecontrol.phone.CmdHandler;
import com.agilecontrol.phone.CmdResult;
import com.agilecontrol.phone.PhoneController;
import com.agilecontrol.phone.wechat.auth.BudingAuth;

import org.json.*;
/**
 
 ���ad_sql������ad_param, db����Ա����ģʽ
 
 ����portal6�޷�����ά��ad_sql����Ҫ�� adsql.datasource ָ����datasource�ж�ȡʱ��ȵ�ǰ�������µ�ad_sql��ͬ������schema

 * @author yfzhu
 *
 */
public class ReloadADSQL extends CmdHandler {
	

	/**
	 * Guest can execute this task, default to false
	 * 
	 * @return
	 */
	public boolean allowGuest() {
		return true;
	}

	@Override
	public CmdResult execute(JSONObject jo) throws Exception {
//		checkIsLifecycleManager();
		String ds=ConfigValues.get("adsql.datasource");
		String ret=null;
		if(Validator.isNotNull(ds)){
			SyncADSQL sad=new SyncADSQL(conn);
			ret=sad.syncFromDatasource(ds);
		}
		
		PhoneController.getInstance().clear();
		TableManager.getInstance().reload();
		if(ret==null) ret="���adsql�������";
		
		return new CmdResult(0, ret, null);
	}

}
