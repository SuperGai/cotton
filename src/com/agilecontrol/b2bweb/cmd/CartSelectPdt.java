package com.agilecontrol.b2bweb.cmd;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONObject;

import com.agilecontrol.b2b.cmd.ObjectGet;
import com.agilecontrol.b2b.cmd.Search;
import com.agilecontrol.b2b.schema.Table;
import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.nea.util.Tools;
import com.agilecontrol.phone.Admin;
import com.agilecontrol.phone.CmdHandler;
import com.agilecontrol.phone.CmdResult;
import com.agilecontrol.phone.PhoneConfig;
import com.agilecontrol.phone.PhoneController;
import com.agilecontrol.phone.PhoneUtils;

/**
h1. ���ﳵѡ����Ʒ����ʾ�ۿ�

h2. ����

> {cmd:"b2b.cart.selectpdt",  pdts}
����ѡ��Ĺ��ﳵ���ݣ����㵱ǰ���ۿۣ����������ۿۣ�����Щ��Ʒ��Ϻ󣬾����ۿ۲�������Ҫ��ʾ���ۿ��ڹ��ﳵ�����
����һ����ʱ���㣬ÿ�ι�ѡ�µ���Ʒ������Ҫ���ô˺�̨�������¼���

*pdts* - ��Ʒ���飬������ʽ��[$pdtid], �� [{pdtid, actid}] 
*$pdtid* - �����޹���Ŀ��ֱ�ӷ�pdtid����ʾ����ָ����Ʒ
*{pdt}* - {pdtid, actid} �ֱ�Ϊ��Ʒid�ͻid, ���лid����Ϊ�ջ�-1, ��ʾ�޻����Ʒ�����ﳵ�вμӻ����Ʒactidһ����Ϊ�գ�

h2. ���

> {totAmt}

*totAmt* -- Number �ۺ��ע�ⲻ�ǵ�Ʒ���ĺϼƣ�Ҳ���������ۿۡ�����

h2. ����

����b_cart.is_order�ֶΣ����µ�ǰѡ����Ʒ��Ȼ�����b_cart_selectpdt(p_user_id in number��r_amt out number)) �洢���̣�����ѡ����Ʒ���ۺ���

 * @author yfzhu
 *
 */
@Admin(mail="yfzhu@lifecycle.cn")
public class CartSelectPdt extends CmdHandler {
	
	public CmdResult execute(JSONObject jo) throws Exception {
		
		//�������is_order������
		engine.executeUpdate("update b_cart set is_order='N' where user_id=?", new Object[]{usr.getId()}, conn);
		JSONArray pdts=jo.getJSONArray("pdts");
		for(int i=0;i<pdts.length();i++){
			Object one= pdts.get(i);
			if(one instanceof JSONObject){
				//{pdtid, actid}
				int pdtId= ((JSONObject)one).getInt("pdtid");
				int actId= ((JSONObject)one).optInt("actid",-1);
				if(actId==-1){
					engine.executeUpdate("update b_cart set is_order='Y' where user_id=? and m_product_id=? and B_PRMT_ID is null", new Object[]{usr.getId(), pdtId}, conn);
				}else{
					engine.executeUpdate("update b_cart set is_order='Y' where user_id=? and m_product_id=? and B_PRMT_ID=?", new Object[]{usr.getId(), pdtId, actId}, conn);
				}
			}else{
				int pdtId=Tools.getInt(one, -1);
				if(pdtId==-1) throw new NDSException("pdts��Ҫ������������������");
				engine.executeUpdate("update b_cart set is_order='Y' where user_id=? and m_product_id=?", new Object[]{usr.getId(), pdtId}, conn);
			}
		}
		
		ArrayList params=new ArrayList();
		params.add(usr.getId());
		ArrayList resultsClass=new ArrayList();
		resultsClass.add(Double.class);
		
		Collection spres=engine.executeFunction("b_cart_selectpdt", params, resultsClass, conn);
		double amt= Double.parseDouble( String.valueOf( spres.iterator().next()));
		
		JSONObject ret=new JSONObject();
		ret.put("totAmt", amt);
		
		CmdResult res=new CmdResult(ret);
		return res;
		
	}
	
}
