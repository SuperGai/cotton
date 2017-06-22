package com.agilecontrol.phone;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import org.apache.velocity.VelocityContext;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.agilecontrol.b2b.schema.Column;
import com.agilecontrol.b2b.schema.Column.Type;
import com.agilecontrol.b2b.schema.Table;
import com.agilecontrol.b2b.schema.TableManager;
import com.agilecontrol.b2b.sms.YunTXSMSSender;
import com.agilecontrol.nea.core.query.QueryEngine;
import com.agilecontrol.nea.core.query.QueryUtils;
import com.agilecontrol.nea.core.util.CookieKeys;
import com.agilecontrol.nea.util.Configurations;
import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.nea.util.NDSRuntimeException;
import com.agilecontrol.nea.util.ObjectNotFoundException;
import com.agilecontrol.nea.util.Tools;
import com.agilecontrol.nea.util.Validator;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 
 * ������
 * 
 * @author yfzhu
 *
 */
@Admin(mail="wang.cun@lifecycle.cn")
public class PhoneUtils {
	private static final Logger logger = LoggerFactory.getLogger( PhoneUtils.class);
	
	/**
	 * �ҵ��û���ָ��com�µ�emp, ���� usr:$uid:emp ������hash, key: comid, value: empid
	 * ����Ҫ�г��û��������̼ҵ�ʱ�򣬿��԰�empid��˳����һ��������(�л����̵�ʱ��
	 * ���ֵ����Ա����ְ��ʱ��ά���ģ�ɾ����
	 * @param comId
	 * @param usrId 
	 * @param vc
	 * @param conn
	 * @param jedis
	 * @return -1 if not found
	 * @throws Exception
	 */
	public static long findEmpId(long comId, long usrId, VelocityContext vc, Connection conn, Jedis jedis)
	        throws Exception{
		String key = "usr:" + usrId + ":emp";
		long empId = -1;
		if( !jedis.exists( key)) {
			//loading all emp of that usr
			HashMap<String, String> emps = new HashMap();
			JSONArray ja = QueryEngine.getInstance().doQueryObjectArray(
			        "select id, com_id from emp where u_id=? and en='Y'", new Object[]{ usrId }, conn);
			for( int i = 0; i < ja.length(); i++){
				JSONObject one = ja.getJSONObject( i);
				long cId = one.getLong( "com_id");
				long eId = one.getLong( "id");
				emps.put( String.valueOf( cId), String.valueOf( eId));
				if( comId == cId) empId = eId;
			}
			if( ja.length() > 0) jedis.hmset( key, emps);
			
		}
		else{
			empId = Tools.getLong( jedis.hget( key, String.valueOf( comId)), -1);
		}
		return empId;
	}
	
	/**
	 * 
	 * @param tableName
	 * @param ids
	 * @param colNames  �ֶ����ƿ�����Ӣ�Ķ��ŷָ�
	 * @param useDBWhenNotFound
	 * @param vc
	 * @param conn
	 * @param jedis
	 * @return
	 * @throws Exception
	 */
	public static JSONArray getRedisObjectArray(String tableName, JSONArray ids, String colNames,
	        boolean useDBWhenNotFound, Connection conn, Jedis jedis) throws Exception{
		Table table = TableManager.getInstance().getTable( tableName);
		return getRedisObjectArray( tableName, ids, table.getColumns( colNames), useDBWhenNotFound, conn, jedis);
	}
	
	/**
	 * ����ָ��ids���϶�Ӧ�Ķ����б�
	 * ����ͨ�õķ���ʵ���б���ÿ��id�Ķ����ȡ��������ص��б���û�У���ʾ���󲻴���redis����Ҫ���Դ����ݿ��ȡ
	 * @param tableName
	 * @param ids elements: Long
	 * @useDBWhenNotFound �Ƿ���Ҫ��db��ȥ���Ի�ȡָ��id������ǣ�������getRedisObj
	 * @param vc
	 * @param conn
	 * @param jedis
	 * @return Ԫ��ΪJSONObject
	 * @throws Exception
	 */
	public static JSONArray getRedisObjectArray(String tableName, JSONArray ids, ArrayList<Column> columns,
	        boolean useDBWhenNotFound, Connection conn, Jedis jedis) throws Exception{
		if( columns == null || columns.size() == 0 || !columns.get( 0).getName().equals( "id"))
		    throw new NDSException( tableName+"����ȷ��columns���ã��׸�Ԫ�ر�����id");
			
		//		logger.debug("getRedisObjectArray("+ tableName+", "+ ids+","+ Tools.toString(columns, ",")+", "+useDBWhenNotFound +")");
		
		JSONArray objs = new JSONArray();
		
		String[] fields = new String[columns.size()];
		for( int i = 0; i < columns.size(); i++){
			fields[i] = columns.get( i).getName();
		}
		Table table=TableManager.getInstance().getTable(tableName);
		Pipeline pipeline = jedis.pipelined();
		for( int i = 0; i < ids.length(); i++){
			long objectId = ids.getLong( i);
			String key = tableName + ":" + objectId;
			Response<List<String>> values = pipeline.hmget( key, fields);
		}
		List ret = pipeline.syncAndReturnAll();
		pipeline.close();
		
		//���ÿ��Ԫ�صĴ����ԣ�ά��redis��һ����
		for( int i = 0; i < ids.length(); i++){
			JSONObject jo;
			long objectId = ids.getLong( i);
			List<String> values = (List<String>) ret.get( i);
			if( values.get( 0) == null) {
				//read from db
				//				PhoneController pc=PhoneController.getInstance();
				//				vc.put("objectid", objectId);
				//				JSONObject joFull=pc.getObjectByADSQL(tableName+":obj", vc, conn);
				
				JSONObject joFull = QueryEngine.getInstance()
				        .doQueryObject( "select "+ getAllColumnsForSQLSelection(table)+" from " +table.getRealName()+" "+ tableName + " where id=?", new Object[]{ objectId }, conn);
				if( joFull == null) {
					logger.error( "table=" + tableName + ",id=" + objectId + " not found in db");
					continue;
					//					throw new ObjectNotFoundException("����δ�ҵ�(talbe="+tableName+",id="+objectId+")");
				}
				String key = tableName + ":" + objectId;
				jedis.hmset( key, toMap( joFull));
				
				jo = new JSONObject();
				for( Column col : columns){
					jo.put( col.getName(), joFull.opt( col.getName()));
					//���������⴦������ͻ�����Ҫǿ��ĳFK�ֶ�������ȡ�����󣬶����ǵ���Ϊֹ�����������ֶε���չ����:"fetchobj:true"��ʵ��
					if( col.getFkTable() != null && Boolean.TRUE.equals( col.getJSONProp( "fetchobj"))) {
						long foId = Tools.getLong( joFull.opt( col.getName()), -1);
						jo.put( col.getName(), fetchObject( col.getFkTable(), foId,  conn, jedis));
					}
				}
			}
			else{
				jo = new JSONObject();
				for( int j = 0; j < columns.size(); j++){
					Object v = values.get( j);
					if( v == null) v = JSONObject.NULL;
					Column col = columns.get( j);
					jo.put( col.getName(), v);
					//���������⴦������ͻ�����Ҫǿ��ĳFK�ֶ�������ȡ�����󣬶����ǵ���Ϊֹ�����������ֶε���չ����:"fetchobj:true"��ʵ��
					if( col.getFkTable() != null && Boolean.TRUE.equals( col.getJSONProp( "fetchobj"))) {
						long foId = Tools.getLong( v, -1);
						jo.put( col.getName(), fetchObject( col.getFkTable(), foId,  conn, jedis));
					}
				}
				toPrimeType(table,jo);
			}
			objs.put( jo);
			//			logger.debug("this is refbytable: "+ tableName+":"+ jo);
		}
		//		pipeline.sync();
		
		//		logger.debug("after primary type:"+ toPrimeType(TableManager.getInstance().getTable(tableName),objs));
		return objs;
	}
	
