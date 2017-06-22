package rpt.page;

import org.apache.velocity.VelocityContext;
import org.json.JSONArray;
import org.json.JSONObject;

import rpt.RptCmdHandler;

import com.agilecontrol.nea.util.*;
import com.agilecontrol.nea.core.query.*;
import com.agilecontrol.b2b.schema.*;
import com.agilecontrol.nea.core.security.Directory;
import com.agilecontrol.nea.core.velocity.VelocityUtils;
import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.phone.CmdHandler;
import com.agilecontrol.phone.CmdResult;
import com.agilecontrol.phone.PhoneController;
import com.agilecontrol.phone.PhoneUtils;
/**

page是一个报表页面，由 多个widget 和 filter 组成， 支持表格式布局。widget 包括panel,grid,chart.

page id 的后台ad_sql name定义规则：rpt:page:{{key}}


h3. 后台定义

<pre>
{title,config,list,filters}
</pre>

* title - string page 标题
* config - jsonobj 格式 ,{theme} 举例
<pre>
{ 
    theme: 'dark', //当前页主题,echarts提供的六种
}
</pre>
* list  - jsonarray 格式，每个元素是{widget}, widget 是指panel,chart,grid类型组件，定义
 {id, type,title, width,height}
** id - string, widget 唯一id
** type -  string 固定为"panel","chart","grid"
** title - string 标题
** width - double 屏幕占比，如0.5，mobile端忽略
** height - double 高度占屏幕可用高度比例,HD端忽略

* filters - jsonobj 格式{quick, advanced} 
** quick - [filter]
** advanced - [filter]

filter为jsonobj, 定义如下：
<pre>
{column, key, desc, type, multiple, options, default, sql,sqlfunc, where} 
</pre>
* column - string 指定字段，字段名需要是 "table.column"形式，如"c_store.name"，可以不设置, 对普通类型的字段，将获取字段的所有值（记录将限定为当前用户可见,叠加用户的安全过滤条件)作为options(如果options未定义), 构成的option的key是table.id, 对于下拉框类型，与下拉宽定义一致
* key - 将显示在客户端的key，省略是使用column，如果column也未定义，将报错
* desc - 界面显示的名称，如果为空，用column.description来显示， 如果column也未定义，将报错
* type - list | number | date | text 目前支持这4种类型，如果设置了column，默认将是list
* multiple: boolean 是否支持多值，目前面向list 和 date 类型， number 默认就是, 默认为false
* options 设置的情况下将无视column的类型，如果设置，强制设置type=list, 针对list的可选值 [{key,value}] key|value 都是string, 比如 
<pre>
{options: [{key: 13, value: "虹口龙之梦"}, {key:1, value:"淮海百盛"}] } 
</pre>
分别代表门店id和门店名称，界面显示value即可，回传服务器用key
* default string 默认值，如果有英文逗号，表示多值，否则就是单值
* sql - 是ad_sql.name 指定了一条sql语句来获取数据，sql必须有2个select 字段，第一个是key，第二个是value，支持的参数有$userid, 一旦设置，不会通过column来自动构造sql
* sqlfunc - 是一个pl/sql function名称，是标准的接口，返回sql语句直接运行, 一旦设置，不会通过column来自动构造sql
* where - column 构造过滤条件时读取此定义，不错到过滤条件中, 仅在column自动构造时有效

举例:
<pre>
{
    title:"销售分析"，
    config: {theme:"dark"},
    list:[
        {id:"rpt:widget:chart:sale1", type:"chart", width:0.5,height:0.5},
        {id:"rpt:widget:panel:sale1", type:"panel", width:0.5,height:0.5}
    ],
    filters:{
        quick:[
            {column:"c_store.name", where: "type='c'" },
            {key:"year",desc:"年份", options:[1999,2001],default: 1999 }
        ],
        advanced:[
            {key:"amt",desc:"金额", type:"number"}
        ]
    }
}
</pre>


h3. 客户端请求

发出以下命令获取报表布局

{cmd:"rpt.page.meta", id}

* id - page id，格式 "rpt.page.{{key}}"

返回的信息格式
<pre>
{ id,title,config,list,filters}
</pre>
* id - string page id
其他属性见后台定义

h3. 客户端请求单个widget信息

命令 
<pre>
{cmd: "rpt.widget.{{type}}.search", id, filter}
</pre>
* cmd - 需要有widget类型，如 "rpt.widget.chart.search"
* id -  string 指定widget的唯一id
* filter - jsonobj 当前界面的过滤条件，客户端可以获取界面的当前过滤值，发起查询，对应初始的默认值，也从客户端获得,是 key/value 形式，每个key都是界面的过滤条件字段name，每个value是过滤条件选项值，可以是jsonarray或string等形式

返回格式需要查询各个widget的具体格式定义

 * @author yfzhu
 *
 */
