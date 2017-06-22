package com.agilecontrol.portal.cmd;

import org.json.JSONArray;
import org.json.JSONObject;

import com.agilecontrol.nea.core.query.QueryEngine;
import com.agilecontrol.nea.util.Validator;
import com.agilecontrol.phone.Admin;
import com.agilecontrol.phone.CmdHandler;
import com.agilecontrol.phone.CmdResult;
import com.agilecontrol.phone.PhoneController;

/**
����

���

[
    {
    	title:"",
    	img:"",
    },
    {
    	title:"",
    	img:"",
    },
]

��1����ȡ carousal ������(ad_sql��ȡĬ�ϲ˵����� jsonArray �������� )�� ad_sql#portal:carousal
��2���˵��� roles ����Ϊ��ʱ����ʾĬ��Ȩ�ޣ��������˿ɼ�
 ��3������roles��� Ȩ�ޣ���ǰ�û���Ȩ�ޱ�������� roles �У����򲻿ɼ�
��4�����ظ��ͻ���
 * 
 * @author stao
 *
 */
@Admin(mail="sun.tao@lifecycle.cn")
public class GetPortalCarousal extends CmdHandler {
	
	@Override
	public CmdResult execute(JSONObject jo) throws Exception{
		String adSql = "portal:carousal";
		JSONArray carouArray = (JSONArray) PhoneController.getInstance().getValueFromADSQLAsJSON( adSql, conn);
		return new CmdResult( carouArray);
	}
	
	
	/**
	 * Guest can execute this task, default to false
	 * @return
	 */
	public boolean allowGuest(){
		return true;
	}
}