	/**
	 * ѭ����ȥ�ö���ֱ��û���ֶ���Ҫ��������
	 * @param table
	 * @param objectId -1 means not found
	 * @param vc
	 * @param conn
	 * @param jedis
	 * @return
	 * @throws Exception
	 */
	public static JSONObject fetchObjectAllColumns(Table table, long objectId,  Connection conn,
	        Jedis jedis) throws Exception{
			
		ArrayList<Column> cols = table.getColumns();
		
		if( objectId <= 0) {
			return createBlankFKObj( table);
		}
		
		JSONObject obj = getRedisObj( table.getName(), objectId, cols, conn, jedis);
		
		return obj;
	}
	
	/**
	 * ѭ����ȥ�ö���ֱ��û���ֶ���Ҫ��������
	 * @param table
	 * @param objectId -1 means not found
	 * @param vc
	 * @param conn
	 * @param jedis
	 * @return
	 * @throws Exception
	 */
	public static JSONObject fetchObject(Table table, long objectId,  Connection conn, Jedis jedis)
	        throws Exception{
			
		ArrayList<Column> cols = table.getColumnsInObjectView();
		
		if( objectId <= 0) {
			return createBlankFKObj( table);
		}
		
		JSONObject obj = getRedisObj( table.getName(), objectId, cols,  conn, jedis);
		
		return obj;
	}
	
	/**
	 * ����һ���յĶ�����Ҫ��fk�������(display key)�����磺usr���dks: ["name","phone"]����������:
	 * {id, name,phone} ����id�Ǳ��� 
	 * @param table
	 * @return 
	 * @throws Exception
	 */
	public static JSONObject createBlankFKObj(Table table) throws Exception{
		JSONObject jo = new JSONObject();
		for( Column col : table.getColumnsInObjectView()){
			jo.put( col.getName(), JSONObject.NULL);
		}
		return jo;
	}
	
	/**
	 * Convert throwable message to simple one. For instance, message from db contains ora-
	 * Will use com.agilecontrol.nea.core.util.MessagesHolder to do internal wildcard replacement
	 * should be parsed to readable one
	 * @param t
	 * @return
	 */
	public static String getExceptionMessage(Throwable t, Locale locale){
		Throwable rs = com.agilecontrol.nea.util.StringUtils.getRootCause( t);
		String s = rs.getMessage();
		if( s == null) {
			s = rs.getClass().getName();
			//    		StringWriter sw = new StringWriter();  
			//            PrintWriter pw = new PrintWriter(sw);  
			//    		t.printStackTrace(pw);
			//    		s=sw.toString();
		}
		s = s.trim();
		int p = s.indexOf( "ORA-");
		int q = s.indexOf( "ORA-", p + 1);
		String r;
		if( p >= 0) {
			//convert index message
			if( s.indexOf( "00001", p) == p + 4) { // ORA-00001
				// UNIQUE CONSTRAINT(INDEX_NAME) VIOLATED
				r = parseOracle001Error( s, p, locale);
			}
			else{
				if( q > 0)
					r = s.substring( p + 11, q - 1);
				else
					r = s.substring( p + 11);
			}
			r = s.substring( 0, p) + r;
		}
		else
			r = s;
		return com.agilecontrol.nea.core.util.MessagesHolder.getInstance().translateMessage( r, locale);
		//+"("+ org.slf4j.LoggerFactory.getNDC()+")";
	}
	
	/**
	 * ��������
	 * Convert throwable message to simple one. For instance, message from db contains ora-
	 * Will use com.agilecontrol.nea.core.util.MessagesHolder to do internal wildcard replacement
	 * 
	 * 1����ȡ�쳣��Ϣ�������С�ORA-����Ϣʱ�����з�֧����:1,���� 00001���������Ѷ���;2,������ORA-���������ͳһ����;
	 * 2�����쳣��Ϣƥ�䵽    ORA20000  ��  ORA29999 ʱ��Ϊ���ݿⶨ����쳣��ֱ�ӷ�����Ϣ;
	 * 3���� �쳣�Ȳ���  NDSException Ҳ���� NDSRuntimeExceptionʱ���������쳣����ʱ����ͳһ����
	 * 4��ͨ�� 3���� ɾѡ ,���� ��ʣ�� NDSException ����  NDSRuntimeException �������Ϊjava�����쳣��ֱ�ӷ�����Ϣ
	 * ע:�������ķ�֧������Խ��е���������ʱע��ɾѡ������ת��
	 * @param params
	 * @param t
	 * @param locale
	 * @return
	 * @author stao 2016/7/20
	 */
	public static String getExceptionMessage(JSONObject params, Connection conn,Jedis jedis, Throwable t, Locale locale){
		Throwable rs = com.agilecontrol.nea.util.StringUtils.getRootCause( t);
		String s = rs.getMessage();
		if( s == null) {
			s = rs.getClass().getName();
		}
		s = s.trim();
		int p = s.indexOf( "ORA-");
		int q = s.indexOf( "ORA-", p + 1);
		String r = new String();
		if( p >= 0) {
			/*convert index message*/
			if( s.indexOf( "00001", p) == p + 4) { // ORA-00001
				// UNIQUE CONSTRAINT(INDEX_NAME) VIOLATED
				r = parseOracle001Error( s, p, locale);
				r = s.substring( 0, p) + r;
			}
			else{
				r = parseOtherError( params,conn,jedis, r, t);
			}
		}
		else if( s.matches( "^.*[O][R][A][2][0-9]{4}[^0-9].*$")) {
			//���쳣Ϊ  ORA20000��ORA29999 ʱ
			r = s;
		}
		else if( (!(t instanceof NDSException)) && (!(t instanceof NDSRuntimeException))) {
			//���쳣���� NDSException, NDSRuntimeExceptionʱ�����쳣�Ĵ���
			r = parseOtherError( params,conn,jedis, r, t);
		}
		else{
			//�˴�Ϊ NDSException ��  NDSRuntimeException�����账�����ڴ˴��޸�
			r = s;
		}
		return r;
	}
	
