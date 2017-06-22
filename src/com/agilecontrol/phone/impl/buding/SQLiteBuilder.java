package com.agilecontrol.phone.impl.buding;

import java.io.StringWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Collection;
import java.util.Locale;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.agilecontrol.nea.core.control.event.DefaultEventContext;
import com.agilecontrol.nea.core.query.JSONResultSet;
import com.agilecontrol.nea.core.query.QueryEngine;
import com.agilecontrol.nea.core.query.QuerySessionImpl;
import com.agilecontrol.nea.core.schema.TableManager;
import com.agilecontrol.nea.core.util.MessagesHolder;
import com.agilecontrol.nea.core.velocity.VelocityUtils;
import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.nea.util.StringUtils;
import com.agilecontrol.nea.util.Validator;
import com.agilecontrol.phone.PhoneController;
import com.agilecontrol.phone.PhoneController.SQLWithParams;

/**
 * ���sqlite�Ĺ��죬�����ṹ���������׼��

 ����ad_sql#sqlite_conf
 ������ڣ���ʽ���£�
 
 { global: ["sqlite_pdt","sqlite_param"], user:["sqlite_areapdt", "sqlite_fav"]}
 
 ���У� globalΪ�������ȫ�ֱ����綩������Ʒ��Ϣ����Ӧ��sql�ڹ����ʱ������ǵ�ǰ�������id�� �� $fairid ��ʾvelocity����
  	   userΪ�û�����ر���Ӧ��sql�ڹ����ʱ������ǵ�ǰ���ֵ�id�� �� $funitid ��ʾvelocity����
  	   ��Щsqlname�ڽ�����ʱ��Ĭ�Ͻ�sqlite_����Ĳ��־͵�����Ӧsqlite��ı�������sqlite_areapdt��Ӧ��sqlite��Ϊareapdt
 
 * @author yfzhu
 *
 */
public class SQLiteBuilder {
	
	private static final Logger logger = LoggerFactory.getLogger(SQLiteBuilder.class);
	private Connection sqliteConn=null;
	private  Statement stmt=null;
	
	
	public SQLiteBuilder(){
		
	}
	
	/**
	 * ��ʼ�����ݿ�
	 * @throws Exception
	 */
	void init() throws Exception{
		try{
			Class.forName("org.sqlite.JDBC");
		}catch(ClassNotFoundException ex){
			throw new NDSException("Make sure sqlite jdbc driver installed:"+ "org.sqlite.JDBC");
		}
		
		//new memory db 
		sqliteConn = DriverManager.getConnection("jdbc:sqlite:");
		stmt = sqliteConn.createStatement();
		
		createStructure();
		
		sqliteConn.setAutoCommit(false);
	}
	
	/**
	 * �������ݿ�ṹ��������ṹ����Ӧ�������
	 * @throws Exception
	 */
	private void createStructure() throws Exception{
		
		String[] scripts=SQLiteFactory.getInstance().getSQLiteCreationScripts();
		if(scripts==null|| scripts.length==0) throw new NDSException("SQLite�������δ���壬����ad_sql#sqlitebuilder_init"); 
		for(String script: scripts){
			logger.debug(script);
			stmt.executeUpdate(script);
		}
		
	}
	
	/**
	 * Insert data using sql 
	 * @param insertSQL
	 * @param data ��ά����
	 */
	private void insertData(String insertSQL, JSONArray data) throws Exception{
		logger.debug(insertSQL );
		
		PreparedStatement pstmt=null;
		// insert
		try{
			pstmt= sqliteConn.prepareStatement(insertSQL);
			//data may have just one column
			boolean onlyOneColumn=false;
			if(data.length()>0){
				onlyOneColumn = !(data.get(0) instanceof JSONArray);
			}
			if(onlyOneColumn){
				//ֻ��1�У�ֱ��ȡ
				for(int i=0;i< data.length();i++){
					pstmt.setObject(1, data.get(i));
					pstmt.addBatch();
				}
			}else{
				//ÿ��Ԫ�ض���jsonarray
				for(int i=0;i< data.length();i++){
					
					JSONArray row= data.getJSONArray(i);
					for(int j=0;j<row.length();j++){
						pstmt.setObject(j+1, row.get(j));
					}
					pstmt.addBatch();
				}
			}
			pstmt.executeBatch();
	
			sqliteConn.commit();
		}finally{
			try{pstmt.close();}catch(Throwable tx){}
		}
	}
	
	
	/**
	 * �û��������û���������
	 * @throws Exception
	 */
	public void insertTables(long userId,long storeId, Connection conn) throws Exception{
		PhoneController controller=PhoneController.getInstance();
		QueryEngine engine=QueryEngine.getInstance();
		SQLiteFactory factory= SQLiteFactory.getInstance();

		JSONArray sqlnames=factory.getConfig();
		
		VelocityContext vc=VelocityUtils.createContext();
		SQLiteFactory.initVelocityVariables(userId, storeId, factory, vc);
		
		
		for(int i=0;i< sqlnames.length();i++){
			String name= sqlnames.getString(i);
			String tableName= StringUtils.replace(name, "sqlite_", ""); //�Ժ��������Ϊ����
			SQLWithParams sp= controller.parseADSQL(name, vc,conn);
			if(sp==null){
				throw new NDSException("δ�ҵ�"+ name+"��Ӧ��ad_sql����");
			}

			JSONResultSet jrs=engine.doQueryArrayResultSet(sp.getSQL(), sp.getParams(), conn);
			String sqliteInsertSQL= factory.parseSQLForSQLiteInsert(tableName,jrs);
			insertData(sqliteInsertSQL, jrs.getData());
		}
		

		// ���н����Ľű��Ľ�����Ȼ������ִ�д���
		StringWriter output = new StringWriter();
		Velocity.evaluate(vc, output, getClass().getName(), factory.getScriptsEnd());

		String[] scripts= output.toString().split(SQLiteFactory.BLANK_LINE_REGEXPR);
		for(String script: scripts){
			logger.debug(script);
			stmt.executeUpdate(script);
		}

	}
	
	/**
	 * Write to file .db
	 * @param dbfile full path, should not have space in file path
	 * @throws Exception
	 */
	public void writeToFile(String dBFilefullPath) throws Exception{
		sqliteConn.setAutoCommit(true);
		logger.debug("Save sqlite "+ dBFilefullPath);
		stmt.executeUpdate("backup to \""+ dBFilefullPath+"\"");
	}
	/**
	 * Must call this after create 
	 */
	public void destroy(){
		if(stmt!=null )try{ stmt.close();}catch(Throwable t){}
		if(sqliteConn!=null)try{ sqliteConn.close();}catch(Throwable t){}
	}
}
