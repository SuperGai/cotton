package com.agilecontrol.phone.cmd;

import java.io.StringWriter;

import javax.servlet.http.HttpServletRequest;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.json.*;

import com.agilecontrol.nea.core.schema.Table;
import com.agilecontrol.nea.core.schema.TableManager;
import com.agilecontrol.nea.core.security.SecurityUtils;
import com.agilecontrol.nea.core.util.MessagesHolder;
import com.agilecontrol.nea.core.velocity.VelocityUtils;
import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.nea.util.Validator;
import com.agilecontrol.phone.CmdHandler;
import com.agilecontrol.phone.CmdResult;
import com.agilecontrol.phone.PhoneController;
import com.agilecontrol.phone.util.UserSchema;

import java.util.*;
/**
 ֧��ad_sql���ִ��
 ad_sql��value�����Ǻ���?��sql��䣬params��д����$xxx ��velocity������
 runsql ��Ҫָ�������������:
 {
  type: QUERY | UPDATE | PROCEDURE | PROCEDURE_WITH_RESULT | FUNCTION | JSON | TEXT, // Optional, default is 'QUERY'
  name: ad_sql#name
  var: {"key":"value"}
 }
 type:
   QUERY: ִ�в�ѯ
   UPDATE: ִ��insert��update��delete��ddl������Ӱ��������int
   PROCEDURE: ִ��DB�洢���̣��޷���
   PROCEDURE_WITH_RESULT: ִ��DB�洢���̣�����code��message
   FUNCTION: ִ��DB������Ψһ���أ�String
   JSON: ����ad_sql�ı������õ�json�����json����
   TEXT: ֱ�ӷ���ad_sql�ı�
 name - ad_sql#name ��ǰ�û�������ж�ȡ��ad_sql��ǰ��¼��Ȩ��
 var: �������ϣ�key�Ǳ��������ƣ�value�Ǳ�����ֵ��ע��key��Ҫд$����
 
 row_is_obj: true|false, ���ص������е����Ƕ���������
 
 java����ݵ�ǰǿ��д��$userid, $username�� �Լ��������û�����������һ��Сд����ϸ����AD_USER_ATTR������ֹ�ͻ��˴۸�
 
 ����: 
 [[col1,col2],..] ��sql��Լ����
 * @author yfzhu
 *
 */
public class RunSQL extends CmdHandler {
	
	public static final String TYPE_QUERY = "QUERY";
	public static final String TYPE_UPDATE = "UPDATE";
	public static final String TYPE_PROCEDURE = "PROCEDURE";
	public static final String TYPE_PROCEDURE_WITH_RESULT = "PROCEDURE_WITH_RESULT";
	public static final String TYPE_FUNCTION = "FUNCTION";
	public static final String TYPE_JSON = "JSON";
	public static final String TYPE_TEXT = "TEXT";

	@Override
	public CmdResult execute(JSONObject jo) throws Exception {
		
		String name=jo.getString("name");
		
		boolean rowIsObj=jo.optBoolean("row_is_obj", false);
		
		int sqlId= engine.doQueryInt("select id from ad_sql where name=?", new Object[]{name}, conn);
		
		VelocityContext vc=VelocityUtils.createContext();

		JSONObject var= jo.optJSONObject("var");
		if(var!=null){
			for(Iterator it=var.keys();it.hasNext();){
				String key=(String) it.next();
				vc.put(key, var.get(key));
			}
		}
		vc.put("conn",conn);
		vc.put("c", this);
		vc.put("userid", usr.getId());
		vc.put("username",usr.getName());
		//�û���������
		//todo
		
		
		PhoneController ctrl = PhoneController.getInstance();
		Object o = new JSONObject();
		String type = jo.optString("type", TYPE_QUERY);
		if (TYPE_QUERY.equalsIgnoreCase(type)) {
			o = ctrl.getDataArrayByADSQL(name, vc, conn, rowIsObj);
		} else if (TYPE_UPDATE.equalsIgnoreCase(type)) {
			o = ctrl.executeUpdate(name, vc, conn);
		} else if (TYPE_PROCEDURE.equalsIgnoreCase(type)) {
			ctrl.executeProcedure(name, vc, conn);
		} else if (TYPE_PROCEDURE_WITH_RESULT.equalsIgnoreCase(type)) {
			o = ctrl.executeProcedure(name, vc, conn, true);
		} else if (TYPE_FUNCTION.equalsIgnoreCase(type)) {
			o = ctrl.executeFunction(name, vc, conn);
		} else if (TYPE_JSON.equalsIgnoreCase(type)) {
			//o = ctrl.getValueFromADSQLAsJSON(name, conn);
			/*
			 * ��@tranlation-key@������
			 * modified on 20161028
			 */
			String data = ctrl.getValueFromADSQL(name, conn);
			data=MessagesHolder.getInstance().translateMessage(data, this.locale);
			
			if( data.trim().startsWith("{")){
				o=new JSONObject(data);
			}else{
				JSONArray ja=new JSONArray(data);
				o=ja;
			}
		} else if (TYPE_TEXT.equalsIgnoreCase(type)) {
			o = ctrl.getValueFromADSQL(name, conn);
		}
		
		CmdResult cr=new CmdResult();
		cr.setObject(o);
		return cr;
	}
}