	/**
	 * ���������쳣
	 * ����ǰ�쳣��error_id�������� redis ��Ϣ������Ϣ�ӿ�
	 * error_id��Դ��racl �� sequence:seq_errorid
	 * @param params ���ڴ����Ϣ, ��:error_id,�û������Ϣ ��,����Ҫ,���Կ����Զ�����װ���� 
	 * @param conn
	 * @param jedis
	 * @param msg ��Ϣ�����쳣��Ϣ����ȡ�������ַ���
	 * @param throwable
	 * @return msg ��װ����Ϣ���û����ؽ�����ʾ
	 */
	private static String parseOtherError(JSONObject params,Connection conn,Jedis jedis, String msg, Throwable throwable){		
			//��ȡsequence
			long errorId =-1;
			try{
				errorId = PhoneController.getInstance().getNextId( "errorid", conn);
				msg = "��⣬��������쳣���������ݣ����Ժ����ԣ������:" + errorId+")";
				params.put( "errorid", errorId);
			}
			catch( Exception e){
				logger.error( e.getLocalizedMessage(), e);
			}
			logger.error( "error_" + errorId, throwable);
			
			//����json���󣬽���Ҫ�õ������ݴ��ݵ�����������;
			//params���ڴ��Ͳ���   ��: error_i �� port
			
			//������Ϣ���ͽӿ�
			ExceptionDispatcher dispatcher = ExceptionDispatcher.getInstance();
			dispatcher.notifyException( throwable, params,jedis);
		return msg;
	}
	
	/**
	 * ORA-00001 unique constraint (string.string) violated
	 * @param s
	 * @param startIdx
	 * @return
	 */
	private static String parseOracle001Error(String s, int fromIndex, Locale locale){
		TableManager manager = TableManager.getInstance();
		
		int idxStart = s.indexOf( "(", fromIndex);
		int idxEnd = s.indexOf( ")", fromIndex);
		if( !(idxStart > 0 && idxEnd > idxStart)) return s;
		String idxName = s.substring( idxStart + 1, idxEnd);
		int idxTableEnd = idxName.indexOf( '.');
		
		String tableName = null;
		Table table = null;
		
		String indexName = idxName.substring( idxTableEnd + 1);
		String errorMsg = "�����Ѵ���";//+idxName+"";
		try{
			// skip ad_client_id column
			List al = com.agilecontrol.nea.core.query.QueryEngine.getInstance().doQueryList(
			        "select table_name, column_name from USER_IND_COLUMNS where column_name<>'AD_CLIENT_ID' and index_name="
			                + QueryUtils.TO_STRING( indexName));
			if( al.size() > 0) {
				StringBuilder sb = new StringBuilder( "�����Ѵ��ڣ�����������:");
				for( int i = 0; i < al.size(); i++){
					if( i > 0) sb.append( "+");
					String tname = (String) ((List) al.get( i)).get( 0);
					if( table == null && manager.isInitialized()) {
						table = manager.getTable( tname);
						tableName = tname;
					}
					String cname = (String) ((List) al.get( i)).get( 1);
					Column col = manager.isInitialized() ? manager.getColumn( tname, cname) : null;
					if( col != null) cname = col.getNote();
					
					sb.append( cname);
				}
				//if(table!=null)sb.append(",@in-table@:").append( table.getDescription(locale));
				//else 
				//sb.append("(@unqiue-index-name@:").append(indexName).append(")");
				errorMsg = sb.toString();
			}
		}
		catch( Throwable t){
			logger.error( "Faile to parse 0001 error", t);
			
		}
		return errorMsg;
	}
	
	/**
	 * ����throwable��stacktrace���ҵ���һ������AdminAdminע����࣬���ض�Ӧemail��ַ.�����û��Adminע�⣬���ؿ�
	 * 
	 * ���ҷ���ע�⣬���У�������ע�⣬���ط���ע���email��ַ��û�з�����ע���email��ַ
	 * ȡ�������򷽷���admin��������ΪЧ�ʵ��£�getDeclaredMethods Ŀǰ���ñ����ķ�ʽ��Ӱ��ϵͳ���ܡ�δ���ĸĽ����Կ��ǣ�
	 * ���첽�߳��д���stackTrace����������ͬ���߳���ʶ���admin mail (yfzhu)
	 * 
	 * @param throwable �쳣��Ϣ
	 * @author li.shuhao
	 * @return email��ַ, null if not found
	 */
	@Admin(mail="li.shuhao@lifecycle.cn")
	public static String getAdminMailFromFirstAnnotation(Throwable throwable) {
		String mailTo=null;
		for(StackTraceElement st: throwable.getStackTrace()){
			//�����쳣��ջ��Ϣst�����st�е�ÿһ���쳣��
			Class cls=null;
			try {
				cls = Class.forName(st.getClassName());
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
				continue;
			}				
			//�õ����쳣�������ж��쳣���е�ע���Ƿ�Ϊ��
			Annotation ann= cls.getAnnotation(Admin.class);
			if(ann!=null){
				mailTo = ((Admin)ann).mail();
				/**
				 * Ŀǰȥ����������λ����
				 */
//				//�����쳣���еķ������ж��쳣��ջ����ķ�������Щ�����
//				for(Method method: cls.getDeclaredMethods()){
//					if(method.getName().equals(st.getMethodName())){
//						//�ҵ������쳣������ע�⣬Ȼ��õ������ֵ
//						Annotation mann=method.getAnnotation(Admin.class);
//						//�жϷ�������û��ע�⣬�����еĻ�������������Ҫ��
//						if(mann!=null){
//							mailTo = ((Admin)mann).mail();
//							break;
//						}
//					}
//				}
				break;
			}
		}
		return mailTo;
	}	
	/**
	 * ���ÿ�н���ת��
	 * Read each value of jo key, and parse to prime type according to column type
	 * @param table
	 * @param ja ֱ����element�����Ͻ���ת��
	 * @return ja
	 */
	public static JSONArray toPrimeType(Table table, JSONArray ja) throws Exception{
		for( int i = 0; i < ja.length(); i++){
			JSONObject jo = ja.getJSONObject( i);
			toPrimeType( table, jo);
		}
		return ja;
	}
	
