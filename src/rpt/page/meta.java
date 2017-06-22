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

page��һ������ҳ�棬�� ���widget �� filter ��ɣ� ֧�ֱ��ʽ���֡�widget ����panel,grid,chart.

page id �ĺ�̨ad_sql name�������rpt:page:{{key}}


h3. ��̨����

<pre>
{title,config,list,filters}
</pre>

* title - string page ����
* config - jsonobj ��ʽ ,{theme} ����
<pre>
{ 
    theme: 'dark', //��ǰҳ����,echarts�ṩ������
}
</pre>
* list  - jsonarray ��ʽ��ÿ��Ԫ����{widget}, widget ��ָpanel,chart,grid�������������
 {id, type,title, width,height}
** id - string, widget Ψһid
** type -  string �̶�Ϊ"panel","chart","grid"
** title - string ����
** width - double ��Ļռ�ȣ���0.5��mobile�˺���
** height - double �߶�ռ��Ļ���ø߶ȱ���,HD�˺���

* filters - jsonobj ��ʽ{quick, advanced} 
** quick - [filter]
** advanced - [filter]

filterΪjsonobj, �������£�
<pre>
{column, key, desc, type, multiple, options, default, sql,sqlfunc, where} 
</pre>
* column - string ָ���ֶΣ��ֶ�����Ҫ�� "table.column"��ʽ����"c_store.name"�����Բ�����, ����ͨ���͵��ֶΣ�����ȡ�ֶε�����ֵ����¼���޶�Ϊ��ǰ�û��ɼ�,�����û��İ�ȫ��������)��Ϊoptions(���optionsδ����), ���ɵ�option��key��table.id, �������������ͣ�����������һ��
* key - ����ʾ�ڿͻ��˵�key��ʡ����ʹ��column�����columnҲδ���壬������
* desc - ������ʾ�����ƣ����Ϊ�գ���column.description����ʾ�� ���columnҲδ���壬������
* type - list | number | date | text Ŀǰ֧����4�����ͣ����������column��Ĭ�Ͻ���list
* multiple: boolean �Ƿ�֧�ֶ�ֵ��Ŀǰ����list �� date ���ͣ� number Ĭ�Ͼ���, Ĭ��Ϊfalse
* options ���õ�����½�����column�����ͣ�������ã�ǿ������type=list, ���list�Ŀ�ѡֵ [{key,value}] key|value ����string, ���� 
<pre>
{options: [{key: 13, value: "�����֮��"}, {key:1, value:"������ʢ"}] } 
</pre>
�ֱ�����ŵ�id���ŵ����ƣ�������ʾvalue���ɣ��ش���������key
* default string Ĭ��ֵ�������Ӣ�Ķ��ţ���ʾ��ֵ��������ǵ�ֵ
* sql - ��ad_sql.name ָ����һ��sql�������ȡ���ݣ�sql������2��select �ֶΣ���һ����key���ڶ�����value��֧�ֵĲ�����$userid, һ�����ã�����ͨ��column���Զ�����sql
* sqlfunc - ��һ��pl/sql function���ƣ��Ǳ�׼�Ľӿڣ�����sql���ֱ������, һ�����ã�����ͨ��column���Զ�����sql
* where - column �����������ʱ��ȡ�˶��壬��������������, ����column�Զ�����ʱ��Ч

����:
<pre>
{
    title:"���۷���"��
    config: {theme:"dark"},
    list:[
        {id:"rpt:widget:chart:sale1", type:"chart", width:0.5,height:0.5},
        {id:"rpt:widget:panel:sale1", type:"panel", width:0.5,height:0.5}
    ],
    filters:{
        quick:[
            {column:"c_store.name", where: "type='c'" },
            {key:"year",desc:"���", options:[1999,2001],default: 1999 }
        ],
        advanced:[
            {key:"amt",desc:"���", type:"number"}
        ]
    }
}
</pre>


h3. �ͻ�������

�������������ȡ������

{cmd:"rpt.page.meta", id}

* id - page id����ʽ "rpt.page.{{key}}"

���ص���Ϣ��ʽ
<pre>
{ id,title,config,list,filters}
</pre>
* id - string page id
�������Լ���̨����

h3. �ͻ������󵥸�widget��Ϣ

