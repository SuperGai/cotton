package com.agilecontrol.b2bweb.cmd;


import org.json.JSONArray;
import org.json.JSONObject;

import com.agilecontrol.b2bweb.WebController;
import com.agilecontrol.nea.core.query.QueryEngine;
import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.nea.util.Validator;
import com.agilecontrol.phone.Admin;
import com.agilecontrol.phone.CmdHandler;
import com.agilecontrol.phone.CmdResult;
import com.agilecontrol.phone.PhoneController;

/**


h1. ��ȡ��Ʒ�����µ�ǰ��������Ʒ

h2. ����

��ҳ���б�

h2. ����

> {cmd:"b2b.cat.get", cat, withsubcat, subcatcnt, pdtcnt}

*dim* - ��Ʒ���ԣ�����CatTree��������������"13.34" ��dim1��id��13����dim2��id��34�ķ��ඨ�� 

*withsubcat* - �Ƿ���ʾ�¼����� boolean default false
*subcatcnt* - �¼��������, int��default 3
*pdtcnt* - ��Ҫ��ʾ��ÿ�������е���Ʒ������ int��default 10


h2. ���

��withsubcat=false��ʱ��

> [{pdt}]

��withsubcat=true,

> [{cat}]

pdt ������
> {pdtid,note,no,mainPic,price,tags}
*pdtid* - int pdtid
*note* - string ��Ʒ��ע 
*no* - string ��Ʒ���
*mainPic* -string ��ƷͼƬ
*price* - double ��ʾ�ļ۸�
*tags* - jsonarray of string ��Ʒ��ǩ����: ["����","������ͬ��"]

cat ������
> {dim,name,pic,content}

*id* - m_dim.id 
*title* - string ��������, m_dim.desc
*img* - string ����ͼƬ
*content* - [{pdt}]  ��Ʒ�б�

h3. ʵ��

���ָ��������¼�����ķ���

����CatTree����ȡ "b2b:cat:tree:$marketname:$lang:dim", ���ݿͻ��˵�dimֵ���ҵ�value��list of subcat dim obj {dim,name,pic},
��ȡ�󣬻���Ҫ����

��λ�ȡdim�µ���Ʒ�б�
redis ����: key: "b2b:cat:pdts:$marketname:$dimid", value: list of pdtid 
$dimid - "132.34" ��������ʽ����cat��dimid���ɽ���Ϊ��Ӧ�� ad_sql#b2b:cat:tree_conf �ж���dim�㼶������: ["dim1", "dim3", "dim14"] �ĸ���ֵ���Ӷ����Թ����Ӧ��sql���
*pdts* - �ڵ�ǰ��Ʒ�����µ�pdt id array��order by B_MK_PDT.orderno

{pdt} �������

redis���ж�ȡpdtid, Ȼ�������redis obj����ȡ���壬key: "pdt:$id", value: {pdt list columns}


 * @author yfzhu
 *
 */
@Admin(mail="yfzhu@lifecycle.cn")
public class CatGet extends CatTree {
	
	/**
	 * @param jo - {cat:"14.4", withsubcat:true, subcatcnt:6, pdtcnt:12}
	 */
	public CmdResult execute(JSONObject jo) throws Exception {
		//�õ�ad_sql�Ĳ㼶������cat�е�dimidƥ��
		String ad_sql = "b2b:cat:tree_conf";//���ad_sql
		JSONArray level = (JSONArray)PhoneController.getInstance().getValueFromADSQLAsJSON(ad_sql, conn);
		//����cattree
		getAllDim((usr.getMarketId()), (usr.getLangId()));
		//�õ�pdtint��Ĭ��ֵ��������Ӧ��Ŀ����Ʒjsonobject
		int pdtcnt = jo.optInt("pdtcnt",12);
		
		String[] str = jo.optString("cat").split("\\.");//��ȡ����cat�ĸ����㼶��ֵ
		//�ж�dim�㼶�Ƿ���catƥ�䣬��ƥ���޷�ƴ������sql
		if(level.length() < str.length){
			throw new NDSException("dim��β��������󲻷�"); 
		}
		
		//��������dimidƴ�ճ�����sql
		StringBuilder pdimid = new StringBuilder();
		for(int i = 0;i < str.length;i++){
			if(i==0){
				pdimid.append("p.m_"+level.getString(i)+"_id = "+str[i]);
			}
			else{
				pdimid.append(" and p.m_"+level.getString(i)+"_id = "+str[i]);
			}
		}
		String sql = "select"
				+ " p.id from b_mk_pdt m, m_product p "
				+ "where p.id = m.m_product_id "
				+ "and m.b_market_id = ? "
				+ "and "+pdimid+" "
						+ "and m.isactive = 'Y'  "
						+ "order by m.orderno desc";
		logger.debug("sql�����:"+sql);
		JSONArray ja = QueryEngine.getInstance().doQueryObjectArray(sql, new Object[]{usr.getMarketId()},conn);
		//��õ�һ�������ֶ�Ϊp.id��JSONObject��JSONArray��������ͨ����������һ�����������JSONObject��Ȼ�������Ҫ���ص�array��ȥ
		JSONArray newarray = new JSONArray();
		//����Ĭ��������Ʒ���������������С�����ɵ�������ֱ�ӱ������󣬷�֮��ֱ�ӱ������������ɵ�pdt
		if(pdtcnt < ja.length()){
			for(int j = 0;j < pdtcnt;j++){
				String pid = new String();
				pid = ja.getJSONObject(j).optString("id");
				JSONObject obj = fetchObject(Long.valueOf(pid), "pdt",false);
				JSONObject objTrans = WebController.getInstance().replacePdtValues(obj, usr, vc, jedis, conn);
				newarray.put(objTrans);
			}
		}else{
			for(int j = 0;j < ja.length();j++){
				String pid = new String();
				pid = ja.getJSONObject(j).optString("id");
				JSONObject obj = fetchObject(Long.valueOf(pid), "pdt",false);
				JSONObject objTrans = WebController.getInstance().replacePdtValues(obj, usr, vc, jedis, conn);
				newarray.put(objTrans);
			}
		}
		  
		return new CmdResult(newarray);
	}
	
}