	/**
	 * Read each value of jo key, and parse to prime type according to column type
	 * @param table
	 * @param jo ֱ������������Ͻ���ת��
	 * @return jo ��jo
	 */
	public static JSONObject toPrimeType(Table table, JSONObject jo) throws Exception{
		ArrayList<String> keys = new ArrayList();
		for( Iterator it = jo.keys(); it.hasNext();){
			String key = (String) it.next();
			keys.add( key);
		}
		for( String key : keys){
			Object value = jo.get( key);
			Column col = table.getColumn( key);
			if( col == null) {
				//				logger.debug("not found column "+ table+"."+ key);
				continue;
			}
			if( value instanceof JSONObject && col.getFkTable() != null) {
				toPrimeType( col.getFkTable(), (JSONObject) value);
			}
			else if( col.getType() == Type.NUMBER && Validator.isNumber( value.toString())) {
				jo.put( key, Double.parseDouble( value.toString()));
			}
		}
		return jo;
	}
	
	/**
	 * ��ָ��columns��fieldֵ����
	 * @param tableName
	 * @param ids
	 * @param colNames  �ֶ����ƿ�����Ӣ�Ķ��ŷָ�
	 * @param vc
	 * @param conn
	 * @param jedis
	 * @return
	 * @throws Exception
	 */
	public static JSONObject getRedisObj(String tableName, long objectId, String colNames, 
	        Connection conn, Jedis jedis) throws Exception{
		Table table = TableManager.getInstance().getTable( tableName);
		return getRedisObj( tableName, objectId, table.getColumns( colNames), conn, jedis);
	}
	
	
	/**
	 * ��ָ��columns��fieldֵ����, ��Ҫȫ������redis����fields��Ҫ�ò���columns�����ĺ���
	 * ���jedis key ����: $tablename$:$objectId,  (value: hset����)�����ڣ��������ݿ����� <tableName>:obj��Ӧ��䴴�����󣬲�����
	 * @param tableName
	 * @param objectId
	 * @param columns ע��Ҫ��֤��һ���ֶ���id���������û��ѯ�������ڵ�һ������ֵ����Ϊnil ������ֵ����nil����ȷ��idΪnil�ض���ʾ������)
	 * @param vc
	 * @param conn
	 * @param jedis
	 * @return ����һ����JSONObject�������ֶ���ad_sql#$tableName:obj ����
	 * @throws Exception ������󲻴������ݿ⣬���׳�ObjedctNotFoundException
	 */
	public static JSONObject getRedisObj(String tableName, long objectId, ArrayList<Column> columns, 
	        Connection conn, Jedis jedis) throws Exception{
		if( objectId <= 0) return null;
		//		logger.debug("columns="+ Tools.toString(columns, ","));
		if( columns == null || columns.size() == 0 || !columns.get( 0).getName().equals( "id"))
		    throw new NDSException( "����ȷ��columns���ã��׸�Ԫ�ر�����id");
		
		Table table=TableManager.getInstance().getTable( tableName);
		String key = tableName + ":" + objectId;
		String[] fields = new String[columns.size()];
		for( int i = 0; i < columns.size(); i++){
			fields[i] = columns.get( i).getName();
		}
		List<String> values = jedis.hmget( key, fields);
		
		JSONObject jo;
		if( values.get( 0) == null) {
			//read from db
			//			PhoneController pc=PhoneController.getInstance();
			//			vc.put("objectid", objectId);
			//			JSONObject joFull=pc.getObjectByADSQL(tableName+":obj", vc, conn);
			JSONObject joFull = QueryEngine.getInstance().doQueryObject( "select "+getAllColumnsForSQLSelection(table)+" from " +  table.getRealName()+" where id=?",
			        new Object[]{ objectId }, conn);
			if( joFull == null) {
				logger.error( "table=" + tableName + ",id=" + objectId + " not found in db");
				throw new ObjectNotFoundException( "����δ�ҵ�(table=" + tableName + ",id=" + objectId + ")");
			}
			jedis.hmset( key, toMap( joFull));
			
			jo = new JSONObject();
			for( Column col : columns){
				jo.put( col.getName(), joFull.opt( col.getName()));
				//���������⴦������ͻ�����Ҫǿ��ĳFK�ֶ�������ȡ�����󣬶����ǵ���Ϊֹ�����������ֶε���չ����:"fetchobj:true"��ʵ��
				if( col.getFkTable() != null && Boolean.TRUE.equals( col.getJSONProp( "fetchobj"))) {
					long foId = Tools.getLong( joFull.opt( col.getName()), -1);
					jo.put( col.getName(), fetchObject( col.getFkTable(), foId,conn, jedis));
				}
			}
			
		}
		else{
			jo = new JSONObject();
			for( int i = 0; i < columns.size(); i++){
				Object v = values.get( i);
				if( v == null) v = JSONObject.NULL;
				Column col = columns.get( i);
				jo.put( col.getName(), v);
				//���������⴦������ͻ�����Ҫǿ��ĳFK�ֶ�������ȡ�����󣬶����ǵ���Ϊֹ�����������ֶε���չ����:"fetchobj:true"��ʵ��
				if( col.getFkTable() != null && Boolean.TRUE.equals( col.getJSONProp( "fetchobj"))) {
					long foId = Tools.getLong( v, -1);
					jo.put( col.getName(), fetchObject( col.getFkTable(), foId,  conn, jedis));
				}
				
			}
		}
		return toPrimeType( table, jo);
		
	}
	
