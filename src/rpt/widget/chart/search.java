package rpt.widget.chart;

import java.io.StringWriter;
import java.sql.Connection;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.json.JSONArray;
import org.json.JSONObject;

import rpt.RptCmdHandler;

import com.agilecontrol.nea.util.*;
import com.agilecontrol.nea.core.query.*;
import com.agilecontrol.nea.core.schema.*;
import com.agilecontrol.nea.core.security.Directory;
import com.agilecontrol.nea.core.velocity.DateUtil;
import com.agilecontrol.nea.core.velocity.VelocityUtils;
import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.phone.CmdHandler;
import com.agilecontrol.phone.CmdResult;
import com.agilecontrol.phone.PhoneController;
import com.agilecontrol.phone.PhoneUtils;
import com.agilecontrol.phone.PhoneController.SQLWithParams;
/**

h2. chart图形组件

chart 组件使用 http://echarts.baidu.com

h3. 后台定义

ad_sql.name: "rpt:widget:chart:{{key}}"
value:
<pre>
{ type: "chart", sqlfunc,sql, swap,isobj ,sqls, title, template, jumpid, tag,ds, mapsql }
</pre>

* sqlfunc  - "sqlfunc" 用来获取数据，见 #sqlfunc
* sql - string ad_sql.name，见 #sql
* swap - boolean 在sql/sqlfunc 设置时有效，指定sql/sqlfunc对应的查询结果是否需要行转列，默认false
* isobj - boolean 是否将查询结果的每一行以jsonobj的形式来构造，默认false, 如果否，每行都是jsonarray，swap参数为true的时候，这个参数无效，总是以jsonarray的方式来构造结果行数据* sqls - jsonarray, 各位复杂的多数据项构造式，见 #sqls (目前仅chart widget支持)
* sqls - jsonarray，在sql/sqlfunc未定义的时候读取，将无视swap/isobj参数
* title -  string 报表的标题
* template - string 报表的配置定义，是ad_sql.name所指定的详细报表config定义，可以不设定，默认的模板名称规则: "rpt:widget:chart:{{key}}:config"
* jumpid - string 点击报表某一个元素后的跳转, 这里设置page的id，如: "rpt:page:page1"， 在跳转报表的时候，需要传输所有当前元素的维度值到下一个报表的filter中，直接发起针对这些过滤条件的查询
* tag - string, 客户端的tag，原样传回客户端
* ds - string datasource 参数，在nea-ds.xml中定义，默认是"DataSource"
* mapsql - string，对应ad_sql.name，若当前组件是地图类型，需要设置此语句获取gps位置信息，sql语句样式: select name, x, y from c_store . 这里是按列号取值，顺序必须依次是: 位置名称，经度，维度。 位置名称可以是门店名或城市名，经度纬度按标准，例如: 上海，121.4648,31.2891 ，乌鲁木齐,87.9236,43.5883。注意这里的name需要与数据语句中的name 一致


h3. config定义

存放常规的配置定义项，存放在ad_sql 中的name对应的配置模板，建议的名称规则: "rpt",模板中使用以下变量

$rpt.title - 报表的标题
$rpt.names - 报表数据结果中的所有name字段的jsonarray.toString(), 一般用于构造legend.data 字段
$rpt.data - 二维数组 jsonarray.toString，目前暂定以key/value 的对象作为单行，与grid不同，数据一次性取出

生成的config 来读取其后的数据，举例
<pre>
config: {
            jumpId: "rpt:page:page1", //若要跳转,目标报表id,不能跳转则没有属性
            isjump: true,
            width: 0.6, //宽度占屏幕可以宽度比例,mobile端忽略
            height: 0.6, //高度占屏幕可用高度比例,HD端忽略
            option: {
                //使用echarts配置,原样传递,模板估值后,不做处理
                title: {
                    text: '销售状况',
                    x: 'left'
                },
                tooltip: {
                    trigger: 'item',
                    formatter: "{a} <br/>{b}: {c}万 ({d}%)" 
                },
                legend: {
                    orient: 'horizontal',
                    bottom: 0,
                    data: ['百货', '购物中心', '街店', '林特']
                },
                series: [{
                    name: '销售状况',
                    type: 'pie',
                    radius: ['50%', '70%'],
                    avoidLabelOverlap: false,
                    label: {
                        normal: {
                            show: true,
                            position: 'left'
                        }
                    },
                    labelLine: {
                        normal: {
                            show: false
                        }
                    },
                    data: [{
                        value: 211,
                        name: '百货'
                    }, {
                        value: 141,
                        name: '购物中心'
                    }, {
                        value: 191,
                        name: '街店'
                    }, {
                        value: 1,
                        name: '林特'
                    }]
                }]
            }
        }
</pre>

h3. 客户端请求

发出命令
<pre>
{cmd: "rpt.widget.chart.search", id, filter}
</pre>
* id - string widget的id, 格式: "rpt:widget:chart:{{key}}"
* filter - jsonarray {key:value} 其中key 是filters 定义的key, value 是用户选中的值， value 可以是string,也可以是array (多选范围)

返回格式：
<pre>
{option:{title,tooltip,legend,series}}
</pre>
详细定义见客户端wiki定义内容

h3. 关于mapsql

mapsql 用于地图类型的图表，一旦定义，将可以使用$rpt.mapdata 来获取数据。定义格式：
<pre>
data: $rpt.mapdata ;//针对单一配置sql的情况， 或
data: $rpt.mapdata[0] ;// 针对配置sqls 的情况
</pre>

datamap的格式：
<pre>
 [{name:"dataname", value: [x,y, datavalue]}]
</pre>

这里的dataname 需要从查询语句的第一列读取，datavalue 从查询语句的第二列读取，x, y 从name 对应到mapsql 的name列(首列)，如果未匹配到，将不在地图上显示（在server.log 里会打印日志便于核对）

 * @author yfzhu
 *
 */
