package com.agilecontrol.phone.util;

import com.agilecontrol.nea.core.control.web.UserWebImpl;
import com.agilecontrol.nea.core.query.ColumnLink;
import com.agilecontrol.nea.core.query.Expression;
import com.agilecontrol.nea.core.query.QueryEngine;
import com.agilecontrol.nea.core.query.QueryUtils;
import com.agilecontrol.nea.core.schema.*;
import com.agilecontrol.nea.core.security.Directory;
import com.agilecontrol.nea.core.query.web.SubSystemView;
import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.nea.util.Validator;
import com.agilecontrol.phone.Admin;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;

import org.json.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * 用户对应的schema的所见范围，比如用户不可以看到某些按钮，或不可以获取某些字段等
 * init 2014.12.29
 * @author yfzhu
 *
 */
@Admin(mail="yfzhu@lifecycle.cn")
public class UserSchema {
	private static Logger logger=LoggerFactory.getLogger(UserSchema.class);
	
	private UserWebImpl userWeb;
	private TableManager manager;
	private QueryEngine engine;
	private Connection conn;
	private HttpServletRequest request;
	private Locale locale;
	/**
	 * 当前schema中最大到columnid
	 */
	private int maxColumnId;
	/**
	 * 获取指定的扩展属性，未指定则获取全部
	 * Added by zhangbh on 20151011
	 */
	private String propKey;
	
	public UserSchema(HttpServletRequest request,UserWebImpl userWeb,TableManager manager, QueryEngine engine,Connection conn) throws Exception{
		this.request= request;
		this.userWeb=userWeb;
		this.manager=manager;
		this.engine=engine;
		this.conn=conn;
		locale= userWeb.getLocale();
		maxColumnId= engine.doQueryInt("select max(id) from ad_column", null, conn) ;
		
	}
	/**
	 * 
	 * @param subSystemId
	 * @return 
	 * @throws Exception
	 */
	private JSONArray getTableCategories(SubSystemView ssv,int subSystemId) throws Exception{
		List tabcategorylist=ssv.getTableCategories(request,subSystemId);// elements can be TableCategory or WebAction
		if(tabcategorylist==null || tabcategorylist.size()==0){
			return null;
		}
		HashMap actionEnv=new HashMap();
		actionEnv.put("httpservletrequest", request);
		actionEnv.put("userweb", userWeb);
		
		JSONArray cates=new JSONArray();
		for(int i=0;i<tabcategorylist.size();i++){  
			List al= (List)tabcategorylist.get(i);
			
			
			if(al.get(0) instanceof TableCategory){
				
				TableCategory tablecategory=(TableCategory)al.get(0);
				List categoryChildren= (List) al.get(1);
				int tablecategoryId =tablecategory.getId();
				String url=tablecategory.getPageURL();
				String cdesc=tablecategory.getDescription(locale);
			    if(url!=null){
			    	//not support for json
			    	continue;
			    }
			    
			    JSONArray cat=new JSONArray();
			    for(int j=0;j<categoryChildren.size();j++){
			    	Object cc=categoryChildren.get(j);
			    	if( cc  instanceof Table){
						Table table=(Table)cc;
						int tableId =table.getId(); 
						String tdesc=table.getDescription(locale);
						
						if(!userWeb.isPermissionEnabled(table.getSecurityDirectory(), Directory.READ)) continue;
						
						JSONObject jo=new JSONObject();
						jo.put("text", tdesc);
						jo.put("icon", "table.gif");
						jo.put("table", table.getName());
						
						cat.put(jo);
						
			    	}else if(cc instanceof WebAction){
			    		WebAction action=(WebAction)cc;
			    		Object jo= action.toJSON(locale,actionEnv);
			    		if(jo!=null){
				    		if(jo instanceof JSONArray){
				    			JSONArray ja=(JSONArray)jo;
				    			for(int k=0;k< ja.length();k++){
				    				cat.put(ja.opt(k));
				    			}
				    		}else if(jo instanceof JSONObject){
				    			//just one obj
				    			cat.put(jo);
				    			
				    		}else{
				    			logger.warn("Unexpected web action json type:"+ jo.getClass().getName()+", action="+ action.getName()+":"+ jo);
				    			throw new NDSException("Unexpected web action json type:"+ jo.getClass().getName());
				    		}
			    		}
			    		
			    	}else{
			    		throw new NDSException("Unexpected category type:"+ cc.getClass().getName());
			    	}
			    }
			    
			    if(cat.length()>0 ){
			    	//has children
				    JSONObject node=new JSONObject();
				    node.put("text", cdesc);
				    node.put("icon", "folder.gif");
				    node.put("folder", cat);
				    
				    cates.put(node);
			    }
			}else if(al.get(0) instanceof WebAction){
				WebAction action= (WebAction)al.get(0);
				Object jo= action.toJSON(locale,actionEnv);
				if(jo==null) continue;
	    		if(jo instanceof JSONArray){
	    			JSONArray ja=(JSONArray)jo;
	    			for(int k=0;k< ja.length();k++){
	    				cates.put(ja.opt(k));
	    			}
	    		}else if(jo instanceof JSONObject){
	    			//just one obj
	    			cates.put(jo);
	    			
	    		}else{
	    			logger.warn("Unexpected web action json type, action="+ action.getName()+":"+ jo);
	    			throw new NDSException("Unexpected web action json type:"+ jo.getClass().getName());
	    		}
			}
		}
		return cates;
		
	}
	
	
	/**
	 * tree: [ {text:String, icon:String, table*:String(table.name), rpt*:id,script*:String, folder*:[]} , {}]
	 * @throws Exception
	 */
	public JSONArray getSubSystems() throws Exception{
		SubSystemView ssv=new SubSystemView();
		List<SubSystem> subsystems =ssv.getSubSystems(request);
		JSONArray ss=new JSONArray();
		SubSystem subSystem;
		Integer categoryId,subSystemId;
		String subSystemDesc;
		JSONObject jc;
		for (int i=0; i< subsystems.size(); i++){   
		     subSystem=(SubSystem)subsystems.get(i);        
		     subSystemId=subSystem.getId();
		     subSystemDesc=subSystem.getDescription(locale);
		     jc=new org.json.JSONObject();
			 jc.put("text", subSystemDesc);
			 jc.put("icon", subSystem.getIconURL());
			 
			 JSONArray tbcs= getTableCategories(ssv ,subSystemId);
//			 convertTableNameToId( tbcs);
			 
			 if(tbcs!=null && tbcs.length()>0){
				 jc.put("folder", tbcs);
				 
				 ss.put(jc);
			 }
		} 
		return ss;
	}
	
