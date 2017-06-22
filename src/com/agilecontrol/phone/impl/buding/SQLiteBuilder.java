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
 * 完成sqlite的构造，包括结构定义和数据准备

 关于ad_sql#sqlite_conf
 如果存在，格式如下：
 
 { global: ["sqlite_pdt","sqlite_param"], user:["sqlite_areapdt", "sqlite_fav"]}
 
 其中： global为订货会的全局表，比如订货会商品信息，对应的sql在构造的时候传入的是当前订货会的id， 用 $fairid 表示velocity变量
  	   user为用户的相关表，对应的sql在构造的时候传入的是当前买手的id， 用 $funitid 表示velocity变量
  	   这些sqlname在解析的时候，默认将sqlite_后面的部分就当作对应sqlite里的表名，如sqlite_areapdt对应的sqlite表即为areapdt
 
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
	 * 初始化数据库
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
	 * 创建数据库结构，包括表结构和相应程序代码
	 * @throws Exception
	 */
	private void createStructure() throws Exception{
		
		String[] scripts=SQLiteFactory.getInstance().getSQLiteCreationScripts();
		if(scripts==null|| scripts.length==0) throw new NDSException("SQLite生成语句未定义，请检查ad_sql#sqlitebuilder_init"); 
		for(String script: scripts){
			logger.debug(script);
			stmt.executeUpdate(script);
		}
		
	}
	
	/**
	 * Insert data using sql 
	 * @param insertSQL
	 * @param data 二维数组
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
				//只有1列，直接取
				for(int i=0;i< data.length();i++){
					pstmt.setObject(1, data.get(i));
					pstmt.addBatch();
				}
			}else{
				//每个元素都是jsonarray
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
	 * 用户表，比如用户订单数据
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
			String tableName= StringUtils.replace(name, "sqlite_", ""); //以后面的内容为表名
			SQLWithParams sp= controller.parseADSQL(name, vc,conn);
			if(sp==null){
				throw new NDSException("未找到"+ name+"对应的ad_sql定义");
			}

			JSONResultSet jrs=engine.doQueryArrayResultSet(sp.getSQL(), sp.getParams(), conn);
			String sqliteInsertSQL= factory.parseSQLForSQLiteInsert(tableName,jrs);
			insertData(sqliteInsertSQL, jrs.getData());
		}
		

		// 进行结束的脚本的解析，然后生成执行代码
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
