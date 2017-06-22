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
 *   ����ȡ�����ļ� ad_sql#table:<table>:meta ����ʽ
  Table
  {
  	name, dks:[column1, column2], perm, ac, am, bd, rac, ram, rbd 
  	cols:[{Column}],
  	refbys:[{RefByTable}]
  }
  name - string ����
  dk - ������string, �� [String],  ��ʾ��ǰ����Ϊ�����ʱ����Ҫ������ʾ���ֶ�
  perm - [String] Ȩ�ޣ���com�� ��ʾֻ��com�ڵ�Ա���ɼ���Ҫ��ǰuser��comid=��ǰ��¼��com_id�ֶ�, "admin"��ʾ��ǰ�û���Ҫ�ǹ���Ա���
  ac/am/bd ���ݿ�Ĵ洢�����������ݵĲ���Ϊ ac(objId, userId)
  rad/ram/rbd redis��lua�ű���, ���ݵĲ���Ϊ rad(objId, userId)
  
  Column
  {
  	name, fktable, type, edit,sgrade
  }
  name - string �ֶε����ݿ���
  fktable - String fk������ƣ���fktable is not null ���ֶζ��󻯣���ֱ�������ֶ�����Ӧ��������
  type - String "string","long", "time", "datenumber"
  edit - boolean �Ƿ������޸ģ�����insert/update, Ĭ��Ϊtrue
  nullable - boolean �Ƿ�����Ϊ�գ�Ĭ��true
  sgrade - ��ʾ��� 0 �����ͨ�û���1 ��Թ���Ա��Ĭ�� 0�� ������Ʒ��Ĳɹ��ۣ���ͨ�û�����ʾ
  
  RefByTable
  {
 ����˵����
 * table - ��ǰ�ӱ������
 * column - ��ǰ�ӱ�ָ�򸸱�ID���ֶ�
 * assoc - 1  1:1, 2 1:m
 * adsql - ��ʽ: select id from item_table where fk_id=$objectid and other condition,  ����ŵ���ad_sql.name, ������չ��������
              sql ��lua����ʱ��Ч��sqlһ�����壬����ʹ�ñ�׼sql ��䣬 sql �еĿ�ѡ���� $objectid, $uid, $comid, $stid, $empid
              
 * lua - ��ʽ: lua(json):json �Ǳ�׼��lua���������ع�����List�����ٶ�ȡsql���壬Ҳ���ٷ�����׼sql���
    
  }
  ������
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
	private JSONObject jsonProps;//��չ����
	private String ac,am,bd,rac,ram,rbd;
	private ArrayList<RefByTable> refbys;
	private ArrayList<TagColumn> tagColumns;
	/**
	 * props.ja=true���ֶζ��壬�������е�tagcolumn, all_pic �ֶΣ������ݶ���json array ����
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
	 * ColumnOfJSONArrayType, ���Ǳ�ǩ�ֶ�
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
	 * TagColumn, ���Ǳ�ǩ�ֶ�
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
	 * ������fk�����columns
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
	 * ��������༭���ֶ�
	 * @param sgrade �����û���Ȩ�ޣ�0 ��ʾ��ͨ��1��ʾ����Ա
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
	 * �ж�col�Ƿ��ڵ�ǰdks��
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
	 * ����json����ʱ������
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
			if(ja==null) throw new NDSException("��"+ table.name+"δ����cols");
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
	 * ��ȡָ�����ƶ�Ӧ��refbytable
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
	 * ��ȡ��ǰ���refbyTable
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
	 * ��ȡָ�����Ƶ�column
	 * @param colName
	 * @return
	 * @throws ObjectNotFoundException
	 */
	public Column getColumn(String colName) {
		for(Column col: columns){
			if(col.getName().equalsIgnoreCase(colName)) return col;
		}
		return null;//throw new ObjectNotFoundException("Columnδ�ҵ�:"+ colName);
	}
	/**
	 *  columns ����fkδָ��
	 * @throws Exception
	 */
	void finishParse(TableManager mgr) throws Exception{
		for(Column col: columns){
			col.finishParse(mgr);
		}
		for(RefByTable rbf: this.refbys) rbf.finishParse(mgr, this);
	}
	/**
	 * ʵ�ʵı���
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
	 * ��ȡָ�����ֶ�����Ӧ���ֶ�
	 * @param colNames ��Ӣ�Ķ��ŷָ����ֶ�����, ������д�����ֶ����������Զ�������
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
	 * ����������µ������ֶΣ��������ʾmask[1]=1 (start index=0)
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
	 * �б�����µ������ֶΣ��б�����ʾmask[0]=1 
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
		 // ��ͨ����
        public static String getName(int index) {
            for (Perm c : Perm.values()) {
                if (c.getIndex() == index) {
                    return c.name;
                }
            }
            return null;
        }

        // get set ����
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
	 * �ж����Ƿ�������ָ����Ȩ��Ҫ��
	 * @param perm
	 * @return true����ж�Ӧ�趨
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