public class search extends RptCmdHandler {
	private String widgetId;
	private String pageId;
	/**
	 * 获取所有的key
	 * @param data 【{key, value}】
	 * @return [key,...]
	 * @throws Exception
	 */
	private JSONArray getKeys(JSONArray data) throws Exception{
		JSONArray keys=new JSONArray();
		if(data.opt(0) instanceof JSONObject){
			for(int i=0;i<data.length();i++){
				JSONObject row=data.getJSONObject(i);
				String key=row.optString("name");
				if(Validator.isNotNull(key))
					keys.put(key);
			}
		}
		return keys;
	}
	
	/**
	 * 根据sqlMap 指定的语句生成 { "col1": [col2,col3]} 格式的数据
	 * @param sqlMap
	 * @return {"col1": [col2,col3]} 
	 * @throws Exception
	 */
	private  JSONObject loadGPS(String sqlMap,VelocityContext vc) throws Exception{
		JSONObject map=new JSONObject();
		JSONArray data= PhoneController.getInstance().getDataArrayByADSQL(sqlMap, vc, conn, false);
		for(int i=0;i<data.length();i++){
			JSONArray row=data.getJSONArray(i);
			try{
				String key=row.optString(0);
				double x=row.optDouble(1);
				double y=row.optDouble(2);
				
				JSONArray xy=new JSONArray();
				xy.put(x);xy.put(y);
				map.put(key, xy);
			}catch(Exception ex){
				logger.error("check sqlMap#"+ row, ex );
				throw new NDSException("map定义出错，检查数据"+ sqlMap+"#"+ row);
			}
		}
		
		return map;
		
		
	}
	/**
	 * 将data 数据格式转换为map 的数据格式
	 * @param data [{name, value}] 这些列名是必须的
	 * @param mapGPS {name: [x,y]}  如果name在mapGPS未匹配到，将不在地图上显示（在server.log 里会打印日志便于核对）
	 * @return jsonarray [{name, value:[x,y,value]}]
	 * @throws Exception
	 */
	private JSONArray convertToMapData(JSONArray data, JSONObject mapGPS) throws Exception{
		JSONArray mapData=new JSONArray();
		if(data.length()>0 && !(data.opt(0) instanceof JSONObject)){
			logger.error("not jsonobj type of element:"+ data.toString());
			throw new NDSException("配置错误:地图数据必须是jsonobj类型, 请查看server.log了解数据格式错误");
		}
		for(int i=0;i<data.length();i++){
			JSONObject one=data.getJSONObject(i);
			String name=one.optString("name");
			if(Validator.isNull(name)) throw new NDSException("地图对应的数据查询语句中需要有name字段代表地图点，比如城市名或店名");
			double value=one.optDouble("value", Double.NaN);
			if(value==Double.NaN) throw new NDSException("地图对应的数据查询语句中需要有value字段代表值");
			JSONArray xy=mapGPS.optJSONArray(name);
			if(xy==null){
				logger.debug("Not found gps for name="+ name);
				//skip
			}else{
				//merge
				JSONArray values=new JSONArray();
				values.put(xy.getDouble(0));
				values.put(xy.getDouble(1));
				values.put(value);
				JSONObject mapOne=new JSONObject();
				mapOne.put("name",name).put("value", values);
				mapData.put(mapOne);
			}
		}
		return mapData;
	}
	