	/**
	 * ����ȫ����redis ��������
	 * ���jedis key ����: $tablename$:$objectId,  (value: hset����)�����ڣ��������ݿ����� <tableName>:obj��Ӧ��䴴�����󣬲�����
	 * @param tableName
	 * @param objectId
	 * @param vc
	 * @param conn
	 * @param jedis
	 * @return ����һ����JSONObject�������ֶ���ad_sql#$tableName:obj ����
	 * @throws Exception ������󲻴������ݿ⣬���׳�ObjedctNotFoundException
	 */
	public static JSONObject getRedisObj(String tableName, long objectId,  Connection conn,
	        Jedis jedis) throws Exception{
		if( objectId <= 0) return null;
		
		Table table= TableManager.getInstance().getTable( tableName);
		return getRedisObj(tableName, objectId, table.getColumns(),conn,jedis);
//		
//		String key = tableName + ":" + objectId;
//		Table table= TableManager.getInstance().getTable( tableName);
//		Map<String, String> values = jedis.hgetAll( key);
//		JSONObject jo;
//		if( values.isEmpty()) {
//			//read from db
//			
//			jo = QueryEngine.getInstance().doQueryObject( "select * from " + table.getRealName()+ " where id=?",
//			        new Object[]{ objectId }, conn);
//			//			PhoneController pc=PhoneController.getInstance();
//			//			vc.put("objectid", objectId); $objectid
//			//			jo=pc.getObjectByADSQL(tableName+":obj", vc, conn);
//			if( jo == null) {
//				logger.error( "table=" + tableName + ",id=" + objectId + " not found in db");
//				throw new ObjectNotFoundException( "����δ�ҵ�");
//			}
//			jedis.hmset( key, toMap( jo));
//		}
//		else{
//			jo = new JSONObject( values);
//		}
//		return toPrimeType(table, jo);
		
	}
	
	/**
	 * ���򵥶�����ӵ�Redis��, ������϶�����rac������������Ӧ��lua����
	 * �����sql�����: select * from <table> where id=?
	 * @param objectId
	 * @param table
	 * @param argsObj
	 * @param conn
	 * @param jedis
	 * @return jedis����
	 * @throws Exception
	 */
	public static JSONObject addRedisObj(long objectId, Table table, JSONObject argsObj, Connection conn, Jedis jedis)
	        throws Exception{
			
		String scriptName = table.getRedisAC();//ad_sql#name
		return setRedisObj( objectId, table, scriptName, argsObj, conn, jedis);
	}
	
	/**
	 * ���򵥶�����µ�Redis��, ������϶�����ram������������Ӧ��lua����
	 * �����sql�����: select * from <table> where id=?
	 * @param objectId
	 * @param table
	 * @param argsObj �������� {comid, stid, uid, objectid}
	 * @param conn
	 * @param jedis
	 * @throws Exception
	 */
	public static JSONObject voidRedisObj(long objectId, Table table, JSONObject argsObj, Connection conn, Jedis jedis)
	        throws Exception{
		String scriptName = table.getRedisBD();//ad_sql#name
		return setRedisObj( objectId, table, scriptName, argsObj, conn, jedis);
	}
	
	/**
	 * ���򵥶�����µ�Redis��, ������϶�����ram������������Ӧ��lua����
	 * �����sql�����: select * from <table> where id=?
	 * @param objectId
	 * @param table
	 * @param argsObj �������� {comid, stid, uid, objectid}
	 * @param conn
	 * @param jedis
	 * @throws Exception
	 */
	public static JSONObject modifyRedisObj(long objectId, Table table, JSONObject argsObj, Connection conn,
	        Jedis jedis) throws Exception{
		String scriptName = table.getRedisAM();//ad_sql#name
		return setRedisObj( objectId, table, scriptName, argsObj, conn, jedis);
	}
	
	/**
	 *  ���򵥶�����µ�Redis��, ���scriptName��Ϊ�գ�����������Ӧ��lua����
	 *  scriptName ��Ӧ��ad_sql#name��ϵͳ������sha1������㣬�������������
	 * @param objectId
	 * @param table
	 * @param scriptName ad_sql#name, ȡ��������Ϊscript����
	 * @param argsObj �������� {comid, stid, uid, objectid}
	 * @param conn
	 * @param jedis
	 * @throws Exception
	 */
	private static JSONObject setRedisObj(long objectId, Table table, String scriptName, JSONObject argsObj,
	        Connection conn, Jedis jedis) throws Exception{
		String tableName = table.getName();
		String key = tableName + ":" + objectId;
		//read from db
		PhoneController pc = PhoneController.getInstance();
		JSONObject jo = QueryEngine.getInstance().doQueryObject( "select "+getAllColumnsForSQLSelection(table)+" from " + table.getRealName() + " where id=?",
		        new Object[]{ objectId }, conn);
		String ret = jedis.hmset( key, toMap( jo));
		if( !"OK".equals( ret))
		    throw new NDSException( "Redis�洢�쳣:" + ret + "(" + table.getName() + ":" + objectId + ")");
		if( Validator.isNotNull( scriptName)) {
			JSONObject ojo = (JSONObject) execRedisLua( scriptName, argsObj, conn, jedis);
			if( ojo != null) jo = ojo;
		}
		
		return jo;
	}
	/**
	 * ����ֶ��б�������Ҫ���� .., name aliasname, .. ���������ݣ����쵽select �����
	 * @param table
	 * @return 
	 */
	private static String getAllColumnsForSQLSelection(Table table){
		StringBuilder sb=new StringBuilder();
		for(Column col: table.getColumns()){
			sb.append(col.getRealName());
			if(!col.getRealName().equals(col.getName()))
				sb.append(" ").append(col.getName());
			sb.append(",");
		}
		sb.deleteCharAt(sb.length()-1);//last ","
		return sb.toString();
	}
	
	/**
	 * ����Lua�ű����ű���ad_sql.scriptNameָ���Ľű��ж��壬�������: JSONObject, ����JSONObject
	 * @param scriptName ad_sql.name
	 * @param args �ű����ܵĲ���������: {comid, stid, uid, objectid}, ����Ϊnull����Ҫ��luaԼ��
	 * @param jedis
	 * @return JSONObject��JSONArray, Լ��lua����jsonobject�ǿͻ���Ҫ��ĸ�ʽ��lua�ڲ����ص���String cjson.encode(result), ��Ҫ�ٴν���
	 * @throws Exception
	 */
	public static Object execRedisLua(String scriptName, JSONObject argsObj, Connection conn, Jedis jedis)
	        throws Exception{
		//read from db
		
		try{
			String sha1 = PhoneController.getInstance().getRedisScript( scriptName, conn, jedis, false);
			ArrayList keys = new ArrayList();
			ArrayList args = new ArrayList();
			if( argsObj != null) args.add( argsObj.toString());
			String res = (String) jedis.evalsha( sha1, keys, args);
			if( Validator.isNull( res)) return null;
			logger.debug( "get result of " + scriptName + "(" + argsObj + "):'" + res + "'");
			if( res.startsWith( "{"))
				return new JSONObject( res);
			else if( res.startsWith( "["))
				return new JSONArray( res);
			else
				return res;
		}
		catch( Exception e){
			// TODO Auto-generated catch block
			logger.error( "found error:execRedisLua(" + scriptName + "," + argsObj + ")" + e.getLocalizedMessage());
			throw e;
		}
	}
	
