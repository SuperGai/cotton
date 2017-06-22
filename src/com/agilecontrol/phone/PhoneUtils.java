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
 * 工具类
 * 
 * @author yfzhu
 *
 */
@Admin(mail="wang.cun@lifecycle.cn")
public class PhoneUtils {
	private static final Logger logger = LoggerFactory.getLogger( PhoneUtils.class);
	
	/**
	 * 找到用户在指定com下的emp, 建立 usr:$uid:emp 里面是hash, key: comid, value: empid
	 * 在需要列出用户的所有商家的时候，可以按empid的顺序做一次排序获得(切换店铺的时候）
	 * 这个值是在员工入职的时候维护的（删除）
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
	 * @param colNames  字段名称可以用英文逗号分隔
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
	 * 返回指定ids集合对应的对象列表
	 * 调用通用的方法实现列表中每个id的对象获取，如果返回的列表中没有，表示对象不存在redis，需要尝试从数据库读取
	 * @param tableName
	 * @param ids elements: Long
	 * @useDBWhenNotFound 是否需要到db中去尝试获取指定id，如果是，将调用getRedisObj
	 * @param vc
	 * @param conn
	 * @param jedis
	 * @return 元素为JSONObject
	 * @throws Exception
	 */
	public static JSONArray getRedisObjectArray(String tableName, JSONArray ids, ArrayList<Column> columns,
	        boolean useDBWhenNotFound, Connection conn, Jedis jedis) throws Exception{
		if( columns == null || columns.size() == 0 || !columns.get( 0).getName().equals( "id"))
		    throw new NDSException( tableName+"不正确的columns设置，首个元素必须是id");
			
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
		
		//检查每个元素的存在性，维护redis的一致性
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
					//					throw new ObjectNotFoundException("数据未找到(talbe="+tableName+",id="+objectId+")");
				}
				String key = tableName + ":" + objectId;
				jedis.hmset( key, toMap( joFull));
				