	@Override
	public CmdResult execute(JSONObject jo) throws Exception {
		widgetId= getString(jo, "id");
		pageId=getString(jo, "pageid");
		JSONObject filter= jo.optJSONObject("filter");
		if(filter==null)filter=new JSONObject();
		/**
		 { type: "chart", sqlfunc, title, template, jumpid, ds, sqlmap  }		 */
		JSONObject def= (JSONObject)PhoneController.getInstance().getValueFromADSQLAsJSON(widgetId, conn, false);
		String templateName= def.optString("template");
		if(Validator.isNull(templateName)) templateName= widgetId+":config";
		String template=PhoneController.getInstance().getValueFromADSQL(templateName, conn);
		if(Validator.isNull(template)) throw new NDSException("需要定义ad_sql#"+ templateName);
		
		String jumpId= def.optString("jumpid");
		
		JSONObject rpt=new JSONObject();
		String title= def.optString("title");
		if(Validator.isNull(title)) title="";
		rpt.put("title", title);
		
		//创建velocity 环境，sql/sqls 等都需要
		VelocityContext vc=this.createVelocityContext(null);
		Connection dsconn=getConnection(def);
		vc.put("filter", filter);
		vc.put("dsconn", dsconn);
		vc.put("rpt", rpt);
		
		String sqlName=def.optString("sql");
		String sqlfunc=def.optString("sqlfunc");
		boolean swap=def.optBoolean("swap",false);//行列颠倒
		boolean isobj=def.optBoolean("isobj",false);
		JSONArray sqls=def.optJSONArray("sqls");
		String sqlMap=def.optString("sqlmap");
		JSONObject mapGPS=null;
		if(Validator.isNotNull(sqlMap)) mapGPS=loadGPS(sqlMap, vc);
		if(Validator.isNotNull(sqlName)){
			//直接定义的sql语句，这里放置的ad_sql#name
			
			SQLWithParams sp= PhoneController.getInstance().parseADSQL(sqlName, vc,conn);
			
			JSONResultSet jrs=(swap||!isobj)?engine.doQueryArrayResultSet(sp.getSQL(), sp.getParams(),dsconn): engine.doQueryObjectResultSet(sp.getSQL(), sp.getParams(),dsconn);
			
			//result data
			JSONArray data=jrs.getData();
			logger.debug("result of "+ sqlName+":"+ data);
			
			if(swap) data=JSONUtils.swapMatrix(data);
			rpt.put("data", data);
			if(mapGPS!=null){
				rpt.put("mapdata", convertToMapData(data,mapGPS));
			}
			//result names
			rpt.put("names", getKeys(data));
			
		}else if(Validator.isNotNull(sqlfunc)){
			
			String sql=getSQLByFunc(sqlfunc,pageId,widgetId,filter);
			JSONResultSet jrs=(swap||!isobj)?engine.doQueryArrayResultSet(sql,null,dsconn):engine.doQueryObjectResultSet(sql, null, dsconn);
			
			//result data
			JSONArray data=jrs.getData();
			logger.debug("result of "+ sql+":"+ data);
			
			if(swap) data=JSONUtils.swapMatrix(data);
			rpt.put("data", data);
			if(mapGPS!=null){
				rpt.put("mapdata", convertToMapData(data,mapGPS));
			}
			//result names
			rpt.put("names", getKeys(data));

		}else if(sqls!=null){
			//sqls: [  {sql, sqlfunc, swap} ]
			JSONArray data=new JSONArray();
			JSONArray mapdata=new JSONArray();
			for(int i=0;i<sqls.length();i++){
				JSONObject one=sqls.getJSONObject(i);
				sqlName= one.optString("sql");
				sqlfunc=one.optString("sqlfunc");
				boolean sqlswap=one.optBoolean("swap", false);
				boolean sqlisobj=one.optBoolean("isobj",false);
				//是用于map展示的数据？最大值最小值是不用于map画点的，需要设置为false
				boolean ismap=one.optBoolean("ismap",true);
				
				if(Validator.isNotNull(sqlName)){
					//直接定义的sql语句，这里放置的ad_sql#name
					SQLWithParams sp= PhoneController.getInstance().parseADSQL(sqlName, vc,conn);
					
					
					JSONResultSet jrs=(sqlswap||!sqlisobj)?engine.doQueryArrayResultSet(sp.getSQL(), sp.getParams(),dsconn): engine.doQueryObjectResultSet(sp.getSQL(), sp.getParams(),dsconn);
					//result data
					JSONArray dt=jrs.getData();
					logger.debug("result of "+ sqlName+":"+ dt);
					if(sqlswap){
						//only swap data, not mapdata
						dt=JSONUtils.swapMatrix(dt);
					}
					data.put(dt);
					if(mapGPS!=null && ismap){
						mapdata.put(convertToMapData(dt,mapGPS));
					}
					
				}else if(Validator.isNotNull(sqlfunc)){
					
					String sql=getSQLByFunc(sqlfunc,null,widgetId,filter);
					JSONResultSet jrs=(sqlswap||!sqlisobj)? engine.doQueryArrayResultSet(sql, null,dsconn): engine.doQueryObjectResultSet(sql, null,dsconn);
					
					//result data
					JSONArray dt=jrs.getData();
					logger.debug("result of "+ sql+":"+ dt);
					if(sqlswap){
						//only swap data, not mapdata
						dt=JSONUtils.swapMatrix(dt);
					}
					
					data.put(dt);
					if(mapGPS!=null && ismap){
						mapdata.put(convertToMapData(dt,mapGPS));
					}

				}else{
					throw new NDSException("需要在sqls中的每个元素对象中配置sql/sqlfun");
				}
				
			}
			//sqls不使用这个参数
			//if(swap) data=JSONUtils.swapMatrix(data);
			rpt.put("data", data);//[[row1.col1,row1.col2...], [row2.col1, row2.col2]...]
			
			rpt.put("mapdata",mapdata ); //[[{name,value}]]
			
		}else throw new NDSException("需要配置sql/sqlfun/sqls");
		

		StringWriter output = new StringWriter();
		Velocity.evaluate(vc, output, VelocityUtils.class.getName(), template);
		String config=output.toString();
		
		JSONObject ret=new JSONObject();
		try{
			ret.put("config", new JSONObject(config));
		}catch(Throwable tx){
			logger.error("Fail to parse to jsonobject:"+ config, tx);
			throw new NDSException("ad_sql#"+templateName+"配置有问题,查看server.log获取详情");
		}
		//新增chart类型的过滤参数jumpkey
		String jumpkey = def.optString("jumpkey","");
		if (Validator.isNotNull(jumpkey)) {
			ret.put("jumpkey", jumpkey);
		}
		
		ret.put("id",  widgetId);
		ret.put("type",  "chart");
		this.copyKey(jo, ret, "tag", true);
		String jumpPageId= def.optString("jumpid");
		if(Validator.isNotNull(jumpPageId))  ret.put("jumpid", jumpPageId);

		return new CmdResult(ret);
	}

}
