package com.agilecontrol.b2b.schema;

import java.sql.Connection;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Locale;
import java.util.Properties;

import org.json.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.agilecontrol.nea.core.query.QueryEngine;
import com.agilecontrol.nea.core.query.QueryUtils;
import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.nea.util.NDSRuntimeException;
import com.agilecontrol.nea.util.Tools;
import com.agilecontrol.nea.util.Validator;
import com.agilecontrol.phone.Admin;
import com.agilecontrol.phone.PhoneConfig;
import com.agilecontrol.phone.PhoneController;

/**
 * ����ȡ���������ļ� ad_sql#table:<table>:meta ���浽��ǰ������
 * ����ϵͳ���� phone.load_ad_table_meta=true, ������ȡad_sql:table:���������ݣ����Ǵ�core.schema.TableManager�ж�ȡ��ת��
 * ����ģʽ
 * @author yfzhu
 *
 */
@Admin(mail="yfzhu@lifecycle.cn")
public class TableManager {
    private static Logger logger=LoggerFactory.getLogger(TableManager.class);

    /**
     * This is public instance
     */
    private static TableManager instance=null;

    private boolean isInitialized=false;

    /**
     * key:tableName(String)
     * value:table(Table)
     */
    Hashtable<String, Table> tableNames;
    /**
     * key, "tablename"."columnname"
     */
    Hashtable<String, Column> colNames;
    /**
     * ���еı�
     * @return
     */
    public Collection<Table> getAllTables(){
    	return tableNames.values();
    }
    /**
     * ����ָ���ֶ�
     * @param tableName
     * @param colName
     * @return
     */
    public Column getColumn(String tableName, String colName){
    	if(!isInitialized) throw new NDSRuntimeException("����TableManager����");
    	return colNames.get((tableName+"."+colName).toLowerCase());
    }
    /**
     * ������ָ���ı�
     * @param tablename
     * @return
     */
    public Table getTable(String tablename){
    	if(!isInitialized) throw new NDSRuntimeException("����TableManager����");
    	if(Validator.isNull(tablename)) return null;
    	Table table= tableNames.get(tablename.toLowerCase());
    	if(table==null) throw new NDSRuntimeException("��"+ tablename+"δ����");
    	return table;
    }
    
    /**
     * @return true if the logging system has already been initialized.
     */
    public boolean isInitialized() {
        return this.isInitialized;
    }
    /**
     * ���fk���͵��ֶΣ���û�����úõ�
     * @throws Exception
     */
    private void reviseAll() throws Exception{
    	for(Table tbl: tableNames.values()){
    		tbl.finishParse(this);
    		for(Column col: tbl.getColumns()){
    			colNames.put(tbl.getName()+"."+col.getName(), col);
    		}
    	}
    }
    /**
     * ���¼��أ����ʧ�ܣ����ᵼ�µ�ǰinstance������
     * @throws Exception
     */
    public void reload() throws Exception{
    	TableManager tmp=new TableManager();
    	tmp.init();
    	this.tableNames=tmp.tableNames;
    	this.colNames=tmp.colNames;
    	this.isInitialized=true;
    	tmp=null;
    }
    
