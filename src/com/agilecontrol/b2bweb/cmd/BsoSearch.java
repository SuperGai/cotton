package com.agilecontrol.b2bweb.cmd;

import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONObject;

import com.agilecontrol.b2b.cmd.Search;
import com.agilecontrol.b2b.schema.Column;
import com.agilecontrol.b2b.schema.Table;
import com.agilecontrol.b2bweb.WebController;
import com.agilecontrol.b2bweb.cmd.PdtSearch.DimValue;
import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.nea.util.Validator;
import com.agilecontrol.phone.Admin;
import com.agilecontrol.phone.CmdHandler;
import com.agilecontrol.phone.CmdResult;
import com.agilecontrol.phone.LanguageManager;
import com.agilecontrol.phone.PhoneConfig;
import com.agilecontrol.phone.PhoneController;

/**
 
h1. ���������� 
 
����Search, ����"b_bso"����������Ŀǰ��֧�ֻ��ڶ���id������

> {cmd:"b2b.bso.search", exprs:{"b_bfo_id":12}, table:"bso"}

h2. ����
  2016-11-25 ����@author lsh
> [{id, billdate,tot_qty_send, docno, pdt:{},lname, ldocno, bfo_id:{docno,id},l_com_id;{name,code} }]

*pdt* - ѡ���������е�һ����Ʒ����ͼƬչʾ����Ʒ��Ϣ��{id,no,note,mainpic,color,size}
*bfo_id* - �������������󻯣�ȡ�������б��ֶ�����չʾ


 * @author yfzhu
 *
 */
@Admin(mail="yfzhu@lifecycle.cn")
public class BsoSearch extends Search {
	
	/**
	 * �������Ʒһ��
	 * 
	 * ��������SearchResult.toJSON������и�װ���Է��Ͽͻ���Ҫ��
	 * @param ret redis SearchResult.toJSON��ֱ���ٴα༭
	 * 
	 * @throws Exception
	 */
	protected void postAction(JSONObject ret) throws Exception{
		JSONArray ja=ret.getJSONArray("bso_s");//���صķ������б�
		//bso_pdts, select �Ĳ��֣�id��note,no,mainpic,color,size�� ����ֻ��һ�� $ids,  ֱ�ӷ� in (?)
		StringBuilder sb=new StringBuilder();
		
		HashMap<Integer, JSONObject> orders=new HashMap();//key: orderid, value: order obj
		if(ja.length()>0){
			for(int i=0;i<ja.length();i++){
				JSONObject one=ja.getJSONObject(i);
				int id=one.getInt("id");
				sb.append(id).append(";");// sql ����velocity������ʱ���ǽ�,�����ָ�������ģ�����������ʹ��,�����÷ֺţ�sql��������Ҳ����ڷֺ�ת��
				orders.put(id, one);
			}
			sb.deleteCharAt(sb.length()-1);
		}else{
			return;
		}
		vc.put("ids",sb.toString());
		JSONArray pdts=PhoneController.getInstance().getDataArrayByADSQL("bso_pdts", vc, conn, true	);//��÷������ϵ���Ʒ
		for(int i=0;i<pdts.length();i++){
			JSONObject pdt= pdts.getJSONObject(i);
			WebController.getInstance().replacePdtValues(pdt, usr.getLangId(), usr.getMarketId(), vc, jedis, conn);
			int soId=pdt.getInt("orderid");
			JSONObject orderObj= orders.get(soId);
			if(orderObj!=null) orderObj.put("pdt", pdt);
		}
	}
	
	/**
	 * ��ǰ������Ӧ�Ķ����Ĵ����˱����ǵ�ǰ������
	 * 
	 * @param jo ԭʼ�Ĳ�ѯ�������п����ڴ˵ر��ع�
	 * @return ����Ĳ�ѯ�����������ӵ���ѯwhere�����
	 * key: ��Ҫ���䵽sql where ���ֵ�clause������ݣ����� "emp_id=?"
	 * value ���ʺŶ�Ӧ��ʵ��ֵ��Ŀǰkey��֧��һ���ʺű����Ӧ�����value= ��ǰemp��id
	 * ���value��java.util.List���������ֵ���棿��
	 */
	protected HashMap<String, Object> reviseSearchCondition(JSONObject jo) throws Exception{
		HashMap<String,Object> data=new HashMap();
		//��ǰ������Ӧ�Ķ����Ĵ����˱����ǵ�ǰ������
		data.put("exists(select id from b_bfo where b_bfo.id=bso.b_bfo_id and bso.ownerid=?)", usr.getId());
		return data;
	}

}
