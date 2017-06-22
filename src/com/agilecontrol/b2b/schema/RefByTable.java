package com.agilecontrol.b2b.schema;
import org.json.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.nea.util.NDSRuntimeException;
import com.agilecontrol.nea.util.Validator;

/**
 * 主表的关联表，比如订单明细，物流明细，支付记录等
 * 
 * 支持1对多，也支持1对1，一般1对多显示列表字段，一对多显示单对象字段
 * 
 * 关联关系都是通过master table的字段，子表的fk字段来对应，正常拼接的sql语句格式
 * select id from table where fkcolumnid=maintable.id
 * table:{id,name,cols, refby:[{table:"itemtable", column:"fkcolumn", assoc:1,filter:"itemtable.othercolumn=some"}]}
 * 
 * 例如：
 * spo:{... refbys: [ {table:"spo_item", column:"spo_id", assoc:2, sql:"refby:spo_item:list", lua:"lua:spo_item:list"} ] }
 * 
 * 属性说明：
 * name - 子表的别名，便于在获取对象的时候指定，如果不设定，默认就用table.name+"_s"作为别名
 * table - 当前子表的名称
 * column - 当前子表指向父表ID的字段
 * assoc - 1  1:1, 2 1:m
 * adsql - 形式: select id from item_table where fk_id=$objectid and other condition,  这里放的是ad_sql.name, 用于扩展过滤条件
 *             sql 在lua定义时无效，sql一旦定义，不再使用标准sql 语句， sql 中的可选参数 $objectid, $uid, $comid, $stid, $empid
 *             
 * lua - 形式: lua(json):json 是标准的lua程序，用于重构整个List，不再读取sql定义，也不再发出标准sql语句
 * filter - 针对itemtable的额外过滤条件
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
	 * 用于明细表数据获取的sql语句对应的ad_sql的name
	 */
	private String adsql;
	/**
	 * 用于重构整个List的ad_sql的name
	 */
	private String lua;
	/**
	 * 过滤条件，形式: itemtable.othercolumn=xxx, 将结合到当前明细表的筛选语句中，正常的筛选语句:
	 * itemtable.column=maintable.id 
	 * 补充后的显示为: itemtable.column=maintable.id and itemtable.othercolumn=xxx
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
		if(rbf.assocType !=1 && rbf.assocType!=2) throw new NDSException(masterTable.getName()+"的reftable "+ rbf.table+"#assoc定义有问题:"+ rbf.assocType+" 无效！");
		
		rbf.adsql=ref.optString("adsql");
		rbf.lua=ref.optString("lua");
		rbf.filter=ref.optString("filter");
		
		if(Validator.isNull(rbf.name)) rbf.name=rbf.table+"_s";//name rule
		
		if(masterTable.getColumn(rbf.name)!=null){
			throw new NDSException("请为"+ masterTable.getName()+"的子表"+ rbf.name+"重新命名name，与字段重名");
		}
		return rbf;
	}
	/**
	 * 将校验table,column都存在
	 * @param mgr
	 * @throws Exception
	 */
	void finishParse(TableManager mgr,  Table masterTable) throws Exception{
		Table tb=mgr.tableNames.get(this.table);
		if(tb==null) throw new NDSException("请检查"+ masterTable.getName()+"的子表"+ name+",定义的table未找到("+ this.getTable()+")");
		if(tb.getColumn(this.column)==null)
			throw new NDSException("请检查"+ masterTable.getName()+"的子表"+ name+",定义的column未在表("+ this.getTable()+")上找到");
	}
	/**
	过滤条件，形式: itemtable.othercolumn=xxx, 将结合到当前明细表的筛选语句中，正常的筛选语句:
	 * itemtable.column=maintable.id 
	 * 补充后的显示为: itemtable.column=maintable.id and itemtable.othercolumn=xxx
	 * @return
	 */
	public String getFilter(){
		return filter;
	}
	/**
	 * 获取名称
	 * @return
	 */
	public String getName(){
		return name;
	}
	
	/**
	 *  形式: lua(json):json 是标准的lua程序，用于重构整个List，不再读取sql定义，也不再发出标准sql语句, lua 脚本维护在ad_sql中
	 *  
	 * @return
	 */
	public String getLuaName(){
		return lua;
	}
	/**
	 * 形式: select id from item_table where fk_id=$objectid and other condition,  这里放的是ad_sql.name, 用于扩展过滤条件
 *             sql 在lua定义时无效，sql一旦定义，不再使用标准sql 语句， sql 中的可选参数 $objectid, $uid, $comid, $stid, $empid
	 * @return ad_slq#name
	 */
	public String getAdSQLName(){
		return adsql;
	}
	/**
	 * 这个选项影响到item明细表的显示的字段
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
	 * 是在 {@link #getTable()} 表上的字段，FK类型，指向主表ID
	 * @return 指向主表ID的当前表的字段 
	 */
	public String getColumn() {
		return column;
	}

	
}