	/**
	 * 
	 * Original one replace table name to id for property: table
	 * @param data json array / json obj / plan object
	 * @return
	 * @throws Exception
	 */
	private void convertTableNameToId(Object data) throws Exception{
		if(data==null) return;
		if( data instanceof JSONObject ){
			Object table= ((JSONObject) data).opt("table");
			if(table!=null){
				if(table instanceof String){
					int tableId= manager.getTable((String)table).getId();
					 ((JSONObject) data).put("table", tableId);
				}
			}else{
				//has folder property?
				JSONArray folder=(JSONArray) ((JSONObject) data).opt("folder");
				if(folder!=null){
					 convertTableNameToId(folder);
				}
			}
		}else if(data instanceof JSONArray){
			JSONArray rows= (JSONArray) data;
			for(int i=0;i< rows.length();i++){
				convertTableNameToId(rows.get(i));
			}
		}
	}
	/**
	 * 根据ad_table的mobile属性定义以及一些系统规则来建立客户端的扩展属性
	 * @param table
	 * @return
	 * @throws Exception
	 */
	private JSONObject getTableProps(Table table) throws Exception{
		 Object mob = propKey == null ? table.getJSONProps() : table.getJSONProp(propKey);
		 JSONObject jo;
		 if(mob!=null && (mob instanceof JSONObject)){
			 jo=(JSONObject)mob;
		 }else{
			 jo=new JSONObject();
		 }
		 /**
		  * quickedit: true|false 控制内容：新增/编辑的时候，如果点击”保存“，是否立刻关闭当前单对象窗口，
		  * 从而返回列表界面，并刷新。主要应用场景：明细类的条目编辑，如巡店明细、订单明细等
		  */
		 if(!jo.has("quickedit")){
			 boolean quickEdit=table.getParentTable()!=null;
			 if(quickEdit){
				 jo.put("quickedit", true);//false will not go
			 }
		 }
		 return jo;
		 
	}
	
	/**
	 * Get table schema by table name
	 * Added by zhangbh on 20151011
	 */
	public JSONObject getTable(String tableName) throws Exception {
		String sql = "select id from ad_table where name = ?";
		Object tableId = engine.doQueryOne(sql, new Object[] {tableName}, conn);
		return tableId == null ? null : getTable(((BigDecimal) tableId).intValue());
	}
	
