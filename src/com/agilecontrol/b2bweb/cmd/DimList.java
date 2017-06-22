package com.agilecontrol.b2bweb.cmd;

import java.sql.Connection;
import java.util.*;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.aop.framework.HashMapCachingAdvisorChainFactory;

import com.agilecontrol.b2b.schema.Column;
import com.agilecontrol.b2b.schema.Table;
import com.agilecontrol.b2bweb.DimDefTranslator;
import com.agilecontrol.b2bweb.DimTranslator;
import com.agilecontrol.b2bweb.cmd.PdtSearch.DimValue;
import com.agilecontrol.nea.core.query.QueryEngine;
import com.agilecontrol.nea.core.query.QueryException;
import com.agilecontrol.nea.core.util.MessagesHolder;
import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.nea.util.Tools;
import com.agilecontrol.nea.util.Validator;
import com.agilecontrol.phone.Admin;
import com.agilecontrol.phone.CmdHandler;
import com.agilecontrol.phone.CmdResult;
import com.agilecontrol.phone.LanguageManager;
import com.agilecontrol.phone.PhoneConfig;
import com.agilecontrol.phone.PhoneController;
import com.agilecontrol.phone.UserObj;

import redis.clients.jedis.Jedis;

/**

h1. ��ȡ��Ʒ�����嵥

h2. ����

> {cmd:"b2b.dim.list", selected, actid, isfav, cat, pdtsearch}

*selected* - jsonobj, key �ǵ�ǰѡ�е�column, value ��column��Ӧ��id, ����: {dim3: 12, dim4:2}
*actid* - int �id, Ĭ��-1���Ƿ����ָ���Ļ�������Թ���
*isfav* - boolean �Ƿ������ղؼн������Թ��ˣ�Ĭ��false, true��ʱ�򲻶�ȡactid
*cat* - ��ǰ�û�ѡ���������ඨ�壬ÿ��cat�����n��dimid��ɣ���b2b:cat:tree_conf���幹��
*pdtsearch* �����������������

h2. ���

> [{column,desc, values}]

*column* - string����ǰ�ֶε�����, ��Ϊkey�ش�������
*desc* - string ��ǰ�ֶε���������ʾ�ڽ�����
*values* - [{k:v}] ���飬ÿ��Ԫ�ض���һ����һ���ԵĶ������Ե�key��ֵid��value��ֵ����ʾ����������

<pre>
[
   {column:"dim3", desc:"����", values:  [{1:"��"}, {2:"��"}, {3:"��"}]},
   {column:"dim4", desc:"�۸��", values:  [{1:"~100"}, {2:"200~1000"}, {3:"1000~"}]}
   {column:"dim14", desc:"Ʒ��", values:  []}
]   
</pre>  

h2. ����˵��

Ĭ��selected Ϊ�յ�ʱ��ϵͳ������ȫ���Ŀ�ѡ�����б�selected��Ӧ���ֶβ����ڽ��д���

���� ad_sql#b2b:dim_conf�����������ã��ṹ["dimname"]������:
> ["dim14", "dim3", "dim5"]
��ʾ��ʹ����Ʒ���dim14,dim3,dim5�ֶν���dim��ʾ��������е�key������selected�У������������������Ҫ��Ե�ǰselected
�����ݽ��й��ˣ�����Ҫ���cat��isfav��actid��

2016-12-12 @author lsh
���b2b��ͬģʽ���������� *DimList�����㲻ͬ����

 * @author yfzhu
 *
 */
@Admin(mail="yfzhu@lifecycle.cn")
public class DimList extends CmdHandler {
	
	private boolean isFav=false;
	private boolean isCart=false;//�Ƿ��ڹ��ﳵ������

