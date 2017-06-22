package rpt;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;

import org.apache.velocity.VelocityContext;
import org.json.JSONArray;
import org.json.JSONObject;

import com.agilecontrol.nea.core.velocity.DateUtil;
import com.agilecontrol.nea.core.velocity.VelocityUtils;
import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.nea.util.Validator;
import com.agilecontrol.phone.CmdHandler;
import com.agilecontrol.phone.CmdResult;

/**
 * 作为整个rpt包cmdhandler的基础类
 * 
 * @author yfzhu
 */
public abstract class RptCmdHandler extends CmdHandler {
	/**
	 * 如果获取过getConnection(ds)，并且返回值不是conn, 这里就放置特殊定义的这个connection，并且
	 * 在关闭的时候被主动close
	 */
	protected Connection dsConn=null;
	
	/**
	 * 创建通用的报表类context,应用于config/sql等语句的构造
	 * @param addtionalMap，取出其中的每个key，构造到context中
	 * @return
	 * @throws Exception
	 */
	protected VelocityContext createVelocityContext(Map addtionalMap) throws Exception{
		VelocityContext vc=VelocityUtils.createContext();
		vc.put("conn",conn);
		vc.put("userid", this.usr.getId()); 
		//vc.put("username", usr.getNkname());
		vc.put("comid",  this.usr.getComId());
		//vc.put("empid", usr.getEmpId());
		vc.put("t", DateUtil.getInstance());
		if(addtionalMap!=null)
		for(Object key: addtionalMap.keySet()){
			
			vc.put((String)key, addtionalMap.get(key));
		}
		return vc;
	}
	/**
	 * 在处理完业务后被调用，清理必要的连接或内存变量
	 */
	public void destroy() {
		try{
			if(dsConn!=null && !dsConn.isClosed()){
				dsConn.close();
			}
		}catch(Throwable tx){
			logger.error("Fail to close dsConn", tx);
		}
	}
	/**
	 * 获取数据库连接，默认是当前数据库连接
	 * @param jo 数据配置，读取其中的ds属性, 注意来自后台定义
	 * @return
	 */
	protected Connection getConnection(JSONObject jo) throws Exception{
		Connection cn=null;
		String ds=jo.optString("ds");
		if(Validator.isNotNull(ds)){
			if(dsConn!=null) return dsConn;
			
			Context ctx = new InitialContext();
	        logger.info("Using datasource:"+ds);
	        DataSource datasource = (DataSource) ctx.lookup ("java:/"+ds);
	        
	        cn=datasource.getConnection();
	        if(cn==null || cn.isClosed()) throw new NDSException("ds配置有问题:"+ds);
	        dsConn=cn;
	        return dsConn;
		}else{
			cn=this.conn;
		}
		return cn;
	}
	
	/**
sqlfunc - 是pl/sql函数名称，接口定义：

function sqlfunc(json in varchar2) return clob

- 输入格式

- param json {pageid,widgetid, userid,filter,key }

pageid - string 当前page对象的id， 格式: "rpt:page:1"
widgetid - string 当前widget id， 格式: "rpt:widget:panel:13"
userid - int 当前查询报表的用户的id
filter - json 当前传入的报表参数， key 是界面的字段， value 是界面的选项（或默认值）举例：{area:"南宁", year: "2016"}
key - 专门面向filter类型的查询的对应的filter的key
- 输出格式：
clob - string 是一条完整的sql语句	 
	 * @param func sqlfunc
	 * @param pageId 当前报表页面id
	 * @param widgetId 组件id
	 * @param filter key/value形式的对象
	 * @return 
	 * @throws Exception
	 */
	protected String getSQLByFunc(String func, String pageId, String widgetId, JSONObject filter) throws Exception{
		JSONObject params=new JSONObject();
		if(pageId!=null)params.put("pageid", pageId);
		if(widgetId!=null)params.put("widgetid", widgetId);
		if(filter!=null)params.put("filter", filter);
		params.put("userid",this.usr.getId());
		//params.put("stid",this.usr.getStoreId());
		//params.put("empid",this.usr.getEmpId());
		params.put("comid",this.usr.getComId());
		ArrayList args=new ArrayList();
		args.add(params.toString());
		
		ArrayList results=new ArrayList();
		results.add(java.sql.Clob.class);
		
		
		Collection ret=engine.executeFunction(func, args, results,conn);
		Object obj=ret.iterator().next();
		String str;
		if(obj instanceof java.sql.Clob) {
    		str=((java.sql.Clob)obj).getSubString(1, (int) ((java.sql.Clob)obj).length());
    	}else str=obj.toString();
		
		return str;
	}
	protected String getSQLByFunc(String func,String pageId, String key) throws Exception{
		JSONObject params=new JSONObject();
		params.put("userid",usr.getId());
		if(pageId!=null)params.put("pageid", pageId);
		params.put("key", key);
		ArrayList args=new ArrayList();
		args.add(params.toString());
		
		ArrayList results=new ArrayList();
		results.add(java.sql.Clob.class);
		
		
		Collection ret=engine.executeFunction(func, args, results,conn);
		Object obj=ret.iterator().next();
		String str;
		if(obj instanceof java.sql.Clob) {
    		str=((java.sql.Clob)obj).getSubString(1, (int) ((java.sql.Clob)obj).length());
    	}else str=obj.toString();
		
		return str;
	}
	/**
	 * 将某个key 从src复制到target，如果 key 在src中不存在，将使用默认值，如果默认值不为空
	 * @param src 源对象，从其中取出key对应的属性值， 为空将直接使用默认值
	 * @param target 目标对象，不能为空
	 * @param key 
	 * @param defaultValue 默认值，null 表示不设置
	 * @throws Exception
	 */
	public void copyKey(JSONObject src, JSONObject target, String key, Object defaultValue) throws Exception{
		if(target==null) throw new NDSException("target not allow null");
		Object value=null;
		if(src==null){
			if(defaultValue!=null) value=defaultValue;
		}else{
			value=src.opt(key);
			if(value==null) value=defaultValue;
		}
		if(value!=null) target.put(key, value);
	}
	
	/**
	 * 将某个key 从src复制到target，如果 key 在src中不存在，将使用默认值，如果默认值不为空
	 * @param src 源对象，从其中取出key对应的属性值， 为空将直接使用默认值
	 * @param target 目标对象，不能为空
	 * @param key 
	 * @param nullable 是否允许key在src中不存在
	 * @throws Exception
	 */
	public void copyKey(JSONObject src, JSONObject target, String key, boolean nullable) throws Exception{
		if(target==null) throw new NDSException("target not allow null");
		Object value=null;
		if(src==null){
			if(!nullable) throw new NDSException("src is null");
		}else{
			value=src.opt(key);
			if( (value==null || Validator.isNull(value.toString())) && !nullable) throw new NDSException("value of key#"+key+" is null");
		}
		if(value!=null) target.put(key, value);
	}
	
}