	public JSONObject getTable(int tableId) throws Exception{
		JSONObject jo=new JSONObject();
		Table table= manager.getTable(tableId);
		jo.put("id", table.getId());
		jo.put("name", table.getName());
		jo.put("description", table.getDescription(locale));
		jo.put("iconurl", table.getJSONProp("iconurl", "table.gif"));
		jo.put("rowurl", table.getJSONProp("rowurl"));
		jo.put("ak", table.getAlternateKey().getName());
		jo.put("dk", table.getDisplayKey()==null? table.getAlternateKey().getName(): table.getDisplayKey().getName());
		
		StringBuilder mask=new StringBuilder();
		
		
		int perm= userWeb.getPermission(table);
		if( (perm & Directory.READ) == Directory.READ){
			if(table.isActionEnabled(Table.QUERY))mask.append("Q");
		}
		if( (perm & Directory.WRITE) == Directory.WRITE){
			//进行新的明细权限的判定
			if(table.isActionEnabled(Table.ADD) && (perm & Directory.ADD) == Directory.ADD){
				mask.append("A");
			}
			if(table.isActionEnabled(Table.MODIFY) && (perm & Directory.MODIFY) == Directory.MODIFY){
				mask.append("M");
			}
			if(table.isActionEnabled(Table.DELETE) && (perm & Directory.DELETE) == Directory.DELETE){
				mask.append("D");
			}
			
			
		}
		
		if( (perm & Directory.SUBMIT) == Directory.SUBMIT){
			if(table.isActionEnabled(Table.SUBMIT))mask.append("S");
		}
		
		if( (perm & Directory.AUDIT) == Directory.AUDIT){
			if(table.isActionEnabled(Table.AUDIT))mask.append("U");
		}

		if( (perm & Directory.EXPORT) == Directory.EXPORT){
			if(table.isActionEnabled(Table.QUERY))mask.append("B"); //Batch
		}
		jo.put("mask", mask.toString());
		jo.put("isDropdown", table.isDropdown());
		
		TreeTableDefine ttd=table.getTreeDefine();
		if(ttd!=null){
			jo.put("tree", new JSONObject( ttd.toString()));
		}
		
		jo.put("isBig", table.isBig());
		jo.put("props", getTableProps(table)); //only for mobile props will download
		
		
		JSONArray columns= getColumns(table);
		//add refby table
		addRefByTablesAsColumns(table, columns);
		
		jo.put("columns", columns);
		
		
		
		
		JSONArray actionsList=new JSONArray();
		
		HashMap actionEnv=new HashMap();
		actionEnv.put("httpservletrequest", request);
		actionEnv.put("userweb", userWeb);
		actionEnv.put("connection", conn);
	  	List<WebAction> was=table.getWebActions(WebAction.DisplayTypeEnum.ListButton);
	  	for(int wasi=0;wasi<was.size();wasi++){
	  		WebAction wa=was.get(wasi);
	  		if(wa.canDisplay(actionEnv)){
	  			actionsList.put(wa.toJSON(locale, actionEnv));
	  		}
	  	}
	  	was=table.getWebActions(WebAction.DisplayTypeEnum.ListMenuItem);
	  	for(int wasi=0;wasi<was.size();wasi++){
	  		WebAction wa=was.get(wasi);
	  		if(wa.canDisplay(actionEnv)){
	  			actionsList.put(wa.toJSON(locale, actionEnv));
	  		}
	  	}
		jo.put("actions_list", actionsList ); //ListButton+ListMenuItem
		
		
		actionsList=new JSONArray();
		was=table.getWebActions(WebAction.DisplayTypeEnum.ObjButton);
	  	for(int wasi=0;wasi<was.size();wasi++){
	  		WebAction wa=was.get(wasi);
  			actionsList.put(wa.toJSON(locale, actionEnv)); //no permission considerasion, since object not specified yet!
	  	}
	  	was=table.getWebActions(WebAction.DisplayTypeEnum.ObjMenuItem);
	  	for(int wasi=0;wasi<was.size();wasi++){
	  		WebAction wa=was.get(wasi);
  			actionsList.put(wa.toJSON(locale, actionEnv));
	  	}
		jo.put("actions_obj", actionsList ); 
		
		return jo;
		
		
	}
	/**
	 * 添加refby table 作为column的特殊类型，按ios的要求构建

refbytable_column{
  	uiobj: 固定值 "refbytable", 这是关键标识
	id: integer (是系统最大的columnid+当前ad_refbytable.id，避免与现有column重复id）
	name: 关联表的英文代码
	description:中文描述，可用于显示 
  	objectManner: 固定值: "trigger"
  	refTable: 关联的表的名称name
  	mask:  固定值: 0010001001
  	filter: 空 "",
  	type: 固定值 "str",
  	length:固定值 20,
  	scale: 固定值 0,
  	isVirtual: 固定值: true
  	props: {assoctype:"1"|"n", expr: {Expression} 对象，放置了查询条件, 用于过滤明细纪录，其中有$objectid需要替换为当前对象 }
}
	 * @param table
	 * @param columns 将添加到里面
	 * @throws Exception
	 */
	private void addRefByTablesAsColumns(Table table, JSONArray columns) throws Exception{
		for(RefByTable rft: table.getRefByTables()){
			columns.put(toMetaJSONObject(rft));
		}
	}
	