				jo = new JSONObject();
				for( Column col : columns){
					jo.put( col.getName(), joFull.opt( col.getName()));
					//这里是特殊处理，如果客户端需要强制某FK字段再向下取出对象，而不是到此为止，可以配置字段的扩展属性:"fetchobj:true"来实现
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
					//这里是特殊处理，如果客户端需要强制某FK字段再向下取出对象，而不是到此为止，可以配置字段的扩展属性:"fetchobj:true"来实现
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
	 * 循环下去拿对象，直到没有字段需要再拿上来
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
	 * 循环下去拿对象，直到没有字段需要再拿上来
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
	 * 创建一个空的对象，需要有fk表的属性(display key)，例如：usr表的dks: ["name","phone"]，则输出结果:
	 * {id, name,phone} 其中id是必配 
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
	 * 方法重载
	 * Convert throwable message to simple one. For instance, message from db contains ora-
	 * Will use com.agilecontrol.nea.core.util.MessagesHolder to do internal wildcard replacement
	 * 
	 * 1、获取异常消息，当含有“ORA-”消息时，进行分支处理:1,含有 00001，处理方法已定义;2,其他“ORA-”情况进行统一处理;
	 * 2、当异常信息匹配到    ORA20000  到  ORA29999 时，为数据库定义的异常，直接返回信息;
	 * 3、当 异常既不是  NDSException 也不是 NDSRuntimeException时，即其他异常，此时进行统一处理
	 * 4、通过 3、的 删选 ,现在 还剩下 NDSException 或者  NDSRuntimeException 的情况，为java定义异常，直接返回信息
	 * 注:第三第四分支情况可以进行调换，调换时注意删选条件的转换
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
			//当异常为  ORA20000到ORA29999 时
			r = s;
		}
		else if( (!(t instanceof NDSException)) && (!(t instanceof NDSRuntimeException))) {
			//当异常不是 NDSException, NDSRuntimeException时，对异常的处理
			r = parseOtherError( params,conn,jedis, r, t);
		}
		else{
			//此处为 NDSException 或  NDSRuntimeException，若需处理，可在此处修改
			r = s;
		}
		return r;
	}
	
	/**
	 * 处理其他异常
	 * 将当前异常绑定error_id，并调用 redis 消息推送消息接口
	 * error_id来源于racl 的 sequence:seq_errorid
	 * @param params 用于存放信息, 如:error_id,用户相关信息 等,如需要,可以可以自定义组装内容 
	 * @param conn
	 * @param jedis
	 * @param msg 消息，从异常消息中提取出来的字符串
	 * @param throwable
	 * @return msg 组装的信息，用户返回界面显示
	 */
	private static String parseOtherError(JSONObject params,Connection conn,Jedis jedis, String msg, Throwable throwable){		
			//获取sequence
			long errorId =-1;
			try{
				errorId = PhoneController.getInstance().getNextId( "errorid", conn);
				msg = "糟糕，服务出现异常。请检查数据，或稍后再试（错误号:" + errorId+")";
				params.put( "errorid", errorId);
			}
			catch( Exception e){
				logger.error( e.getLocalizedMessage(), e);
			}
			logger.error( "error_" + errorId, throwable);
			
			//构造json对象，将需要用到的数据传递到其他函数中;
			//params用于传送参数   如: error_i 和 port
			
			//调用消息推送接口
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
		String errorMsg = "数据已存在";//+idxName+"";
		try{
			// skip ad_client_id column
			List al = com.agilecontrol.nea.core.query.QueryEngine.getInstance().doQueryList(
			        "select table_name, column_name from USER_IND_COLUMNS where column_name<>'AD_CLIENT_ID' and index_name="
			                + QueryUtils.TO_STRING( indexName));
			if( al.size() > 0) {
				StringBuilder sb = new StringBuilder( "数据已存在，请检查输入项:");
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
	 * 遍历throwable的stacktrace，找到第一个含有AdminAdmin注解的类，返回对应email地址.如果都没有Admin注解，返回空
	 * 
	 * 查找方法注解，若有，覆盖类注解，返回方法注解的email地址；没有返回类注解的email地址
	 * 取消了面向方法的admin搜索，因为效率低下，getDeclaredMethods 目前采用遍历的方式，影响系统性能。未来的改进可以考虑：
	 * 在异步线程中处理stackTrace，而不是在同步线程中识别出admin mail (yfzhu)
	 * 
	 * @param throwable 异常信息
	 * @author li.shuhao
	 * @return email地址, null if not found
	 */
	@Admin(mail="li.shuhao@lifecycle.cn")
	public static String getAdminMailFromFirstAnnotation(Throwable throwable) {
		String mailTo=null;
		for(StackTraceElement st: throwable.getStackTrace()){
			//遍历异常堆栈信息st，获得st中的每一个异常类
			Class cls=null;
			try {
				cls = Class.forName(st.getClassName());
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
				continue;
			}				
			//得到该异常类名，判断异常类中的注解是否为空
			Annotation ann= cls.getAnnotation(Admin.class);
			if(ann!=null){
				mailTo = ((Admin)ann).mail();
				/**
				 * 目前去除以提升定位性能
				 */
//				//遍历异常类中的方法，判断异常堆栈中类的方法有哪些报错的
//				for(Method method: cls.getDeclaredMethods()){
//					if(method.getName().equals(st.getMethodName())){
//						//找到出现异常方法的注解，然后得到其参数值
//						Annotation mann=method.getAnnotation(Admin.class);
//						//判断方法中有没有注解，若是有的话，查找我们想要的
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
	 * 针对每行进行转换
	 * Read each value of jo key, and parse to prime type according to column type
	 * @param table
	 * @param ja 直接在element对象上进行转换
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
	 * @param jo 直接在这个对象上进行转换
	 * @return jo 即jo
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
	 * 按指定columns的field值返回
	 * @param tableName
	 * @param ids
	 * @param colNames  字段名称可以用英文逗号分隔
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
	 * 按指定columns的field值返回, 需要全部返回redis缓存fields需要用不带columns参数的函数
	 * 如果jedis key 规则: $tablename$:$objectId,  (value: hset类型)不存在，将从数据库中用 <tableName>:obj对应语句创建对象，并返回
	 * @param tableName
	 * @param objectId
	 * @param columns 注意要保证第一个字段是id，这样如果没查询到，将在第一个返回值发现为nil （其他值可能nil是正确，id为nil必定表示不存在)
	 * @param vc
	 * @param conn
	 * @param jedis
	 * @return 创建一个新JSONObject，具体字段由ad_sql#$tableName:obj 定义
	 * @throws Exception 如果对象不存在数据库，将抛出ObjedctNotFoundException
	 */
	public static JSONObject getRedisObj(String tableName, long objectId, ArrayList<Column> columns, 
	        Connection conn, Jedis jedis) throws Exception{
		if( objectId <= 0) return null;
		//		logger.debug("columns="+ Tools.toString(columns, ","));
		if( columns == null || columns.size() == 0 || !columns.get( 0).getName().equals( "id"))
		    throw new NDSException( "不正确的columns设置，首个元素必须是id");
		
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
				throw new ObjectNotFoundException( "数据未找到(table=" + tableName + ",id=" + objectId + ")");
			}
			jedis.hmset( key, toMap( joFull));
			
			jo = new JSONObject();
			for( Column col : columns){
				jo.put( col.getName(), joFull.opt( col.getName()));
				//这里是特殊处理，如果客户端需要强制某FK字段再向下取出对象，而不是到此为止，可以配置字段的扩展属性:"fetchobj:true"来实现
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
				//这里是特殊处理，如果客户端需要强制某FK字段再向下取出对象，而不是到此为止，可以配置字段的扩展属性:"fetchobj:true"来实现
				if( col.getFkTable() != null && Boolean.TRUE.equals( col.getJSONProp( "fetchobj"))) {
					long foId = Tools.getLong( v, -1);
					jo.put( col.getName(), fetchObject( col.getFkTable(), foId,  conn, jedis));
				}
				
			}
		}
		return toPrimeType( table, jo);
		
	}
	
	/**
	 * 返回全部的redis 对象属性
	 * 如果jedis key 规则: $tablename$:$objectId,  (value: hset类型)不存在，将从数据库中用 <tableName>:obj对应语句创建对象，并返回
	 * @param tableName
	 * @param objectId
	 * @param vc
	 * @param conn
	 * @param jedis
	 * @return 创建一个新JSONObject，具体字段由ad_sql#$tableName:obj 定义
	 * @throws Exception 如果对象不存在数据库，将抛出ObjedctNotFoundException
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
//				throw new ObjectNotFoundException( "数据未找到");
//			}
//			jedis.hmset( key, toMap( jo));
//		}
//		else{
//			jo = new JSONObject( values);
//		}
//		return toPrimeType(table, jo);
		
	}
	
	/**
	 * 将简单对象添加到Redis中, 如果表上定义了rac，还将呼叫相应的lua代码
	 * 构造的sql语句是: select * from <table> where id=?
	 * @param objectId
	 * @param table
	 * @param argsObj
	 * @param conn
	 * @param jedis
	 * @return jedis对象
	 * @throws Exception
	 */
	public static JSONObject addRedisObj(long objectId, Table table, JSONObject argsObj, Connection conn, Jedis jedis)
	        throws Exception{
			
		String scriptName = table.getRedisAC();//ad_sql#name
		return setRedisObj( objectId, table, scriptName, argsObj, conn, jedis);
	}
	
	/**
	 * 将简单对象更新到Redis中, 如果表上定义了ram，还将呼叫相应的lua代码
	 * 构造的sql语句是: select * from <table> where id=?
	 * @param objectId
	 * @param table
	 * @param argsObj 参数，如 {comid, stid, uid, objectid}
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
	 * 将简单对象更新到Redis中, 如果表上定义了ram，还将呼叫相应的lua代码
	 * 构造的sql语句是: select * from <table> where id=?
	 * @param objectId
	 * @param table
	 * @param argsObj 参数，如 {comid, stid, uid, objectid}
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
	 *  将简单对象更新到Redis中, 如果scriptName不为空，还将呼叫相应的lua代码
	 *  scriptName 对应于ad_sql#name，系统将进行sha1缓存计算，以提高运行性能
	 * @param objectId
	 * @param table
	 * @param scriptName ad_sql#name, 取其内容作为script运行
	 * @param argsObj 参数，如 {comid, stid, uid, objectid}
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
		    throw new NDSException( "Redis存储异常:" + ret + "(" + table.getName() + ":" + objectId + ")");
		if( Validator.isNotNull( scriptName)) {
			JSONObject ojo = (JSONObject) execRedisLua( scriptName, argsObj, conn, jedis);
			if( ojo != null) jo = ojo;
		}
		
		return jo;
	}
	/**
	 * 表的字段有别名，需要返回 .., name aliasname, .. 这样的内容，构造到select 语句中
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
	 * 运行Lua脚本，脚本在ad_sql.scriptName指定的脚本中定义，输入参数: JSONObject, 返回JSONObject
	 * @param scriptName ad_sql.name
	 * @param args 脚本接受的参数，例如: {comid, stid, uid, objectid}, 允许为null，需要和lua约定
	 * @param jedis
	 * @return JSONObject或JSONArray, 约定lua返回jsonobject是客户端要求的格式，lua内部返回的是String cjson.encode(result), 需要再次解析
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
	 * 创建单号，具有唯一性的单号
	 * @param table
	 * @return
	 * @throws Exception
	 */
	public static String createDocNo(String table, Connection conn) throws Exception{
		return String.valueOf( PhoneController.getInstance().getIdGenerator().nextId());// UUIDUtils.compressedUuid();
	}
	
	/**
	 * 用通配符参数批量删除keys，将调用ad_sql#lua:delkeys 
	 * @param pattern keys 命令支持的参数类型
	 * @exception 如果 delkeys返回的不是OK，就是错误信息
	 */
	public static void deleteRedisKeys(String pattern, Connection conn, Jedis jedis) throws Exception{
		String sha1 = PhoneController.getInstance().getRedisScript( "lua:delkeys", conn, jedis, false);
		ArrayList keys = new ArrayList();
		ArrayList args = new ArrayList();
		args.add( pattern);
		Object res = jedis.evalsha( sha1, keys, args);
		
		logger.debug( "get result of lua:delkeys(" + pattern + "):" + res);
		
		if( !"OK".equals( res)) throw new NDSException( "批量删除遇到错误:" + res);
		
	}
	
	/**
	 * 将jo转换为可存储到redis的hash
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
	 * 格式: "yyyy-MM-dd HH:mm:ss"
	 */
	public static ThreadLocal<SimpleDateFormat> dateTimeSecondsFormatter = new ThreadLocal() {
		protected synchronized Object initialValue(){
			SimpleDateFormat a = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss");
			a.setLenient( false);
			return a;
		}
	};
	
	/**
	 * 更新数据库记录 en='N', 并非真的删除
	 * @param tableName
	 * @param obj 需要有id属性
	 * @return 修改的行数
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
	 * 在数据库中删除
	 * @param tableName
	 * @param objectId 
	 * @param conn
	 * @return 修改的行数
	 * @throws Exception
	 */
	public static int deleteTable(String tableName, long objectId, Connection conn) throws Exception{
		Table table=TableManager.getInstance().getTable(tableName);
		String sql = "delete from " + table.getRealName() + " where id=?";
		
		return QueryEngine.getInstance().executeUpdate( sql, new Object[]{ objectId }, conn);
	}
	
	/**
	 * 如果表中有tint字段，读取img字段内容，如果和redis内容不同，将img图片计算平均色
	 * @param tableName
	 * @param dbObj 将要写入数据库的内容
	 * @param oldDBObj 当前在数据库中的内容
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
	 * 在数据库中写入
	 * @param tableName
	 * @param obj name需要和数据库一致
	 * @param conn
	 * @return 修改的行数
	 * @throws Exception
	 */
	public static int insertTable(String tableName, JSONObject obj, Connection conn) throws Exception{
		//tint的特殊处理
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
					//日期类型，客户端格式yyyy-MM-dd kk:mm:ss
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
	 * 修改表记录
	 * @param tableName
	 * @param obj
	 * @turn 修改的行数
	 * @throws Exception
	 */
	public static int modifyTable(String tableName, JSONObject obj, Connection conn) throws Exception{
		return modifyTable( tableName, obj, null, conn);
	}
	
	/**
	 * 修改表记录
	 * @param tableName
	 * @param obj
	 * @turn 修改的行数
	 * @throws Exception
	 */
	public static int modifyTable(String tableName, JSONObject obj, JSONObject oldDBObj, Connection conn)
	        throws Exception{
		//tint的特殊处理
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
					//日期类型，客户端格式yyyy-MM-dd kk:mm:ss
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
	 * 获取随机uuid, 并在redis中校验存在性，直到没有重复的
	 * @param template 格式: "usr:1324:{0}", 将使用MessageFormat去格式化，将uuid替换{0}, 并在redis中检查
	 * @return uuid的值，将是jedis.key
	 * @throws Exception
	 */
	public static String createDistinctUUID(String template) throws Exception{
		String token = UUIDUtils.compressedUuid();
		String key = MessageFormat.format( template, token);
		return key;
	}
	
	/**
	 * 更新内存中的参数
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
	 * 猜PhoneConfig的public static 变量在数据库(ConfigValues)中对应的值，如果没有找到，返回null,
	 * 
	 * @param name 在PhoneConfig中的值的对应规则：. -> _, 大写
	 * @param value 需要考虑解析为各种实际值
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
	 * @return 4位随机码
	 */
	public static String createCheckCode(){
		StringBuffer sbCode = new StringBuffer();
		String str = "0123456789";
		Random r = new Random();
		//验证码长度
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
	 * 发送手机验证码
	 * @param phoneNum 手机号
	 * @param templateId 模板id
	 * @return{
	 * 		checkcode:"2534"  --4位随机数
	 * 		code：0——成功，1——失败但当天可以再次发送，2——失败且当天不能再次发送
	 *      msg:"成功或失败的提示信息"
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
		
		//写入redis
		String key="checkcode:"+ phoneNum;
		HashMap<String, String> hash=new HashMap();
		hash.put("checkcode", checkCode	);
		hash.put("fail","0");
		jedis.hmset(key, hash);
		jedis.expire(key, 300);
		logger.debug("发送手机验证码------------号码："+phoneNum+"--验证码："+checkCode);
		return ret;
	}
	
	/**
	 * 重新计算指定商家订单信息
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
			// 待付款
			jedis.hdel( "com:" + comid + ":spostatus", "WP");
			// 待发货
			jedis.hdel( "com:" + comid + ":spostatus", "AP");
			// 待收货
			jedis.hdel( "com:" + comid + ":spostatus", "AS");
		}
		else{
			for( int i = 0; i < order.length(); i++){
				JSONObject obj = order.getJSONObject( i);
				orderMap.put( obj.getString( "status"), obj.getString( "cnt"));
			}
			// 变更redis中的库存信息
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
	 * 解析clob
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
		//这里会有1种情况：table manager 加载不成功，但不要导致cookie 无法建立，进而无法重载 table manager(重载方法需要先完成cookie 构建）
		boolean isTableInit=TableManager.getInstance().isInitialized();
		
		JSONObject usr=isTableInit? PhoneUtils.getRedisObj("usr", id,  conn, jedis):new JSONObject();
		String key=null;
		String token;
		while(true){
			token=UUIDUtils.compressedUuid();//   PhoneUtils.createDistinctUUID("usr:"+id+":{0}");
			//从redis获取用户信息
			key="usr:"+id+":"+token;
			if(jedis.hsetnx(key, "remoteip", req.getRemoteAddr())==0)continue;
			jedis.hsetnx(key, "agent", req.getHeader("User-Agent"));
			//由于服务器有多session同一个用户的问题，用户在切换lang的时候，其他session不受影响
			jedis.hsetnx(key, "lang_id",String.valueOf( usr.optInt("lang_id", LanguageManager.getInstance().getDefaultLangId())));
			
			break;
		}
		jedis.expire(key,  PhoneConfig.COOKIE_TIMEOUT);
		logger.debug("jedis key "+ key +" ttl "+ jedis.ttl(key));
		//设置cookie
		String clientToken=id+":"+token;
		Cookie cookie=new Cookie("token", clientToken);
		CookieKeys.addCookie(req, res, cookie, false, PhoneConfig.COOKIE_TIMEOUT);
		
		usr.put("token", clientToken);
		if(isTableInit)PhoneUtils.toPrimeType(TableManager.getInstance().getTable("usr"),  usr);
		return clientToken;
	}	
}
