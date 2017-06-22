package rpt.widget.panellist;

import java.sql.Connection;

import org.apache.velocity.VelocityContext;
import org.json.JSONArray;
import org.json.JSONObject;

import com.agilecontrol.nea.core.query.QueryEngine;
import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.nea.util.Validator;
import com.agilecontrol.phone.CmdResult;
import com.agilecontrol.phone.PhoneController;
import com.agilecontrol.phone.PhoneController.SQLWithParams;

import rpt.RptCmdHandler;
/**
 panelList������
 
 panelList�� panel��grid,chart һ��Ҳ�Ǳ��������, һ�������ֻ��������, panellistid ��ʽ��"rpt:widget:panellist:{{key}}"

h3. ��̨����

ad_sql.name�� ��ʽ "rpt:widget:panellist:{{key}}"

value:
<pre>
{ sqlfunc, sql,template��{title,panel}, jumpid}
{
sqlfunc:"",
sql:"",
jumpid:"",
template:{
	title:{name:"",jumpid:""},
	panel:""
},
maxcnt:3
}
</pre>
* sqlfunc - 
* sql - ad_sql.name����Ӧ��sql��䣬֧�ֵĲ�������: $userid, $panellistid
* jumpid - jump page id
* template.title -panellist�ı�����Ϣ��jumpidָ���ǵ������ʱ��ת�ĵ�ַ ,����û��
* template.panel - ����������ʾ���õ�panelģ��     
* maxcnt - �����ʾ��                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     

h3. �ͻ�������

��������
<pre>
{cmd: "rpt.widget.panellist.search",  id, filter}
</pre>
���ظ�ʽ��
<pre>
config��{
	title��{name:"",jumpid:""},
	data:[
		{id, values,template} 
	]
     maxcnt - �����ʾ��
}
</pre>
title - �����б�ı���
data - ���ݼ��ϣ��ڲ���������ݺ͵���panel������һ��
* id - panel.id
* values - jsonobj, key ��sql ���ĵ�һ�У�value�ǵڶ��У���Щֵ���滻panel template�еı���


 * @author sunyifan
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
	
		JSONArray values=new JSONArray();
		

		String sqlName=def.optString("sql");
		String sqlfunc=def.optString("sqlfunc");
		Connection dsconn=getConnection(def);
		//�����ʾ��
		int maxcnt = def.optInt("maxcnt",-1); 
		if(maxcnt!=-1){
			filter.put("maxcnt", maxcnt);                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   
		}
		
		if(Validator.isNotNull(sqlName)){
			//ֱ�Ӷ����sql��䣬������õ�ad_sql#name
			VelocityContext vc=this.createVelocityContext(null);
			vc.put("filter", filter);
			vc.put("dsconn", dsconn);
			SQLWithParams sp= PhoneController.getInstance().parseADSQL(sqlName, vc,conn);
			JSONArray options=sp==null?new JSONArray():QueryEngine.getInstance().doQueryObjectArray(sp.getSQL(), sp.getParams(), dsconn);
			values = alsPanelListData(options);
		}else if(Validator.isNotNull(sqlfunc)){
			String sql=getSQLByFunc(sqlfunc,pageId,widgetId,filter);
			JSONArray options=engine.doQueryObjectArray(sql, null, dsconn);
			values = alsPanelListData(options);
		}else throw new NDSException("��Ҫ����sql/sqlfun");
		if(values.length()<=0){
			values = new JSONArray();
		}
		config.put("values", values);
		//ͳһ�Ŀͻ���ģʽ
		JSONObject ret=new JSONObject();
		this.copyKey(def, config, "template", false);
		ret.put("config", config);
		
		JSONObject panelObj= def.getJSONObject("panel");
		String panelTemplate = panelObj.optString("template");
		if(Validator.isNotNull(panelTemplate))  ret.put("panel", panelObj);
		else throw new NDSException("���ô��󣺱����ṩ��������ģ������");
		
		JSONObject titleObj= def.optJSONObject("title");
		if (titleObj!=null) {
			ret.put("title", titleObj);
		}
		ret.put("id", widgetId);
		ret.put("type",  "panellist");
		this.copyKey(jo, ret, "tag", true);
		return new CmdResult(ret);
	}
	
	/**
	 * �ع�panellist������
	 * @param options
	 * @return
	 * @throws Exception
	 */
	private JSONArray alsPanelListData(JSONArray options) throws Exception{
		JSONArray allData = new JSONArray();
		JSONObject row = new JSONObject();
		if(options.length()<=0){
			return allData;
		}
		String lineStr = options.getJSONObject(0).optString("lineid","");
		if(Validator.isNull(lineStr)){
			return options;
		}
		for(int i=0;i<options.length();i++){
			JSONObject cellObj = options.getJSONObject(i);
			String key  = cellObj.getString("key");
			String val = cellObj.getString("value");
			String style = cellObj.optString("style");
			String lineid = cellObj.getString("lineid");
			if(lineStr.equals(lineid)){
				row.put(key, cellObj);
			}else{
				allData.put(row);
				row = new JSONObject();
				row.put(key, cellObj);
				lineStr = lineid;
			}
		}
		return allData;
	}

}
