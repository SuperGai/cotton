package com.agilecontrol.b2b.schema;

import java.util.*;

import org.json.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.agilecontrol.b2b.schema.Column.Type;
import com.agilecontrol.nea.util.JSONUtils;
import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.nea.util.NDSRuntimeException;
import com.agilecontrol.nea.util.ObjectNotFoundException;

/**
 *   将读取配置文件 ad_sql#table:<table>:meta ，格式
  Table
  {
  	name, dks:[column1, column2], perm, ac, am, bd, rac, ram, rbd 
  	cols:[{Column}],
  	refbys:[{RefByTable}]
  }
  name - string 表名
  dk - 可以是string, 或 [String],  表示当前表作为外键的时候，需要连带显示的字段
  perm - [String] 权限，“com” 表示只有com内的员工可见，要求当前user的comid=当前记录的com_id字段, "admin"表示当前用户需要是管理员身份
  ac/am/bd 数据库的存储过程名，传递的参数为 ac(objId, userId)
  rad/ram/rbd redis的lua脚本名, 传递的参数为 rad(objId, userId)
  
  Column
  {
  	name, fktable, type, edit,sgrade
  }
  name - string 字段的数据库名
  fktable - String fk表的名称，将fktable is not null 的字段对象化，并直接塞到字段名对应的属性中
  type - String "string","long", "time", "datenumber"
  edit - boolean 是否允许修改，用于insert/update, 默认为true
  nullable - boolean 是否允许为空，默认true
  sgrade - 显示与否 0 针对普通用户，1 针对管理员，默认 0。 对于商品表的采购价，普通用户不显示
  
  RefByTable
  {
 属性说明：
 * table - 当前子表的名称
 * column - 当前子表指向父表ID的字段
 * assoc - 1  1:1, 2 1:m
 * adsql - 形式: select id from item_table where fk_id=$objectid and other condition,  这里放的是ad_sql.name, 用于扩展过滤条件
              sql 在lua定义时无效，sql一旦定义，不再使用标准sql 语句， sql 中的可选参数 $objectid, $uid, $comid, $stid, $empid
              
 * lua - 形式: lua(json):json 是标准的lua程序，用于重构整个List，不再读取sql定义，也不再发出标准sql语句
    
  }
  举例：
 spo:{... refby: [ {table:"spo_item", column:"spo_id", assoc:2, sql:"refby:spo_item:list", lua:"lua:spo_item:list"} ] }


 * @author yfzhu
 *
 */
public class Table implements org.json.JSONString {
	private static Logger logger=LoggerFactory.getLogger(Table.class);
	private String name;
	private String realName;
	private String note;
	private ArrayList<Column> dks;
	private ArrayList<Perm> perms;
	private ArrayList<Column> columns;
	private JSONObject jsonProps;//扩展属性
	private String ac,am,bd,rac,ram,rbd;
	private ArrayList<RefByTable> refbys;
	private ArrayList<TagColumn> tagColumns;
	/**
	 * props.ja=true的字段定义，比如所有的tagcolumn, all_pic 字段，其内容都是json array 类型
	 */
	private ArrayList<Column> jaColumns;
	
	public String toString(){
		return name;
	}
	
	/**
	 * @return the note
	 */
	public String getNote() {
		return note;
	}
	
	public JSONObject toJSONObject()throws Exception{
		JSONObject jo=new JSONObject();
		jo.put("name", name);
		jo.put("realname", this.realName);
		jo.put("note", note);
		jo.putOpt("dks", JSONUtils.toJSONArray(dks));
		jo.putOpt("perm", JSONUtils.toJSONArray(perms));
		jo.putOpt("cols", JSONUtils.toJSONArray(columns));
		jo.putOpt("refbys", JSONUtils.toJSONArray(refbys));
		
		jo.putOpt("props", jsonProps);
		jo.putOpt("ac", ac);
		jo.putOpt("am", am);
		jo.putOpt("bd", bd);
		jo.putOpt("rac", rac);
		jo.putOpt("ram", ram);
		jo.putOpt("rbd", rbd);
		
		return jo;
	}
	/**
	 * ColumnOfJSONArrayType, 就是标签字段
	 * @return
	 */
	public ArrayList<Column> getColumnsOfJSONType(){
		if(jaColumns==null){
			ArrayList<Column> cls=new ArrayList();
			for(Column col:columns){
				if(Boolean.TRUE.equals( col.getJSONProp("ja", false)) || Boolean.TRUE.equals( col.getJSONProp("jo", false))) cls.add(col);
			}
			jaColumns=cls;
		}
		return jaColumns;
	}
	/**
	 * TagColumn, 就是标签字段
	 * @return
	 */
	public ArrayList<TagColumn> getTagColumns(){
		if(tagColumns==null){
			ArrayList<TagColumn> cls=new ArrayList();
			for(Column col:columns){
				if(col instanceof TagColumn) cls.add((TagColumn)col);
			}
			tagColumns=cls;
		}
		return tagColumns;
	}
	/**
	 * 所有是fk外键的columns
	 * @return never null
	 */
	public ArrayList<Column> getFKColumns(){
		ArrayList<Column> cs=new ArrayList();
		for(Column c: columns){
			if(c.getFkTable()!=null){
				cs.add(c);
			}
		}
		return cs;
	}
	/**
	 * 所有允许编辑的字段
	 * @param sgrade 这是用户的权限，0 表示普通，1表示管理员
	 * @return 
	 */
	public ArrayList<Column> getEditColumns(int sgrade){
		ArrayList<Column> cs=new ArrayList();
		for(Column c: columns){
			if(c.isEdit() && sgrade>=c.getSgrade() ){
				cs.add(c);
			}
		}
		return cs;
	}
	/**
	 * 判定col是否在当前dks中
	 * @param col
	 * @return
	 */
	boolean isDisplayKey(Column col){
		for(Column c: dks){
			if(col.equals(c)) return true;
		}
		return false;
	}
	
