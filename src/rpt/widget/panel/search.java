package rpt.widget.panel;

import java.sql.Connection;

import org.apache.velocity.VelocityContext;
import org.json.JSONArray;
import org.json.JSONObject;

import rpt.RptCmdHandler;

import com.agilecontrol.nea.util.*;
import com.agilecontrol.nea.core.query.*;
import com.agilecontrol.nea.core.schema.*;
import com.agilecontrol.nea.core.security.Directory;
import com.agilecontrol.nea.core.velocity.VelocityUtils;
import com.agilecontrol.phone.CmdHandler;
import com.agilecontrol.phone.CmdResult;
import com.agilecontrol.phone.PhoneController;
import com.agilecontrol.phone.PhoneUtils;
import com.agilecontrol.phone.PhoneController.SQLWithParams;
 /**
  * 

h2. panel������

panel��grid,chart һ��Ҳ�Ǳ��������֧�ֱ��ʽ���֣�ÿ����Ԫ���Ϊpanelcell, һ�������ֻ��������, panelid ��ʽ��"rpt:widget:panel:{{key}}"

ÿ��panel ���ж�Ӧ��html template��template �ļ��洢�ڿͻ���Լ����Ŀ¼������Ϊ panelid ��key.html ��ģ����֧�����ɲ���key����sqlfunc��Ӧ��sql���������

h3. ��̨����

ad_sql.name�� ��ʽ "rpt:widget:panel:{{key}}"

value:
<pre>
{ sqlfunc, sql,template, jumpid}
</pre>
* sqlfunc - oracle function���ƣ��� #sqlfunc������Ҫ��sql���ص�����2�У���һ������template�е�key�� �ڶ�����Ҫ���õ�����ֵ
* sql - ad_sql.name����Ӧ��sql��䣬֧�ֵĲ�������: $userid, $panelid
* jumpid - jump page id

h3. �ͻ�������

��������
<pre>
{cmd: "rpt.widget.panel.search",  id, filter}
</pre>
���ظ�ʽ��
<pre>
{id, values,template} 
</pre>
* id - panel.id
* values - jsonobj, key ��sql ���ĵ�һ�У�value�ǵڶ��У���Щֵ���滻panel template�еı���

 * 
 * @author yfzhu
 *
 */
public class search extends RptCmdHandler {
	private String pageId;
	@Override
	public CmdResult execute(JSONObject jo) throws Exception {
		String widgetId= getString(jo, "id");
		pageId=getString(jo, "pageid");
		JSONObject filter= jo.optJSONObject("filter");
		if(filter==null)filter=new JSONObject();
		
		JSONObject def= (JSONObject)PhoneController.getInstance().getValueFromADSQLAsJSON(widgetId, conn, false);
		JSONObject config=new JSONObject();
	
		JSONObject values=new JSONObject();

		String sqlName=def.optString("sql");
		String sqlfunc=def.optString("sqlfunc");
		Connection dsconn=getConnection(def);

		if(Validator.isNotNull(sqlName)){
			//ֱ�Ӷ����sql��䣬������õ�ad_sql#name
			VelocityContext vc=this.createVelocityContext(null);
			vc.put("filter", filter);
			vc.put("dsconn", dsconn);

			SQLWithParams sp= PhoneController.getInstance().parseADSQL(sqlName, vc,conn);
			JSONArray options=sp==null?new JSONArray():QueryEngine.getInstance().doQueryObjectArray(sp.getSQL(), sp.getParams(), dsconn);
			values = alsPanelData(options);
		}else if(Validator.isNotNull(sqlfunc)){
			String sql=getSQLByFunc(sqlfunc,pageId,widgetId,filter);
			JSONArray options=engine.doQueryObjectArray(sql, null, dsconn);
			values = alsPanelData(options);
		}else throw new NDSException("��Ҫ����sql/sqlfun");
		config.put("values", values);
		//ͳһ�Ŀͻ���ģʽ
		JSONObject ret=new JSONObject();
		this.copyKey(def, config, "template", false);
		ret.put("config", config);
		
		String jumpPageId= def.optString("jumpid");
		if(Validator.isNotNull(jumpPageId))  ret.put("jumpid", jumpPageId);
//		else throw new NDSException("���ô��󣺱����ṩjumpid����");
		ret.put("id", widgetId);
		ret.put("type",  "panel");
		this.copyKey(jo, ret, "tag", true);
		return new CmdResult(ret);
	}
	/**
	 * �ع�panel������
	 * @param options
	 * @return
	 * @throws Exception
	 */
	private JSONObject alsPanelData(JSONArray options) throws Exception{
		JSONObject allData = new JSONObject();
		if(options.length()<=0){
			return allData;
		}
		String lineStr = options.getJSONObject(0).optString("lineid","");
		if(Validator.isNull(lineStr)){
			for(int i=0;i<options.length();i++){
				JSONObject cellObj = options.getJSONObject(i);
				String name = cellObj.getString("name");
				String value = cellObj.optString("value","");
				if(Validator.isNull(value))value="";
				allData.put(name, value);
			}
			return allData;
		}
		for(int i=0;i<options.length();i++){
			JSONObject cellObj = options.getJSONObject(i);
			String key  = cellObj.getString("key");
			String val = cellObj.getString("value");
			String style = cellObj.optString("style");
			String lineid = cellObj.getString("lineid");
			allData.put(key, cellObj);
		}
		return allData;
	}

}
