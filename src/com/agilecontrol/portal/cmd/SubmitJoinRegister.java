package com.agilecontrol.portal.cmd;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.json.JSONObject;

import com.agilecontrol.phone.Admin;
import com.agilecontrol.phone.CmdHandler;
import com.agilecontrol.phone.CmdResult;

/**
 * ����
 * 
 * request ���� name:�û���
 * 
 * ���
 * 
 * { success: true/false }
 * 
 * ע��ɹ�ʱ��success Ϊ true������Ϊ false
 * 
 * 
 * 
 * �����������˳�򣬵��� p_cu_apply_insert ����������������õ� ���ص�����id������ p_st_detail_insert �������̻�������ӱ��� ��p_cu_apply_idʹ�øղŵõ�������id������
 * p_dist_apply_insert �� ����Ȧ�����ӱ����� p_cu_apply_id ʹ�øղŵõ�������id�� ������ p_cu_apply_subsubmit �ύ��������Ϊ�ղŵõ�������id��
 * Ȼ��ϵͳ���Զ�����һ���û����û���Ϊ�û������䣬����Ϊ123456
 * 
 */

/**
 * @author stao
 */
@Admin(mail="sun.tao@lifecycle.cn")
public class SubmitJoinRegister extends CmdHandler {
	@Override
	public CmdResult execute(JSONObject jo) throws Exception{
		JSONObject obj = new JSONObject().put( "success", false);
		String name = jo.getString("truename");
		String ismale =jo.getString("sex");
		int age =jo.getInt("age");
		String mobi =jo.getString("telphone");
		String email = jo.getString("email");
		String address = jo.getString("address");
		String zip = jo.getString("post");
		String phone =jo.getString("othercontact");
		String fax = jo.getString("fax");
		String experience =jo.getString("experience");
		String industry =jo.getString("industry");
		String entretype = jo.getString("entretype");
		String budget = jo.getString("budget");
		String mngtype = jo.getString("mngtype");
		String contactstate =jo.optString("contactstate","");
		String hasshop = jo.getString("hasshop");
		int opendate = jo.getInt("shoptime");
		String underway = jo.getString("underway");
		String remark = jo.getString("remark");
		String area = jo.getString("area");
		
		
		ArrayList ls = new ArrayList();
		ls.add(name);
		ls.add(ismale);
		ls.add(age);
		ls.add(mobi);
		ls.add(email);
		ls.add(address);
		ls.add(zip);
		ls.add(phone);
		ls.add(fax);
		ls.add(experience);
		ls.add(industry);
		ls.add(entretype);
		ls.add(budget);
		ls.add(mngtype);
		ls.add(contactstate);
		ls.add(hasshop);
		ls.add(opendate);
		ls.add(underway);
		ls.add(remark);
		ls.add(area);
		
		ArrayList lres = new ArrayList();
		lres.add(Integer.class);
		engine.executeFunction("p_cu_apply_info_insert", ls, lres, conn);
		return new CmdResult( obj);
	}
	
	/**
	 * Guest can execute this task, default to false
	 * @return
	 */
	public boolean allowGuest(){
		return true;
	}
	
}
