package com.agilecontrol.phone;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.StringWriter;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;

import com.agilecontrol.b2b.schema.TableManager;
import com.agilecontrol.nea.core.control.ejb.command.GetObject;
import com.agilecontrol.nea.core.query.QueryEngine;
import com.agilecontrol.nea.core.query.QueryUtils;
import com.agilecontrol.nea.core.query.SPResult;
import com.agilecontrol.nea.core.schema.Table;
import com.agilecontrol.nea.core.util.ConfigValues;
import com.agilecontrol.nea.core.velocity.VelocityUtils;
import com.agilecontrol.nea.core.web.config.QueryListConfig;
import com.agilecontrol.nea.core.web.config.QueryListConfigManager;
import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.nea.util.StringUtils;
import com.agilecontrol.nea.util.Tools;
import com.agilecontrol.nea.util.Validator;
import com.agilecontrol.phone.util.PhoneQLC;


/**
 * 
 * singleton
 * @author yfzhu
 *
 */
@Admin(mail="wang.cun@lifecycle.cn")
public class PhoneController {
	private static final Logger logger = LoggerFactory.getLogger(PhoneController.class);
	
	private static  PhoneController instance;
	/**
	 * Packages for looking up CmdHandler class
	 */
	private String[] cmdHandlerPackages;
	
	private PhoneController(){
	}

	/**
	 * key: ad_sql.name, value: String or json(JSONArray/JSONObject)
	 * 包括其他类型的缓存也放在这里
	 */
	private Hashtable<String, Object> cacheValues=new Hashtable();
	
	/**
	 * 校验服务器配置是否正常
	 */
	private Boolean isServerOK=null;
	
	/**
	 * 单号生成器
	 */
	private IdGenerator idWorker;
	
	
	/**
	 * 在Redis中运行的脚本，有sha1需要临时计算
	 * @author yfzhu
	 *
	 */
	private static class LuaScript{
		/** ad_sql.value */
		String code;
		/** lua sha1 */
		String sha1;
		/** ad_sql.name */
		String name;
	}
	
	
	
