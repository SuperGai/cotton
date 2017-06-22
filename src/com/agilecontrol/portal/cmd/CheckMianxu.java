package com.agilecontrol.portal.cmd;

import org.json.JSONArray;
import org.json.JSONObject;

import com.agilecontrol.b2b.cmd.ObjectGet;
import com.agilecontrol.b2b.cmd.Search;
import com.agilecontrol.b2b.schema.Table;
import com.agilecontrol.b2bweb.WebController;
import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.nea.util.Validator;
import com.agilecontrol.phone.Admin;
import com.agilecontrol.phone.CmdHandler;
import com.agilecontrol.phone.CmdResult;
import com.agilecontrol.phone.PhoneController;
import com.agilecontrol.phone.PhoneUtils;

/**

 ����

bao_number- ����˺�

���
{1������˺Ŵ�����ɾ����Ϣ�����һ��У���¼����ʾУ��ɹ�
 2������˺Ų���������ʾ���˺Ŵ��������ʹ��
}



 * @author Supergai
 *
 */
@Admin(mail="xuwj@cottonshop.com")
public class CheckMianxu extends ObjectGet {
	
	@Override
	public CmdResult execute(JSONObject jo) throws Exception {
		String bao_number= jo.getString("bao_number");
		if(Validator.isNotNull(bao_number)){
			bao_number= bao_number.trim();
		}else throw new NDSException("��Ҫbao_number");
		/**
		 * �߼�Ϊͨ��bao_number������ĵ�ֻ��������������Ʒ
		 */
	    String sql = "SELECT BAO_ID FROM M_COTTON_SAVINGS WHERE BAO_NUMER=?";
		int BAO_ID = engine.doQueryInt(sql, new Object[]{bao_number}, conn);
        if(BAO_ID!=0){ 
        	String sqlinsert="insert * into BAO_INFO values(?,?,?)";
        	engine.executeUpdate(sqlinsert, new Object[]{bao_number}, conn);
        	throw new NDSException("У��ɹ�");
        }
        if(BAO_ID==0){
        	throw new NDSException("�˺Ŵ��������ʹ��");
        }
		return new CmdResult();
	}

}
