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
 
����Search, ����"b_bro"���˻�����

> {cmd:"b2b.bro.search", exprs:{"b_bfo_id":12}, table:"bro"}


h2. ����
> [{id, billdate,tot_qty,tot_amt, docno, bfo_id:{docno,id}}]

*pdt* - ѡ���˻����е�һ����Ʒ����ͼƬչʾ����Ʒ��Ϣ��{id,no,note,mainpic,color,size}
*bfo_id* - �������������󻯣�ȡ�������б��ֶ�����չʾ


 * @author wu.qiong
 *
 */
public class BroSearch extends Search {
	
	/**
	 * �������Ʒһ��
	 * 
	 * ��������SearchResult.toJSON������и�װ���Է��Ͽͻ���Ҫ��
	 * @param ret redis SearchResult.toJSON��ֱ���ٴα༭
	 * 
	 * @throws Exception
	 */
	protected void postAction(JSONObject ret) throws Exception{
		JSONArray ja=ret.getJSONArray("bro_s");
		//bfo_pdts, select �Ĳ��֣�id��note,no,mainpic,color,size�� ����ֻ��һ�� $ids,  ֱ�ӷ� in (?)
		StringBuilder sb=new StringBuilder();
		
		HashMap<Integer, JSONObject> rOrders=new HashMap();//key: orderid, value: order obj
		if(ja.length()>0){
			for(int i=0;i<ja.length();i++){
				JSONObject one=ja.getJSONObject(i);
				int id=one.getInt("id");
				sb.append(id).append(";");// sql ����velocity������ʱ���ǽ�,�����ָ�������ģ�����������ʹ��,�����÷ֺţ�sql��������Ҳ����ڷֺ�ת��
				rOrders.put(id, one);
			}
			sb.deleteCharAt(sb.length()-1);
		}else{
			return;
		}
		vc.put("ids",sb.toString());
		JSONArray pdts=PhoneController.getInstance().getDataArrayByADSQL("bro_pdts", vc, conn, true	);
		for(int i=0;i<pdts.length();i++){
			JSONObject pdt= pdts.getJSONObject(i);
			WebController.getInstance().replacePdtValues(pdt, usr, vc, jedis, conn);
			int rorderId=pdt.getInt("rorderid");
			JSONObject rorderObj= rOrders.get(rorderId);
			if(rorderObj!=null) rorderObj.put("pdt", pdt);
		}
	}
	/**
	 * ������Ĳ�ѯ���������ع�����֧������Ĳ�ѯ����
	 * @param jo ԭʼ�Ĳ�ѯ�������п����ڴ˵ر��ع�
	 * @return ����Ĳ�ѯ�����������ӵ���ѯwhere�����
	 * key: ��Ҫ���䵽sql where ���ֵ�clause������ݣ����� "emp_id=?"
	 * value ���ʺŶ�Ӧ��ʵ��ֵ��Ŀǰkey��֧��һ���ʺű����Ӧ�����value= ��ǰemp��id
	 * ���value��java.util.List���������ֵ���棿��
	 */
	protected HashMap<String, Object> reviseSearchCondition(JSONObject jo) throws Exception{
		HashMap<String,Object> data=new HashMap();
		data.put("ownerid=?", usr.getId());
		
		String querystr=jo.optString("pdtsearch");
		//��Ϊ�˻�����������
		if (Validator.isNotNull(querystr) && querystr.toLowerCase().startsWith("bro") ) {
			data.put("bro.docno=?", querystr.toUpperCase());
			return data;
		}
		StringBuilder sb=new StringBuilder();
		ArrayList params=new ArrayList();
		
		//dims
		if(usr.getLangId()!=LanguageManager.getInstance().getDefaultLangId()){
			sb=new StringBuilder("exists(select 1 from b_broitem i, m_product d, M_PRODUCT_TRANS r where i.b_bro_id=bro.id and i.m_product_id=d.id and r.m_product_id=d.id and r.b_language_id=?");
			params.add(usr.getLangId());
		}else{
			sb=new StringBuilder("exists(select 1 from b_broitem i,m_product d where i.b_bro_id=bro.id and i.m_product_id=d.id");
		}
		// search
		Table table=manager.getTable("pdt");
		// fuzzy search
		if (Validator.isNotNull(querystr)) {
			querystr = querystr.toLowerCase();
			
			//�������ݺ��Դ�Сд
			sb.append(" and (");
			String scs = (String) table.getJSONProp("search_on");
			if (Validator.isNotNull(scs)) {
				String[] scss = scs.split(",");
				sb.append("(");
				boolean isFirst = true;
				for (String cname : scss) {
					Column cl = table.getColumn(cname);
					if (cl == null)
						throw new NDSException(table + ".search_on��չ�����е��ֶ�δ����:" + cname);

					if (!isFirst)   
						sb.append(" or ");
					if (cl.getFkTable() == null) {
						//name �ֶ����⣬��Ҫ��ȡM_PRODUCT_TRANS 
						if(usr.getLangId()!=LanguageManager.getInstance().getDefaultLangId() && cname.equals("note")){
							sb.append(" lower(r.value) like ?");
						}else{
							sb.append(" lower(d.").append(cl.getRealName()).append(") like ?");
						}
						params.add("%" + querystr + "%");
					} else {
						Table fkt = cl.getFkTable();
						String sdk = null;
						for (Column dk : fkt.getDisplayKeys()) {
							if (!"id".equals(dk.getName())) {
								sdk = dk.getName();
								break;
							}
						}
						if (sdk == null)
							throw new NDSException(table + ".search_on��չ�����е��ֶ�:" + cname + "��FK���dks��id������ֶ�");
						sb.append(" exists(select 1 from " + fkt + " where " + fkt + ".id=d." + cl.getName()
								+ " and " + fkt + "." + sdk + " like ?)");
						params.add("%" + querystr + "%");

					}
					isFirst = false;
				}
				sb.append(")");

			} else
				throw new NDSException("��Ҫ����" + table + "��search_on��չ����");
			
			//
			sb.append(")");
			sb.append(")");
			data.put(sb.toString(), params);
		}		
		
		
		return data;
	}

}