public class meta extends RptCmdHandler {
	private String pageId;

	/**
	 * 
	 * @param array elements are JSONArray [[]]
	 * @param errorKey 报错时提示客户端的语句，配置错
	 * @return [{key, value}]
	 * @throws Exception
	 */
	private JSONArray toKeyValueArray(JSONArray array,String errorKey) throws Exception{
		JSONArray options=new JSONArray();
		logger.debug("toKeyValueArray("+ array+")");
		if(array!=null){
			if(array.length()>0 && !(array.opt(0) instanceof JSONArray)) throw new NDSException("配置错误:数据需要有至少2列:"+ errorKey);
			for(int i=0;i<array.length();i++){
			JSONArray row=array.getJSONArray(i);
			JSONObject one=new JSONObject();
			one.put("key", row.get(0));
			one.put("value", row.get(1));
			options.put(one);
			}
		}
		return options;
	}
	/**
	 * 根据定义来创建客户端需要的filter对象, options 的构造模式
	 * column + where 
	 * sql
	 * sqlfunc
	 * options 直接写
	 * @param def {column, key, desc, type, multiple, options, default, sql,sqlfunc, where} 
	 * @return {
            key: 'year',
            desc: '年份',
            type: 'list',
            options: ['2013', '2014', '2015'],
            multiple: boolean
            default: '2013'
        }
	 * @throws Exception
	 */
	private JSONObject createFilter(JSONObject def) throws Exception{
		JSONObject filter=new JSONObject();
		String column=def.optString("column");
		String sqlName=def.optString("sql");
		String sqlfunc=def.optString("sqlfunc");
		if(Validator.isNotNull(column)){
			if(true) throw new NDSException("麦加不支持column定义");
//			TableManager manager=TableManager.getInstance();
//			Column col=manager.getColumn(column);
//			if(col==null) {
//				logger.error("column "+ column + " not exists in meta");
//				throw new NDSException("字段"+column+"不在TableManager中");
//				
//			}
//			// 根据column + id 构建2列数据的sql语句，首列是id,第二列是column
//			String sql=createSQLByColumn(col, def.optString("where"));
//			JSONArray qr=engine.doQueryJSONArray(sql, null, conn);
//			
//			JSONArray options= toKeyValueArray(qr, "过滤条件:"+ column);
//			filter.put("options", options);
//			this.copyKey(def, filter, "key", column);
//			this.copyKey(def, filter, "desc", col.getNote());
//			this.copyKey(def, filter, "type", "list");
		}else if(Validator.isNotNull(sqlName)){
			//直接定义的sql语句，这里放置的ad_sql#name
			VelocityContext vc=VelocityUtils.createContext();
			vc.put("conn",conn);
			vc.put("userid", this.usr.getId());
			//vc.put("username",usr.getNkname());
			vc.put("comid",  usr.getComId());
			//vc.put("stid",  usr.getStoreId());
			//vc.put("empid",  usr.getEmpId());
			JSONArray qr=PhoneController.getInstance().getDataArrayByADSQL(sqlName, vc, conn, false);
			JSONArray options= toKeyValueArray(qr, "过滤条件:"+ sqlName);
			this.copyKey(def, filter, "key", false);
			filter.put("options", options);
			this.copyKey(def, filter, "type", "list");
		}else if(Validator.isNotNull(sqlfunc)){
			this.copyKey(def, filter, "key", false);
			String key= filter.getString("key");
			String sql=getSQLByFunc(sqlfunc,pageId, key);
			JSONArray qr=engine.doQueryJSONArray(sql, null, conn);
			JSONArray options= toKeyValueArray(qr, "过滤条件:"+ sqlfunc);
			filter.put("options", options);
			this.copyKey(def, filter, "type", "list");
		}else{
			logger.debug("not set sql/sqlfunc");
			this.copyKey(def, filter, "key", false);
			this.copyKey(def, filter, "type", false);
			this.copyKey(def, filter, "options", false);
		}
		//override if set
		this.copyKey(def, filter, "desc", true);
		this.copyKey(def, filter, "multiple", true);
		this.copyKey(def, filter, "replacekey", true);
		
		//重新处理default，如果设置了用户环境变量
		this.copyKey(def, filter, "default", true);
		
		//不支持
		
//		String defaultValue=filter.optString("default");
//		if(defaultValue!=null){
//			defaultValue=QueryUtils.replaceVariables(defaultValue, userWeb.getSession());
//			filter.put("default", defaultValue);
//		}
		return filter;
	}
//	/**
//	 * 根据当前用户的权限构造sql语句，格式: select id, col from table where permision and addwhere
//	 * @param col  
//	 * @param addWhere 补充的where条件，将添加前缀 and，其他都靠自己了
//	 * @return
//	 * @throws Exception
//	 */
//	private String createSQLByColumn(Column col, String addWhere) throws Exception{
//		QueryRequestImpl query=engine.createRequest();
//		query.setMainTable(col.getTable().getId());
//		query.addSelection(col.getTable().getPrimaryKey().getId());
//		query.addSelection(col.getId());
//		query.addParam(userWeb.getSecurityFilter(col.getTable().getName(), Directory.READ));
//		if(Validator.isNotNull(addWhere))query.addParam(addWhere);
//		query.addOrderBy(new int[]{col.getId()}, true);
//		
//		return query.toSQL();
//		
//	}
	