    /**
     * ����ad_table�������޸����ڸ���ad_sql#table:xxx:meta ��¼
     * @param tableName 
     * @param obj д�� ad_sql#table:xxx:meta.value
     * @param dbMeta ��ǰ��ad_sql:table:xxx:meta �� {value, md} ���Զ��󣬿���Ϊnull
     * @param modifiedDate ad_table ������޸�ʱ�䣬ad_sql��ʱ����������ʱ�򣬽������µ�
     * @throws Exception
     */
    private void syncToDb(String tableName, JSONObject obj,JSONObject dbMeta,  java.util.Date modifiedDate, Connection conn) throws Exception{
//    	logger.debug("-------------"+ tableName+"-------------");
//    	logger.debug(obj.toString());
    	if(dbMeta==null){
    		QueryEngine.getInstance().executeUpdate("insert into ad_sql(id,ad_client_id,name,value,description,creationdate,modifieddate)values(get_sequences('ad_sql'), ?,?,?,?,sysdate,sysdate) ", 
    				new Object[]{37, "table:"+ tableName+":meta",obj.toString(),obj.optString("note")}, conn);
    	}else{
    		String md= dbMeta.getString("md");//format YYYY-MM-DD HH24:MI:SS
    		if(QueryUtils.dateTimeSecondDashFormatter.get().parse(md).before(modifiedDate)){
    			QueryEngine.getInstance().executeUpdate("update ad_sql set value=?,modifieddate=sysdate where name=?",
    					new Object[]{obj.toString(), "table:"+ tableName+":meta",obj.toString()}, conn);
    		}
    	}
    	
    }
    /**
     * Initialize the table system. Need to be called once before calling
     * <code>getTable()</code>.
     */
    public synchronized void  init() {
    	if(isInitialized) {
    		logger.debug("b2b.TableManager already initialized, hashcode="+ this.hashCode());
    		return;
    	}else{
    		logger.debug("b2b.TableManager is initializing, hashcode="+ this.hashCode());
    	}
    	Connection conn=null;
    	String tbname=null;
    	StringBuilder sb=new StringBuilder();
    	boolean errorFound=true;
        try{
        	isInitialized=false;
        	tableNames=new Hashtable();
        	colNames=new Hashtable();

        	conn=QueryEngine.getInstance().getConnection();
        	QueryEngine engine=QueryEngine.getInstance();

        	if(PhoneConfig.LOAD_AD_TABLE_META){
        		if(true) throw new NDSException("�Ѿ�����,��ͨ��cmd:b2b.meta����ȡ");
        		// ֱ�Ӵ�ad_table����ת��
        		logger.debug("Loading b2b.TableManager from core.schema.TableManager");
        		HashMap<String, JSONObject> dbMeta=new HashMap();
        		JSONArray rows=engine.doQueryObjectArray("select name,value,to_char(modifieddate,'YYYY-MM-DD HH24:MI:SS') md from ad_sql where name like 'table:%:meta'", null, conn);
        		for(int i=0;i<rows.length();i++){
        			JSONObject row=rows.getJSONObject(i);
        			dbMeta.put(row.getString("name"), row);
        		}
        		for(com.agilecontrol.nea.core.schema.Table table: com.agilecontrol.nea.core.schema.TableManager.getInstance().getAllTables()){
        			JSONObject tbJSONObj=( (com.agilecontrol.nea.core.schema.TableImpl)table).toMaiJiaObject();
        			//sync to db
        			String tname= tbJSONObj.getString("name");
        			syncToDb(tname.toLowerCase(),tbJSONObj, dbMeta.get("table:"+tname+":meta"), table.getModifiedDate(), conn);
        			Table tbl=Table.parse(tbJSONObj);
        			tableNames.put(tname.toLowerCase(), tbl);
        		}
        	}else{
	        	
	        	JSONArray rows=engine.doQueryJSONArray("select name,value from ad_sql where name like 'table:%:meta' and isactive='Y'", null, conn);
	        	for(int i=0;i<rows.length();i++){
	        		
	        		try{
	            		JSONArray row=rows.getJSONArray(i);
	            		tbname=row.getString(0);
	            		String tb=row.getString(1);
	            		if(Validator.isNull(tb))continue;
	
	            		JSONObject table=new JSONObject(tb);
	        			Table tbl=Table.parse(table);
	       		
	        			tableNames.put(tbl.getName(), tbl);
	        		}catch(Throwable ex){
	        			logger.error("Fail to init "+ tbname, ex);
	        			sb.append("��"+ tbname+"���ô���:"+ ex.getLocalizedMessage()+"; ");
	        		}
	        	}
        	}
            // this check will not prohibit tables from loading into memory
       		reviseAll();
       		errorFound=false;
        }catch(Throwable tx){
        	logger.error("Fail to init table manager", tx);
        	throw new NDSRuntimeException("����TableManager����:"+ tx.getLocalizedMessage());
        }finally{
        	try{if(conn!=null)conn.close();}catch(Throwable tx){}
        }
        if(errorFound){
        	throw new NDSRuntimeException(sb.toString());
        }else
       		isInitialized=true;

    }
    
	public static TableManager getInstance(){
		 if( instance ==null ) {
	            instance= new TableManager();
	            instance.init();
	     }
	     return instance;
	}
}
