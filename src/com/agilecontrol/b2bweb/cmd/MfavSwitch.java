package com.agilecontrol.b2bweb.cmd;

import org.json.JSONArray;
import org.json.JSONObject;

import com.agilecontrol.b2b.cmd.Search;
import com.agilecontrol.b2bweb.WebController;
import com.agilecontrol.phone.Admin;
import com.agilecontrol.phone.CmdHandler;
import com.agilecontrol.phone.CmdResult;

/**
 
h1. �л��ղ�

h2. ����

> {cmd:"b2b.mfav.switch" , pdtid, en}

*pdtid* - int ��Ʒ
*en* - string "Y"|"N"

����ȡB_FAVOURITE��ʶ����add����update��en��Ӧisactive�ֶ�

h2. ���

{code,message}


 * @author yfzhu
 *
 */
@Admin(mail="yfzhu@lifecycle.cn")
public class MfavSwitch extends CmdHandler {
	
	/**
	 * ִ������
	 * 
	 * @param jo
	 *            �������
	 * @return ���ص����ݽ�ȫ����Ӧ��ValueHolder�����
	 */
	public  CmdResult execute(JSONObject jo) throws Exception{
		int pdtId= this.getInt(jo, "pdtid");
		String en= this.getString(jo, "en");
		int id=engine.doQueryInt("select id from b_favourite where user_id=? and m_product_id=?", new Object[]{usr.getId(),pdtId}, conn);
		if(!"Y".equals(en)) {
			//ȡ���ղ�
			if(id>0){
				engine.executeUpdate("delete from b_favourite where user_id=? and m_product_id=?",new Object[]{usr.getId(), pdtId	}, conn);
				//engine.executeUpdate("update b_favourite set isactive=?,modifieddate=sysdate where user_id=? and m_product_id=?",new Object[]{en,usr.getId(), pdtId	}, conn);
				jedis.del("mfav:"+id );
			}
		}else{
			//����ղ�
			if(id>0){
				engine.executeUpdate("update b_favourite set isactive=?,modifieddate=sysdate where user_id=? and m_product_id=?",new Object[]{en,usr.getId(), pdtId	}, conn);
				jedis.del("mfav:"+id );
			}else{
				engine.executeUpdate("insert into b_favourite (id,user_id,m_product_id,isactive) values (get_sequences('b_favourite'), ?,?,?)",
						new Object[]{usr.getId(), pdtId,en}, conn);
			}
		}
		
		return CmdResult.SUCCESS;
	}
}
