package com.agilecontrol.phone.impl.buding;

import java.io.StringWriter;
import java.sql.Connection;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.*;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.compass.core.util.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.agilecontrol.nea.core.query.JSONResultSet;
import com.agilecontrol.nea.core.query.QueryEngine;
import com.agilecontrol.nea.core.schema.TableManager;
import com.agilecontrol.nea.core.util.ConfigValues;
import com.agilecontrol.nea.core.util.MessagesHolder;
import com.agilecontrol.nea.core.velocity.VelocityUtils;
import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.nea.util.NDSRuntimeException;
import com.agilecontrol.nea.util.ObjectQueue;
import com.agilecontrol.nea.util.Validator;
import com.agilecontrol.phone.*;
import com.agilecontrol.phone.PhoneController.SQLWithParams;

/**
 * 
 sqlite ���ݿ�Ĺ����࣬�����ڴ��д򿪶��SQLiteBuilder����׼����global�����ݣ�
 �û������ʱ�򣬽��û����ݹ��룬д���ļ�ϵͳ���Ӷ���������Ч��
 
 
 
 * @author yfzhu
 *http://www.chinanews.com/fz/2015/01-24/7001149.shtml
 */
public class SQLiteFactory {
	private static final Logger logger = LoggerFactory.getLogger(SQLiteFactory.class);
	private static SQLiteFactory instance=null;
	/** ʶ��հ�����Ϊ�ָ�����*/
	static String BLANK_LINE_REGEXPR="\\s*\n\n";
	/**
	 * 
	 * 
	 */
	private String[] scriptsInit;
	private String scriptsEnd;
	/**
	 * ��"sqlite_areapdt", "sqlite_fav"��
	 */
	private JSONArray conf=null;
	
	private SQLiteFactory() throws Exception{
		
	}
	/**
	 * ����Velocity����
	 * @param userId
	 * @param storeId
	 * @param vc
	 */
	static void initVelocityVariables(long userId, long storeId,SQLiteFactory fac, VelocityContext vc ){
		vc.put("factory", fac);
		vc.put("userid", userId);
		vc.put("storeid", storeId);
	}
	/**
	 * ��û����ת����ԭʼģ��
	 * @return
	 */
	String getScriptsEnd(){
		return scriptsEnd;
	}
	public void reload()throws Exception{
		
		initConfig();
		
		scriptsEnd= PhoneController.getInstance().getValueFromADSQL("sqlitebuilder_end");// getResourceAsString("META-INF/conf/sqlitebuilder_end.sql");
		scriptsInit=null;
		String sc=PhoneController.getInstance().getValueFromADSQL("sqlitebuilder_init");//.getResourceAsString("META-INF/conf/sqlitebuilder_init.sql");
		
		if(Validator.isNull(scriptsEnd) && Validator.isNull(sc)){
			logger.warn("Not found sqlitebuilder_end and sqlitebuilder_init in ad_sql");
			return;
		}
		
		VelocityContext vc=VelocityUtils.createContext();
		vc.put("factory",this);

		StringWriter output = new StringWriter();
		Velocity.evaluate(vc, output, VelocityUtils.class.getName(), sc);
		String s=output.toString();
		
		scriptsInit=s.split(BLANK_LINE_REGEXPR);
		if(scriptsInit.length< 2){
			logger.error("Fail to split using "+BLANK_LINE_REGEXPR+" (only "+scriptsInit.length +")for  ad_sql#sqlitebuilder_init:"+ sc );
			throw new NDSException("�������: ad_sql#sqlitebuilder_init ����̫��:"+scriptsInit.length );
		}
	}
	