	/**
	 * @param jo - contains id 格式 "rpt.page.{{key}}"
	 * 
	 */
	public CmdResult execute(JSONObject jo) throws Exception {
		pageId= getString(jo, "id");
		JSONObject pageDef= (JSONObject)PhoneController.getInstance().getValueFromADSQLAsJSON(pageId, conn, false);
		
		
		JSONObject page=new JSONObject();
		page.put("id", pageId);
		this.copyKey(pageDef, page, "title", true);
		this.copyKey(pageDef, page, "config", new JSONObject());
		this.copyKey(pageDef, page, "list", new JSONArray());
		
		//filter 需要转换，因为定义里没有具体值
		JSONObject  filtersDef= pageDef.optJSONObject("filters");
		JSONObject flts=new JSONObject();
		JSONArray quick= new JSONArray();
		flts.put("quick",quick);
		JSONArray advanced =new JSONArray();
		flts.put("advanced", advanced);
		JSONArray toggle = new JSONArray();
		flts.put("toggle", toggle);
		if(filtersDef==null){
			page.put("filters", flts);
		}else{
			JSONArray quickDef= filtersDef.optJSONArray("quick");
			if(quickDef!=null)for(int i=0;i<quickDef.length();i++){
				JSONObject filter=createFilter(quickDef.getJSONObject(i));
				quick.put(filter);
			}
			
			JSONArray advanceDef= filtersDef.optJSONArray("advanced");
			if(advanceDef!=null)for(int i=0;i<advanceDef.length();i++){
				JSONObject filter=createFilter(advanceDef.getJSONObject(i));
				advanced.put(filter);
			}
			
			JSONArray toggleDef= filtersDef.optJSONArray("toggle");
			if(toggleDef!=null)for(int i=0;i<toggleDef.length();i++){
				JSONObject filter=toggleDef.getJSONObject(i);
				toggle.put(filter);
			}
			page.put("filters", flts);
		}
		page.put("type",  "page");
		return new CmdResult(page);
	}

}
