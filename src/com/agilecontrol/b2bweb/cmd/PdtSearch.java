package com.agilecontrol.b2bweb.cmd;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONObject;

import com.agilecontrol.b2b.cmd.Search;
import com.agilecontrol.b2b.query.SearchResult;
import com.agilecontrol.b2b.schema.Column;
import com.agilecontrol.b2b.schema.Table;
import com.agilecontrol.b2b.schema.TableManager;
import com.agilecontrol.b2bweb.WebController;
import com.agilecontrol.b2bweb.cmd.DimList.DimValue;
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
import com.agilecontrol.phone.PhoneUtils;

/**
 ����Search, ÿ����Ʒ������Ҫ��ȡ�����M_PRODUCT_TRANS
 ���з���

> {cmd:"b2b.pdt.search", ������������, dims, actid, isfav, cat,  pdtsearch, iscart}

*dims* - jsonobj, key �ǵ�ǰѡ�е�column, value ��column��Ӧ��id, ����: {dim3: 12, dim4:2}
*actid* - int �id, Ĭ��-1���Ƿ����ָ���Ļ�������Թ���
*isfav* - boolean �Ƿ������ղؼн������Թ��ˣ�Ĭ��false, true��ʱ�򲻶�ȡactid
*cat* - ��ǰ�û�ѡ���������ඨ�壬ÿ��cat�����n��dimid��ɣ���b2b:cat:tree_conf���幹��
*pdtsearch* �����������������
*iscart* - �Ƿ���ڹ��ﳵ����

 �Աȱ�׼��search������
 {
    table, exprs, querystr,  maxcnt,pagesize, mask, usepref, orderby
 }
 
 
 
 * @author yfzhu
 *
 */
@Admin(mail="yfzhu@lifecycle.cn")
public class PdtSearch extends Search {
	private boolean isFav=false;
	private int actId=-1;
	private boolean isCart=false;//�Ƿ��ڹ��ﳵ������
	private JSONArray types =null;// ����������
	private String blank;//'�հ�'�����Է���
	class DimValue{
		String dim; //eg: dim15
		int id; // ��ǰѡ���ֵ��eg: 13���� m_dim15_id=13
		public String toString(){
			return "{dim:"+dim+",id:"+id+"}";
		}
	}
	private ArrayList<DimValue> dimNodes;
	/**
	 * ���ݴ��˵�cat�ֽ��dim��ֵ, ��������ѡ���dimҲһ������
	 * @param cat - ��ʽ��"12.145.8438", �ֱ��Ӧ b2b:cat:tree_conf ��ÿ��dim������
	 * @param selectedDims - format  {dim3: 12, dim4:2}
	 * @throws Exception
	 */
	private ArrayList<DimValue> initCatNodes(String cat,JSONObject selectedDims ) throws Exception{
		ArrayList dimNodes=new ArrayList();
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
		logger.debug("dimNodes:"+ Tools.toString(dimNodes, ","));
		return dimNodes;
	}
	/**
	 * �������¹������������Թ��ˣ�����ղأ�pdtsearch
	 * 
	 * ������Ĳ�ѯ���������ع�����֧������Ĳ�ѯ����
	 * @param jo ԭʼ�Ĳ�ѯ�������п����ڴ˵ر��ع�
	 * @return ����Ĳ�ѯ�����������ӵ���ѯwhere�����
	 * key: ��Ҫ���䵽sql where ���ֵ�clause������ݣ����� "emp_id=?"
	 * value ���ʺŶ�Ӧ��ʵ��ֵ�����value��java.util.List���������ֵ���棿��
	 */
	protected HashMap<String, Object> reviseSearchCondition(JSONObject jo) throws Exception{
		//stao ���� vc����, Ϊ����������ѯ��׼��
		Table usrTable=TableManager.getInstance().getTable("usr");
		JSONObject usrObj = PhoneUtils.fetchObject(usrTable, usr.getId(), conn, jedis);
		vc.put("1",1);
		vc.put("langid", 1);
		vc.put("uid", usr.getId());
		vc.put("marketid",usr.getMarketId());
		vc.put("storeid",usrObj.getInt("c_store_id"));
		
		HashMap<String, Object> map=new HashMap<String, Object>();
		//market
		map.put("b_mk_pdt.b_market_id=?", usr.getMarketId());
		map.put("b_mk_pdt.isactive=?", "Y");//��Ҫ���ϼ���Ʒ 2016.9.10
		
		StringBuilder sb=new StringBuilder();
		ArrayList params=new ArrayList();
		/**
		 * paramsΪ�գ���ʾû��where ����������ʱisnull������ʽ������Ҫ
		 */
		boolean hasWhereClause=false;
		//dims
		if(usr.getLangId()!=LanguageManager.getInstance().getDefaultLangId()){
			sb=new StringBuilder("exists(select 1 from m_product_view d, M_PRODUCT_TRANS r where r.m_product_id=d.id and r.b_language_id=? and d.isactive='Y' and d.id=b_mk_pdt.m_product_id");
			params.add(usr.getLangId());
		}else{
			sb=new StringBuilder("exists(select 1 from m_product_view d where d.isactive='Y' and d.id=b_mk_pdt.m_product_id");
		}
		for( DimValue dv:dimNodes){
			//�����и�����ֵ����2����ʾΪ��, ����catree�����õĶ���
			if(dv.id==-2){
				if(dv.dim.equals(PhoneConfig.PRICE_RANGE_DIM))
					sb.append(" and b_mk_pdt.m_").append(dv.dim).append("_id is null");
				else
					sb.append(" and d.m_").append(dv.dim).append("_id is null");
				hasWhereClause=true;
			}else{
				if(dv.dim.equals(PhoneConfig.PRICE_RANGE_DIM))
					sb.append(" and b_mk_pdt.m_").append(dv.dim).append("_id=?");
				else
					sb.append(" and d.m_").append(dv.dim).append("_id=?");
				params.add(dv.id);
			}
		}
		// search
		Table table=manager.getTable("m_product_view");
		String querystr=jo.optString("pdtsearch");
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
		}		
		