	public boolean equals(Object obj){
		if(obj instanceof Table){
			Table t=(Table)obj;
			if(t.getName().equalsIgnoreCase(this.getName())) return true;
		}
		return false;
	}
	/**
	 * 解析json，暂时不生成
	 * @param jo
	 * @return
	 * @throws Exception
	 */
	static Table parse(JSONObject jo) throws Exception{
		Table table=new Table();
		table.name=SchemaUtils.getString(jo,"name").toLowerCase();
		table.realName=SchemaUtils.getString(jo,"realname").toLowerCase();
		table.note=jo.optString("note",table.name);
		table.ac=jo.optString("ac");
		table.am=jo.optString("am");
		table.bd=jo.optString("bd");
		table.rac=jo.optString("rac");
		table.ram=jo.optString("ram");
		table.rbd=jo.optString("rbd");
		
		table.perms=new ArrayList<Perm>();
		table.columns=new ArrayList<Column>();
		table.dks=new ArrayList();
		table.jsonProps= jo.optJSONObject("props");
		Object pm=jo.opt("perm");
		if(pm !=null){
			
			if(pm instanceof JSONArray){
				JSONArray ja=(JSONArray)pm;
				for(int i=0;i<ja.length();i++){
					Perm p=Perm.parse(ja.getString(i));
					table.perms.add(p);
				}
			}else if(pm instanceof String){
				String[] ps=((String)pm).split(",");
				for(String pstr:ps){
					Perm p=Perm.parse(pstr);
					table.perms.add(p);
				}
				
			}else throw new NDSException("perm "+ pm +" not valid(table="+ table.name+")");
		}
		
		{
			JSONArray ja=jo.optJSONArray("cols");
			if(ja==null) throw new NDSException("表"+ table.name+"未定义cols");
			for(int i=0;i<ja.length();i++){
				JSONObject col=ja.getJSONObject(i);
				Column column=Column.parse(col, table);
				table.columns.add(column);
			}
		}
		
		
		Object tmpdks=jo.opt("dks");
		if(tmpdks !=null){
			
			if(tmpdks instanceof JSONArray){
				JSONArray ja=(JSONArray)tmpdks;
				for(int i=0;i<ja.length();i++){
					String c=ja.getString(i);
					Column col=table.getColumn(c);
					table.dks.add(col);
				}
			}else if(tmpdks instanceof String){
				Column col=table.getColumn((String)tmpdks);
				table.dks.add(col);
				
			}else throw new NDSException("perm "+ pm +" not valid(table="+ table.name);
		}else
			throw new NDSException("dks not set(table="+ table.name);
		
		
		table.refbys= new ArrayList();
		
		JSONArray trfs=jo.optJSONArray("refbys");
		if(trfs!=null)for(int i=0;i<trfs.length();i++){
			JSONObject trf=trfs.getJSONObject(i);
			table.refbys.add(RefByTable.parse(trf, table));
		}
		
		return table;
		
	}
	/**
	 * 获取指定名称对应的refbytable
	 * @param name
	 * @return null if not found
	 */
	public RefByTable getRefByTable(String name){
		for(RefByTable rbf: this.refbys){
			if(rbf.getName().equalsIgnoreCase(name)) return rbf;
		}
		return null;
	}
	/**
	 * 获取当前表的refbyTable
	 * @return
	 */
	public ArrayList<RefByTable> getRefByTables(){
		return this.refbys;
	}
	/**
	 * db procedure After Create
	 * @return
	 */
	public String getProcAC(){
		return ac;
	}
	/**
	 * db procedure After Modify
	 * @return
	 */
	public String getProcAM(){
		return am;
	}
	/**
	 * db procedure Before Delete
	 * @return
	 */
	public String getProcBD(){
		return bd;
	}
	/**
	 * Redis After Create
	 * @return
	 */
	public String getRedisAC(){
		return rac;
	}
	/**
	 * Redis After Modify
	 * @return
	 */
	public String getRedisAM(){
		return ram;
	}
	/**
	 * Redis Before Delete
	 * @return
	 */
	public String getRedisBD(){
		return rbd;
	}
	
