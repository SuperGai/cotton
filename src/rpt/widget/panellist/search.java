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
 panelList面板组件
 
 panelList和 panel，grid,chart 一样也是报表组件，, 一般由文字或数字组成, panellistid 格式："rpt:widget:panellist:{{key}}"

h3. 后台定义

ad_sql.name： 格式 "rpt:widget:panellist:{{key}}"

value:
<pre>
{ sqlfunc, sql,template：{title,panel}, jumpid}
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
* sql - ad_sql.name所对应的sql语句，支持的参数包括: $userid, $panellistid
* jumpid - jump page id
* template.title -panellist的标题信息，jumpid指的是点击更多时跳转的地址 ,可以没有
* template.panel - 单行数据显示所用的panel模板     
* maxcnt - 最大显示数                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     

h3. 客户端请求

请求数据
<pre>
{cmd: "rpt.widget.panellist.search",  id, filter}
</pre>
返回格式：
<pre>
config：{
	title：{name:"",jumpid:""},
	data:[
		{id, values,template} 
	]
     maxcnt - 最大显示数
}
</pre>
title - 数据列表的标题
data - 数据集合，内部单项的数据和单条panel的数据一样
* id - panel.id
* values - jsonobj, key 是sql 语句的第一列，value是第二列，这些值将替换panel template中的变量


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
		//最大显示数
		int maxcnt = def.optInt("maxcnt",-1); 
		if(maxcnt!=-1){
			filter.put("maxcnt", maxcnt);                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   
		}
		
		if(Validator.isNotNull(sqlName)){
			//直接定义的sql语句，这里放置的ad_sql#name
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
		}else throw new NDSException("需要配置sql/sqlfun");
		if(values.length()<=0){
			values = new JSONArray();
		}
		config.put("values", values);
		//统一的客户端模式
		JSONObject ret=new JSONObject();
		this.copyKey(def, config, "template", false);
		ret.put("config", config);
		
		JSONObject panelObj= def.getJSONObject("panel");
		String panelTemplate = panelObj.optString("template");
		if(Validator.isNotNull(panelTemplate))  ret.put("panel", panelObj);
		else throw new NDSException("配置错误：必须提供单条数据模板属性");
		
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
	 * 重构panellist的数据
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
