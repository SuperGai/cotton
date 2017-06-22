package com.agilecontrol.b2b.cmd;

import org.apache.velocity.VelocityContext;
import org.json.*;

import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.sql.*;
import java.text.SimpleDateFormat;

import com.agilecontrol.b2b.schema.*;
import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.phone.CmdHandler;
import com.agilecontrol.phone.CmdResult;
import com.agilecontrol.phone.PhoneUtils;

/**
 * 
  ����ָ���ı���ȫ��������ݵ�redis��Ŀǰ��֧��hset���͵�key
   {
   table : ֧��ָ���ı��� * ��ʾ���б� 
   debug: false|true �Ƿ���ʾ��־��������������������ݿⲻһ�£���Ҫ���������ʾ��־��������������������
   }
   Ŀǰ�ȿ�����jedis pipeline ģʽ�㶨
   
   
   --δʵ��:
   ������ data �ļ������е��룬�÷�: 
   cat data.txt | redis-cli --pipe
   
   data.txt �ļ���ʽ:

SET Key0 Value0
SET Key1 Value1
...
SET KeyN ValueN
   
 * @author yfzhu
 *
 */
public class ReloadRedis extends CmdHandler {
	
	private boolean debug=true;
	
    public  SimpleDateFormat dateFormatter;


	/**
	 * Guest can execute this task, default to false
	 * 
	 * @return
	 */
	public boolean allowGuest() {
		return true;
	}

	@Override
	public CmdResult execute(JSONObject jo) throws Exception {
//		checkIsLifecycleManager();
		dateFormatter=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	    	
		debug = jo.optBoolean("debug",false);
		String tbname=getString(jo, "table").trim();
		long cnt=0;
		
		JSONObject ro=new JSONObject();
		
		if("*".equals(tbname)){
			for(Table table: manager.getAllTables()){
				ro.put(table.getName(), reloadTable(table));
			}
		}else{
			Table table=manager.getTable(tbname);
			if(table==null) throw new NDSException("Table:"+ tbname+"δ����");
			ro.put(table.getName(), reloadTable(table));
		}
		
		CmdResult res=new CmdResult(ro );
		return res;
	}
	
	public final static int FieldType_STRING=1, FieldType_NUMBER=2, FieldType_DATE=3;
	/**
	 * �ֶε����ͺ����ƣ����redis�����Ͷ���
	 * @author yfzhu
	 *
	 */
	private  class Field{
		public int type; 
		public String name;
		
