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

h2. grid表格组件

grid 表格组件, 底层js框架 http://www.ag-grid.com

h3. 后台定义

ad_sql名称格式 "rpt:widget:grid:{{key}}", 表示对应id

<pre>
{
    header: "sqlfunc",
    data: "sqlfunc",
    type: "grid",
    coldef: jsonobj,
    jumpid: "pageid"
}
</pre>


h3. header 参数定义

指定一个plsql function 返回sql语句，见 #sqlfunc, java将此格式转换为客户端需要的格式，形式如：

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

具体参见wiki前段设计说明文档《Lily报表前端设计》，其中， headerName是sql的值，filter是sql的字段名，width 从 colwidth中读取， filter 是字段类型，目前仅支持: number,text

h3. data 参数定义

data 的sqlfunc 定义见 #sqlfunc

data 向客户端的输出格式

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

注意到每行都是jsonobj，是key/value 的形式

h3. type 参数定义

目前为 "grid" 表示是表格

h3. jumpid 参数定义

jumpid 是要跳转的报表(page)的id，在跳转定义后，将默认读取field字段名作为filter的key，当前行的value作为value，打开相应报表page

h3. coldef 参数定义

coldef 是针对字段的扩展属性定义，jsonobj, key是字段标识， value是jsonobj，参见https://www.ag-grid.com/javascript-grid-column-definitions/index.php， 比如 width，cellrender function，cssstyle等.

常规情况下，我们只需要定义sql字段名称来指定扩展属性，但对于pivot类型的查询，在定义时无法知晓字段名称，需要考虑通过header的sql语句来获取水平展开后的列的定义。coldef支持2种定义形式的key：使用data对应的sql语句中的字段名称，或用"#"起头标识字段来自于header对应的sql语句中的字段名称，将在行转列的时候搜索同名的所有字段匹配值。

例如：
<pre>
 { 
 	"#amt": {cellrender:"render_jsfunc1", width: 50},
 	"#qty": {cellrender:"render_jsfunc2", width: 30}
 }
</pre>


h3. 客户端访问格式

发出命令
<pre>
{cmd: "rpt.widget.grid.search", id, filter}
</pre>
* id - string widget的id, 格式: "rpt:widget:grid:{{key}}"
* filter - jsonarray {key:value} 其中key 是filters 定义的key, value 是用户选中的值， value 可以是string,也可以是array (多选范围)

返回格式：
<pre>
{config:{columnDefs,rowData},jumpid, id}
</pre>
* columnDefs - jsonarray 格式[{headerName,children}] , 
    * headerName - string
    * children - jsonobj, {headerName, field, width, filter}
        * headerName - string, sql的指定列的值
        * field - 字段名 sql 的column.name
        * width - 来自 colwidth 的定义，默认值读取 ad_param#rpt_width_text, ad_param#rpt_width_number
        * filter - "text"|"number" 
    
* rowData - jsonarray [{row}], row - jsonobj
* id - widget的id
* jumpid - 要跳转的报表的id

 * @author yfzhu
 *
 */
public class search extends RptCmdHandler {
	private String pageId;
	private String  widgetId;
	private JSONObject filter;
	private JSONObject def;// from ad_sql
	
	/**
	 * 指定一个plsql function 返回sql语句，见 #sqlfunc, java将此格式转换为客户端需要的格式
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
]其中， headerName是sql的值，filter是sql的字段名，width 从 colwidth中读取， filter 是字段类型，目前仅支持: number,text
	 @param coldef coldef支持2种定义形式的key：使用data对应的sql语句中的字段名称，或用"#"起头标识字段来自于header对应的sql语句中的字段列，比如#0 表示第0列，#3表示第3列
	 @param toTree boolean if false, 将直接返回数据库的查询结果，不做树结构转换
	 * @throws Exception
	 */
	private JSONArray createHeader(String func, JSONObject coldef, boolean toTree) throws Exception{
		String sql=getSQLByFunc(func,pageId,widgetId,filter);
//		if(!sql.toLowerCase().contains("order")) throw new NDSException("数据库函数"+func+"的返回sql需要orderby");
		JSONResultSet jrs=engine.doQueryArrayResultSet(sql, null, getConnection(def));
		//格式: col1, col2
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
	 * @param coldef coldef支持3种定义形式的key：
	 * 	使用data对应的sql语句中的字段名称，或
	 * 	用"#"起头标识字段来自于header对应的sql语句中的字段列，比如#0 表示第0列，#3表示第3列， 或
	 *  用">" 起头的字段，例如 ">3" 表示从第4列（0起始）计数的列
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
				//都是null的child
				fieldName="#"+node.col;
				one.put("field", fieldName);
			}
		}
		//coldef中放置了扩展定义，匹配方式: fieldname或字段名匹配, fieldname优先
		JSONObject def=coldef.optJSONObject(fieldName);
		if(def==null) def=coldef.optJSONObject(node.value.toString());
		if(def==null && !node.hasChildren()){
			////遍历coldef中有">"号的column定义，赋予所有fileld, 找到一个符合就结束，仅针对子节点
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
	 * @return 注意到每行都是jsonobj，如果export=false, 是key/value 的形式, 如果export=true，行是jsonarray
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
	 * 增加了一个参数export: boolean default false , 如果设置，将生成xls文件到客户端，返回{url}
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
			//导出到excel
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
	 * 将当前grid的数据导出到Excel, 并提供下载的地址
	 * @param header 
		//格式: col1, col2
		//     a      x
		//     a      y
		//     b      z
		//     total1  null
		//     total2  null
	 * 需要转换为:
	 * a  a    b   total1   total2
	 * x  y    z    
	 * @param rowData  正常的行数据
	 * @return 在当前用户home目录下的文件
	 * @throws Exception
	 */
	private String export(JSONArray header, JSONArray rowData) throws Exception{
		if(true) throw new NDSException("开发中，功能未上线");
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
//			if(ja.length()==0 || !(ja.opt(0) instanceof JSONArray) ) throw new NDSException("检查数据，标题矩阵格式有误");	
//			firstRow=ja.getJSONArray(0);
//		}
//		
//		
//		Workbook wb = ConfigValues.EXCEL_DEFAULT_XLSX? new XSSFWorkbook(): new HSSFWorkbook();
//		Sheet sheet=wb.createSheet("Sheet1");
//
//		//构造标题行，多行头
//		for(int i=0;i<firstRow.length();i++){
//			JSONObject title=new JSONObject();
//			title.put("title", "");
//			title.put("type", Column.STRING);
//			titles.put(title);
//		}
//		PhoneUtils.addSheetData(sheet,true,titles,ja,0, 0,false,null);
//		//构造数据行
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
//			throw new NDSException("内部错误:文件生成失败");
//		}
//		String url="/servlets/binserv/Download?filename="+ fileName;
//		return url;
	}

}