	/**
	 * 检查服务器配置是否正常，比如：当前服务器的phone.id_worker需要在redis中是唯一的
	 * 做法: 读取phone.id_worker （1~1023)，将这个值作为 server:$idworker 在jedis中读取string，配置格式为: host:port 格式，
	 * 如果内容和当前服务器的配置不一致，就认为是有混用id_worker的情况，将抛出错误
	 * @param request
	 * @throws Exception
	 */
	public void checkServerOK(HttpServletRequest request, Jedis jedis, Connection conn) throws NDSException{
//		if(this.isServerOK==null){
//			if( PhoneConfig.ID_WORKER > 1023 ||  PhoneConfig.ID_WORKER<1) throw new NDSException("phone.id_worker未配置");
//			String localName=request.getLocalName();
//			int port=request.getLocalPort();
//			String key="server:"+ PhoneConfig.ID_WORKER;
//			
//			String value=jedis.get(key);
//			String myval=localName+":"+port;
//			if(Validator.isNull(value)){
//				//set to redis
//				jedis.set(key, myval);
//				isServerOK=true;
//			}else{
//				boolean sok=value.equalsIgnoreCase(myval);
//				if(!sok){
//					logger.error( "jedis "+ key+"="+ value+"!="+ myval);
//					throw new  NDSException("服务器配置不正确，请通知管理员");
//				}else{
//					isServerOK=true;
//				}
//			}
//		}
//		if(!this.isServerOK){
//			logger.error("server not ok: id_worker="+ PhoneConfig.ID_WORKER+ ", val="+   request.getLocalName()+":"+ request.getLocalPort()+ ", redisval="+ jedis.get(key);
//			//throw new NDSException("服务器配置不正确，请通知管理员");
//		}
	}
	/**
	 * 读取ad_sql#name 对应的脚本，并确认是否已经生成sha1, 如果没有，将load到jedis，然后返回sha1
	 * @param adsqlName
	 * @param conn
	 * @param jedis 
	 * @param force 是否强制重载脚本从数据库
	 * @return sha1 of jedis 
	 * @throws Exception
	 */
	public String getRedisScript(String adsqlName, Connection conn, Jedis jedis, boolean force) throws Exception{
		LuaScript script=force?null:(LuaScript)cacheValues.get(adsqlName);
		if(script==null){
			String sql= "select value from ad_sql where name=?";
			String code= QueryUtils.parseClobOrString(QueryEngine.getInstance().doQueryOne(sql, new Object[]{adsqlName}, conn));
			if(Validator.isNull(code)) throw new NDSException("ad_sql#"+adsqlName+"未定义");
			String sha1=jedis.scriptLoad(code);
			script=new LuaScript();
			script.code=code;
			script.sha1=sha1;
			script.name=adsqlName;
			cacheValues.put(adsqlName, script);
		}
		
		return script.sha1;
	}
	
	
	/**
	 * Load value from ad_sql, not only sqls,but also json objects
	 * @param name
	 * @param conn
	 * @return
	 * @throws Exception
	 */
	public String getValueFromADSQL(String name) throws Exception{
		Connection conn=null;
		try{
			conn=QueryEngine.getInstance().getConnection();
			return getValueFromADSQL(name,conn);
		}finally{
			try{conn.close();}catch(Throwable tx){}
		}
	}
	
	
	/**
	 * Query list config, 如果未定义，将在内部创建, 客户端统一都要phone的配置，这里将通过类PhoneQLC完成自动创建
	 * @param table
	 * @param configName
	 * @throws Exception
	 */
	public QueryListConfig getQueryListConfig(Table table, String configName, Locale locale) throws Exception{
		QueryListConfig qlc= QueryListConfigManager.getInstance().getQueryListConfig(table.getId(), configName);
		if(qlc==null){
			String key= "qlc_"+table.getId()+"_"+configName+"_"+locale.toString();
			qlc= (QueryListConfig)cacheValues.get(key);
			if(qlc==null){
				PhoneQLC pqlc=new PhoneQLC(table.getId(), locale);
				qlc=pqlc.getQLC();
				cacheValues.put(key, qlc);
			}
		}
		
		return qlc;
	}
	
	/**
	 * Load value from ad_sql, not only sqls,but also json objects
	 * @param name
	 * @param conn
	 * @return
	 * @throws Exception
	 */
	public String getValueFromADSQL(String name,Connection conn) throws Exception{
		String conf=null;
		if(!PhoneConfig.DEBUG) conf= (String)cacheValues.get(name);
		if(conf==null){
			String sql= "select value from ad_sql where name=?";
			conf= QueryUtils.parseClobOrString(QueryEngine.getInstance().doQueryOne(sql, new Object[]{name}, conn));
			if(!PhoneConfig.DEBUG){
				cacheValues.put(name, conf);
			}
		}
		return conf;
		
	}
	
