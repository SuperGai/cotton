package rpt.widget.grid;

import java.io.File;
import java.io.FileOutputStream;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.json.JSONArray;
import org.json.JSONObject;

import rpt.RptCmdHandler;
import rpt.widget.grid.TreeConverter.TreeNode;

import com.agilecontrol.nea.util.JSONUtils;
import com.agilecontrol.nea.util.Tools;
import com.agilecontrol.nea.util.Validator;
import com.agilecontrol.nea.core.control.web.UserWebImpl;
import com.agilecontrol.nea.core.query.*;
import com.agilecontrol.nea.core.schema.*;
import com.agilecontrol.nea.core.security.Directory;
import com.agilecontrol.nea.core.util.ConfigValues;
import com.agilecontrol.nea.core.velocity.VelocityUtils;
import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.phone.CmdHandler;
import com.agilecontrol.phone.CmdResult;
import com.agilecontrol.phone.PhoneController;
import com.agilecontrol.phone.PhoneUtils;
import com.agilecontrol.phone.PhoneController.SQLWithParams;

/**

h2. grid������

grid ������, �ײ�js��� http://www.ag-grid.com

h3. ��̨����

ad_sql���Ƹ�ʽ "rpt:widget:grid:{{key}}", ��ʾ��Ӧid

<pre>
{
    header: "sqlfunc",
    data: "sqlfunc",
    type: "grid",
    coldef: jsonobj,
    jumpid: "pageid"
}
</pre>


h3. header ��������

ָ��һ��plsql function ����sql��䣬�� #sqlfunc, java���˸�ʽת��Ϊ�ͻ�����Ҫ�ĸ�ʽ����ʽ�磺

<pre>
[{
    headerName: "Athlete Details",
    children: [{
        headerName: "Athlete",
        field: "athlete",
        width: 150,
        filter: 'text'
    }, {
        headerName: "Age",
        field: "age",
        width: 90,
        filter: 'number'
    }, {
        headerName: "Country",
        field: "country",
        width: 120
    }]
},{
    headerName,children
  }
]
</pre>

����μ�wikiǰ�����˵���ĵ���Lily����ǰ����ơ������У� headerName��sql��ֵ��filter��sql���ֶ�����width �� colwidth�ж�ȡ�� filter ���ֶ����ͣ�Ŀǰ��֧��: number,text

h3. data ��������

data ��sqlfunc ����� #sqlfunc

data ��ͻ��˵������ʽ

<pre>
 [{
    "athlete": "Michael Phelps",
    "age": 23,
    "country": "United States",
    "year": 2008,
    "date": "24/08/2008",
    "sport": "Swimming",
    "gold": 8,
    "silver": 0,
    "bronze": 0,
    "total": 8
},...]
</pre>

ע�⵽ÿ�ж���jsonobj����key/value ����ʽ

h3. type ��������

ĿǰΪ "grid" ��ʾ�Ǳ��

h3. jumpid ��������

jumpid ��Ҫ��ת�ı���(page)��id������ת����󣬽�Ĭ�϶�ȡfield�ֶ�����Ϊfilter��key����ǰ�е�value��Ϊvalue������Ӧ����page

h3. coldef ��������

coldef ������ֶε���չ���Զ��壬jsonobj, key���ֶα�ʶ�� value��jsonobj���μ�https://www.ag-grid.com/javascript-grid-column-definitions/index.php�� ���� width��cellrender function��cssstyle��.

��������£�����ֻ��Ҫ����sql�ֶ�������ָ����չ���ԣ�������pivot���͵Ĳ�ѯ���ڶ���ʱ�޷�֪���ֶ����ƣ���Ҫ����ͨ��header��sql�������ȡˮƽչ������еĶ��塣coldef֧��2�ֶ�����ʽ��key��ʹ��data��Ӧ��sql����е��ֶ����ƣ�����"#"��ͷ��ʶ�ֶ�������header��Ӧ��sql����е��ֶ����ƣ�������ת�е�ʱ������ͬ���������ֶ�ƥ��ֵ��

���磺
<pre>
 { 
 	"#amt": {cellrender:"render_jsfunc1", width: 50},
 	"#qty": {cellrender:"render_jsfunc2", width: 30}
 }
</pre>


h3. �ͻ��˷��ʸ�ʽ

��������
<pre>
{cmd: "rpt.widget.grid.search", id, filter}
</pre>
* id - string widget��id, ��ʽ: "rpt:widget:grid:{{key}}"
* filter - jsonarray {key:value} ����key ��filters �����key, value ���û�ѡ�е�ֵ�� value ������string,Ҳ������array (��ѡ��Χ)

���ظ�ʽ��
<pre>
{config:{columnDefs,rowData},jumpid, id}
</pre>
* columnDefs - jsonarray ��ʽ[{headerName,children}] , 
    * headerName - string
    * children - jsonobj, {headerName, field, width, filter}
        * headerName - string, sql��ָ���е�ֵ
        * field - �ֶ��� sql ��column.name
        * width - ���� colwidth �Ķ��壬Ĭ��ֵ��ȡ ad_param#rpt_width_text, ad_param#rpt_width_number
        * filter - "text"|"number" 
    
* rowData - jsonarray [{row}], row - jsonobj
* id - widget��id
* jumpid - Ҫ��ת�ı����id

 * @author yfzhu
 *
 */