	/**
     * Set in ad_column.props as json object
     * @return null or a valid object
     */
    public JSONObject getJSONProps(){
    	return jsonProps;
    }
    /**
     * Get property by name in getJSONProps
     * @param name
     * @return null or a valid object
     */
    public Object getJSONProp(String name){
    	return jsonProps==null? null: jsonProps.opt(name);
    }
    /**
     * if getJSONProp is null, return defaultValue
     */
    public Object getJSONProp(String name, Object defaultValue){
    	Object obj=(jsonProps==null? null: jsonProps.opt(name));
    	if(obj==null) return defaultValue;
    	
    	return obj;
    }
    public void setJSONProps(JSONObject jo){
    	this.jsonProps= jo;
    }
	/**
	 * 获取指定名称的column
	 * @param colName
	 * @return
	 * @throws ObjectNotFoundException
	 */
	public Column getColumn(String colName) {
		for(Column col: columns){
			if(col.getName().equalsIgnoreCase(colName)) return col;
		}
		return null;//throw new ObjectNotFoundException("Column未找到:"+ colName);
	}
	/**
	 *  columns 还有fk未指向
	 * @throws Exception
	 */
	void finishParse(TableManager mgr) throws Exception{
		for(Column col: columns){
			col.finishParse(mgr);
		}
		for(RefByTable rbf: this.refbys) rbf.finishParse(mgr, this);
	}
	/**
	 * 实际的表名
	 * @return
	 */
	public String getRealName(){
		return this.realName;
	}
	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}
	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}
	/**
	 * @return the dks
	 */
	public ArrayList<Column> getDisplayKeys() {
		return dks;
	}
	/**
	 * @param dks the dks to set
	 */
	public void setDisplayKeys(ArrayList<Column> dks) {
		this.dks = dks;
	}
	
	/**
	 * @return the columns
	 */
	public ArrayList<Column> getColumns() {
		return columns;
	}
	/**
	 * 获取指定的字段名对应的字段
	 * @param colNames 用英文逗号分隔的字段名称, 如果含有错误的字段名，将忽略而不报错
	 * @return
	 */
	public ArrayList<Column> getColumns(String colNames) {
		ArrayList<Column> cols=new ArrayList();
		for(String n: colNames.split(",")){
			Column col=getColumn(n);
			if(col!=null)cols.add(col);
		}
		return cols;
	}
	
	private ArrayList<Column> columnsInObjectView=null, columnsInListView=null;
	/**
	 * 单对象界面下的所有字段，单对象表示mask[1]=1 (start index=0)
	 * @return the columns
	 */
	public ArrayList<Column> getColumnsInObjectView() {
		if(columnsInObjectView==null){
			ArrayList<Column> cols=new ArrayList();
			for(Column col:columns){
				if(col.isMaskSet(1)) cols.add(col);
			}
			columnsInObjectView=cols;
		}
		return columnsInObjectView;
	}
	/**
	 * 列表界面下的所有字段，列表界面表示mask[0]=1 
	 * @return the columns
	 */
	public ArrayList<Column> getColumnsInListView() {
		if(columnsInListView==null){
			ArrayList<Column> cols=new ArrayList();
			for(Column col:columns){
				if(col.isMaskSet(0)) cols.add(col);
			}
			columnsInListView= cols;
		}
		return columnsInListView;
	}
	/**
	 * @param columns the columns to set
	 */
	public void setColumns(ArrayList<Column> columns) {
		this.columns = columns;
	}
	public static enum Perm{
		COM("com",0), ADMIN("admin",1), USER("user",2);
		private String name;
		private int index;
		private Perm(String t, int ord){
			this.name=t;
			this.index=ord;
		}
		
		public static Perm parse(String n) throws ObjectNotFoundException{
            for (Perm c : Perm.values()) {
                if (c.getName().equals(n)) {
                    return c;
                }
            }
            throw new ObjectNotFoundException("perm:"+n);
			
		}
		 // 普通方法
        public static String getName(int index) {
            for (Perm c : Perm.values()) {
                if (c.getIndex() == index) {
                    return c.name;
                }
            }
            return null;
        }

        // get set 方法
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getIndex() {
            return index;
        }

        public void setIndex(int index) {
            this.index = index;
        }
        
//        public boolean equals(Object p){
//        	return p!=null && (p instanceof Perm) && ((Perm)p).index=this.index;
//        }
    
	}
	/**
	 * 判定表是否设置了指定的权限要求
	 * @param perm
	 * @return true如果有对应设定
	 */
	public boolean hasPermSet(Perm perm){
		return perms.contains(perm);
	}
	/**
	 * @return the perms
	 */
	public ArrayList<Perm> getPerms() {
		return perms;
	}
	/**
	 * @param perms the perms to set
	 */
	public void setPerms(ArrayList<Perm> perms) {
		this.perms = perms;
	}

	@Override
	public String toJSONString() {
		try{
			return toJSONObject().toString();
		}catch(Throwable t){
			throw new NDSRuntimeException("Fail to to json:"+name+":"+ t.getMessage() );
		}
		
	}	
	
}
