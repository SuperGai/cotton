package com.agilecontrol.b2bweb.cmd;

import java.util.*;

import org.json.*;

import com.agilecontrol.b2b.schema.Column;
import com.agilecontrol.b2bweb.WebController;
import com.agilecontrol.b2bweb.grid.GridBuilder;
import com.agilecontrol.b2bweb.grid.GridViewDefine;
import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.nea.util.Tools;
import com.agilecontrol.nea.util.Validator;
import com.agilecontrol.phone.CmdResult;
import com.agilecontrol.phone.PhoneController;
import com.agilecontrol.phone.PhoneUtils;
import com.agilecontrol.phone.PhoneController.SQLWithParams;

/**

h1. ����Grid�ṩ�����������

�� "b2b.pdt.search"�����������չ��֧��kvs������ѡ�� kvs�Ķ��������� ad_sql#online_edit_kvs ��[{key, desc, filtersql, valuesql, default}] 
�Ӷ�֧�ֶ������Զ�����Ʒ���Ϲ���ѡ�񣬴�ͳ��b2b.pdt.search �е�actid, isfav�� iscart��ͨ������Ϊ�����kv�������

�� "b2b.pdt.search" ��ͬ����������������grid�ͻ�������Ҫ�ġ�

> {cmd:"b2b.grid.search", dims, cat, pdtsearch, kvs, meta}

*dims* - jsonobj, key �ǵ�ǰѡ�е�column, value ��column��Ӧ��id, ����: {dim3: 12, dim4:2}
*cat* - ��ǰ�û�ѡ���������ඨ�壬ÿ��cat�����n��dimid��ɣ���b2b:cat:tree_conf���幹��
*pdtsearch* �����������������
*kvs* ����ѡ��Ĺ�������, jsonobj, key Ϊad_sql#online_edit_kvs��key��value�ǽ���ѡ���ֵ��boolean��checkbox�������ֵ(select)
*meta* boolean �Ƿ��ȡmeta��Ϣ����ֻ��gridviewdefine, 

 * 
 * 
 * @author yfzhu
 *
 */
public class GridSearch extends PdtSearch {
	/**
	 * ԭʼ������
	 */
	private JSONObject requestObj;
	
	private GridBuilder builder;