public class search extends RptCmdHandler {
	private String pageId;
	private String  widgetId;
	private JSONObject filter;
	private JSONObject def;// from ad_sql
	
	/**
	 * ָ��һ��plsql function ����sql��䣬�� #sqlfunc, java���˸�ʽת��Ϊ�ͻ�����Ҫ�ĸ�ʽ
	 * @param func
	 * @return
[{
    headerName: "Athlete Details",
    children: [{
        headerName: "Athlete",
        field: "athlete",
        width: 150,
        filter: 'text'
    }, {
        headerName: "Age",
        field: "age",
        width: 90,
        filter: 'number'
    }, {
        headerName: "Country",
        field: "country",
        width: 120
    }]
},{
    headerName,children
  }
]���У� headerName��sql��ֵ��filter��sql���ֶ�����width �� colwidth�ж�ȡ�� filter ���ֶ����ͣ�Ŀǰ��֧��: number,text
	 @param coldef coldef֧��2�ֶ�����ʽ��key��ʹ��data��Ӧ��sql����е��ֶ����ƣ�����"#"��ͷ��ʶ�ֶ�������header��Ӧ��sql����е��ֶ��У�����#0 ��ʾ��0�У�#3��ʾ��3��
	 @param toTree boolean if false, ��ֱ�ӷ������ݿ�Ĳ�ѯ������������ṹת��
	 * @throws Exception
	 */
	private JSONArray createHeader(String func, JSONObject coldef, boolean toTree) throws Exception{
		String sql=getSQLByFunc(func,pageId,widgetId,filter);
//		if(!sql.toLowerCase().contains("order")) throw new NDSException("���ݿ⺯��"+func+"�ķ���sql��Ҫorderby");
		JSONResultSet jrs=engine.doQueryArrayResultSet(sql, null, getConnection(def));
		//��ʽ: col1, col2
		//     a      x
		//     a      y
		//     b      z
		//     total1  null
		//     total2  null
		// what we want: 
		// [{headerName:a, childern:[{headerName: x}, {headerName: y}]},
		//  {headerName:b, childern:[{headerName: z}]}    
		//  {headerName:total1}, {headerName:total2}
		//]
		if(toTree){
			JSONArray header=new JSONArray();
			TreeConverter tree=new TreeConverter(jrs.getData());
			for(TreeConverter.TreeNode node: tree.toTree()){
				JSONObject one=toHeaderObj(node,coldef);
				if(one!=null) header.put(one);
			}
			return header;
		}else{
			return jrs.getData();
		}
	}
	/**
	 * 
	 * @param node 
	 * @param coldef coldef֧��3�ֶ�����ʽ��key��
	 * 	ʹ��data��Ӧ��sql����е��ֶ����ƣ���
	 * 	��"#"��ͷ��ʶ�ֶ�������header��Ӧ��sql����е��ֶ��У�����#0 ��ʾ��0�У�#3��ʾ��3�У� ��
	 *  ��">" ��ͷ���ֶΣ����� ">3" ��ʾ�ӵ�4�У�0��ʼ����������
	 *  
	 * @return null if node value is JSONObject.NULL
	 */
	private JSONObject toHeaderObj(TreeConverter.TreeNode node, JSONObject coldef) throws Exception{
		if(JSONObject.NULL.equals(node.value))return null;
		JSONObject one=new JSONObject();
		one.put("headerName", node.value);
		String fieldName=null;
		if(!node.hasChildren()){
			fieldName="#"+node.col;
			one.put("field", fieldName);

		}else{
			JSONArray chds=new JSONArray();
			for(TreeNode child: node.children){
				JSONObject cdObj=toHeaderObj(child,coldef);
				if(cdObj!=null)chds.put(cdObj);
			}
			if(chds.length()>0)one.put("children", chds);
			else {
				//����null��child
				fieldName="#"+node.col;
				one.put("field", fieldName);
			}
		}
		//coldef�з�������չ���壬ƥ�䷽ʽ: fieldname���ֶ���ƥ��, fieldname����
		JSONObject def=coldef.optJSONObject(fieldName);
		if(def==null) def=coldef.optJSONObject(node.value.toString());
		if(def==null && !node.hasChildren()){
			////����coldef����">"�ŵ�column���壬��������fileld, �ҵ�һ�����Ͼͽ�����������ӽڵ�
			for(Iterator it=coldef.keys();it.hasNext();){
				String key= (String)it.next();
				if(key.startsWith(">")){
					int idx=Tools.getInt(key.substring(1), -1);
					if(idx>0 && idx < node.col){
						//match
						def= coldef.getJSONObject(key);
						break;
					}
				}
			}
		}
		if(def!=null)for(Iterator it= def.keys();it.hasNext();){
			String key= (String)it.next();
			one.put(key, def.get(key));
		}
		
		
		
		return one;
	}
	/**
	 * 
	 * @param func
	 * @return ע�⵽ÿ�ж���jsonobj�����export=false, ��key/value ����ʽ, ���export=true������jsonarray
	 * 
	 * 
	 * @throws Exception
	 */
	private JSONArray createData(String func, boolean export) throws Exception{
		
		String sql=getSQLByFunc(func,pageId,widgetId,filter);
		JSONResultSet jrs=export? engine.doQueryArrayResultSet(sql, null,getConnection(def)):engine.doQueryObjectResultSet(sql, null,getConnection(def));
		
		//result data
		JSONArray data=jrs.getData();
		return data;
	}
	
	
	/**
	 * ������һ������export: boolean default false , ������ã�������xls�ļ����ͻ��ˣ�����{url}
	 */
	public CmdResult execute(JSONObject jo) throws Exception {
		widgetId= getString(jo, "id");
		pageId=getString(jo, "pageid");
		filter= jo.optJSONObject("filter");
		boolean isExport=jo.optBoolean("export", false);
		if(filter==null)filter=new JSONObject();
		/**
		 * {
		    header: "sqlfunc",
		    data: "sqlfunc",
		    type: "grid",
		    coldef: jsonobj,
		    props:jsonobj
		    jumpid: "pageid" 
			}
		 */
		def= (JSONObject)PhoneController.getInstance().getValueFromADSQLAsJSON(widgetId,conn, false);
		String headerFunc= getString(def, "header");
		String dataFunc= getString(def,"data");
		JSONObject coldef=def.optJSONObject("coldef");
		JSONObject props=def.optJSONObject("props");
		String jumpId= def.optString("jumpid");
		
		JSONArray columnDefs=createHeader(headerFunc,coldef, !isExport);
		
		JSONArray rowData=createData(dataFunc,isExport);
		
		
		JSONObject ret=new JSONObject();
		if(isExport){
			//������excel
			String url=export(columnDefs,rowData );
			ret.put("url", url);
		}else{
			JSONObject opt=new JSONObject();
			//copy props to result
			if(props!=null){
				for(Iterator it= props.keys();it.hasNext();){
					String key=(String)it.next();
					opt.put(key, props.opt(key));
				}
			}
			opt.put("rowData", rowData);
			opt.put("columnDefs", columnDefs);
			JSONObject option=new JSONObject();
			option.put("gridOptions", opt);
			ret.put("config", option);
			ret.put("type",  "grid");
			ret.put("id",  widgetId);
	
			if(Validator.isNotNull(jumpId))  ret.put("jumpid", jumpId);
			this.copyKey(jo, ret, "tag", true);
		}
		return new CmdResult(ret);
		
		
		
	}
	/**
	 * ����ǰgrid�����ݵ�����Excel, ���ṩ���صĵ�ַ
	 * @param header 
		//��ʽ: col1, col2
		//     a      x
		//     a      y
		//     b      z
		//     total1  null
		//     total2  null
	 * ��Ҫת��Ϊ:
	 * a  a    b   total1   total2
	 * x  y    z    
	 * @param rowData  ������������
	 * @return �ڵ�ǰ�û�homeĿ¼�µ��ļ�
	 * @throws Exception
	 */
	private String export(JSONArray header, JSONArray rowData) throws Exception{
		if(true) throw new NDSException("�����У�����δ����");
		return null;
//		SimpleDateFormat fmt=new SimpleDateFormat("MMddHHmmss");
//		String fileName="grid"+ fmt.format(new java.util.Date())+".xls"+(ConfigValues.EXCEL_DEFAULT_XLSX?"x":"");
//		String filePath= userWeb.getWebFolder()+"/"+ fileName;
//
//		JSONArray ja = JSONUtils.swapMatrix(header);
//		
//		
//		//titles
//		JSONArray titles=new JSONArray();//[{title, type}]
//		JSONArray firstRow;
//		if(rowData.length()>0){
//			firstRow=rowData.getJSONArray(0);
//		}else{
//			if(ja.length()==0 || !(ja.opt(0) instanceof JSONArray) ) throw new NDSException("������ݣ���������ʽ����");	
//			firstRow=ja.getJSONArray(0);
//		}
//		
//		
//		Workbook wb = ConfigValues.EXCEL_DEFAULT_XLSX? new XSSFWorkbook(): new HSSFWorkbook();
//		Sheet sheet=wb.createSheet("Sheet1");
//
//		//��������У�����ͷ
//		for(int i=0;i<firstRow.length();i++){
//			JSONObject title=new JSONObject();
//			title.put("title", "");
//			title.put("type", Column.STRING);
//			titles.put(title);
//		}
//		PhoneUtils.addSheetData(sheet,true,titles,ja,0, 0,false,null);
//		//����������
//		titles=new JSONArray();
//		for(int i=0;i<firstRow.length();i++){
//			JSONObject title=new JSONObject();
//			title.put("title", "");
//			if(firstRow.opt(i) instanceof String){
//				title.put("type", Column.STRING);
//			}else
//				title.put("type", Column.NUMBER);
//			titles.put(title);
//		}
//		PhoneUtils.addSheetData(sheet,true,titles,rowData, ja.length() , 0,false,null);
//		
//		FileOutputStream fileOut = new FileOutputStream(filePath);
//        wb.write(fileOut);
//        fileOut.close();
//        wb=null;
//        
//		if(!(new File(filePath).exists())){
//			logger.error("Fail to create export grid file "+ filePath);
//			throw new NDSException("�ڲ�����:�ļ�����ʧ��");
//		}
//		String url="/servlets/binserv/Download?filename="+ fileName;
//		return url;
	}

}
