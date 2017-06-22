package com.agilecontrol.b2b.schema;
import org.json.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.nea.util.NDSRuntimeException;
import com.agilecontrol.nea.util.Validator;

/**
 * ����Ĺ��������綩����ϸ��������ϸ��֧����¼��
 * 
 * ֧��1�Զ࣬Ҳ֧��1��1��һ��1�Զ���ʾ�б��ֶΣ�һ�Զ���ʾ�������ֶ�
 * 
 * ������ϵ����ͨ��master table���ֶΣ��ӱ��fk�ֶ�����Ӧ������ƴ�ӵ�sql����ʽ
 * select id from table where fkcolumnid=maintable.id
 * table:{id,name,cols, refby:[{table:"itemtable", column:"fkcolumn", assoc:1,filter:"itemtable.othercolumn=some"}]}
 * 
 * ���磺
 * spo:{... refbys: [ {table:"spo_item", column:"spo_id", assoc:2, sql:"refby:spo_item:list", lua:"lua:spo_item:list"} ] }
 * 
 * ����˵����
 * name - �ӱ�ı����������ڻ�ȡ�����ʱ��ָ����������趨��Ĭ�Ͼ���table.name+"_s"��Ϊ����
 * table - ��ǰ�ӱ������
 * column - ��ǰ�ӱ�ָ�򸸱�ID���ֶ�
 * assoc - 1  1:1, 2 1:m
 * adsql - ��ʽ: select id from item_table where fk_id=$objectid and other condition,  ����ŵ���ad_sql.name, ������չ��������
 *             sql ��lua����ʱ��Ч��sqlһ�����壬����ʹ�ñ�׼sql ��䣬 sql �еĿ�ѡ���� $objectid, $uid, $comid, $stid, $empid
 *             
 * lua - ��ʽ: lua(json):json �Ǳ�׼��lua���������ع�����List�����ٶ�ȡsql���壬Ҳ���ٷ�����׼sql���
 * filter - ���itemtable�Ķ����������
 * @author yfzhu
 *
 */
public class RefByTable implements org.json.JSONString{
	private static Logger logger=LoggerFactory.getLogger(RefByTable.class);
	/**
	 * Like BParnter vs Vendor
	 */
	public final static int ONE_TO_ONE=1;
	/**
	 * Like Order vs Order Lines
	 */
	public final static int ONE_TO_MANY=2;
	private int assocType; //ONE_TO_ONE, ONE_TO_MANY
	private String table; // ref-table
	private String column; // ref-table's column
	private String name;//ref-table alias name
	/**
	 * ������ϸ�����ݻ�ȡ��sql����Ӧ��ad_sql��name
	 */
	private String adsql;
	/**
	 * �����ع�����List��ad_sql��name
	 */
	private String lua;
	/**
	 * ������������ʽ: itemtable.othercolumn=xxx, ����ϵ���ǰ��ϸ���ɸѡ����У�������ɸѡ���:
	 * itemtable.column=maintable.id 
	 * ��������ʾΪ: itemtable.column=maintable.id and itemtable.othercolumn=xxx
	 */
	private String filter;
	
	@Override
	public String toJSONString() {
		try{
			return toJSONObject().toString();
		}catch(Throwable t){
			throw new NDSRuntimeException("Fail to to json:"+name+":"+ t.getMessage() );
		}
		
	}	
	
	public JSONObject toJSONObject() throws JSONException{
		JSONObject jo=new JSONObject();
		jo.putOpt("name", name);
		jo.putOpt("column", column);
		jo.putOpt("table", table);
		jo.putOpt("adsql", adsql);
		jo.putOpt("lua", lua);
		jo.putOpt("assoc", assocType);
		jo.putOpt("filter", filter);
		
		return jo;
	}
	
	/**
	 * 
	 * @param ref {name,table,column,assoc, adsql, lua}
	 * @param masterTable
	 * @return
	 * @throws Exception
	 */
	static RefByTable parse(JSONObject ref, Table masterTable) throws Exception{
		RefByTable rbf=new RefByTable();
		rbf.name= ref.optString("name");
		rbf.table= SchemaUtils.getString(ref,"table").toLowerCase();
		rbf.column=SchemaUtils.getString(ref,"column").toLowerCase();
		
		rbf.assocType=ref.optInt("assoc", ONE_TO_MANY);
		if(rbf.assocType !=1 && rbf.assocType!=2) throw new NDSException(masterTable.getName()+"��reftable "+ rbf.table+"#assoc����������:"+ rbf.assocType+" ��Ч��");
		
		rbf.adsql=ref.optString("adsql");
		rbf.lua=ref.optString("lua");
		rbf.filter=ref.optString("filter");
		
		if(Validator.isNull(rbf.name)) rbf.name=rbf.table+"_s";//name rule
		
		if(masterTable.getColumn(rbf.name)!=null){
			throw new NDSException("��Ϊ"+ masterTable.getName()+"���ӱ�"+ rbf.name+"��������name�����ֶ�����");
		}
		return rbf;
	}
	/**
	 * ��У��table,column������
	 * @param mgr
	 * @throws Exception
	 */
	void finishParse(TableManager mgr,  Table masterTable) throws Exception{
		Table tb=mgr.tableNames.get(this.table);
		if(tb==null) throw new NDSException("����"+ masterTable.getName()+"���ӱ�"+ name+",�����tableδ�ҵ�("+ this.getTable()+")");
		if(tb.getColumn(this.column)==null)
			throw new NDSException("����"+ masterTable.getName()+"���ӱ�"+ name+",�����columnδ�ڱ�("+ this.getTable()+")���ҵ�");
	}
	/**
	������������ʽ: itemtable.othercolumn=xxx, ����ϵ���ǰ��ϸ���ɸѡ����У�������ɸѡ���:
	 * itemtable.column=maintable.id 
	 * ��������ʾΪ: itemtable.column=maintable.id and itemtable.othercolumn=xxx
	 * @return
	 */
	public String getFilter(){
		return filter;
	}
	/**
	 * ��ȡ����
	 * @return
	 */
	public String getName(){
		return name;
	}
	
	/**
	 *  ��ʽ: lua(json):json �Ǳ�׼��lua���������ع�����List�����ٶ�ȡsql���壬Ҳ���ٷ�����׼sql���, lua �ű�ά����ad_sql��
	 *  
	 * @return
	 */
	public String getLuaName(){
		return lua;
	}
	/**
	 * ��ʽ: select id from item_table where fk_id=$objectid and other condition,  ����ŵ���ad_sql.name, ������չ��������
 *             sql ��lua����ʱ��Ч��sqlһ�����壬����ʹ�ñ�׼sql ��䣬 sql �еĿ�ѡ���� $objectid, $uid, $comid, $stid, $empid
	 * @return ad_slq#name
	 */
	public String getAdSQLName(){
		return adsql;
	}
	/**
	 * ���ѡ��Ӱ�쵽item��ϸ�����ʾ���ֶ�
	 * 
	 * @return the assocType {@link RefByTable#ONE_TO_ONE} or {@link RefByTable#ONE_TO_MANY}
	 */
	public int getAssocType() {
		return assocType;
	}
	/**
	 * @return table
	 */
	public String getTable() {
		return table;
	}
	/**
	 * ���� {@link #getTable()} ���ϵ��ֶΣ�FK���ͣ�ָ������ID
	 * @return ָ������ID�ĵ�ǰ����ֶ� 
	 */
	public String getColumn() {
		return column;
	}

	
}
