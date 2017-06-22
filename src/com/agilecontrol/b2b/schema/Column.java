package com.agilecontrol.b2b.schema;

import java.util.*;

import org.json.*;

import com.agilecontrol.b2b.schema.Table.Perm;
import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.nea.util.NDSRuntimeException;
import com.agilecontrol.nea.util.ObjectNotFoundException;
import com.agilecontrol.nea.util.Validator;
/**
 * Column
  {
  	name, fktable, type, edit, masks
  }
  name - string 字段的数据库名
  fktable - String fk表的名称，将fktable is not null 的字段对象化，并直接塞到字段名对应的属性中
  type - String "string","number", "time", "datenumber"
  edit - boolean 是否允许修改，用于insert/update, 默认为true
  null - boolean 是否允许为空，默认true
  mask - [boolean] 1 表示是显示，0 表示不显示，idx=0 对应列表，idx=1对应单对象，默认列表仅有id,dks，所有字段在单对象上都有
  dfv - string , default value 里面存储的变量目前限定为$uid, $comid, $stid, $empid, $docno
 * @author yfzhu
 *
 */
public class Column implements org.json.JSONString{
	
	private String name;
	private String realName;
	private String note;
	private Table fkTable;
	private Column.Type type;
	private boolean isEdit;
	private boolean isNull;
	private boolean[] mask;// 0 - list, 1 - obj 是否需要显示
	private byte sgrade; // 0 - 所有用户可见, 1 管理员可见
	private JSONObject jsonProps;
	private Table table;
	private String defaultValue;
	/**
	 * 解析fktable
	 */
	private String tmp;
	
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
		jo.putOpt("realname", realName);
		jo.putOpt("note", note);
		jo.putOpt("edit", isEdit);
		jo.putOpt("null", isNull);
		jo.putOpt("type", type.name);
		jo.putOpt("dfv", defaultValue);
		jo.putOpt("props", jsonProps);
		String mk= (mask[0]?"1":"0") +( mask[1]?"1":"0");
		jo.putOpt("mask", mk);
		jo.putOpt("sgrade", sgrade);
		if(fkTable!=null)jo.putOpt("fktable", fkTable.getName());
		
		
		return jo;
	}
	/**
	 * 解析
	 * @param jo
	 * @return
	 * @throws Exception
	 */
	static Column parse(JSONObject jo, Table table) throws Exception{
		
		Column col;
		JSONObject pros=jo.optJSONObject("props");
		if(pros!=null && pros.has("tagtable")){
			TagColumn tcol=new TagColumn();
			col=tcol;
			JSONObject tagProps=pros.getJSONObject("tagtable");
			tcol.setTagTable(tagProps.getString("tb"));
			tcol.setTagColumn(tagProps.getString("tag_column"));
			tcol.setTagMainColumn(tagProps.getString("main_column"));
			tcol.setTagStoreTable(tagProps.getString("storetb"));	
			pros.put("ja", true);// 强制设置为jsonarry类型的存储格式定义，见 #Table.getColumnsOfJSONArrayType
		}else{
			col=new Column();
		}
		col.name=SchemaUtils.getString(jo,"name").toLowerCase();
		col.realName=SchemaUtils.getString(jo,"realname").toLowerCase();
		col.isEdit=jo.optBoolean("edit",true);
		col.isNull=jo.optBoolean("null",true);
		col.note=jo.optString("note");
		col.type=Type.parse(SchemaUtils.getString(jo,"type"));
		col.tmp=jo.optString("fktable");
		col.defaultValue=jo.optString("dfv");
		col.jsonProps=pros;// jo.optJSONObject("props");
		String mks=jo.optString("mask");
		if(Validator.isNull(mks)) col.mask=new boolean[]{true,true};
		else{
			mks=mks.trim();
			col.mask=new boolean[2];
			col.mask[0]= "1".equals(mks.substring(0, 1));
			col.mask[1]=mks.length()>1?  "1".equals(mks.substring(1, 2)):false;
		}
		col.sgrade=(byte)jo.optInt("sgrade", 0);
		col.table=table;
		return col;
	}
	/**
	 * 确认mask[idx]是否=true
	 * @param idx ， 从0开始，不能大于1
	 * @return
	 */
	public boolean isMaskSet(int idx){
		if(idx<0 || idx>1) throw new NDSRuntimeException("下标越界:"+idx+"(only 0,1)");
		return mask[idx];
	}
	/**
	 * 默认值，一般出现在edit=false的字段上，用于从后台强制赋值给字段
	 * @return
	 */
	public String getDefaultValue(){
		return defaultValue;
	}
	/**
	 * 安全等级， 0 表示所有用户可见， 1表示仅管理员可见
	 * @return
	 */
	public byte getSgrade(){
		return sgrade;
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
    
    public String toString(){
		return  table.getName()+"."+name;
	}
	/**
	 * 所在表
	 * @return
	 */
	public Table getTable(){
		return table;
	}
	/**
	 *  columns 还有fk未指向
	 * @throws Exception
	 */
	void finishParse(TableManager mgr) throws Exception{
		if(Validator.isNotNull(tmp)){
			fkTable=mgr.tableNames.get(tmp);
			if(fkTable==null) throw new NDSException("未找到字段"+table.getName()+"."+ name+"的FK表"+ tmp);
		}
		if(this.mask==null){
			if("id".equalsIgnoreCase(name)) mask=new boolean[]{true,true};
			else if(table.isDisplayKey(this)) mask=new boolean[]{true,true};
			else mask=new boolean[]{false,true};
		}
		tmp=null;
	}
	
	public boolean equals(Object obj){
		if ((obj instanceof Column ) && ((Column)obj).getName().equalsIgnoreCase(name) && ((Column)obj).getTable().equals(table) )
			return true;
		return false;
	}
	
	public static enum Type{
		STRING("string",0), NUMBER("number",1), DATE("date",2), DATENUMBER("datenumber",3), CLOB("clob",4);
		private String name;
		private int index;
		private Type(String t, int ord){
			this.name=t;
			this.index=ord;
		}
		
		public static Type parse(String n) throws ObjectNotFoundException{
            if("number".equalsIgnoreCase(n)) return NUMBER;
            else if("string".equalsIgnoreCase(n) || "varchar".equalsIgnoreCase(n)) return STRING;
            else if("date".equalsIgnoreCase(n)) return DATE;
            else if("datenumber".equalsIgnoreCase(n)) return DATENUMBER;
            else if("clob".equalsIgnoreCase(n)) return CLOB;
            throw new ObjectNotFoundException("type:"+n);
			
		}
		 // 普通方法
        public static String getName(int index) {
            for (Type c : Type.values()) {
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
        
        
    
	}



	/**
	 * @return the note
	 */
	public String getNote() {
		return note;
	}

	/**
	 * 实际的字段名
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
	 * @return the fkTable
	 */
	public Table getFkTable() {
		return fkTable;
	}




	/**
	 * @param fkTable the fkTable to set
	 */
	public void setFkTable(Table fkTable) {
		this.fkTable = fkTable;
	}




	/**
	 * @return the type
	 */
	public Column.Type getType() {
		return type;
	}




	/**
	 * @param type the type to set
	 */
	public void setType(Column.Type type) {
		this.type = type;
	}




	/**
	 * @return the isEdit
	 */
	public boolean isEdit() {
		return isEdit;
	}




	/**
	 * @param isEdit the isEdit to set
	 */
	public void setEdit(boolean isEdit) {
		this.isEdit = isEdit;
	}




	/**
	 * @return the isNull
	 */
	public boolean isNull() {
		return isNull;
	}




	/**
	 * @param isNull the isNull to set
	 */
	public void setNull(boolean isNull) {
		this.isNull = isNull;
	}
}
