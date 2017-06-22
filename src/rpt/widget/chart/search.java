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

h2. chartͼ�����

chart ���ʹ�� http://echarts.baidu.com

h3. ��̨����

ad_sql.name: "rpt:widget:chart:{{key}}"
value:
<pre>
{ type: "chart", sqlfunc,sql, swap,isobj ,sqls, title, template, jumpid, tag,ds, mapsql }
</pre>

* sqlfunc  - "sqlfunc" ������ȡ���ݣ��� #sqlfunc
* sql - string ad_sql.name���� #sql
* swap - boolean ��sql/sqlfunc ����ʱ��Ч��ָ��sql/sqlfunc��Ӧ�Ĳ�ѯ����Ƿ���Ҫ��ת�У�Ĭ��false
* isobj - boolean �Ƿ񽫲�ѯ�����ÿһ����jsonobj����ʽ�����죬Ĭ��false, �����ÿ�ж���jsonarray��swap����Ϊtrue��ʱ�����������Ч��������jsonarray�ķ�ʽ��������������* sqls - jsonarray, ��λ���ӵĶ��������ʽ���� #sqls (Ŀǰ��chart widget֧��)
* sqls - jsonarray����sql/sqlfuncδ�����ʱ���ȡ��������swap/isobj����
* title -  string ����ı���
* template - string ��������ö��壬��ad_sql.name��ָ������ϸ����config���壬���Բ��趨��Ĭ�ϵ�ģ�����ƹ���: "rpt:widget:chart:{{key}}:config"
* jumpid - string �������ĳһ��Ԫ�غ����ת, ��������page��id����: "rpt:page:page1"�� ����ת�����ʱ����Ҫ�������е�ǰԪ�ص�ά��ֵ����һ�������filter�У�ֱ�ӷ��������Щ���������Ĳ�ѯ
* tag - string, �ͻ��˵�tag��ԭ�����ؿͻ���
* ds - string datasource ��������nea-ds.xml�ж��壬Ĭ����"DataSource"
* mapsql - string����Ӧad_sql.name������ǰ����ǵ�ͼ���ͣ���Ҫ���ô�����ȡgpsλ����Ϣ��sql�����ʽ: select name, x, y from c_store . �����ǰ��к�ȡֵ��˳�����������: λ�����ƣ����ȣ�ά�ȡ� λ�����ƿ������ŵ����������������γ�Ȱ���׼������: �Ϻ���121.4648,31.2891 ����³ľ��,87.9236,43.5883��ע�������name��Ҫ����������е�name һ��


h3. config����

��ų�������ö���������ad_sql �е�name��Ӧ������ģ�壬��������ƹ���: "rpt",ģ����ʹ�����±���

$rpt.title - ����ı���
$rpt.names - �������ݽ���е�����name�ֶε�jsonarray.toString(), һ�����ڹ���legend.data �ֶ�
$rpt.data - ��ά���� jsonarray.toString��Ŀǰ�ݶ���key/value �Ķ�����Ϊ���У���grid��ͬ������һ����ȡ��

���ɵ�config ����ȡ�������ݣ�����
<pre>
config: {
            jumpId: "rpt:page:page1", //��Ҫ��ת,Ŀ�걨��id,������ת��û������
            isjump: true,
            width: 0.6, //���ռ��Ļ���Կ�ȱ���,mobile�˺���
            height: 0.6, //�߶�ռ��Ļ���ø߶ȱ���,HD�˺���
            option: {
                //ʹ��echarts����,ԭ������,ģ���ֵ��,��������
                title: {
                    text: '����״��',
                    x: 'left'
                },
                tooltip: {
                    trigger: 'item',
                    formatter: "{a} <br/>{b}: {c}�� ({d}%)" 
                },
                legend: {
                    orient: 'horizontal',
                    bottom: 0,
                    data: ['�ٻ�', '��������', '�ֵ�', '����']
                },
                series: [{
                    name: '����״��',
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
                        name: '�ٻ�'
                    }, {
                        value: 141,
                        name: '��������'
                    }, {
                        value: 191,
                        name: '�ֵ�'
                    }, {
                        value: 1,
                        name: '����'
                    }]
                }]
            }
        }
</pre>

h3. �ͻ�������

��������
<pre>
{cmd: "rpt.widget.chart.search", id, filter}
</pre>
* id - string widget��id, ��ʽ: "rpt:widget:chart:{{key}}"
* filter - jsonarray {key:value} ����key ��filters �����key, value ���û�ѡ�е�ֵ�� value ������string,Ҳ������array (��ѡ��Χ)

���ظ�ʽ��
<pre>
{option:{title,tooltip,legend,series}}
</pre>
��ϸ������ͻ���wiki��������

h3. ����mapsql

mapsql ���ڵ�ͼ���͵�ͼ��һ�����壬������ʹ��$rpt.mapdata ����ȡ���ݡ������ʽ��
<pre>
data: $rpt.mapdata ;//��Ե�һ����sql������� ��
data: $rpt.mapdata[0] ;// �������sqls �����
</pre>