	public static SQLiteFactory getInstance(){
		if(instance==null){
			try{
				instance=new SQLiteFactory();
				instance.reload();
			}catch(Exception ex){
				instance=null;
				logger.error("Fail to init sqlite:", ex);
				throw new NDSRuntimeException("Fail to load SQLiteFactory", ex);
			}
		}
		return instance;
	}
	
	
	/**
	 * ad_sql#sqlite_conf
	 * ["sqlite_pdt","sqlite_param","sqlite_pair"]
	 * @return
	 */
	public JSONArray getConfig(){
		return conf;
	}
	private void initConfig() throws Exception{
		Connection conn=null;
		try{
			conn=QueryEngine.getInstance().getConnection();
			conf= (JSONArray) PhoneController.getInstance().getValueFromADSQLAsJSON("sqlite_conf",conn,true);
		}finally{
			try{conn.close();}catch(Throwable tx){}
		}
		if(conf==null){
			throw new NDSException("��Ҫ����ad_sql#sqlite_conf");
		}

	}
	/**
	 * SQLite create table for specified name
	 * ��ͨ��ad_sql#sqlite_<tableName> ����ȡsql��������Ԥ�ڵ�funitid,fairid �Ȳ��������ݽ�������������ֶ�����
	 * @param tableName - sqlite.table name, not from ad_table, such as "pdt", "fav"
	 * @return "create table " ���
	 * @throws Exception
	 */
	public String getSqlCreate(String tableName) throws Exception{
		VelocityContext vc=VelocityUtils.createContext();
		
		QueryEngine engine=QueryEngine.getInstance();
		PhoneController controller=PhoneController.getInstance();
		Connection conn=null;
		try{
			conn=engine.getConnection();
			//userid & storeid is random, we just need create table sql
			initVelocityVariables(1,1,this,vc);

			SQLWithParams sp= controller.parseADSQL("sqlite_"+tableName, vc,conn);
			JSONResultSet jrs=engine.doQueryArrayResultSet(sp.getSQL(), sp.getParams(), conn);
			
			String sql=  parseSQLForSQLiteCreateTable(tableName, jrs);
			return sql;
		}finally{
			try{conn.close();}catch(Throwable tx){}
		}
		
	}
	/**
	 * ����Ϊsqlite��insert into xxx (a,b,c) values (?,?,?) �����
	 * @param jrs
	 * @return
	 * @throws Exception
	 */
	String parseSQLForSQLiteInsert(String tableName, JSONResultSet jrs) throws Exception{
		
		StringBuilder sb=new StringBuilder("insert into ");
		StringBuilder sb2=new StringBuilder();
		sb.append( tableName.toLowerCase()).append(" (");
		String[] colnames= jrs.getColumnLabels();
		int[] types=jrs.getColumnTypes();
		for(int i=0;i< colnames.length;i++){
			sb.append(colnames[i].toLowerCase()).append(",");
			sb2.append("?,");
		}
		sb.deleteCharAt(sb.length()-1);
		sb2.deleteCharAt(sb2.length()-1);
		
		sb.append(") values(").append(sb2.toString()).append(")");
		
		return sb.toString();
	}
	
	/**
	   * Convert java.sql.Types to SQLTypes
	   * @param javaSQLType defined in java.sql.Types
	   * @return string defined in SQLTypes
	   */
	  private final static String convertToSQLiteType(int javaSQLType, int scale){
	    String type=null;;
	    switch (javaSQLType)
	    {
			case java.sql.Types.BIT:
			case java.sql.Types.TINYINT:
			case java.sql.Types.SMALLINT:
			case java.sql.Types.INTEGER :
				type="integer";break;
			case java.sql.Types.BIGINT :
			case java.sql.Types.FLOAT:
			case java.sql.Types.REAL:
			case java.sql.Types.DOUBLE 	:
			case java.sql.Types.NUMERIC 	:
			case java.sql.Types.DECIMAL	:
				type="real";break;
			case java.sql.Types.CHAR	:
			case java.sql.Types.VARCHAR 	:
			case java.sql.Types.LONGVARCHAR :
			case java.sql.Types.BINARY	:
			case java.sql.Types.VARBINARY 	:
			case java.sql.Types.LONGVARBINARY :
		    case java.sql.Types.CLOB       :
				type="text";break;
			case java.sql.Types.DATE 	:
			case java.sql.Types.TIME 	:
			case java.sql.Types.TIMESTAMP 	:
				type="numeric";break;
			default:
				throw new NDSRuntimeException("Unsupported type for sqlite:"+ javaSQLType);
	    }
	    return type;
	  }
	/**
	 * ����Ϊsqlite��create table table (a type, b type)
	 * @param jrs
	 * @return
	 * @throws Exception
	 */
	String parseSQLForSQLiteCreateTable(String tableName, JSONResultSet jrs) throws Exception{
		
		StringBuilder sb=new StringBuilder("create table ");
		sb.append(tableName.toLowerCase()).append(" (");
		String[] colnames= jrs.getColumnLabels();
		int[] types=jrs.getColumnTypes();
		int[] scales=jrs.getScales();
		for(int i=0;i< colnames.length;i++){
			
			int type= types[i];
			String colType=convertToSQLiteType(type, scales[i]);
			
			sb.append(colnames[i].toLowerCase()).append(" ").append(colType).append(",");
		}
		sb.deleteCharAt(sb.length()-1);
		
		sb.append(")");
		
		return sb.toString();
	}
	
	String[] getSQLiteCreationScripts() {
		return scriptsInit;
	}
	
	
	/**
	 * Create a sqlite file
	 * @throws Exception
	 */
	public void createSqliteFile(String dBFilefullPath,long userId,long storeId,Connection conn) throws Exception{
		long beginTime=System.currentTimeMillis();
		SQLiteBuilder sb=new SQLiteBuilder();
		sb.init();
		sb.insertTables(userId,storeId,conn);
		sb.writeToFile(dBFilefullPath);
		logger.debug("Created SQLiteBuilder takes "+ (System.currentTimeMillis()-beginTime)/1000+" seconds");
	}
	
	
}