	/**
	 * �������ţ�����Ψһ�Եĵ���
	 * @param table
	 * @return
	 * @throws Exception
	 */
	public static String createDocNo(String table, Connection conn) throws Exception{
		return String.valueOf( PhoneController.getInstance().getIdGenerator().nextId());// UUIDUtils.compressedUuid();
	}
	
	/**
	 * ��ͨ�����������ɾ��keys��������ad_sql#lua:delkeys 
	 * @param pattern keys ����֧�ֵĲ�������
	 * @exception ��� delkeys���صĲ���OK�����Ǵ�����Ϣ
	 */
	public static void deleteRedisKeys(String pattern, Connection conn, Jedis jedis) throws Exception{
		String sha1 = PhoneController.getInstance().getRedisScript( "lua:delkeys", conn, jedis, false);
		ArrayList keys = new ArrayList();
		ArrayList args = new ArrayList();
		args.add( pattern);
		Object res = jedis.evalsha( sha1, keys, args);
		
		logger.debug( "get result of lua:delkeys(" + pattern + "):" + res);
		
		if( !"OK".equals( res)) throw new NDSException( "����ɾ����������:" + res);
		
	}
	
	/**
	 * ��joת��Ϊ�ɴ洢��redis��hash
	 * @param jo should not be null
	 * @return
	 */
	private static Map<String, String> toMap(JSONObject jo){
		HashMap<String, String> map = new HashMap();
		SimpleDateFormat sdf = dateTimeSecondsFormatter.get();//QueryUtils.dateTimeSecondDashFormatter.get();
		Object value;
		String val;
		for( Iterator<String> it = jo.keys(); it.hasNext();){
			String key = it.next();
			value = jo.opt( key);
			if( value instanceof java.util.Date)
				val = sdf.format( (java.util.Date) value);
			else
				val = value.toString();
				
			if( Validator.isNull( val)) val = "";
			
			map.put( key, val);
		}
		return map;
	}
	
	/**
	 * ��ʽ: "yyyy-MM-dd HH:mm:ss"
	 */
	public static ThreadLocal<SimpleDateFormat> dateTimeSecondsFormatter = new ThreadLocal() {
		protected synchronized Object initialValue(){
			SimpleDateFormat a = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss");
			a.setLenient( false);
			return a;
		}
	};
	
	/**
	 * �������ݿ��¼ en='N', �������ɾ��
	 * @param tableName
	 * @param obj ��Ҫ��id����
	 * @return �޸ĵ�����
	 * @throws Exception
	 */
	public static int voidTable(String tableName, JSONObject obj, Connection conn) throws Exception{
		Table table=TableManager.getInstance().getTable(tableName);
		StringBuilder sql = new StringBuilder( "update " + table.getRealName() + " set isactive='N', modifieddate=sysdate,modifierid=? where id=?");
//		StringBuilder sql = new StringBuilder( "update " + tableName + " set en='N', mtime=sysdate,musr=? where id=?");
		ArrayList params = new ArrayList();
		params.add( obj.optLong( "modifierid"));
		params.add( obj.getLong( "id"));
		
		return QueryEngine.getInstance().executeUpdate( sql.toString(), params.toArray(), conn);
		
	}
	
	/**
	 * �����ݿ���ɾ��
	 * @param tableName
	 * @param objectId 
	 * @param conn
	 * @return �޸ĵ�����
	 * @throws Exception
	 */
	public static int deleteTable(String tableName, long objectId, Connection conn) throws Exception{
		Table table=TableManager.getInstance().getTable(tableName);
		String sql = "delete from " + table.getRealName() + " where id=?";
		
		return QueryEngine.getInstance().executeUpdate( sql, new Object[]{ objectId }, conn);
	}
	
	/**
	 * ���������tint�ֶΣ���ȡimg�ֶ����ݣ������redis���ݲ�ͬ����imgͼƬ����ƽ��ɫ
	 * @param tableName
	 * @param dbObj ��Ҫд�����ݿ������
	 * @param oldDBObj ��ǰ�����ݿ��е�����
	 * @throws Exception
	 */
	private static void setTint(String tableName, JSONObject newDBObj, JSONObject oldDBObj) throws Exception{
		TableManager manager = TableManager.getInstance();
		Column col = manager.getColumn( tableName, "tint");
		if( col == null) return;
		String newImg = newDBObj.optString( "img");
		String oldImg = (oldDBObj == null ? null : oldDBObj.optString( "img"));
		if( newImg != null && !newImg.equals( oldImg)) {
			try{
				String tint = ImageMagicController.getInstance().getAvgColorOfImage( newImg);
				newDBObj.put( "tint", tint);
			}
			catch( Throwable tx){
				logger.error( "Fail to set tint for " + tableName, tx);
			}
		}
	}
	
	/**
	 * �����ݿ���д��
	 * @param tableName
	 * @param obj name��Ҫ�����ݿ�һ��
	 * @param conn
	 * @return �޸ĵ�����
	 * @throws Exception
	 */
	public static int insertTable(String tableName, JSONObject obj, Connection conn) throws Exception{
		//tint�����⴦��
		setTint( tableName, obj, null);
		Iterator it = obj.sortedKeys();
		TableManager manager = TableManager.getInstance();
		Table table=manager.getTable(tableName);
		StringBuilder sql = new StringBuilder( "insert into " + table.getRealName() + "(");
		StringBuilder sql2 = new StringBuilder( " values(");
		ArrayList params = new ArrayList();
		while (it.hasNext()){
			String column = (String) it.next();
			Column col = manager.getColumn( tableName, column);
			Object value;
			if( col != null && col.getType() == Column.Type.CLOB) {
				value = obj.get( column);
				if( value instanceof String) value = new StringBuilder( (String) value);
			}
			else{
				if( column.toLowerCase().endsWith( "time")) {
					//�������ͣ��ͻ��˸�ʽyyyy-MM-dd kk:mm:ss
					value = getDate( obj, column);
				}
				else{
					value = obj.get( column);
				}
			}
			//if(col==null) throw new NDSException("not find column:"+ tableName+"."+ column+" in tablemanagger");
			sql.append( col==null?column:col.getRealName() ).append( ",");
			sql2.append( "?,");
			
			params.add( value);
		}
		
		sql.deleteCharAt( sql.length() - 1);
		sql2.deleteCharAt( sql2.length() - 1);
		sql.append( ")").append( sql2).append( ")");
		
		return QueryEngine.getInstance().executeUpdate( sql.toString(), params.toArray(), conn);
		
	}
	
