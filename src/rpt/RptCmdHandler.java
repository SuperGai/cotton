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
 * ��Ϊ����rpt��cmdhandler�Ļ�����
 * 
 * @author yfzhu
 */
public abstract class RptCmdHandler extends CmdHandler {
	/**
	 * �����ȡ��getConnection(ds)�����ҷ���ֵ����conn, ����ͷ������ⶨ������connection������
	 * �ڹرյ�ʱ������close
	 */
	protected Connection dsConn=null;
	
	/**
	 * ����ͨ�õı�����context,Ӧ����config/sql�����Ĺ���
	 * @param addtionalMap��ȡ�����е�ÿ��key�����쵽context��
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
	 * �ڴ�����ҵ��󱻵��ã������Ҫ�����ӻ��ڴ����
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
	 * ��ȡ���ݿ����ӣ�Ĭ���ǵ�ǰ���ݿ�����
	 * @param jo �������ã���ȡ���е�ds����, ע�����Ժ�̨����
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
	        if(cn==null || cn.isClosed()) throw new NDSException("ds����������:"+ds);
	        dsConn=cn;
	        return dsConn;
		}else{
			cn=this.conn;
		}
		return cn;
	}
	
	/**
sqlfunc - ��pl/sql�������ƣ��ӿڶ��壺

function sqlfunc(json in varchar2) return clob

- �����ʽ

- param json {pageid,widgetid, userid,filter,key }

pageid - string ��ǰpage�����id�� ��ʽ: "rpt:page:1"
widgetid - string ��ǰwidget id�� ��ʽ: "rpt:widget:panel:13"
userid - int ��ǰ��ѯ������û���id
filter - json ��ǰ����ı�������� key �ǽ�����ֶΣ� value �ǽ����ѡ���Ĭ��ֵ��������{area:"����", year: "2016"}
key - ר������filter���͵Ĳ�ѯ�Ķ�Ӧ��filter��key
- �����ʽ��
clob - string ��һ��������sql���	 
	 * @param func sqlfunc
	 * @param pageId ��ǰ����ҳ��id
	 * @param widgetId ���id
	 * @param filter key/value��ʽ�Ķ���
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
	 * ��ĳ��key ��src���Ƶ�target����� key ��src�в����ڣ���ʹ��Ĭ��ֵ�����Ĭ��ֵ��Ϊ��
	 * @param src Դ���󣬴�����ȡ��key��Ӧ������ֵ�� Ϊ�ս�ֱ��ʹ��Ĭ��ֵ
	 * @param target Ŀ����󣬲���Ϊ��
	 * @param key 
	 * @param defaultValue Ĭ��ֵ��null ��ʾ������
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
	 * ��ĳ��key ��src���Ƶ�target����� key ��src�в����ڣ���ʹ��Ĭ��ֵ�����Ĭ��ֵ��Ϊ��
	 * @param src Դ���󣬴�����ȡ��key��Ӧ������ֵ�� Ϊ�ս�ֱ��ʹ��Ĭ��ֵ
	 * @param target Ŀ����󣬲���Ϊ��
	 * @param key 
	 * @param nullable �Ƿ�����key��src�в�����
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