	/**
	 * ��ȡָ��key��Ӧ��json����
	 * @param key 
	 * @param kvDef ad_sql#online_edit_kvs
	 * @return null ���key δ����
	 * @throws Exception
	 */
	private JSONObject getKVDefine(String key, JSONArray kvDef) throws Exception{
		for(int i=0;i<kvDef.length();i++){
			JSONObject jo=kvDef.getJSONObject(i);
			String k=jo.getString("key");
			if(k.equals(key)) return jo;
		}
		return null;
	}
	/**
	 * to Array
	 * @param params
	 * @return
	 */
	private ArrayList toList(Object[] params) {
		ArrayList al=new ArrayList();
		for(Object o: params) al.add(o);
		return al;
	}
	/**
	 * ��ȡgrid����
	 * @return 
	 * @throws Exception
	 */
	private JSONObject getGridMetaDefine() throws Exception{
		GridViewDefine gvd=builder.getViewDefine();
		return gvd.toJSONObjet();
	}
	/**
	 *��ͷ���ݺͱ�ͷ����
	 h1.����
	 
	 {cmd:"b2b.grid.search",meta,table,id}
	 
	 h2. ���
	 
  	 {columns:[{childDesc:[{editable,field,headerName,width}],desc,isSize},dataDefine,linewrap,options,preDefine:{pre_css,pre_desc},sizeGroup,sumline:[{fmt,name,value}],buttons}
	 
	 *meta* -true���ر�ͷ���壬false���ر�ͷ����
	 
	 ����ͷ���壩
	 *colums* -��ͷ����
	 *cellStyle* -��ǰ�е���ʽ����
	 *desc* -��ͷ����
	 *editable* -��ǰ�е������Ƿ���Ա༭
     *field* -ǰ�˽���������
     *isSize* -�Ƿ��ǳ�����
     *width* -��ǰ�еĿ��
     *childDesc* -��������ο��еı�ͷ���� ���������Ϣ���вο��У���ѳ����кͲο��еı�ͷ�ϲ�
     *dataDefine* -��ǰ�е����ݶ���ʱ����ʽ����
     *linewrap* -grid cell����ĸ߶�
     *options* -��չ����
     *preDefine* -�ο��еĶ��� 
     *pre_css* -�ο��е���ʽ����
     *pre_desc* -�ο��е����� 
     *sizeGroup* -ǰ�˶���
     *sumline* -grid�ܼ����ö���ģ��
     *isPreSize* -�Ƿ���ǰ׺�ο���
     *isEnter* -�ǳ����У�����ǰ׺�ο��У���cell����������Ƿ��Ƕ���
     *buttons* -�����������㲻ͬģʽ�µĶ๦�ܰ�ť
     *headerName* -����ǰ׺�ο���ʱ���ںϲ���Ԫͷ���������               ��������
     ==============================================
     =       S��desc��               =		M��desc��	  =
     ==============================================
     =       44��desc��            =			45��desc��	  =
     ==============================================
     =headerName=headerName= 	�ο�         =		����	  =
     ==============================================
    
     h2. ���
     
           ����ͷ���ݣ�
	 *cachekey* -����
     *cnt* -��ǰ��������
     *rowData* -��ͷ����
     *asi* -�����е�Ψһ��ʶ
	 *cc* -��ɫ��ʶ
     *pdtId* -��Ʒid
     *pic* -ͼƬ��ַ
     *start* -
     *sum* -grid����ĺϼ�������
     *total* -���㵱ǰ������������Ʒ����
     
    
	 * @return JSONObject
	 * @throws Exception 
	 * @throws Exception
	 */
	public CmdResult execute(JSONObject jo) throws Exception {
		logger.debug("marketId:"+usr.getMarketId()+"-"+usr.getId());
		requestObj=jo;
		builder=new GridBuilder();
		if(Validator.isNull(jo.optString("table"))) throw new NDSException("@b2bedit-config@"+"ad_sql#grid:"+jo.optString("table")+":online_edit_kvs"+"@b2bedit-found@");
		//�����д��˵Ĳ�����������vc�й�����
		for(Iterator it=jo.keys();it.hasNext();){
			String key=(String)it.next();
			Object v=jo.get(key);
			vc.put(key, v);
		}
		
		builder.init(usr, requestObj,  event, vc, jedis, conn);

		if(jo.optBoolean("meta", false)){
			return new CmdResult(getGridMetaDefine());
		}
		return super.execute(jo);
	}
	/**
	 * ��������SearchResult.toJSON������и�װ���Է��Ͽͻ���Ҫ��
	 * @param ret redis SearchResult.toJSON��ֱ���ٴα༭. ����ĸ�ʽ������PdtSearch�к���idonly�Ŀ��ƣ��ʽ�����id
	 * sample
	 * ret:{"total":438,"b_mk_pdt_s":[473,472...],"cnt":54,"cachekey":"list:b_mk_pdt:893:438:list:eNkdT1jsQF274DBrMxLC9Q","start":0}
	 * @return {"total":438,"cnt":54,"cachekey":"list:b_mk_pdt:893:438:list:eNkdT1jsQF274DBrMxLC9Q","start":0, rowData, sum}
	 * rowData: 
	 * sum: {
	    "sumamt":"��16,378.00",
	    "sumqty":"325,000",
	    "sumamtlist":"��20,378.00"
	  }
	 * @throws Exception
	 */
	protected void postAction(JSONObject ret) throws Exception{
		JSONArray ids=(JSONArray) ret.remove("b_mk_pdt_s");
		
		//return rowData, sum
		JSONObject gbView=builder.getViewData(ids);
		for(Iterator it=gbView.keys();it.hasNext();) {
			String key=(String)it.next();
			ret.put(key, gbView.get(key));
		}
		
	}
	/**
	 * �������¹������������Թ��ˣ�����ղأ�pdtsearch, ��Ҫ���Ӷ�kvs��ʶ��
	 * 
	 * @param jo ���PdtSearch, ������kvs
	 * @return ����Ĳ�ѯ�����������ӵ���ѯwhere�����
	 * key: ��Ҫ���䵽sql where ���ֵ�clause������ݣ����� "emp_id=?"
	 * value ���ʺŶ�Ӧ��ʵ��ֵ�����value��java.util.List���������ֵ���棿��
	 */
	protected HashMap<String, Object> reviseSearchCondition(JSONObject jo) throws Exception{
		HashMap<String, Object> map=super.reviseSearchCondition(jo);
		vc.put("marketid", usr.getMarketId());
		
		JSONObject kvs=jo.optJSONObject("kvs");
		if(kvs!=null && kvs.length()>0){
			JSONArray kvDefs=(JSONArray) PhoneController.getInstance().getValueFromADSQLAsJSON("grid:online_edit_kvs", conn);
			for(Iterator it=kvs.keys();it.hasNext();){
				String key=(String)it.next();
				JSONObject kvDef=getKVDefine(key, kvDefs);
				if(kvDef==null) {
					logger.debug("key "+ key+" not defined in ad_sql#online_edit_kvs");
					continue;
				}
				if(kvDef.isNull("valuesql")){
					//boolean
					if(Tools.getBoolean(kvs.opt(key), false)==false)continue;
				}else{
					//select, �����صĽ��������Ϊ��������filtersql, �� actid->$actid
					vc.put(key, kvs.opt(key));
				}
				String filterSQL=kvDef.getString("filtersql");//ad_sql#name
				
				SQLWithParams spa= PhoneController.getInstance().parseADSQL(filterSQL, vc, conn);
				map.put("exists("+ spa.getSQL()+")", toList( spa.getParams()));
			}
		}
		//��ȡmeta �����ã�pdtfilter_sql�� ������grid:bro:sql:pdtfilter���� �Ƕ���Ʒ�Ķ�����������������˻�������Ʒ�嵥�������˻���ϸ��Ʒ
		JSONObject gridConf=builder.getGridConf();
		String pdtFilterSQLName= gridConf.optString("pdtfilter_sql");
		if(Validator.isNotNull(pdtFilterSQLName)){
			SQLWithParams sqlParams=PhoneController.getInstance().parseADSQL(pdtFilterSQLName, vc, conn);
			map.put(sqlParams.getSQL(), toList(sqlParams.getParams()));
		}
		
		
		return map;
	}
}