		public Field(int t, String n){
			type=t; name=n;
		}
	}
	/**
	 * ��д�����ݵ�redis��ʱ�򣬿��ܻ�����������ÿ��id�Ͷ�Ӧ�ı���Ľ��
	 * 
	 * @author yfzhu
	 *
	 */
	private class RowRes {
		long id;
		Response<String> resp;
		
	}
	/**
	 * ����ָ��������м�¼��redis
	 * @param table
	 * @return ������е���Ϣ {err:[], cnt: long}
	 * @throws Exception
	 */
	private JSONObject reloadTable(Table table) throws Exception{
		
		long cnt= engine.doQueryInt("select count(*) from "+ table.getName(), null, conn);
		
		PreparedStatement pstmt=conn.prepareStatement("select * from "+ table.getName());
		
		ResultSet rs=null;
		
		Pipeline pipe=jedis.pipelined();
		
		
		ArrayList<RowRes> ids=new ArrayList();
		JSONObject ret=new JSONObject();
		ret.put("cnt", cnt);
		try{
			rs=pstmt.executeQuery();
			ResultSetMetaData rsmd=rs.getMetaData();
			int ccnt=rsmd.getColumnCount();
			Field[] cols=new Field[ccnt];
			int idColumnIdx=-1;
			for(int i=0;i<ccnt;i++) {
				int type= convertToFieldType (rsmd.getColumnType(i+1));
				String name=rsmd.getColumnLabel(i+1).toLowerCase();
				cols[i]=new Field(type, name);
				if("id".equals(name))idColumnIdx=i;
			}
			if(idColumnIdx==-1) throw new NDSException("δ�ҵ�id��: "+ table.getName());
			String key;
			HashMap<String, String> map;
			long objectId;
			while(rs.next()){
				map=new HashMap();
				for(int i=0;i<cols.length;i++){
					Object value= rs.getObject(i+1);
					if(rs.wasNull()) value="";
					else{
						if(cols[i].type==FieldType_DATE){
							value=dateFormatter.format((java.util.Date)value);
						}
					}
					map.put(cols[i].name,  convertClob(value).toString());
				}
				objectId=rs.getLong(idColumnIdx+1);
				key= table.getName()+":"+ objectId;
				
				//��ɾ��key
				pipe.del(key);
				
				if(debug){
					
					RowRes rr=new RowRes();
					rr.resp= pipe.hmset(key, map);
					rr.id= objectId;
					ids.add(rr);
				}else{
					pipe.hmset(key, map);
				}
			}
		}finally{
			if(rs!=null) try{rs.close();}catch(Throwable tx){}
			if(pstmt!=null) try{pstmt.close();}catch(Throwable tx){}
		}
		JSONArray errs=new JSONArray();
		pipe.sync();

		if(debug){
			
			for(int i=0;i<ids.size();i++){
				RowRes rr=ids.get(i);
				
				String err=rr.resp.get();
				if(!"OK".equals(err)){
					errs.put("ID="+rr.id+":"+err );
				}
			}
		}
		ret.put("err", errs);
		return ret;
	}
	private Object convertClob(Object obj) throws SQLException{
    	if(obj instanceof java.sql.Clob) {
    		obj=((java.sql.Clob)obj).getSubString(1, (int) ((java.sql.Clob)obj).length());
    	}
    	return obj;
    }
	/**
	   * Convert java.sql.Types to SQLTypes
	   * @param javaSQLType defined in java.sql.Types
	   * @return int defined in FieldType
	   * @see java.sql.Types
	   */
	  private int convertToFieldType(int javaSQLType){
	    int type=javaSQLType;
	    switch (javaSQLType)
	    {
		case java.sql.Types.BIT 	:type=FieldType_NUMBER;break;
		case java.sql.Types.TINYINT 	:type=FieldType_NUMBER;break;
		case java.sql.Types.SMALLINT	:type=FieldType_NUMBER;break;
		case java.sql.Types.INTEGER 	:type=FieldType_NUMBER;break;
		case java.sql.Types.BIGINT 	:type=FieldType_NUMBER;break;
		case java.sql.Types.FLOAT 	:type=FieldType_NUMBER;break;
		case java.sql.Types.REAL 	:type=FieldType_NUMBER;break;
		case java.sql.Types.DOUBLE 	:type=FieldType_NUMBER;break;
		case java.sql.Types.NUMERIC 	:type=FieldType_NUMBER;break;
		case java.sql.Types.DECIMAL	:type=FieldType_NUMBER;break;
		case java.sql.Types.CHAR	:type=FieldType_STRING;break;
		case java.sql.Types.VARCHAR 	:type=FieldType_STRING;break;
		case java.sql.Types.LONGVARCHAR :type=FieldType_STRING;break;
		case java.sql.Types.DATE 	:type=FieldType_DATE;break;
		case java.sql.Types.TIME 	:type=FieldType_DATE;break;
		case java.sql.Types.TIMESTAMP 	:type=FieldType_DATE;break;
		case java.sql.Types.BINARY	:type=FieldType_STRING;break;
		case java.sql.Types.VARBINARY 	:type=FieldType_STRING;break;
		case java.sql.Types.LONGVARBINARY :type=FieldType_STRING;break;
	    case java.sql.Types.CLOB       :type=FieldType_STRING;break;

	      default:
	        break;
	    }

	    if ((type < 1) || (type > 24))
	    {
	      System.err.println("Warning! -  is of a type not recognised. Value : "+type);
	      System.err.println("Defaulting to string");
	      type = 12;
	    }
	    return type;
	  }
	
}
