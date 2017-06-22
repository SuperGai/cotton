package com.agilecontrol.b2bweb.cmd;

import org.json.JSONArray;
import org.json.JSONObject;

import com.agilecontrol.nea.core.control.event.NDSEventException;
import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.nea.util.Validator;
import com.agilecontrol.phone.CmdHandler;
import com.agilecontrol.phone.CmdResult;
import com.agilecontrol.phone.PhoneController;

/**
 * {cmd:"b2b.pdt.colors",id:1} 
 * 
 * id:��Ʒid
 * 
 * ������ͨ����Ʒid�Լ����ı����ƣ�������Ӧ�ĸ��ı�����
 * 
 * �����
{
	"<div><img src="/images/act/carousel2.jpg"></div>"
}       
 * 
 * pdtid:��Ʒid
 * note:��Ʒ����
 * no������
 * mainpic:��ͼ
 * price:��Ʒ�۸�
 * @author sun.yifan
 *
 */
public class PdtRichText extends CmdHandler {

	@Override
	public CmdResult execute(JSONObject jo) throws Exception {
		long pdtid = jo.optLong("pdtid",-1);
		if(pdtid==-1){
			throw new NDSException("pdtid not found��");
		}
		long actid = jo.optLong("actid",-1);
		String colname = jo.optString("colname","pdtdtls");
		String sql = "SELECT "+colname+" value FROM m_product WHERE id = ?";
		String colstr = engine.doQueryString(sql, new Object[]{pdtid},conn);
		if(Validator.isNull(colstr)){
			colstr = "<div></div>";
		}
		return new CmdResult(colstr);
	}

}