	/**
	 *  Load value from ad_sql as json object or array
	 * @param name
	 * @param defaultValue 如果没有找到结果，并且定义了做缓存(PhoneConfig.DEBUG=false)，将以defaultValue缓存
	 * @param connisJSON
	 * @return (JSONArray/JSONObject)
	 * @throws Exception
	 */
	public Object getValueFromADSQLAsJSON(String name, Object defaultValue, Connection conn) throws Exception{
		Object conf=null;
		if(!PhoneConfig.DEBUG) conf= cacheValues.get(name);
		if(conf==null){
			String sql= "select value from ad_sql where name=?";
			String data= QueryUtils.parseClobOrString(QueryEngine.getInstance().doQueryOne(sql, new Object[]{name}, conn));
			if(Validator.isNull(data)) {
				//using default value
				conf=defaultValue;
			}else{
				if( data.trim().startsWith("{")){
					JSONObject jo=new JSONObject(data);
					conf=jo;
				}else{
					JSONArray ja=new JSONArray(data);
					conf=ja;
				}
			}
			if(!PhoneConfig.DEBUG){
				cacheValues.put(name, conf);
			}
		}
		return conf;
		
	}	
	/**
	 *  Load value from ad_sql as json object or array
	 * @param name
	 * @param connisJSON
	 * @return (JSONArray/JSONObject)
	 * @throws Exception
	 */
	public Object getValueFromADSQLAsJSON(String name,Connection conn) throws Exception{
		Object conf=null;
		if(!PhoneConfig.DEBUG) conf= cacheValues.get(name);
		if(conf==null){
			String sql= "select value from ad_sql where name=?";
			String data= QueryUtils.parseClobOrString(QueryEngine.getInstance().doQueryOne(sql, new Object[]{name}, conn));
			if(Validator.isNull(data)) throw new NDSException("ad_sql#"+name+" not found");
			if( data.trim().startsWith("{")){
				JSONObject jo=new JSONObject(data);
				conf=jo;
			}else{
				JSONArray ja=new JSONArray(data);
				conf=ja;
			}
			if(!PhoneConfig.DEBUG){
				cacheValues.put(name, conf);
			}
		}
		return conf;
		
	}	
	
	
	/**
	 * SQL and params parsed
	 * @author yfzhu
	 *
	 */
	public static class SQLWithParams{
		String sql;
		Object[] params;
		public SQLWithParams(String s, Object[] p){
			sql=s;
			params=p;
		}
		
		public String getSQL(){
			return sql;
			
		}
		public Object[] getParams(){
			return params;
		}
		
	}
	public SQLWithParams parseADSQL(String name, VelocityContext vc, Connection conn) throws Exception{
		List al=QueryEngine.getInstance().doQueryList("select value, params from ad_sql where lower(name)=?", new Object[]{name.toLowerCase()}, conn);
		if(al.size()==0){
			return null;
		}
		al=(List)al.get(0);
		String sql=QueryUtils.parseClobOrString(al.get(0));
		String params=QueryUtils.parseClobOrString(al.get(1));
		if(Validator.isNull(sql)) return null;
		
		Object[] pvs;
		if(Validator.isNull(params)){
			pvs=new Object[]{};
		}else{
		
			StringWriter output = new StringWriter();
			Velocity.evaluate(vc, output, VelocityUtils.class.getName(), params);
			String[] paramValues=output.toString().split(",");
			String[] oldStrs=new String[]{"\t","\r","\n"};
			String[] newStrs=new String[]{"","",""};
			pvs=new Object[paramValues.length];
			for(int i=0;i<paramValues.length;i++){
				String pv=(String)paramValues[i];
				pv=StringUtils.replace(pv,oldStrs ,newStrs).trim();
				Object obj=null;
				if(pv.endsWith("(str)")){
					obj=pv.substring(0, pv.length()-5);//remove str
				}else{
					try{
						if(Validator.isNumber(pv)){
							obj=Double.parseDouble(pv);
							if(!pv.contains(".")){
								if( ((Double)obj).doubleValue() > Integer.MAX_VALUE){
									obj=Long.parseLong(pv);
								}else{
									obj=Integer.parseInt(pv);
								}
							}
						}else{
							obj= pv;
						}
						
					}catch(NumberFormatException ex){
						obj= pv;
					}
				}
				pvs[i]=obj;
			}
		}
		return new SQLWithParams(sql, pvs);
	}
	/**
	 * 根据ad_sql#params 和 ad_sql#value, 直接构造返回的数量列表
	 * ad_sql#params 逗号分隔的字符串，系统默认尝试解析为数字，除非尾随(str), 例如：$pdt.name, $fair.name, $foid, 每个元素都可以被vc解析并返回
	 * 处理原理: 将整个字符串通过velocity进行解析, 然后按逗号/换行符号分隔，解析为对应类型
	 * @param name ad_sql#name
	 * @param vc 将用于解析ad_sql#params
	 * @param conn
	 * @param isObjectRow is object row or array row
	 * @return
	 * @throws Exception
	 */
	public JSONArray getDataArrayByADSQL(String name, VelocityContext vc, Connection conn, boolean isObjectRow) throws Exception{
		SQLWithParams sp= parseADSQL(name, vc,conn);
		if(sp==null) return new JSONArray();
		if( isObjectRow)
			return QueryEngine.getInstance().doQueryObjectArray(sp.sql, sp.params, conn);
		else
			return QueryEngine.getInstance().doQueryJSONArray(sp.sql, sp.params, conn);
		
	}
	