	/**
     * For mobile client to read meta
     * 2014.12.29 init
     * @param locale
     * @return
     * @throws JSONException
     */
    public JSONObject toMetaJSONObject(RefByTable rft ) throws Exception{
		JSONObject jo=new JSONObject();
		
		Table refTable = manager.getTable(rft.getTableId());
		
		jo.put("id", rft.getId()+maxColumnId); //虚拟化
		jo.put("name",rft.getName());
		jo.put("description",rft.getDescription(locale));
		jo.put("obtainManner","trigger");
		jo.put("mask", "0010001001");
		jo.put("filter", "");
		jo.put("uiobj","refbytable");
		jo.put("refTable", refTable.getName());
		/*
		 * Expose refTable's AK and DK
		 * Added by zhangbh on 20151120
		 */
		if (refTable.getAlternateKey() != null) {
			jo.put("refTableAK", refTable.getAlternateKey().getName());
		}
		if (refTable.getDisplayKey() != null) {
			jo.put("refTableDK", refTable.getDisplayKey().getName());
		}
		
		jo.put("type", "str");
		jo.put("length",20);
		jo.put("scale",0);
		jo.put("isVirtual", true);
		
		JSONObject jsonProps=new JSONObject();
		Expression expr=new Expression(new ColumnLink(new int[]{rft.getRefByColumnId()}), "=$objectid");
		jsonProps.put("expr", expr.toJSON());
		jsonProps.put("assoctype",  rft.getAssociationType()==RefByTable.ONE_TO_ONE?"1":"n");
		
		jo.put("props", jsonProps);
		
		return jo;
	}
    
    private ArrayList<Column> getTableColumns(Table table){
    	ArrayList<Column> columns= table.getAllColumns();
    	ArrayList<Column> cls=new ArrayList();
    	int j;
    	Column lastColumn=null;
    	for(int i=0;i<columns.size();i++ ){
        	Column col=(Column)columns.get(i);
        	if(col.getSecurityGrade()> userWeb.getSecurityGrade()) continue;
        	int objType=col.getDisplaySetting().getObjectType();
        	if(objType==DisplaySetting.OBJ_BUTTON  || objType==DisplaySetting.OBJ_BLANK  ) continue;
        	cls.add(col);
        	lastColumn=col;
        	
        }
    	//去掉最后一个column
    	if(lastColumn.getDisplaySetting().getObjectType()==DisplaySetting.OBJ_HR){
    		cls.remove(cls.size()-1);
    	}
    	return cls; 
    }
	/**
	 * Column props
	 * @param table
	 * @return array of JSONObject
	 * @throws Exception
	 */
	private JSONArray getColumns(Table table) throws Exception{
		JSONArray rows=new JSONArray();
		ArrayList<Column> cols=getTableColumns(table);//table.getColumns(new int[]{}, true,  userWeb.getSecurityGrade());
		
		for(Column col: cols){
			JSONObject jo= ((ColumnImpl)col).toMetaJSONObject(locale);
			rows.put(jo);
			
			/*
			 * Expose refTable's AK and DK
			 * Added by zhangbh on 20151120
			 */
			Table refTable = col.getReferenceTable();
			if (refTable != null) {
				if (refTable.getAlternateKey() != null) {
					jo.put("refTableAK", refTable.getAlternateKey().getName());
				}
				if (refTable.getDisplayKey() != null) {
					jo.put("refTableDK", refTable.getDisplayKey().getName());
				}
			}
			
			//特殊处理gps字段
			Object gps =col.getJSONProp("gps");
			if(Boolean.TRUE.equals(gps)){
				jo.put("uiobj","gps");
			}
			// time 类型特别处理
			String fmt=(String) col.getJSONProp("fmt");
			if("HH:mm".equals(fmt)){
				jo.put("uiobj","time");
			}
			
			//planlocal
			Object planlocal =col.getJSONProp("planlocal");
			
			if(Boolean.TRUE.equals(planlocal)){
				jo.put("uiobj","planlocal");
			}
			
		}
		
		
		
		return rows;
	}
	/**
	 * key: table.id, value: table obj{id, name, description,iconurl, rowurl, ak,mask, isDropdown,tree:{}, isBig,props, columns:[], actions_list:[],actions_obj:[] }
	 * column:{id,name, description,isNullable,obtainManner,defaultValue,refTableId,isAlternateKey,mask,isValueLimited,values,isUpperCase,
	 * filter,displaySetting,type, length,scale,valueInterpreter, table,isVirtual,isIndexed,props}    
	 * }
	 * action:{id,name,description, iconurl}
	 * @return key: table.name, value table json object
	 * @throws Exception
	 */
	public JSONObject getTables() throws Exception{
		Collection<Table> tables=manager.getAllTables();
		JSONObject jo=new JSONObject();
		for(Table table: tables){
			if(userWeb.isPermissionEnabled(table.getSecurityDirectory(), Directory.READ)){
				jo.put(table.getName(), getTable(table.getId()));
			}
		}
		return jo;
	}
	
