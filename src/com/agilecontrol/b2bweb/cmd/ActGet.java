package com.agilecontrol.b2bweb.cmd;

import org.json.JSONArray;
import org.json.JSONObject;

import com.agilecontrol.b2b.cmd.ObjectGet;
import com.agilecontrol.b2b.cmd.Search;
import com.agilecontrol.b2b.schema.Table;
import com.agilecontrol.b2bweb.WebController;
import com.agilecontrol.nea.util.Validator;
import com.agilecontrol.phone.Admin;
import com.agilecontrol.phone.CmdHandler;
import com.agilecontrol.phone.CmdResult;
import com.agilecontrol.phone.PhoneController;
import com.agilecontrol.phone.PhoneUtils;

/**

 ����
{cmd:"b2b.act.get", id}

id - �id, int b_prmt.id

���
{id, name, begindate, enddate, pic, ptype, pdts:[{pdt}]}

pdt ��������Ҫ�����Ļ�����Ʒ���������

{pdtid,note,no,mainpic,price,tags}

pdtid - int pdtid
note - string ��Ʒ��ע 
no - string ��Ʒ���
mainPic -string ��ƷͼƬ
price - double ��ʾ�ļ۸�
tags - jsonarray of string ��Ʒ��ǩ����: ["����","������ͬ��"]

 * @author yfzhu
 *
 */
@Admin(mail="yfzhu@lifecycle.cn")
public class ActGet extends ObjectGet {
	
//	public CmdResult execute(JSONObject jo) throws Exception {
//		//��Ҫʵ��Ϊ����redis cache����act_pdts�����ݻ�������
//		int actId= this.getInt(jo, "id");
//		vc.put("actid", actId);
//		vc.put("marketid", usr.getMarketId());
//		JSONArray pdts=null;
//		//pdt ids �������е���ϸ��
//		JSONArray rows= PhoneController.getInstance().getDataArrayByADSQL("act_pdts", vc, conn, false);
//		
//		Table table=manager.getTable("pdt");
//		
//		pdts=PhoneUtils.getRedisObjectArray("pdt", rows, table.getColumnsInListView(), true, vc, conn,jedis);
//		for(int i=0;i<pdts.length();i++){
//			JSONObject pdt=pdts.getJSONObject(i);
//			//�������ԣ� �Լ�����г��ļ۸�ͼƬ�������Ϣ
//			WebController.getInstance().replacePdtValues(pdt, usr.getLangId(), usr.getMarketId(), vc, jedis, conn);
//			
//		}				
//		CmdResult res=new CmdResult(pdts );
//		return res;
//		
//	}

}
