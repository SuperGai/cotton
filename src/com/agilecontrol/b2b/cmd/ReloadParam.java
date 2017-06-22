package com.agilecontrol.b2b.cmd;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;

import com.agilecontrol.phone.CmdResult;
import com.agilecontrol.phone.CmdHandler;
import com.agilecontrol.phone.PhoneUtils;
import com.agilecontrol.nea.core.control.web.WebUtils;
import com.agilecontrol.nea.core.query.QueryEngine;
import com.agilecontrol.nea.core.util.ConfigValues;
import com.agilecontrol.nea.core.util.WebKeys;
import com.agilecontrol.nea.util.Configurations;
import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.nea.util.Tools;
import com.agilecontrol.nea.util.Validator;
/**
 * ����ad_param���޸ĸ���ConfigValues �� PhoneConfig�д洢��ֵ
 * 
 * �����ζ�ȡad_param�ļ�ֵ�ԣ�����ConfigValues��ֵ������ʶ����PhoneConfig�е�property��Ȼ����и���
 * PhoneConfig�е�ֵ�Ķ�Ӧ����. -> _, ����ƥ��
 * 
 * @author yfzhu
 *
 */
public class ReloadParam extends CmdHandler {

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
		Configurations conf=(Configurations) WebUtils.getServletContextManager().getActor(WebKeys.CONFIGURATIONS);
		StringBuilder sb=new StringBuilder();
		StringBuilder sb2=new StringBuilder();
		StringBuilder sb3=new StringBuilder();
		//ad_param read
		JSONArray params= engine.doQueryJSONArray("select name,value from ad_param", new Object[]{}, conn);
		for(int i=0;i<params.length();i++){
			JSONArray row= params.getJSONArray(i);
			String name= row.getString(0);
			String value=row.optString(1);
			if(Validator.isNull(value))value="";
			
			if(PhoneUtils.alterConfigValues(conf, name,value)){
				sb2.append(name).append(",");
			}
			if(PhoneUtils.alterPhoneConfigValue(name,value)){
				sb.append(name).append(",");
			}
		}
		
		CmdResult ret= new CmdResult();
		if(sb2.length()>0) sb3.append("ϵͳ��������:").append( sb2).append(". ");
		if(sb.length()>0) sb3.append("Phone�ڴ��������:").append(sb).append(". ");
		if(sb3.length()>0) ret.setMessage(sb3.toString());
		return ret;
	}
	
	
}