	/**
	 * LimitValue Group and their values
	 * @return { id: [[value,desc,css]]
	 * @throws Exception
	 */
	public JSONObject getValueGroups() throws Exception{
		JSONArray rows=engine.doQueryJSONArray("select g.name gname, v.value, v.description, v.cssclass from ad_limitvalue v, ad_limitvalue_group g where g.id=v.AD_LIMITVALUE_GROUP_ID and v.isactive='Y' and exists(select 1 from ad_column c, ad_table t where t.isactive='Y' and c.isactive='Y' and c.ad_table_id=t.id and c.AD_LIMITVALUE_GROUP_ID=g.id) ",
				null, conn);
		JSONObject groups=new JSONObject();
		for(int i=0;i< rows.length();i++){
			JSONArray row= rows.getJSONArray(i);
			String gname= row.getString(0);
			String value= row.getString(1);
			String desc= row.getString(2);
			String css= row.optString(3);
			
			JSONArray ja= groups.optJSONArray(gname);
			if(ja==null){
				ja=new JSONArray();
				groups.put(gname, ja);
			}
			JSONArray vv=new JSONArray();
			vv.put(value);
			vv.put(desc);
			vv.put(css);
			ja.put(vv);
		}
		return groups;
		
	}
	/**
	 * Get user attribute
	 * Added by wu.qiong on 20160128
	 */
	public JSONObject getAdUserAttr() throws Exception {
		JSONObject res = new JSONObject();
		
		JSONArray rows=engine.doQueryJSONArray("select ua.name,ua.value from AD_USER_ATTR ua where ua.isactive = 'Y'",
				null, conn);
		for(int i=0;i< rows.length();i++){
			
			JSONArray row= rows.getJSONArray(i);
			
			String attrName= row.getString(0);
			String value= row.getString(1);
			
			res.put(attrName, value);
		}
		return res;
	}
	/**
	 * Get valueGroup by group name
	 * Added by zhangbh on 20151011
	 */
	public JSONArray getValueGroup(String groupName) throws Exception {
		JSONArray rows=engine.doQueryJSONArray("select v.value, v.description, v.cssclass from ad_limitvalue v, ad_limitvalue_group g where g.id=v.AD_LIMITVALUE_GROUP_ID and g.name=? and v.isactive='Y' and exists(select 1 from ad_column c, ad_table t where t.isactive='Y' and c.isactive='Y' and c.ad_table_id=t.id and c.AD_LIMITVALUE_GROUP_ID=g.id) ",
				new Object[] {groupName}, conn);
		return rows;
	}
	
	public Long getModifiedDate() {
		return manager.getLoadTime();
	}
	
	public String getPropKey() {
		return propKey;
	}
	
	public void setPropKey(String propKey) {
		this.propKey = propKey;
	}
	
	public JSONObject toJSON() throws Exception{
		JSONObject jo=new JSONObject();
		jo.put("tables", getTables());
		jo.put("tree", getSubSystems());
		jo.put("valueGroups", getValueGroups());
		jo.put("modifiedDate", manager.getLoadTime());
		return jo;
	}
	
	
}