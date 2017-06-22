package com.agilecontrol.b2b.schema;

import java.io.StringReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.HashSet;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.agilecontrol.nea.core.db.oracle.QDate;
import com.agilecontrol.nea.core.query.QueryEngine;
import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.nea.util.Validator;

/**
 * 从一个datasource 迁移到另一个datasource
 * 这个问题源于目前portal6没有界面维护ad_sql, ad_sql 不是单纯的key/value，还有一个Param定义
 * 
 * @author yfzhu
 *
 */
public class SyncADSQL {
	private static final Logger logger = LoggerFactory.getLogger(SchemaUtils.class);
	private Connection conn;
	/**
	 * 目标源，应该已经配置好，且不会在这个类中关闭
	 * @param destConn
	 */
	public SyncADSQL(Connection destConn){
		conn=destConn;
	}
	/**
	 * 将ad_sql数据从datasrc指定的datasource中同步到当前库，读取modifieddate, 比当前搞的全部取过来
	 * 
	 * @param datasrc format: java:comp/env/DataSource 
	 * @throws Exception
	 */
	public String syncFromDatasource(String datasrc) throws Exception{
		
		Context ctx = new InitialContext();
        logger.info("Using datasource:"+datasrc);
        DataSource datasource = (DataSource) ctx.lookup (datasrc);
        
        Connection srconn=datasource.getConnection();
        if(srconn==null || srconn.isClosed()) throw new NDSException("datasource "+ datasrc+"配置有问题");
        
        PreparedStatement pstmtUpdate=null,pstmtInsert=null;
        
        try{
		
        	String mdt=(String)QueryEngine.getInstance().doQueryOne("select to_char(max(modifieddate),'YYYY-MM-DD HH24:MI:SS') from ad_sql",new Object[]{},conn);
        	if(Validator.isNull(mdt)) mdt="1970-01-01 00:00:00";
        	
        	JSONArray jo=QueryEngine.getInstance().doQueryObjectArray("select id,name,value,params,description,creationdate,modifieddate from ad_sql where modifieddate>to_date(?,'YYYY-MM-DD HH24:MI:SS')", new Object[]{mdt}, srconn);
        	pstmtUpdate=conn.prepareStatement("update ad_sql set name=?, value=?,params=?,description=?,creationdate=?,modifieddate=? where id=?");
        	pstmtInsert=conn.prepareStatement("insert into ad_sql(name,value,params,description,creationdate,modifieddate,id)values(?,?,?,?,?,?,?)");
        	for(int i=0;i<jo.length();i++){
        		JSONObject one=jo.getJSONObject(i);
        		pstmtUpdate.setString(1, one.getString("name"));
        		pstmtUpdate.setCharacterStream(2, new StringReader( one.getString("value")), one.getString("value").length());//  QueryEngine.getInstance().getClob(new StringBuilder( one.getString("value")), conn));
        		pstmtUpdate.setString(3, optString(one,"params"));
        		pstmtUpdate.setString(4, optString(one,"description"));
        		pstmtUpdate.setTimestamp(5, ( (QDate)one.get("creationdate")).toTimestamp());
        		pstmtUpdate.setTimestamp(6, ( (QDate)one.get("modifieddate")).toTimestamp());
        		pstmtUpdate.setInt(7, one.getInt("id"));
        		if(pstmtUpdate.executeUpdate()==0){
        			//do insert
        			
        			pstmtInsert.setString(1, one.getString("name"));
        			pstmtInsert.setCharacterStream(2, new StringReader( one.getString("value")), one.getString("value").length());
//        			pstmtInsert.setClob(2, new StringReader( one.getString("value")));
        			pstmtInsert.setString(3, optString(one,"params"));
        			pstmtInsert.setString(4, optString(one,"description"));
        			pstmtInsert.setTimestamp(5,  ( (QDate)one.get("creationdate")).toTimestamp());
        			pstmtInsert.setTimestamp(6,  ( (QDate)one.get("modifieddate")).toTimestamp());
        			pstmtInsert.setInt(7, one.getInt("id"));
        			pstmtInsert.executeUpdate();
        		}
        	}
        	
        	//删除srconn中已经没有的id
        	JSONArray allIdSrcs=QueryEngine.getInstance().doQueryJSONArray("select id from ad_sql", new Object[]{}, srconn);
        	JSONArray allIdDests=QueryEngine.getInstance().doQueryJSONArray("select id from ad_sql", new Object[]{}, conn);
        	JSONArray ids=getNotExistsIds(allIdSrcs,allIdDests );
        	for(int i=0;i<ids.length();i++){
        		QueryEngine.getInstance().executeUpdate("delete from ad_sql where id=?", new Object[]{ids.getLong(i)},conn);
        	}
        	
        	return jo.length() +ids.length() >0? "合计更新"+ (jo.length() +ids.length())+"条ad_sql, 从"+datasrc:"已是最新";
        	
        }finally{
        	try{pstmtUpdate.close();}catch(Throwable tx){}
        	try{pstmtInsert.close();}catch(Throwable tx){}
        	try{srconn.close();}catch(Throwable tx){}
        }
		
	}
	/**
	 * 找到allIdSrcs 里没有，但allIdDests有的id
	 * @param allIdSrcs elements: long
	 * @param allIdDests elements: long
	 * @return elements: long
	 * @throws Exception
	 */
	private JSONArray getNotExistsIds(JSONArray allIdSrcs,JSONArray allIdDests) throws Exception{
		HashSet<Long> srcs=new HashSet();
		for(int i=0;i<allIdSrcs.length();i++){
			long id=allIdSrcs.getLong(i);
			srcs.add(id);
		}
		JSONArray ret=new JSONArray();
		for(int i=0;i<allIdDests.length();i++){
			long id=allIdDests.getLong(i);
			if(! srcs.contains(id)) ret.put(id);
		}
		return ret;
	}
	
	
	private String optString(JSONObject obj,String key){
		String v=obj.optString(key);
		if(Validator.isNull(v)) return "";
		return v;
	}
	
}