���� 
<pre>
{cmd: "rpt.widget.{{type}}.search", id, filter}
</pre>
* cmd - ��Ҫ��widget���ͣ��� "rpt.widget.chart.search"
* id -  string ָ��widget��Ψһid
* filter - jsonobj ��ǰ����Ĺ����������ͻ��˿��Ի�ȡ����ĵ�ǰ����ֵ�������ѯ����Ӧ��ʼ��Ĭ��ֵ��Ҳ�ӿͻ��˻��,�� key/value ��ʽ��ÿ��key���ǽ���Ĺ��������ֶ�name��ÿ��value�ǹ�������ѡ��ֵ��������jsonarray��string����ʽ

���ظ�ʽ��Ҫ��ѯ����widget�ľ����ʽ����

 * @author yfzhu
 *
 */
public class meta extends RptCmdHandler {
	private String pageId;

	/**
	 * 
	 * @param array elements are JSONArray [[]]
	 * @param errorKey ����ʱ��ʾ�ͻ��˵���䣬���ô�
	 * @return [{key, value}]
	 * @throws Exception
	 */
	private JSONArray toKeyValueArray(JSONArray array,String errorKey) throws Exception{
		JSONArray options=new JSONArray();
		logger.debug("toKeyValueArray("+ array+")");
		if(array!=null){
			if(array.length()>0 && !(array.opt(0) instanceof JSONArray)) throw new NDSException("���ô���:������Ҫ������2��:"+ errorKey);
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
	 * ���ݶ����������ͻ�����Ҫ��filter����, options �Ĺ���ģʽ
	 * column + where 
	 * sql
	 * sqlfunc
	 * options ֱ��д
	 * @param def {column, key, desc, type, multiple, options, default, sql,sqlfunc, where} 
	 * @return {
            key: 'year',
            desc: '���',
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
			if(true) throw new NDSException("��Ӳ�֧��column����");
//			TableManager manager=TableManager.getInstance();
//			Column col=manager.getColumn(column);
//			if(col==null) {
//				logger.error("column "+ column + " not exists in meta");
//				throw new NDSException("�ֶ�"+column+"����TableManager��");
//				
//			}
//			// ����column + id ����2�����ݵ�sql��䣬������id,�ڶ�����column
//			String sql=createSQLByColumn(col, def.optString("where"));
//			JSONArray qr=engine.doQueryJSONArray(sql, null, conn);
//			
//			JSONArray options= toKeyValueArray(qr, "��������:"+ column);
//			filter.put("options", options);
//			this.copyKey(def, filter, "key", column);
//			this.copyKey(def, filter, "desc", col.getNote());
//			this.copyKey(def, filter, "type", "list");
		}else if(Validator.isNotNull(sqlName)){
			//ֱ�Ӷ����sql��䣬������õ�ad_sql#name
			VelocityContext vc=VelocityUtils.createContext();
			vc.put("conn",conn);
			vc.put("userid", this.usr.getId());
			//vc.put("username",usr.getNkname());
			vc.put("comid",  usr.getComId());
			//vc.put("stid",  usr.getStoreId());
			//vc.put("empid",  usr.getEmpId());
			JSONArray qr=PhoneController.getInstance().getDataArrayByADSQL(sqlName, vc, conn, false);
			JSONArray options= toKeyValueArray(qr, "��������:"+ sqlName);
			this.copyKey(def, filter, "key", false);
			filter.put("options", options);
			this.copyKey(def, filter, "type", "list");
		}else if(Validator.isNotNull(sqlfunc)){
			this.copyKey(def, filter, "key", false);
			String key= filter.getString("key");
			String sql=getSQLByFunc(sqlfunc,pageId, key);
			JSONArray qr=engine.doQueryJSONArray(sql, null, conn);
			JSONArray options= toKeyValueArray(qr, "��������:"+ sqlfunc);
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
		
		//���´���default������������û���������
		this.copyKey(def, filter, "default", true);
		
		//��֧��
		
//		String defaultValue=filter.optString("default");
//		if(defaultValue!=null){
//			defaultValue=QueryUtils.replaceVariables(defaultValue, userWeb.getSession());
//			filter.put("default", defaultValue);
//		}
		return filter;
	}
//	/**
//	 * ���ݵ�ǰ�û���Ȩ�޹���sql��䣬��ʽ: select id, col from table where permision and addwhere
//	 * @param col  
//	 * @param addWhere �����where�����������ǰ׺ and�����������Լ���
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
	 * @param jo - contains id ��ʽ "rpt.page.{{key}}"
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
		
		//filter ��Ҫת������Ϊ������û�о���ֵ
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