		sb.append(")");
		if(params.size()>0 || hasWhereClause )map.put(sb.toString(), params);
		
		// ������Ҫ���� actid,isfav
		if(actId!=-1){
			map.put("exists(select 1 from b_prmt_pdt b_prmt_pdt where b_prmt_pdt.m_product_id=b_mk_pdt.m_product_id and b_prmt_pdt.b_prmt_id=? and b_prmt_pdt.isactive='Y')", actId);
		}
		
		if(isFav){
			
			map.put("exists(select 1 from B_FAVOURITE f where f.m_product_id=b_mk_pdt.m_product_id and f.user_id=? and f.isactive='Y')",usr.getId());
		}
		if(isCart){
			map.put("exists(select 1 from b_cart c where c.b_market_id=b_mk_pdt.b_market_id and c.m_product_id=b_mk_pdt.m_product_id and c.user_id=? and c.isactive='Y')",usr.getId());
		}
		
		/**
		 * ��ӹ������� ���� adsql#pdtsearchConf ���ý��ж������
		 * add by stao 2017/06/21
		 */
		if(null !=types && types.length() !=0){
			JSONObject adJson =(JSONObject)PhoneController.getInstance().getValueFromADSQLAsJSON( "pdtsearchConf", conn);
			if (null == adJson) {
				throw new NDSException("����  ad_sql#pdtsearchConf����");
			}
			for (int i = 0; i < types.length(); i++) {
				String typeStr = types.getJSONObject(i).optString("type");
				if(Validator.isNull(typeStr)){
					throw new NDSException("�ͻ��� type����Ϊ�� ");
				}
				JSONObject typeObj = adJson.optJSONObject(typeStr);
				if (null == typeObj) {
					throw new NDSException("����  ad_sql#pdtsearchConf " + typeStr+ " ����");
				}
				/*
				 * ����type�����õ�filter �����������й���, һ��type��Ӧһ����������
				 */
				JSONObject filter = typeObj.optJSONObject("filter");
				if (null ==filter ) {
					throw new NDSException("����  ad_sql#pdtsearchConf " + typeStr + " filter" + " ����");
				}
				/**
				 * ������������ӵ�map��
				 * ��ȡ ������ sql���Ʋ�ѯ������������Ӧ��sql���
				 */
				String sqlname = filter.getString("sqlname");
				String sql = PhoneController.getInstance().getValueFromADSQL(sqlname, conn);
				if(Validator.isNull(sql)){
					throw new NDSException("����  ad_sql#pdtsearchConf " + typeStr + " filter  sqlname" + " ����");
				}
				JSONArray paramnames =filter.optJSONArray("paramnames");
				if ( null == paramnames || paramnames.length()==0) {
					throw new NDSException("����  ad_sql#pdtsearchConf " + typeStr + " filter  paramnames" + " ����Ϊ��");
				}
				ArrayList<Object> obs = new ArrayList<Object>();
				for (int j = 0, length=paramnames.length(); j<length; j++) {
					String name = paramnames.getString(j);
					Object value =vc.get(name);
					obs.add(value);
				}
				
				//�� filter�����õ�sql��� �Ͳ��� ��ӵ� map����Ϊ��������
				map.put(sql,obs);
			}
		}
		return map;
	}
	
	public CmdResult execute(JSONObject jo) throws Exception {
		String cacheKey=jo.optString("cachekey");
		if(Validator.isNotNull(cacheKey) && jedis.exists(cacheKey) /*������key timeoutҲҪ���²�*/){
			jo.put("table","m_product_view");//cache key wil reload from this table's pk records
		}else{
			jo.put("table","b_mk_pdt");//������ʵ�Ƿ�����select m_product_id from b_mk_pdt where xxx ��������䣬����table��b_mk_pdt
		}
		jo.put("pkcolumn", "m_product_id");// need return b_mk_pdt.m_product_id for search result
		jo.put("idonly", true);// only need m_product_id
		
		if(jo.has("querystr") || jo.has("exprs")) throw new NDSException("����ʹ��querystr��exprs����");
		
		//key:dim, value column value
		JSONObject selectedDims= jo.optJSONObject("dims");
		if(selectedDims==null)selectedDims=new JSONObject();
		
		blank=MessagesHolder.getInstance().getMessage(locale, "blank");
		isFav= jo.optBoolean("isfav",false);
		actId= jo.optInt("actid", -1);
		dimNodes=initCatNodes(jo.optString("cat"),selectedDims );
		isCart=jo.optBoolean("iscart",false);
		types =jo.optJSONArray("types");
		return super.execute(jo);
	}
	
	
	/**
	 * ��������SearchResult.toJSON������и�װ���Է��Ͽͻ���Ҫ��
	 * @param ret redis SearchResult.toJSON��ֱ���ٴα༭
	 * sample:
	 *  ret:{"total":438,"b_mk_pdt_s":[473,472...],"cnt":54,"cachekey":"list:b_mk_pdt:893:438:list:eNkdT1jsQF274DBrMxLC9Q","start":0}
	 * @throws Exception
	 */
	protected void postAction(JSONObject ret) throws Exception{
		logger.debug("ret:"+ ret);
		JSONArray ids=(JSONArray) ret.remove("b_mk_pdt_s");		
		ArrayList<Column> cols=manager.getTable("m_product_view").getColumnsInListView();
		JSONArray data=PhoneUtils.getRedisObjectArray("m_product_view", ids, cols, true, conn, jedis);
		for(int i=0;i<data.length();i++){
			JSONObject jo=data.getJSONObject(i);
			//����
			int pdtid=jo.getInt("id");
			vc.put("marketid",usr.getMarketId());
			vc.put("pdtid", pdtid);
			JSONObject  sale_price=PhoneController.getInstance().getObjectByADSQL("b2b:pdtlist:price", vc, conn);
			if(sale_price!=null){
				if(sale_price.get("description").equals("�ؼ�")){
					jo.put("description","issale");
				}
				if(sale_price.get("description").equals("���")){
					jo.put("description","isqinghuo");
				}
			
			jo.put("sale_price", sale_price.get("price"));
			}
			WebController.getInstance().replacePdtValues(jo, usr, vc, jedis, conn);			
		}
		ret.put("pdt_s", data);
	}
}