	/**
	 * �޸ı��¼
	 * @param tableName
	 * @param obj
	 * @turn �޸ĵ�����
	 * @throws Exception
	 */
	public static int modifyTable(String tableName, JSONObject obj, Connection conn) throws Exception{
		return modifyTable( tableName, obj, null, conn);
	}
	
	/**
	 * �޸ı��¼
	 * @param tableName
	 * @param obj
	 * @turn �޸ĵ�����
	 * @throws Exception
	 */
	public static int modifyTable(String tableName, JSONObject obj, JSONObject oldDBObj, Connection conn)
	        throws Exception{
		//tint�����⴦��
		if( oldDBObj != null) setTint( tableName, obj, oldDBObj);
		Iterator it = obj.sortedKeys();
		TableManager manager = TableManager.getInstance();
		Table table=manager.getTable(tableName);
		StringBuilder sql = new StringBuilder( "update " + table.getRealName() + " set ");
		
		ArrayList params = new ArrayList();
		while (it.hasNext()){
			String column = (String) it.next();
			if( "id".equalsIgnoreCase( column)) continue;
			Column col = manager.getColumn( tableName, column);
			Object value;
			if( col != null && col.getType() == Column.Type.CLOB) {
				value = obj.get( column);
				if( value instanceof String) value = new StringBuilder( (String) value);
			}
			else{
				if( column.toLowerCase().endsWith( "time")) {
					//�������ͣ��ͻ��˸�ʽyyyy-MM-dd kk:mm:ss
					value = getDate( obj, column);
				}
				else{
					value = obj.get( column);
				}
			}
			sql.append( col.getRealName()).append( "=?,");
			
			params.add( value);
		}
		sql.deleteCharAt( sql.length() - 1);
		
		sql.append( " where id=?");
		params.add( obj.getLong( "id"));
		
		return QueryEngine.getInstance().executeUpdate( sql.toString(), params.toArray(), conn);
		
	}
	
	private static long getLong(JSONObject jo, String name) throws Exception{
		long value = jo.optLong( name, -1);
		if( value == -1) throw new NDSException( name + " not found in event");
		return value;
	}
	
	private static int getInt(JSONObject jo, String name) throws Exception{
		int value = jo.optInt( name, -1);
		if( value == -1) throw new NDSException( name + " not found in event");
		return value;
	}
	
	private static String getString(JSONObject jo, String name) throws Exception{
		String value = jo.optString( name);
		if( Validator.isNull( value)) throw new NDSException( name + " not found in event");
		return value;
	}
	
	/**
	 * 
	 * @param jo yyyy-MM-dd kk:mm:ss
	 * @param name
	 * @return
	 * @throws Exception
	 */
	private static java.util.Date getDate(JSONObject jo, String name) throws Exception{
		Object v = jo.opt( name);
		if( v == null) return null;
		if( v instanceof java.util.Date) return (java.util.Date) v;
		
		String value = v.toString();
		if( Validator.isNull( value)) return null;
		return dateTimeSecondsFormatter.get().parse( value);
	}
	
	/**
	 * ��ȡ���uuid, ����redis��У������ԣ�ֱ��û���ظ���
	 * @param template ��ʽ: "usr:1324:{0}", ��ʹ��MessageFormatȥ��ʽ������uuid�滻{0}, ����redis�м��
	 * @return uuid��ֵ������jedis.key
	 * @throws Exception
	 */
	public static String createDistinctUUID(String template) throws Exception{
		String token = UUIDUtils.compressedUuid();
		String key = MessageFormat.format( template, token);
		return key;
	}
	
	/**
	 * �����ڴ��еĲ���
	 * @param name
	 * @param value, true if altered 
	 * @return
	 */
	public static boolean alterConfigValues(Configurations conf, String name, String value){
		String oldValue = conf.getProperties().getProperty( name);
		if( !value.equals( oldValue) && !value.contains( "$rcpdir")/*$rcpdir will not overwrite*/) {
			conf.getProperties().setProperty( name, value);
			return true;
		}
		return false;
	}
	
