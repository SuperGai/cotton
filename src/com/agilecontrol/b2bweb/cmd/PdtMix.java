package com.agilecontrol.b2bweb.cmd;

import org.json.JSONArray;
import org.json.JSONObject;

import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.nea.util.Validator;
import com.agilecontrol.phone.CmdResult;
import com.agilecontrol.phone.PhoneController;

/**
 * ����bothҪ��ǰ̨��Ҫ��Ʒ���飬����PdtGet��PdtDescription�õ���Ʒ����
 * 
 * 
 * h1. ��Ʒ�������

h2. ����

��Ʒ����,��ȡ��Ʒ����

h2. ����

> {cmd:"b2b.pdt.mix",id }

*id* - int pdt.id

h2. ���

> {pdtid, note,no,mainpic,price,tags,allpic,isfav,dtls}

*pdtid* - int pdtid
*note* - string ��Ʒ��ע 
*no* - string ��Ʒ���
*mainpic* -string ��ƷͼƬ
*price* - double ��ʾ�ļ۸�
*tags* - jsonarray of string ��Ʒ��ǩ����: ["����","������ͬ��"]
*allpic* - jsonarray of string ��ƷͼƬ, ������b_img/b_pdt_img
*isfav* -1/0 ״̬ �Ƿ��ղأ������� B_FAVOURITE 	
*dtls* - string ��Ʒ����, ���� M_PRODUCT_TRANS
 [
 	{
 		"title":"@m_dim1_id@","value":"�޹�"
 	},
 	{
 		"title":"@m_dim1_id@","value":"�޹�"
 	},
 	{
 		"title":"@m_dim1_id@","value":"�޹�"
 	}
 ]
 
 ������both��Ʒ����
 * @author lsh
 *
 */
public class PdtMix extends PdtGet {

	@Override
	public CmdResult execute(JSONObject jo) throws Exception {
		
		return super.execute(jo);
	}
	@Override
	protected void postAction(JSONObject retObj) throws Exception {
		long pdtid = retObj.optLong("id",-1);
		if(pdtid==-1){
			throw new NDSException("pdtid not found");
		}
		String pdtsql = PhoneController.getInstance().getValueFromADSQL("b2b:pdt:descget",conn);
		if(Validator.isNull(pdtsql)){
			throw new NDSException("ad_sql#b2b:pdt:desc not found");
		} 
		JSONArray pdtdesc = engine.doQueryObjectArray(pdtsql, new Object[]{pdtid,usr.getLangId(),usr.getLangId()},conn);
		if(pdtdesc==null){
			pdtdesc = new JSONArray();
		}
		retObj.put("desc", pdtdesc);
		super.postAction(retObj);
	}
	
}