	/**
	 * 根据ad_sql#params 和 ad_sql#value, 直接构造返回的json对象
	 * ad_sql#params 逗号分隔的字符串，系统默认尝试解析为数字，除非尾随(str), 例如：$pdt.name, $fair.name, $foid, 每个元素都可以被vc解析并返回
	 * 处理原理: 将整个字符串通过velocity进行解析, 然后按逗号/换行符号分隔，解析为对应类型
	 * @param name ad_sql#name
	 * @param vc 将用于解析ad_sql#params
	 * @param conn
	 * @return null if name not found
	 * @throws Exception
	 */
	public JSONObject getObjectByADSQL(String name, VelocityContext vc, Connection conn) throws Exception{
		SQLWithParams sp= parseADSQL(name, vc,conn);
		if(sp==null) return null;
		return QueryEngine.getInstance().doQueryObject(sp.sql, sp.params, conn);
		
	}
	
	public Object getValueFromADSQLAsJSON(String name) throws Exception{
		Connection conn=null;
		try{
			conn=QueryEngine.getInstance().getConnection();
			return getValueFromADSQLAsJSON(name,conn,false);
		}finally{
			try{conn.close();}catch(Throwable tx){}
		}
	}
	/**
	 *  Load value from ad_sql as json object or array
	 * @param name
	 * @param connisJSON
	 * @param nullable allow null or not for return object
	 * @return (JSONArray/JSONObject)
	 * @throws Exception
	 */
	public Object getValueFromADSQLAsJSON(String name,Connection conn, boolean nullable) throws Exception{
		Object conf=null;
		if(!PhoneConfig.DEBUG) conf= cacheValues.get(name);
		if(conf==null){
			String sql= "select value from ad_sql where name=?";
			String data= QueryUtils.parseClobOrString(QueryEngine.getInstance().doQueryOne(sql, new Object[]{name}, conn));
			if(Validator.isNull(data)){
				if(!nullable) throw new NDSException("ad_sql#"+name+" not found");
				else return null;
			}
			if( data.trim().startsWith("{")){
				JSONObject jo=new JSONObject(data);
				conf=jo;
			}else{
				JSONArray ja=new JSONArray(data);
				conf=ja;
			}
			if(!PhoneConfig.DEBUG){
				cacheValues.put(name, conf);
			}
		}
		return conf;
		
	}
	private void init(){
		this.cmdHandlerPackages= ConfigValues.getArray("phone.cmd.packages");
		if(cmdHandlerPackages == null || cmdHandlerPackages.length == 0){
			String client = ConfigValues.get("phone.client");
			String contextPackage =null;
			if(Validator.isNotNull(client) && !"default".equals(client)){
				client = client.trim();
				contextPackage = "com.agilecontrol.phone.impl." + client;
				cmdHandlerPackages = new String[]{contextPackage,"com.agilecontrol.b2bweb.cmd","com.agilecontrol.b2b.cmd","com.agilecontrol.phone.cmd"};
			}else{
				cmdHandlerPackages = new String[]{"com.agilecontrol.b2bweb.cmd","com.agilecontrol.b2b.cmd","com.agilecontrol.phone.cmd"};
			}
		}
		idWorker=new IdGenerator(PhoneConfig.ID_WORKER);
	}
	
