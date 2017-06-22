package com.agilecontrol.b2bweb.cmd;

import java.util.HashMap;

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
import com.agilecontrol.phone.PhoneConfig;
import com.agilecontrol.phone.PhoneController;
import com.agilecontrol.phone.PhoneUtils;

/**
 h1. b_bro�˻���

> {cmd:"b2b.bro.get",id}
> id -- number �˻���id

	��Ӷ����items[{imageurl,value,name,color,sz,qty,amount,remarks,enclosure}]
	imageurl -- string ��ƷͼƬ
	value -- string ��Ʒ����
	name -- string ��Ʒ����
	color -- ��Ʒ��ɫ
	sz -- ��Ʒ����
	qty -- �˻�����
	amount -- �˻����
	remarks -- ��ע
	enclosure -- ����
 * @author wu.qiong
 *
 */
public class BroGet extends ObjectGet {
	
	/**
	 * redis��ɻ�����º�Ĵ���
	 * @param table
	 * @param retObj ����������ģ������ؿͻ��˵Ķ��󣬿����ع�, �����Ѿ���װ����Ķ���
	 * @throws Exception
	 */
	protected void postAction(Table bfoTable, JSONObject retObj) throws Exception{
		
		vc.put("uid",usr.getId());
		vc.put("marketid", usr.getMarketId());
		vc.put("id", retObj.getInt("id"));

		//select m_product_id pdtid, B_PRMT_ID actid, sum(qty) qty, sum(price*qty) amt, max(modifieddate) mdate
		//from b_cart where xxx and user_id=$uid and is_order='Y' group by  m_product_id, B_PRMT_ID
		JSONArray items= PhoneController.getInstance().getDataArrayByADSQL("b2b_bro_ptds", vc, conn, true);
		
		retObj.put("items", items);
	}

}













