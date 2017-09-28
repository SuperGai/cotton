package com.agilecontrol.portal.cmd;

import java.sql.PreparedStatement;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;

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
	JSONObject mianxuObject ;
	@Override
	public CmdResult execute(JSONObject jo) throws Exception {
		String bao_number= jo.getString("xubaonumber");
		if(Validator.isNotNull(bao_number)){
			bao_number= bao_number.trim();
		}else throw new NDSException("��Ҫ����˺�");
		/**
		 * �߼�Ϊͨ��bao_number������ĵ�ֻ��������������Ʒ
		 */
		
		PreparedStatement	pstmt = conn.prepareStatement("update M_COTTON_SAVINGS  set  VERIFYED='Y',VERIFYDATE=sysdate   where  CODE=? and VERIFYED='N' ");
		pstmt.setString(1, bao_number);
		int BAO_ID = pstmt.executeUpdate();
        if(BAO_ID!=0){     
    		String sql = "select * from M_COTTON_SAVINGS where CODE=?";
    		 mianxuObject = engine.doQueryObject(sql, new Object[]{bao_number},conn);
        }
        if(BAO_ID==0){
        	throw new NDSException("�˺Ŵ��������ʹ��");
        }
		return new CmdResult(mianxuObject);
	}
	


}