	public static PhoneController getInstance(){
		if(instance == null){
			instance = new PhoneController();
			instance.init();
		}
		return instance;
	}
	/**
	 * 清除缓存，比如qlc
	 */
	public void clear(){
		cacheValues.clear();
	}
	/**
	 * 
	 * @return
	 */
	public String[] getCmdHandlerPackages(){
		return this.cmdHandlerPackages;
	}
	/**
	 * id 生成器
	 * @return
	 */
	public IdGenerator getIdGenerator(){
		return idWorker;
	}
	
	/**
	 * 布丁项目，通过IdWorker获取唯一Id
	 * @return tableName 表名，每张表有一个IdWorker
	 * @throws Exception
	 */
	public long getNextId(String tableName,Connection conn) throws Exception{
		return QueryEngine.getInstance().getSequence(tableName, conn);
//		long id=Tools.getLong(QueryEngine.getInstance().doQueryOne("select seq_"+ tableName+".nextval from dual", conn), -1);
//		if(id<0) throw new NDSException("Internal Error: Sequence not found");
//		return id;
	}
//	/**
//	 * 布丁项目，通过IdWorker获取唯一Id
//	 * @return tableName 表名，每张表有一个IdWorker
//	 * @throws Exception
//	 */
//	public long getDistributedNextId(String tableName) throws Exception{
//		
//		
//		IdWorker idworker=idworkers.get(tableName.toLowerCase());
//		if(idworker==null){
//			long worker=PhoneConfig.MQ_CLIENT_ID;
//			if(worker<0 || worker> 99) throw new NDSException("请在nea.properties中设置buding.loginid(0~99)");
//			idworker=new IdWorker(worker);
//			idworkers.put(tableName.toLowerCase(), idworker);
//		}
//		return idworker.getId();
//	}

	/**
	 * Execute insert, update, delete, and ddl
	 * Added by zhangbh on 20150922
	 * @param name
	 * @param vc
	 * @param conn
	 * @return rows effected
	 * @throws Exception
	 */
	public int executeUpdate(String name, VelocityContext vc, Connection conn) throws Exception {
		SQLWithParams sp = parseADSQL(name, vc, conn);
		if(sp == null) return -1;
		int n = QueryEngine.getInstance().executeUpdate(sp.sql, sp.params, conn);
		return n;
	}
	
	/**
	 * Execute stored procedure with only input parameters
	 * Added by zhangbh on 20150922
	 * @param name
	 * @param vc
	 * @param conn
	 * @throws Exception
	 */
	public void executeProcedure(String name, VelocityContext vc, Connection conn) throws Exception {
		executeProcedure(name, vc, conn, false);
	}
	
	/**
	 * Execute stored procedure, and you can choose the method to return code 
	 * and message as the output parameter to the procedure
	 * Added by zhangbh on 20150922
	 * @param name
	 * @param vc
	 * @param conn
	 * @param hasReturnValue true will return code and message from procedure. 
	 *        The last 2 params are out params to the procedure
	 * @return {code: intValue, message: stringValue}
	 * @throws Exception
	 */
	public JSONObject executeProcedure(String name, VelocityContext vc, Connection conn, boolean hasReturnValue) throws Exception {
		SQLWithParams sp = parseADSQL(name, vc, conn);
		if (sp == null) return new JSONObject();
		/*
		 * com.agilecontrol.nea.core.db.oracle.DatabaseManager
		 * 调用存储过程，数字类型的参数只支持Integer和Float，
		 * 如果为Long或Double，则转成int和float
		 * Modified by zhangbh on 20151030
		 */
		for (int i = 0; i < sp.params.length; i++) {
			if (sp.params[i] instanceof Long) {
				sp.params[i] = ((Long) sp.params[i]).intValue();
			} else if (sp.params[i] instanceof Double) {
				sp.params[i] = ((Double) sp.params[i]).floatValue();
			}
		}
		SPResult r = QueryEngine.getInstance().executeStoredProcedure(
				sp.sql, Arrays.asList(sp.params), hasReturnValue, conn);
		JSONObject jo = new JSONObject();
		jo.append("code", r.getCode());
		jo.append("message", r.getMessage());
		return jo;
	}
	
