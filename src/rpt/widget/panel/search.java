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

h2. panel面板组件

panel和grid,chart 一样也是报表组件，支持表格式布局，每个单元格称为panelcell, 一般由文字或数字组成, panelid 格式："rpt:widget:panel:{{key}}"

每个panel 都有对应的html template，template 文件存储在客户端约定的目录，名称为 panelid 中key.html ，模板中支持若干参数key，由sqlfunc对应的sql语句来给出

h3. 后台定义

ad_sql.name： 格式 "rpt:widget:panel:{{key}}"

value:
<pre>
{ sqlfunc, sql,template, jumpid}
</pre>
* sqlfunc - oracle function名称，见 #sqlfunc，这里要求sql返回的列是2列，第一列是在template中的key， 第二列是要放置的内容值
* sql - ad_sql.name所对应的sql语句，支持的参数包括: $userid, $panelid
* jumpid - jump page id

h3. 客户端请求

请求数据
<pre>
{cmd: "rpt.widget.panel.search",  id, filter}
</pre>
返回格式：
<pre>
{id, values,template} 
</pre>
* id - panel.id
* values - jsonobj, key 是sql 语句的第一列，value是第二列，这些值将替换panel template中的变量

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
			//直接定义的sql语句，这里放置的ad_sql#name
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
		}else throw new NDSException("需要配置sql/sqlfun");
		config.put("values", values);
		//统一的客户端模式
		JSONObject ret=new JSONObject();
		this.copyKey(def, config, "template", false);
		ret.put("config", config);
		
		String jumpPageId= def.optString("jumpid");
		if(Validator.isNotNull(jumpPageId))  ret.put("jumpid", jumpPageId);
//		else throw new NDSException("配置错误：必须提供jumpid属性");
		ret.put("id", widgetId);
		ret.put("type",  "panel");
		this.copyKey(jo, ret, "tag", true);
		return new CmdResult(ret);
	}
	/**
	 * 重构panel的数据
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