	/**
	 * ��PhoneConfig��public static ���������ݿ�(ConfigValues)�ж�Ӧ��ֵ�����û���ҵ�������null,
	 * 
	 * @param name ��PhoneConfig�е�ֵ�Ķ�Ӧ����. -> _, ��д
	 * @param value ��Ҫ���ǽ���Ϊ����ʵ��ֵ
	 * @return true if altered
	 */
	public static boolean alterPhoneConfigValue(String name, String value) throws Exception{
		if( !name.startsWith( "phone.")) return false;
		Field field = null;
		try{
			name = name.substring( 5).replace( '.', '_').toUpperCase();
			field = PhoneConfig.class.getField( name);
		}
		catch( NoSuchFieldException ex){
			logger.debug( "not found name " + name + " in PhoneConfig");
			return false;
		}
		int modifier = field.getModifiers();
		if( !Modifier.isPublic( modifier)) return false;
		if( !Modifier.isStatic( modifier)) return false;
		if( Modifier.isFinal( modifier)) return false;
		
		//ok
		Object oldValue = field.get( null);
		boolean valueChanged = false;
		Class ftype = field.getType();
		if( int.class.equals( ftype)) {
			int oldint = field.getInt( null);
			int newint = Tools.getInt( value, oldint);
			if( oldint != newint) {
				field.setInt( null, newint);
				valueChanged = true;
			}
		}
		else if( String.class.equals( ftype)) {
			if( !value.equals( oldValue)) {
				field.set( null, value);
				valueChanged = true;
			}
		}
		else if( boolean.class.equals( ftype)) {
			boolean oldb = field.getBoolean( null);
			boolean newb = Tools.getBoolean( value, oldb);
			if( oldb != newb) {
				field.setBoolean( null, newb);
				valueChanged = true;
			}
		}
		else if( long.class.equals( ftype)) {
			long oldlong = field.getLong( null);
			long newlong = Tools.getLong( value, oldlong);
			if( oldlong != newlong) {
				field.setLong( null, newlong);
				valueChanged = true;
			}
		}
		else
			throw new NDSException( "Unsupported field type " + ftype.getName() + " of PhoneConfig." + field.getName());
			
		if( valueChanged) logger.info( "set PhoneConfig." + name + " from " + oldValue + " to " + value);
		
		return valueChanged;
	}
	/**
	 * @return 4λ�����
	 */
	public static String createCheckCode(){
		StringBuffer sbCode = new StringBuffer();
		String str = "0123456789";
		Random r = new Random();
		//��֤�볤��
		int count = 4;
		for( int i = 0; i < count; i++){
			int num = r.nextInt( str.length());
			sbCode.append( str.charAt( num));
			str = str.replace( (str.charAt( num) + ""), "");
		}
		String checkCode = sbCode.toString();
		return checkCode;
	}
	/**
	 * �����ֻ���֤��
	 * @param phoneNum �ֻ���
	 * @param templateId ģ��id
	 * @return{
	 * 		checkcode:"2534"  --4λ�����
	 * 		code��0�����ɹ���1����ʧ�ܵ���������ٴη��ͣ�2����ʧ���ҵ��첻���ٴη���
	 *      msg:"�ɹ���ʧ�ܵ���ʾ��Ϣ"
	 * } 
	 * @throws Exception
	 */
	public static JSONObject sendSMS(String phoneNum, String checkCode,int templateId,Jedis jedis,JSONArray data) throws Exception{
		JSONObject sendObj = new JSONObject();
		
		sendObj.put( "templateId", templateId);
		sendObj.put( "datas", data);
		YunTXSMSSender sms = new YunTXSMSSender();
		JSONObject ret = new JSONObject();
		ret = sms.sendBatchMessage( String.valueOf( sendObj), phoneNum);
		
		//д��redis
		String key="checkcode:"+ phoneNum;
		HashMap<String, String> hash=new HashMap();
		hash.put("checkcode", checkCode	);
		hash.put("fail","0");
		jedis.hmset(key, hash);
		jedis.expire(key, 300);
		logger.debug("�����ֻ���֤��------------���룺"+phoneNum+"--��֤�룺"+checkCode);
		return ret;
	}
	
	/**
	 * ���¼���ָ���̼Ҷ�����Ϣ
	 * 
	 * @param comid
	 * @throws Exception
	 */
	public static void reloadOrderInfo(long comid, Jedis jedis, Connection conn) throws Exception{
		JSONArray order = QueryEngine.getInstance().doQueryObjectArray(
		        "select status,count(1) cnt from spo where i_com_id=? and otype='INL' group by status",
		        new Object[]{ comid }, conn);
		HashMap<String, String> orderMap = new HashMap();
		if( order.length() == 0) {
			// ������
			jedis.hdel( "com:" + comid + ":spostatus", "WP");
			// ������
			jedis.hdel( "com:" + comid + ":spostatus", "AP");
			// ���ջ�
			jedis.hdel( "com:" + comid + ":spostatus", "AS");
		}
		else{
			for( int i = 0; i < order.length(); i++){
				JSONObject obj = order.getJSONObject( i);
				orderMap.put( obj.getString( "status"), obj.getString( "cnt"));
			}
			// ���redis�еĿ����Ϣ
			jedis.hmset( "com:" + comid + ":spostatus", orderMap);
			if( null == orderMap.get( "WP")) {
				jedis.hdel( "com:" + comid + ":spostatus", "WP");
			}
			
			if( null == orderMap.get( "AP")) {
				jedis.hdel( "com:" + comid + ":spostatus", "AP");
			}
			
			if( null == orderMap.get( "AS")) {
				jedis.hdel( "com:" + comid + ":spostatus", "AS");
			}
		}
	}
	
	/**
	 * ����clob
	 * @param obj
	 * @return
	 * @throws SQLException
	 */
	public static Object convertClob(Object obj) throws SQLException{
		if( obj instanceof java.sql.Clob) {
			obj = ((java.sql.Clob) obj).getSubString( 1, (int) ((java.sql.Clob) obj).length());
		}
		return obj;
	}
	/**
	 * setup cookie and redis key: "usr:$id:$token" - token: newly created one
	 * @param id users.id
	 * @param jo initlized user obj (redis usr:$id)
	 * @return token that client should cache, format: "$id:$token"
	 * @throws Exception
	 */
	public static String setupCookie(int id, JSONObject jo, HttpServletRequest req,HttpServletResponse res, Jedis jedis, Connection conn ) throws Exception{
		//�������1�������table manager ���ز��ɹ�������Ҫ����cookie �޷������������޷����� table manager(���ط�����Ҫ�����cookie ������
		boolean isTableInit=TableManager.getInstance().isInitialized();
		
		JSONObject usr=isTableInit? PhoneUtils.getRedisObj("usr", id,  conn, jedis):new JSONObject();
		String key=null;
		String token;
		while(true){
			token=UUIDUtils.compressedUuid();//   PhoneUtils.createDistinctUUID("usr:"+id+":{0}");
			//��redis��ȡ�û���Ϣ
			key="usr:"+id+":"+token;
			if(jedis.hsetnx(key, "remoteip", req.getRemoteAddr())==0)continue;
			jedis.hsetnx(key, "agent", req.getHeader("User-Agent"));
			//���ڷ������ж�sessionͬһ���û������⣬�û����л�lang��ʱ������session����Ӱ��
			jedis.hsetnx(key, "lang_id",String.valueOf( usr.optInt("lang_id", LanguageManager.getInstance().getDefaultLangId())));
			
			break;
		}
		jedis.expire(key,  PhoneConfig.COOKIE_TIMEOUT);
		logger.debug("jedis key "+ key +" ttl "+ jedis.ttl(key));
		//����cookie
		String clientToken=id+":"+token;
		Cookie cookie=new Cookie("token", clientToken);
		CookieKeys.addCookie(req, res, cookie, false, PhoneConfig.COOKIE_TIMEOUT);
		
		usr.put("token", clientToken);
		if(isTableInit)PhoneUtils.toPrimeType(TableManager.getInstance().getTable("usr"),  usr);
		return clientToken;
	}	
}