	/**
	 * Execute database function. The function will return only one output parameter.
	 * Added by zhangbh on 20150922
	 * @param name
	 * @param vc
	 * @param conn
	 * @param outParamTypes
	 * @return [returnValue1, returnValues2, ...]
	 * @throws Exception
	 */
	public String executeFunction(String name, VelocityContext vc, Connection conn) throws Exception {
		SQLWithParams sp = parseADSQL(name, vc, conn);
		if (sp == null) return "";
		/*
		 * com.agilecontrol.nea.core.db.oracle.DatabaseManager
		 * 调用存储过程，数字类型的参数只支持Integer和Float，
		 * 如果为Long或Double，则转成int和float
		 * Modified by zhangbh on 20151030
		 */
		for (int i = 0; i < sp.params.length; i++) {
			if (sp.params[i] instanceof Long) {
				sp.params[i] = ((Long) sp.params[i]).intValue();
			} else if (sp.params[i] instanceof Double) {
				sp.params[i] = ((Double) sp.params[i]).floatValue();
			}
		}
		Collection r = QueryEngine.getInstance().executeFunction(
				sp.sql, Arrays.asList(sp.params), Arrays.asList(
						new Object[] {String.class}), conn);
		return r.size() > 0 ? (String) r.iterator().next() : "";
	}
	
	/**
	 * 在不使用FairConfig.DEBUG的情况下，允许清除ad_sql的缓存
	 */
	public void clearCacheValues(){
		cacheValues.clear();
	}
	
	/**
	 * Create task by name
	 * @param taskName without dot, will loop over task class packages for first matched path
	 * @return
	 * @throws Exception
	 */
	protected CmdHandler createCmdHandler(String cmdName) throws Exception{
		String className;
		Class clazz;
		CmdHandler task=null;
		for(String packageName: PhoneController.getInstance().getCmdHandlerPackages()){
			className=packageName+"."+cmdName;
			try{
				clazz =Class.forName(className);
				task=(CmdHandler) clazz.newInstance();
				break;
			}catch(ClassNotFoundException cnfe){
				
			}
		}
		if(task==null){
			logger.error("Fail to find cmdhandler "+ cmdName+ " in packages: "+ 
					PhoneController.getInstance().getCmdHandlerPackages());
			throw new NDSException("cmdhandler "+ cmdName+ " not found" );
		}	
		return task;
	}
	
	/**
	 * Get resource as string，路径是相对当前class的classLoadder,例如，需要获取当前jar文件种
	 * META-INF下的文件a.txt, 则写为"META-INF/a.txt"
	 * @param resPath 相对jar的根目录开始的路径
	 * @return 文件内容
	 * @throws Exception
	 */
	public String getResourceAsString(String resPath , String encoding) throws Exception{
		InputStream ins =getClass().getClassLoader().getResourceAsStream(resPath);
		if(ins==null) return null;
		String content=null;
		ByteArrayOutputStream outputstream = new ByteArrayOutputStream();
		byte[] str_b = new byte[8192];
		int i = -1;
		while ((i=ins.read(str_b)) > 0) {
          outputstream.write(str_b,0,i);
		}
	       content = outputstream.toString(encoding);
	       return content;
	}
	/**
	 * Get resource as string，路径是相对当前class的classLoadder,例如，需要获取当前jar文件种
	 * META-INF下的文件a.txt, 则写为"META-INF/a.txt"
	 * @param resPath 相对jar的根目录开始的路径
	 * @return 文件内容
	 * @throws Exception
	 */
	public String getResourceAsString(String resPath) throws Exception{
		
       return getResourceAsString(resPath,"UTF-8");
	}
	/**
	 * 调用getResourceAsString方法，处理异常
	 */
	public String getHelpDetailAsString(String resPath) {
		try {
			return getResourceAsString(resPath);
		} catch (Throwable tx ) {
			logger.error("throw IOException", tx);
		}
		return null;
	}	
		
}