	private int actId=-1;
	private String blank;//'�հ�'�����Է���
	private String querystr;//"pdtsearch" from web ����ģ������
	private JSONObject config;//ǰ̨������jo
	class DimValue{
		String dim; //eg: dim15
		int id; // ��ǰѡ���ֵ��eg: 13���� m_dim15_id=13
	}
	private ArrayList<DimValue> dimNodes;
	/**
	 * Ҫ���ʵ�dim�Ŀ�ѡֵ����Ҫ����cat,actid,isfav�Ƚ��й��ˣ�����û�������������������ȫƥ��ÿ����Ʒ���࣬����ѡ�˷��࣬���ٻ�����Ʒ����������͵ľ���Ʒ�������
	 * @param dim Ҫ���ʵ�dim
	 * @param dimNodes ��������,��Щ���Ѿ�ȷ�ϵ��Ĳ�ѯ����
	 * 
	 * @return [{id:value}] format
	 */
	private JSONArray filterDimValues(String dim,ArrayList<DimValue> dimNodes) throws Exception{
		JSONArray values=new JSONArray();

		StringBuilder sb=new StringBuilder();
		ArrayList params=new ArrayList();
		
		boolean isDefaultLang=(usr.getLangId()==LanguageManager.getInstance().getDefaultLangId());
		
		
		/**
		 * sql format: select distinct dim_id, dim from v_pdtdims dims where catdim_id=xxx 
		 * 
		 * ��Ӧ�۸�����ԣ�dim13��д���ǣ� select distinct dim13.id, dim13.orderno from b_mk_pdt m, v_pdtdims d, m_dim dim13 where m.isactive='Y' and dim13.id=m.m_dim13_id and m.m_product_id=d.pdtid
		 */
		if(PhoneConfig.PRICE_RANGE_DIM.equals(dim)){
			sb.append("select distinct dim13.id dim13_id,dim13.attribname dim13, dim13.orderno dim13_orderno from b_mk_pdt m, v_pdtdims d, m_dim dim13 ");
			//���������ӶȻ������ܶ�
			if(Validator.isNotNull(querystr)){
				if(!isDefaultLang){
					sb.append(", m_product_trans r, m_product p where r.m_product_id=d.pdtid and r.b_language_id=? and p.id=d.pdtid and ");
					params.add(usr.getLangId());
				}else{
					sb.append(", m_product p where p.id=d.pdtid and ");
				}
			}else{
				sb.append(" where ");
			}
			sb.append("m.isactive='Y' and dim13.id=m.m_dim13_id and m.m_product_id=d.pdtid ");
		}else{
			sb.append("select distinct d.").append(dim).append("_id,d.").append(dim).append(",d.").append(dim).append("_orderno from v_pdtdims d, b_mk_pdt m ");
			//���������ӶȻ������ܶ�
			if(Validator.isNotNull(querystr)){
				if(!isDefaultLang){
					sb.append(", m_product_trans r, m_product p where r.m_product_id=d.pdtid and r.b_language_id=? and p.id=d.pdtid and ");
					params.add(usr.getLangId());
				}else{
					sb.append(", m_product p where p.id=d.pdtid and ");
				}
			}else{
				sb.append(" where ");
			}
			sb.append("m.isactive='Y' and d.").append(dim).append("_id is not null and m.m_product_id=d.pdtid");
		}
		for( DimValue dv:dimNodes){
			if(dv.id==-2){
				if(dv.dim.equals(PhoneConfig.PRICE_RANGE_DIM))
					sb.append(" and m.m_").append(dv.dim).append("_id is null");
				else
					sb.append(" and d.").append(dv.dim).append("_id is null");
			}else{
				if(dv.dim.equals(PhoneConfig.PRICE_RANGE_DIM))
					sb.append(" and m.m_").append(dv.dim).append("_id=?");
				else
					sb.append(" and d.").append(dv.dim).append("_id=?");
				params.add(dv.id);
			}
		}
		//market
		sb.append(" and m.b_market_id=?");
		params.add(usr.getMarketId());
		
		// search
		Table table=manager.getTable("pdt");
		// fuzzy search
		if (Validator.isNotNull(querystr)) {
			/**
			 * ��ѯ�ֶΣ����壺String, Ϊ�ֶ����ö��ŷָ�����: "docno,cust_id";
			 * ����fk���͵��ֶΣ���Ĭ��ȥ����fk�ĵ�һ������id��dks �ֶ�
			 * 
			 */
			//�������ݺ��Դ�Сд
			sb.append(" and (");
			querystr = querystr.toLowerCase();
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
						if(!isDefaultLang && cname.equals("note")){
							sb.append(" lower(r.value) like ?");
						}else{
							sb.append(" lower(p.").append(cl.getRealName()).append(") like ?");
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
						sb.append(" exists(select 1 from " + fkt + " where " + fkt + ".id=p." + cl.getName()
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
		}		
		
		// ������Ҫ���� actid,isfav
		if(actId!=-1){
			sb.append(" and exists(select 1 from b_prmt_pdt a where a.m_product_id=d.pdtid and a.b_prmt_id=? and a.isactive='Y')");
			params.add(actId);
		}
		
		if(isFav){
			sb.append(" and exists(select 1 from B_FAVOURITE f where f.m_product_id=d.pdtid and f.user_id=? and f.isactive='Y')");
			params.add(usr.getId());
		}
		if(isCart){
			sb.append(" and exists(select 1 from b_cart c where c.b_market_id=m.b_market_id and c.m_product_id=m.m_product_id and c.user_id=? and c.isactive='Y')");
			params.add(usr.getId());
		}
		//������������������B2B��ͬģʽ�����dimlistѡ�������
		HashMap<String, Object> additionalParam = reviseSearchCondition(config);
		
		if (additionalParam != null) {
			for (String key : additionalParam.keySet()) {
				Object value = additionalParam.get(key);
				sb.append(" and ").append(key);
				if(value instanceof List){
					for(Object v: (List)value){
						params.add(v);
					}
				}else if(value instanceof Object[]){
					for(Object v: (Object[])value){
						params.add(v);
					}
					
				}else
					params.add(value);
			}
		}
		
		sb.append(" order by ").append(dim).append("_orderno");
		
		String sql=sb.toString();
		JSONArray rows=engine.doQueryJSONArray(sql, params.toArray(), conn);
		for(int i=0;i<rows.length();i++){
			JSONArray row=rows.getJSONArray(i);
			int id=row.getInt(0);//dim.id
			
			String val=row.optString(1);//dim.name
			if(Validator.isNull(val)) val=blank;
			//translate 
			val=DimTranslator.getInstance().getTranslateName(id, usr.getLangId(), val, conn);
			JSONObject one=new JSONObject().put(String.valueOf(id), val);
			values.put(one);
		}
		return values;
	}
	/**
	 * ���ݴ��˵�cat�ֽ��dim��ֵ, ��������ѡ���dimҲһ������
	 * @param cat - ��ʽ��"12.145.8438", �ֱ��Ӧ b2b:cat:tree_conf ��ÿ��dim������
	 * @param selectedDims - format  {dim3: 12, dim4:2}
	 * @throws Exception
	 */
	private ArrayList<DimValue> initCatNodes(String cat,JSONObject selectedDims ) throws Exception{
		ArrayList<DimValue> dimNodes=new ArrayList();
		if(Validator.isNotNull(cat)){
			String[] ids =cat.split("\\.");//��ȡ����cat�ĸ����㼶��ֵ
			JSONArray catDef= (JSONArray)PhoneController.getInstance().getValueFromADSQLAsJSON("b2b:cat:tree_conf", conn);
			for(int i=0;i< ids.length;i++){
				DimValue dv=new DimValue();
				dv.id=Tools.getInt( ids[i],-1);
				if(dv.id==-1){
					logger.warn("not a valid int for "+ cat+"["+ i+"] ");
				}else{
					dv.dim= catDef.getString(i);
					dimNodes.add(dv);
				}
			}
		}
		for(Iterator it=selectedDims.keys();it.hasNext();){
			String dim=(String)it.next();
			DimValue dv=new DimValue();
			dv.dim=dim;
			dv.id= selectedDims.getInt(dim);
			dimNodes.add(dv);
		}
		return dimNodes;
	}
	/**
	 * ���ӹ�������
	 * 
	 * @param jo
	 * @return
	 * @throws Exception
	 */
	protected HashMap<String, Object> reviseSearchCondition(JSONObject jo) throws Exception{
		return null;
	}
	/**
	 * ��ʼ��ǰ̨jo
	 */
	private void init(JSONObject jo) {
		this.config = jo;
	}
	@Override
	public CmdResult execute(JSONObject jo) throws Exception {
		
		init(jo);
		//key:dim, value column value
		JSONObject selectedDims= jo.optJSONObject("selected");
		if(selectedDims==null)selectedDims=new JSONObject();
		
		blank=MessagesHolder.getInstance().getMessage(locale, "blank");
		isFav= jo.optBoolean("isfav",false);
		isCart=jo.optBoolean("iscart",false);
		actId= jo.optInt("actid", -1);
		dimNodes=initCatNodes(jo.optString("cat"),selectedDims );
		this.querystr= jo.optString("pdtsearch");
		
		JSONArray conf=(JSONArray)PhoneController.getInstance().getValueFromADSQLAsJSON("b2b:dim_conf", conn, false);
		JSONArray dims=new JSONArray();
		DimDefTranslator ddt=DimDefTranslator.getInstance();
		
		//conf
		
		//�ͻ�Ҫ����������ݺ�ĳ��Ʒ����һ��ʱ��ֱ��ѡ�д����ԣ����������Ʒ����, ���ƾ���������������������ĳƷ�����ƺ�ֱ�Ӿ�ƥ���Ʒ���ˡ�
		//����������Ʒ���Ե������Ͳ����Դ���Ϊ��Ʒ��������
		String querystrDimColumn=null;
		JSONArray querystrDimColumnValues=null;
		for(int i=0;i<conf.length();i++){
			String column=conf.getString(i);
			if(selectedDims.has(column))continue;
			if(Validator.isNotNull(querystr)){
				int dimId=DimTranslator.getInstance().getDimId(usr.getLangId(), querystr, conn);
				if(dimId!=-1){
					querystrDimColumn=column;
					JSONObject one=new JSONObject();
					one.put(String.valueOf(dimId), querystr);
					querystrDimColumnValues=new JSONArray();
					querystrDimColumnValues.put(one);
					querystr=null;//remove
					
					DimValue dv=new DimValue();
					dv.dim=column;
					dv.id=dimId;
					dimNodes.add(dv);
					break;
				}
			}
		}
		
		
		for(int i=0;i<conf.length();i++){
			String column=conf.getString(i);
			if(selectedDims.has(column))continue;
			
			JSONObject row=new JSONObject();
			row.put("column", column);
			String desc= ddt.getTranslateName(column, usr.getLangId(), conn);
			if(desc==null) desc=column;
			row.put("desc", desc);
			if(column.equals(querystrDimColumn)){
				row.put("values", querystrDimColumnValues);
			}else{
				JSONArray values=filterDimValues(column,dimNodes);
				row.put("values",values);
			}
			dims.put(row);
		}
		
		return new CmdResult(dims);
	}
}