datamap�ĸ�ʽ��
<pre>
 [{name:"dataname", value: [x,y, datavalue]}]
</pre>

�����dataname ��Ҫ�Ӳ�ѯ���ĵ�һ�ж�ȡ��datavalue �Ӳ�ѯ���ĵڶ��ж�ȡ��x, y ��name ��Ӧ��mapsql ��name��(����)�����δƥ�䵽�������ڵ�ͼ����ʾ����server.log ����ӡ��־���ں˶ԣ�

 * @author yfzhu
 *
 */
public class search extends RptCmdHandler {
	private String widgetId;
	private String pageId;
	/**
	 * ��ȡ���е�key
	 * @param data ��{key, value}��
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
	 * ����sqlMap ָ����������� { "col1": [col2,col3]} ��ʽ������
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
				throw new NDSException("map��������������"+ sqlMap+"#"+ row);
			}
		}
		
		return map;
		
		
	}
	/**
	 * ��data ���ݸ�ʽת��Ϊmap �����ݸ�ʽ
	 * @param data [{name, value}] ��Щ�����Ǳ����
	 * @param mapGPS {name: [x,y]}  ���name��mapGPSδƥ�䵽�������ڵ�ͼ����ʾ����server.log ����ӡ��־���ں˶ԣ�
	 * @return jsonarray [{name, value:[x,y,value]}]
	 * @throws Exception
	 */
	private JSONArray convertToMapData(JSONArray data, JSONObject mapGPS) throws Exception{
		JSONArray mapData=new JSONArray();
		if(data.length()>0 && !(data.opt(0) instanceof JSONObject)){
			logger.error("not jsonobj type of element:"+ data.toString());
			throw new NDSException("���ô���:��ͼ���ݱ�����jsonobj����, ��鿴server.log�˽����ݸ�ʽ����");
		}
		for(int i=0;i<data.length();i++){
			JSONObject one=data.getJSONObject(i);
			String name=one.optString("name");
			if(Validator.isNull(name)) throw new NDSException("��ͼ��Ӧ�����ݲ�ѯ�������Ҫ��name�ֶδ����ͼ�㣬��������������");
			double value=one.optDouble("value", Double.NaN);
			if(value==Double.NaN) throw new NDSException("��ͼ��Ӧ�����ݲ�ѯ�������Ҫ��value�ֶδ���ֵ");
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
		if(Validator.isNull(template)) throw new NDSException("��Ҫ����ad_sql#"+ templateName);
		
		String jumpId= def.optString("jumpid");
		
		JSONObject rpt=new JSONObject();
		String title= def.optString("title");
		if(Validator.isNull(title)) title="";
		rpt.put("title", title);
		
		//����velocity ������sql/sqls �ȶ���Ҫ
		VelocityContext vc=this.createVelocityContext(null);
		Connection dsconn=getConnection(def);
		vc.put("filter", filter);
		vc.put("dsconn", dsconn);
		vc.put("rpt", rpt);
		
		String sqlName=def.optString("sql");
		String sqlfunc=def.optString("sqlfunc");
		boolean swap=def.optBoolean("swap",false);//���еߵ�
		boolean isobj=def.optBoolean("isobj",false);
		JSONArray sqls=def.optJSONArray("sqls");
		String sqlMap=def.optString("sqlmap");
		JSONObject mapGPS=null;
		if(Validator.isNotNull(sqlMap)) mapGPS=loadGPS(sqlMap, vc);
		if(Validator.isNotNull(sqlName)){
			//ֱ�Ӷ����sql��䣬������õ�ad_sql#name
			
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
				//������mapչʾ�����ݣ����ֵ��Сֵ�ǲ�����map����ģ���Ҫ����Ϊfalse
				boolean ismap=one.optBoolean("ismap",true);
				
				if(Validator.isNotNull(sqlName)){
					//ֱ�Ӷ����sql��䣬������õ�ad_sql#name
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
					throw new NDSException("��Ҫ��sqls�е�ÿ��Ԫ�ض���������sql/sqlfun");
				}
				
			}
			//sqls��ʹ���������
			//if(swap) data=JSONUtils.swapMatrix(data);
			rpt.put("data", data);//[[row1.col1,row1.col2...], [row2.col1, row2.col2]...]
			
			rpt.put("mapdata",mapdata ); //[[{name,value}]]
			
		}else throw new NDSException("��Ҫ����sql/sqlfun/sqls");
		

		StringWriter output = new StringWriter();
		Velocity.evaluate(vc, output, VelocityUtils.class.getName(), template);
		String config=output.toString();
		
		JSONObject ret=new JSONObject();
		try{
			ret.put("config", new JSONObject(config));
		}catch(Throwable tx){
			logger.error("Fail to parse to jsonobject:"+ config, tx);
			throw new NDSException("ad_sql#"+templateName+"����������,�鿴server.log��ȡ����");
		}
		//����chart���͵Ĺ��˲���jumpkey
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
