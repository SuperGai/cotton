package com.agilecontrol.b2bweb.grid;

import java.io.FileInputStream;
import java.io.StringWriter;
import java.sql.Connection;
import java.text.DecimalFormat;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;

import com.agilecontrol.b2b.schema.Column;
import com.agilecontrol.b2b.schema.TableManager;
import com.agilecontrol.b2bweb.WebController;
import com.agilecontrol.b2bweb.grid.GridBuilder.ModelColumn;
import com.agilecontrol.b2bweb.grid.GridBuilder.SQLForColumn;
import com.agilecontrol.b2bweb.grid.GridBuilder.ViewDefine;
import com.agilecontrol.b2bweb.sheet.Color;
import com.agilecontrol.b2bweb.sheet.ProductMatrix;
import com.agilecontrol.b2bweb.sheet.ProductMatrixLoader;
import com.agilecontrol.nea.core.control.event.DefaultWebEvent;
import com.agilecontrol.nea.core.control.event.NDSEventException;
import com.agilecontrol.nea.core.query.QueryEngine;
import com.agilecontrol.nea.core.query.QueryUtils;
import com.agilecontrol.nea.core.velocity.VelocityUtils;
import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.nea.util.StringUtils;
import com.agilecontrol.nea.util.Tools;
import com.agilecontrol.nea.util.Validator;
import com.agilecontrol.phone.Admin;
import com.agilecontrol.phone.PhoneController;
import com.agilecontrol.phone.PhoneUtils;
import com.agilecontrol.phone.PhoneController.SQLWithParams;
import com.agilecontrol.phone.UserObj;
/**
 * 面向在线购物车表格化编辑功能提供基础服务
 * 
 * @author yfzhu
 *
 */
public class GridBuilder {

	protected Logger logger = LoggerFactory.getLogger(getClass());
	
	protected boolean readOnly;
	protected JSONObject conf;//request obj from cmdhandle.exectute() param
	

	protected UserObj usr;
	/**
	 * 标配的jedis连接，和conn一样，需要主动关闭
	 */
	protected Jedis jedis;

	protected DefaultWebEvent event;
	protected Connection conn;
	protected QueryEngine engine;
	protected TableManager manager;
	protected VelocityContext vc;
	//ad_sql#grid_conf
	protected JSONObject gridConf;
	
	/**
	 * View的定义
	 * @return
	 */
	public GridViewDefine getViewDefine() throws Exception{
		//model*:[{col, sql, desc, func, key, sum,sumsql, fmt, css }]
		ArrayList<ModelColumn>  model=createModel(gridConf.getJSONArray("model"));
		//[{title, cols, css, issize, ispresize, width, options}]
		ViewDefine view=new ViewDefine(gridConf.getJSONArray("view"), model, gridConf.opt("options"));
		
		JSONArray sizeGroups=WebController.getInstance().getSizeGroups(usr.getMarketId(), jedis, conn);
		
		GridViewDefine gvd = new GridViewDefine(view,sizeGroups, gridConf,conn);
		return gvd;
	}
	/**
	 * ad_sql#grid:$tablename:meta
	 * @return
	 */
	public JSONObject getGridConf(){
		return gridConf;
	}
	
	/**

构建grid, 首先搜索相应pdtid, 然后拼接处行记录

ad_sql#online_edit_grid, 分为数据(model)定义和显示(view)定义, 显示定义是因为1列可能有多个字段
> {model, view, options, sumline}
*model*:[{col, sql, desc, func, key, sum,sumsql, fmt, css, asilevel }]
*view*: [{title, cols, css, issize, ispresize, width, options}]

model属性说明
> *col* string， col可定义的值：所有的table:pdt:meta的字段名，以字段名称形式，如"no", 如果使用pdt属性，不要设置sql。如果数据内容取自sql语句的列，则用sql语句的字段名作为col的名称，如"qty", 系统在识别到sql语句定义后，在sql对应的行对象中检索对应值
> *sql* string,这是ad_sql的名称，考虑到cols 可能有多列复用相同sql语句，开发上需要保证结果可被复用，sql语句中可以使用的变量与上文kv说明一致
> *desc* string, 支持@xxx@形式的多语言翻译，对应pdt表的字段可以不提供，由系统自动翻译，依据realname属性定位到ad_column来完成
> *func* string, 如果字段是计算列，在此设定计算公式，公式中可以含有其他列，用key来标识，如 "price*qty"
> *key* string， 可以为空，当前数据列的key值，将可以在func中引用
> *sum*  boolean, default false. 当前列是否需要在底部合计，系统需要将所有行的当前列合计起来，如果是计算列，必须提供sumsql
> *sumsql* string, ad_sql.name， 格式 select xxx from yyy , 仍然复用变量。如果设置了sumsql，无视sum
> *fmt* 数据的格式，如果col为array，fmt也必须是array，设定每行的格式
> *css* 决定当前字段的显示效果，注意view上面的css是针对列的，而1列上面可能有多个col
> *asilevel* boolean 是否是asi级别的数据，asi级别的数据， sql的格式是 select pdtid, asi, xxx from xxx 

view属性说明
> *title* grid 的标题行
> *cols* 2种形式: string，或 string array, 如: "no", 或["no","price"], 注意如果某个sql语句对应的字段和pdt字段名称一致，字段名需要增加sql名称作为前缀，加#号，如: "sql_sugg#name", 表示是sql_sugg的name字段的值
> *css* 决定列的显示效果，注意model上面的每个col也有一个css，如果1列只有1个字段，可以任意设置
> *issize* boolean, 是否尺码开始列，尺码实际列数由系统自动生成
> *ispresize* boolean, 当前列的定义是否是尺码相关，意味着每个尺码列都有这个列做前缀
> *width*  int 列的宽度, in pixel
> *options* 客户端可以接受的其他配置，比如要设置特殊列的显示属性等

sumline:
ad_sql的名称，样式举例：“合计数量:<b> [[sumqty]] </b>, 合计金额:<b> [[sumamt]]</b>, 合计零售额: <b>[[sumamtlist]]</b>”，将使用客户端的模板样式，其中的key来自于modelcolumn.col 的定义

	 * @param pdtIds id of m_product.id 
	 * @return {rows, sum}, 其中
	 * rows 是每行的数据格式，格式样式：TODO 李书豪补上
	 * sum: k/v 结构，客户端将按照模板，举例:"sum": {
	    "sumamt":"￥16,378.00",
	    "sumqty":"325,000",
	    "sumamtlist":"￥20,378.00"
	  }
	 * @throws Exception
	 */
	public JSONObject getViewData(JSONArray pdtIds) throws Exception {
		//model*:[{col, sql, desc, func, key, sum,sumsql, fmt, css }]
		ArrayList<ModelColumn>  model=createModel(gridConf.getJSONArray("model"));
		//[{title, cols, css, issize, ispresize, width, options}]
		ViewDefine view=new ViewDefine(gridConf.getJSONArray("view"), model, gridConf.opt("options"));
		//if(pdtIds.length()==0) throw new NDSException("商品列表为空");
		//这是用sql定义的列的信息
		HashMap<String, SQLForColumn> sqlColumns=setupSQLColumns(model, pdtIds,true);
		//这是用pdt属性定义的列的信息
		ArrayList<Column> pdtCols=setupPdtColumns(model);
		//ele are json obj, key is column name
		JSONArray pdtColumnRows=PhoneUtils.getRedisObjectArray("pdt", pdtIds, pdtCols, true, conn, jedis);
		for(int i=0;i<pdtColumnRows.length();i++){
			JSONObject jo=pdtColumnRows.getJSONObject(i);
			//更新其中的多市场定义
			WebController.getInstance().replacePdtValues(jo, usr.getLangId(), usr.getMarketId(), vc, jedis, conn);
		}
		/**
		 * 按model定义准备好的商品信息款色行信息，按view的定义来
		 */
		ArrayList<ViewRow> pdtColorRows=new ArrayList();
		
		ProductMatrixLoader loader=new ProductMatrixLoader(jedis,conn);
		
		//缓存所有的pdtmatrix
		ArrayList<ProductMatrix> pdtMatrixes=new ArrayList();
		for(int i=0;i<pdtIds.length();i++){
			int pdtId=pdtIds.getInt(i);
			ProductMatrix matrix=loader.getSimpleProductMatrix(pdtId);
			pdtMatrixes.add(matrix);
		}
		
		//按model的定义要求组装, 首先是针对标准行，即不到asi的数据, 仅是色码级的字段 
		ArrayList<ModelColumn> pdtColorColumns=filterPdtLevelColumns(model);
		
		for(int i=0;i<pdtIds.length();i++){
			int pdtId=pdtIds.getInt(i);
			JSONObject pdtObj=pdtColumnRows.getJSONObject(i);
			ProductMatrix matrix=pdtMatrixes.get(i);
			ArrayList<Color> colors=matrix.getColors();
			for(Color color:colors){
				ViewRow vr=new ViewRow(view,color,matrix);
				vr.setProductImage(pdtObj.optString("mainpic"));
				for(ModelColumn mc:pdtColorColumns){
					//这里需要将上述数据pdt字段级的，和sql级的内容按model的顺序写入当前行
					if(mc.isPdtColumn&&!mc.isColor){
						Object value=pdtObj.get(mc.col);
						if(mc.format!=null && value!=null) value=mc.format.format(value);
						vr.setCell(mc, value);
					}else if(mc.isColor){
						vr.setCell(mc, color.getName());
					}else{
						SQLForColumn sfc=sqlColumns.get(mc.sql);
						vr.setCell(mc, sfc.getPdtValue(pdtId, color.getCode(), mc.col,mc.format));
					}
				}
				pdtColorRows.add(vr);
			}
		}
		
		/**
		 * size列仅有一个数据，就是尺码下单量
		 * {11_10033:0,11_10034:12,12_9459:15}
		 */
		JSONObject sizeQtys=null;
		//size
		ModelColumn mc=findSizeQtyColumn(model,view);
		if(mc.isPdtColumn) throw new NDSException("@b2bedit-define@"+",issize"+"@b2bedit-column@"+"asileve=true");
		SQLForColumn sfc=sqlColumns.get(mc.sql);
		sizeQtys=sfc.getASIValuesByKey(mc.col,mc.format);
		
		//presize columns
		/**
		 * ispresize所定义的cols的值，key: ${pdtid}_${asi}, value: 列值，符合view定义的cols内容，[] 或单值，举例:
		 * {11_10033:[10, 15, 10.99],11_10034:[9, 12, 10.99]}
		 */
		JSONObject preSize=new JSONObject();
		ArrayList<ModelColumn> mcs=findPreSizeColumns(model,view);
		int arrSize=mcs.size();
		for(int i=0;i<mcs.size();i++){
			mc=mcs.get(i);
			sfc=sqlColumns.get(mc.sql);
			JSONObject sfcObj=sfc.getASIValuesByKey(mc.col,mc.format);
			//添加元素到指定的数组位置
			addToObject(preSize, sfcObj, i, arrSize);
		}
		//合并成为客户端需要的格式
		logger.debug("sizeQtys="+ sizeQtys);
		logger.debug("preSize="+ preSize);
		for(ViewRow vr:pdtColorRows)logger.debug("pdtid="+ vr.getProductId()+", cc="+ vr.getColorCode()+", img="+vr.getProductImage()+":"+ vr.data.toString());
		
		JSONObject ret=new JSONObject();
		ret.put("rowData", mergeProductRows(pdtColorRows,sizeQtys,preSize));
		ret.put("sum", computeSumLineData(gridConf.optString("sumline_sql"),gridConf.optJSONArray("sumline")));
		return ret;
	}
	
	/**
	 * 找到定义presize的数据字段
	 * @param model
	 * @param view
	 * @return
	 */
	private ArrayList<ModelColumn> findPreSizeColumns(ArrayList<ModelColumn> model, ViewDefine view) throws Exception{
		for(ViewColumn col:view.columns){
			if(col.isPreSize) return col.cols;
		}
		return new ArrayList();//new NDSException("当前grid在view中未定义ispresize列");
	}
	/**
	 * 找到定义sizeqty的数据字段
	 * @param model
	 * @param view
	 * @return
	 */
	private ModelColumn findSizeQtyColumn(ArrayList<ModelColumn> model, ViewDefine view) throws Exception{
		for(ViewColumn col:view.columns){
			if(col.isSize) return col.cols.get(0);
		}
		throw new NDSException("view of issize not exist");
	}
    /**
     * 定义：
     * rowData:grid数据
     * 3_field:前缀所需要的显示值
     * pic:图片
     * asi：前台知道是否该cell可以编辑         
	 * 合并普通商品行和色码信息为客户端需要的格式
	 * @param pdtColorRows  [
	 * 	pdtid=122, cc=998, img=/pdt/m/S1007B8255998.jpg:[["S1007B8255998",199],"半袖T恤",21,4179,4179]
	 * 	pdtid=121, cc=200, img=/pdt/m/S1007B8255200.jpg:[["S1007B8255200",199],"半袖T恤",24,4776,4776]
	 * ]
	 * @param sizeQtys sample: {"122_856":6,"122_94":5,"121_355":7,"122_368":2,"121_1542":8,"121_236":9,"122_1551":1,"122_1073":4,"122_1802":3}
	 * @param preSize sample: {"122_94":[57,58,59],"122_856":[31,32,33],"121_686":[29,30,31],"121_355":[4,5,6],"121_1404":[36,37,38],"121_1542":[39,40,41],"122_368":[97,98,99],"121_236":[100,101,102],"122_1551":[4,5,6],"122_1802":[35,36,37],"122_1073":[3,4,5],"121_516":[4,5,6]}
	 * @return [{pic, asi, "0":[1,2], "1":15.3}] "0" 表示第1列的数据， asi: 
	 * @throws Exception
	 */
	@Admin(mail="li.shuhao@lifecycle.cn")
	private JSONArray mergeProductRows(ArrayList<ViewRow> pdtColorRows, JSONObject sizeQtys,JSONObject preSize) throws Exception{
		JSONArray ja = new JSONArray();
		//当前用户所使用的商品的所有尺码组的最大尺码数
		int maxSizeCount=WebController.getInstance().getMaxSizeCount(usr.getMarketId(), jedis, conn);
		//尺码列的位置
		int sizeIndex=-1;
		int preSizeIndex = -1;
		if(pdtColorRows.size()>0){
			//在计算sizeColumnIndex的时候，如果前面有preSizeColumn， 原有的sizeColumnIndex会+1，这里需要去除preSizeColumnIndex的影响
			preSizeIndex = pdtColorRows.get(0).view.preSizeColumnIndex;
			sizeIndex=  pdtColorRows.get(0).view.sizeColumnIndex;
			if(preSizeIndex<sizeIndex && preSizeIndex>-1) sizeIndex--;
			logger.debug("sizeIndex="+ sizeIndex+", maxsizecount="+ maxSizeCount+",preSizeIndex="+preSizeIndex);
		}

		for (int i = 0; i < pdtColorRows.size(); i++) {
			JSONObject jo = new JSONObject();
			JSONObject pdtAndAsiIds = new JSONObject();//存放pdt_asi[]
			ViewRow viewRow = pdtColorRows.get(i);
			ProductMatrix matrix = viewRow.matrix;
			JSONArray asi = matrix.getASIArrays();
			logger.debug("asi of "+asi);
			ArrayList<Color> color = matrix.getColors();
			String colorCode = viewRow.getColorCode();
			int colorRow = 0;
			int pdtId = viewRow.getProductId();
			for(int j = 0;j < color.size();j++){
				if(colorCode.trim().equals(color.get(j).getCode())){
					colorRow = j;
					break;
				}
			}
			for (int j = 0; j < viewRow.data.length(); j++) {
				String string = viewRow.data.optString(j);
				if (j < sizeIndex) {
					if(string.startsWith("["))
						jo.put(String.valueOf(j), viewRow.data.optJSONArray(j));
					else
						jo.put(String.valueOf(j), Validator.isNull(string) ?"":string);
				} else{
					if(string.startsWith("[")){
						if(preSizeIndex > -1)
							jo.put(String.valueOf(j+maxSizeCount*2), viewRow.data.optJSONArray(j));
						else
							jo.put(String.valueOf(j+maxSizeCount), viewRow.data.optJSONArray(j));
					}
					else{
						if(preSizeIndex > -1)
							jo.put(String.valueOf(j+maxSizeCount*2), Validator.isNull(string) ?"":string);
						else
							jo.put(String.valueOf(j+maxSizeCount), Validator.isNull(string) ?"":string);
					}
				} 
			}
			for (int k = 0; k < maxSizeCount; k++) {
				String asiId = asi.getJSONArray(colorRow).optString(k);
				String pdt_asi = new String();
				pdt_asi =  pdtId + "_" + asiId;
				if (!sizeQtys.isNull(pdt_asi)){
					if(preSizeIndex > -1)
						jo.put(String.valueOf((k*2+1) + sizeIndex), sizeQtys.getInt(pdt_asi));
					else
						jo.put(String.valueOf(k + sizeIndex), sizeQtys.getInt(pdt_asi));
				}
				else{
					if(preSizeIndex > -1)
						jo.put(String.valueOf((k*2+1) + sizeIndex), "");
					else
						jo.put(String.valueOf(k + sizeIndex), "");
				}
				
				if(preSizeIndex > -1){
					if(!preSize.isNull(pdt_asi)){
						jo.put(String.valueOf(k*2+sizeIndex), preSize.getJSONArray(pdt_asi));
					}else{
						ViewColumn vc = null;
						if(preSizeIndex > -1){
							for(ViewColumn vcn :pdtColorRows.get(0).view.columns){
								if(vcn.isPreSize){
									vc = vcn;
									break;
								}
							}
							JSONArray array = new JSONArray();
							for(int z = 0;z < vc.cols.size();z++){
								array.put("");
							}
							jo.put(String.valueOf(k*2+sizeIndex),array); 
						}
						
					}
				}
			
				
				String asid = asi.getJSONArray(colorRow).optString(k);
				if(Validator.isNull(asid)){
					if(preSizeIndex > -1)
						pdtAndAsiIds.put(String.valueOf((k*2+1) + sizeIndex),"");
					else
						pdtAndAsiIds.put(String.valueOf(k + sizeIndex),"");
				}
				else{
					if(preSizeIndex > -1)
						pdtAndAsiIds.put(String.valueOf((k*2+1) + sizeIndex),pdtId+"_"+asid);
					else
						pdtAndAsiIds.put(String.valueOf(k + sizeIndex),pdtId+"_"+asid);
				}
			}
			jo.put("asi", pdtAndAsiIds);
			jo.put("pic",viewRow.getProductImage());
			jo.put("cc", viewRow.getColorCode());
			jo.put("pdtId", viewRow.getProductId());
			ja.put(jo);
		}
		return ja;
	}
	/**
	 * 在preSize对象中，每个value都是arraySize指定长度的数组，sfcObj的value将作为pos位置上的值
	 * @param preSize 格式 {key: [v1,...v(arraySize)]}， 如: {"11_1234": [1,2,11]}, 其中key的格式：$pdtid_$asi
	 * @param sfcObj  格式 {key: value}，不能保证每个key在preSize中都有
	 * @param pos 将sfcObj的value放在sfcObj的指定pos位置
	 * @param arraySize preSize如果创建元素，其长度必须是arraySize
	 * @throws Exception
	 */
	private void addToObject(JSONObject preSize, JSONObject sfcObj, int pos, int arraySize) throws Exception{
		for(Iterator it=sfcObj.keys();it.hasNext();){
			String key=(String)it.next();
			JSONArray values=preSize.optJSONArray(key);
			if(values==null){
				values=new JSONArray();
				for(int i=0;i<arraySize;i++) values.put(JSONObject.NULL);
				preSize.put(key, values);
			}
			values.put(pos, sfcObj.get(key));
		}
	}
	
	/**
	 * 不到asi级别的字段
	 * @return
	 */
	private ArrayList<ModelColumn> filterPdtLevelColumns(ArrayList<ModelColumn> model){
		ArrayList<ModelColumn> cols=new ArrayList<ModelColumn>();
		for(ModelColumn mc: model){
			if(!mc.asiLevel) cols.add(mc);
		}
		return cols;
	}
	/**
	 * 基于model创建
	 * @param model
	 * @return 
	 * @throws Exception
	 */
	protected ArrayList<ModelColumn>  createModel(JSONArray model) throws Exception{
		 ArrayList<ModelColumn> am=new  ArrayList<ModelColumn>();
		 for(int i=0;i<model.length();i++){
			 ModelColumn mc=new ModelColumn(model.getJSONObject(i));
			 am.add(mc);
		 }
		 return am;
	}
	/**
	 *  online_edit_grid#model的元素定义
	 *
	 */
	class ModelColumn{
		String col;
		String sql;
		/**
		 * 当前列是否是pdt的column，还是sql中的column
		 */
		boolean isPdtColumn;
		String desc;
		String func;
		String  key;
		boolean sum;
		String sumSQL;
		String fmt;
		String css;
		boolean asiLevel;
		boolean isColor;
		Format format;//可能为空
		/**
		 * 这是在构造ViewDefine的时候回写过来的位置信息，表示在ViewRow的第几列，注意不包含size/presize列的信息
		 */
		int viewColumnIndex;
		/**
		 * 这是在构造ViewDefine的时候回写过来的位置信息，表示在ViewRow指定列的第几个ColumnItem,
		 * 注意不包含size/presize列的信息
		 */
		int viewColumnItemIndex=0;
		/**
		 * 基本从ad_sql#model里获取
		 * @param c
		 * @throws Exception
		 */
		public ModelColumn(JSONObject c) throws Exception{
			col=c.getString("col");
			sql=c.optString("sql");
			isPdtColumn=Validator.isNull(sql) ;
			desc=c.optString("desc");
			func=c.optString("func");
			key=c.optString("key");
			sum=c.optBoolean("sum",false);
			sumSQL=c.optString("sumsql");
			fmt=c.optString("fmt");
			css=c.optString("css");
			asiLevel=c.optBoolean("asilevel",false);
			isColor=c.optBoolean("iscolor",false);
			
			initFormat();  
		}
		/**
		 * 根据字段定义，如果有对应的字段定义，直接读取字段的格式
		 * 如果没有对应字段(sql)，将默认按number来识别fmt定义。fmt可以不提供，就为空。
		 */
		private void initFormat(){
			
			if(isPdtColumn){
				Column c=TableManager.getInstance().getColumn("pdt", col);
				if(c!=null){
					if(Validator.isNull(fmt)){
						com.agilecontrol.nea.core.schema.Column cl=com.agilecontrol.nea.core.schema.TableManager.getInstance().getColumn("m_product",  c.getRealName());
						if(cl!=null) format= cl.getFormat();
						else logger.debug("not find fmt and not a ad_column:"+ col);
					}else{
						//有fmt，且有col 定义，意味着定制格式，优先fmt
						if(c.getType().equals(Column.Type.NUMBER)){
							format= new DecimalFormat(fmt);
						}else if(c.getType().equals(Column.Type.DATENUMBER) || c.getType().equals(Column.Type.DATE)){
							format=new SimpleDateFormat(fmt);
						}else{
							logger.debug("not supported type "+ col+" type="+ c.getType().getName());
						}
					}
				}else{
					//c 为空，默认当fmt 对应decimal
					if(Validator.isNotNull(fmt)){
						format= new DecimalFormat(fmt);
					}
				}
			}else{
				// 非col，仅识别fmt 为number类型
				if(Validator.isNotNull(fmt)){
					format= new DecimalFormat(fmt);
				}
			}
		}
		/**
		 * STRING("string",0), NUMBER("number",1), DATE("date",2), DATENUMBER("datenumber",3), CLOB("clob",4);
		 * @return
		 */
		public int getType(){
			if(isPdtColumn){
				Column c=TableManager.getInstance().getColumn("pdt", col);
				if(c!=null) return c.getType().getIndex();
			}
			return Column.Type.STRING.getIndex();
		}
		/**
		 * 设置当前数据列在ViewRow中的位置信息
		 * @param idx ViewRow中的列号, 注意需要去除size列和presize列的定义的列，如果当前列是在他们后面的话
		 * @param subIdx 当列中有多个元素，这是元素的位置信息
		 */
		public void setColumnIndexInView(int idx, int subIdx) throws Exception{
			if(viewColumnIndex!=0 ||viewColumnItemIndex!=0 ) throw new NDSException("model of col in view"+"@b2bedit-repeat@");
			viewColumnIndex=idx;
			viewColumnItemIndex=subIdx;
		}
	}
	
	/**
	 * 根据model定义知道需要从pdt表获取哪些字段，凡是sql定义的字段一律不需要, 额外再补上mainpic字段
	 * 按设计要去，第一个字段必须是ID字段
	 * @param model
	 * @return
	 * @throws Exception
	 */
	private ArrayList<Column> setupPdtColumns(ArrayList<ModelColumn> model) throws Exception{
		ArrayList<Column> cls=new ArrayList();
		Column column=manager.getColumn("pdt", "id");
		cls.add(column);
		for(ModelColumn col: model){
			if(col.isPdtColumn&&!col.isColor) {
				column=manager.getColumn("pdt", col.col);
				if(column==null) throw new NDSException("pdt of "+ col.col+"@b2bedit-found@");
				cls.add(column);
			}
		}
		//图片字段必须拿
		column=manager.getColumn("pdt", "mainpic");
		cls.add(column);
		return cls;
	}
	
	/**
	 * mc.sumsql 与mc.sql不同的是，合计行的sql语句无需pdtid list或过滤查询条件，而是一个固定语句。
	 * 注意sumsql中需要用到的vc变量，需要预习设置好
	 * @param model ad_sql#online_edit_grid.model
	 * @return key: sumsql.name, value: 已经统计好的jsonobj (就是统计行的对象化, key是sumsql的column名称）
	 * @throws Exception
	 */
	private HashMap<String, JSONObject> setupSumSQLColumns(ArrayList<ModelColumn> model) throws Exception{
		/**
		 * 遍历方式：首先解析字段定义，获取所有sql定义，构造sql语句，获取查询结果，存放在hashmap中 key: 
		 */
		HashMap<String, JSONObject> sqlColumns=new HashMap();//key: ad_sql.name, value sql 查询语句
		for(int i=0;i<model.size();i++){
			ModelColumn row=model.get(i);
			String sqlName=row.sumSQL;
			if(Validator.isNotNull(sqlName)){
				JSONObject sfc=sqlColumns.get(sqlName);
				if(sfc==null){
					JSONArray rows=PhoneController.getInstance().getDataArrayByADSQL(sqlName, vc, conn, true);
					if(rows.length()>0) sfc= rows.getJSONObject(0);
					else throw new NDSException(row.col+ " of sumsql"+"@b2bedit-return@");
					sqlColumns.put(sqlName, sfc);
				}
			}
		}
		return sqlColumns;
	}
	/**
	 * 由于不少的column定义的时候，会复用sql，先进行相关准备工作，将sql对应的结果查出来，以便针对每个pdt，逐列获取相应值
	 * @param model ad_sql#online_edit_grid.model
	 * @param pdtIds elements： int，是当前选择的商品的id
	 * @param includeASILevel 是否将asi级别的统计字段也重新计算，对于GridSave的时候，不再重新计算asi的列，如库存等
	 * @return key: sql.name, value: 已经按pdtid放置好的查询结果，可通过getPdtValue获取指定商品的对应列值
	 * @throws Exception
	 */
	protected HashMap<String, SQLForColumn> setupSQLColumns(ArrayList<ModelColumn> model,JSONArray pdtIds, boolean includeASILevel) throws Exception{
		/**
		 * 遍历方式：首先解析字段定义，获取所有sql定义，构造sql语句，获取查询结果，存放在hashmap中 key: 
		 */
		HashMap<String, SQLForColumn> sqlColumns=new HashMap();//key: ad_sql.name, value: idx in model 
		for(int i=0;i<model.size();i++){
			ModelColumn row=model.get(i);
			if(row.asiLevel && !includeASILevel) continue;
			String sqlName=row.sql;
			if(Validator.isNotNull(sqlName)){
				SQLForColumn sfc=sqlColumns.get(sqlName);
				if(sfc==null){
					sfc=new SQLForColumn(sqlName,row.asiLevel);
					sqlColumns.put(sqlName, sfc);
				}
				sfc.addColumn(i);
			}
		}
		String pdtIdSQL=convertToPdtIdSQL(pdtIds);//select id from m_product p where id in() or id in()
		for(String sqlName:sqlColumns.keySet()){
			SQLWithParams swp=PhoneController.getInstance().parseADSQL(sqlName, vc, conn);
			String sql=StringUtils.replace( swp.getSQL(), "$PDTIDSQL$", pdtIdSQL);
			JSONArray rows=engine.doQueryObjectArray(sql, swp.getParams(), conn);
			SQLForColumn sfc=sqlColumns.get(sqlName);
			sfc.addRows(rows);
		}
		return sqlColumns;
	}
	/**
	 * 转换成select id from m_product p where id in() or id in()格式的语句，由于in 有1000个的限制，故需要拆分为多个in
	 * 套在最终的sql中的形式：
wth p as ( $PDTIDSQL$)
select p.id pdtid, s.asi, s.qty
from p, b_cart s where s.m_product_id=p.id and s.user_id=?
	 * @param pdtIds
	 * @return select id from m_product p where id in() or id in()格式
	 * @throws Exception
	 */
	protected String convertToPdtIdSQL(JSONArray pdtIds) throws Exception{
		if(pdtIds==null || pdtIds.length()==0){
			return "select -1 id from dual";
		}
		StringBuilder sb=new StringBuilder("select id from m_product p where ");
		int length=pdtIds.length();
		
		for(int i=0;i<length/1000+1;i++){
			if(i>0) sb.append(" or ");
			sb.append("p.id in(");
			for(int j=i*1000;j< i*1000+1000 && j< length;j++){
				sb.append(pdtIds.getInt(j)).append(",");
			}
			sb.deleteCharAt(sb.length()-1);
			sb.append(")");
		}
		return sb.toString();
	}
	/**
	 * 
	 * view定义的数据格式
	 *
	 */
	class ViewDefine{
		public ArrayList<ViewColumn> columns;
		/**
		 * 客户端需要的整个view级别的其他定义
		 */
		public Object options;
		
		/**
		 * 尺码列在view定义中的位置，尺码列会
		 */
		private int sizeColumnIndex=-1;
		
		private int preSizeColumnIndex=-1;
		/**
		 * 
		 * @param view  [{title, cols, css, issize, ispresize, width, options}]
		 * @param opts  客户端需要的整个view级别的其他定义
		 * @throws Exception
		 */
		public ViewDefine(JSONArray view,ArrayList<ModelColumn> mcs, Object opts) throws Exception{
			for(int i=0;i<view.length();i++){
				JSONObject obj=view.getJSONObject(i);
				if(obj.optBoolean("issize",false)) sizeColumnIndex=i;
				if(obj.optBoolean("ispresize",false)) preSizeColumnIndex=i;
			}
			columns=new ArrayList();
			for(int i=0;i<view.length();i++){
				ViewColumn vc=new ViewColumn(view.getJSONObject(i),this,i,mcs);
				columns.add(vc);
			}
			this.options=opts;
		}
		/**
		 * 尺码在显示的grid中的列的起始索引
		 * @return
		 */
		public int getSizeColumnIndexInGridView(){
			if(preSizeColumnIndex>-1) sizeColumnIndex--;
			return sizeColumnIndex;
		}
		/**
		 * 当前定义是否有presize列
		 * @return
		 */
		public boolean hasPreSizeColumn(){
			return preSizeColumnIndex>-1;
		}
	}
	/**
	 * view中的字段定义
	 */
	class ViewColumn{
		public String title;
		public String css;
		public ArrayList<ModelColumn> cols;
		public boolean isSize;//尺码数量量，仅1列
		public boolean isPreSize;//尺码前缀，asi级别的其他信息，比如库存，建议量等等，有多值
		public int width;
		public JSONObject options;
		public int indexInViewDefine;//在viewdefine中的列号，从0开始
		public boolean isColor;//是否存在颜色列
		
		/**
		 * 
		 * @param col
		 * {title, cols, css, issize, ispresize, width, options}
		 * @param idx posion in viewdefine
		 * @throws Exception
		 */
		public ViewColumn(JSONObject col,ViewDefine view, int idx,ArrayList<ModelColumn> mcs) throws Exception{
			title=col.optString("title");
			css=col.optString("css");
			isSize=col.optBoolean("issize", false);
			isPreSize=col.optBoolean("ispresize", false);
			width=col.optInt("width", 40);
			options=col.optJSONObject("options");
			isColor=col.optBoolean("iscolor");
			indexInViewDefine=idx;
			
			cols=new ArrayList();
			Object ccol=col.opt("cols");
			JSONArray ss=null;
			if(ccol instanceof String){
				ss=new JSONArray();
				for(String s: ((String)ccol).split(",")) ss.put(s);
			}else if(ccol instanceof JSONArray){
				ss=(JSONArray) ccol;
			}else throw new NDSException("cols only support string|[]:"+ col);
			for(int i=0;i<ss.length();i++){
				String colName=ss.getString(i);
				ModelColumn mc=findModelColumn(colName, mcs);
				int posInView=idx;
				if(idx> view.sizeColumnIndex && view.sizeColumnIndex>-1) posInView--;
				if(idx> view.preSizeColumnIndex && view.preSizeColumnIndex>-1) posInView--;
				//尺码列必须是asi级别
				if(isSize&&!mc.asiLevel&&!isPreSize) throw new NDSException("参数:"+mc.col+"不是asi级别");
				//前缀列必须是asi级别
				if(isPreSize&&!mc.asiLevel&&!isSize) throw new NDSException("非asi级别参数"+mc.col+"不能出现在前缀参考列");
				//非前缀列非尺码列必须是非asi级别
				if(!isPreSize&&mc.asiLevel&&!isSize) throw new NDSException("asi级别参数:"+mc.col+"不能在非尺码列以外配置");
				mc.setColumnIndexInView(posInView, cols.size());//回写idx等位置信息
				cols.add(mc);
				
			}
		}
		/**
		 * 根据colName找到ModelColumn
		 * @param colName 如果某个sql语句对应的字段和pdt字段名称一致，字段名需要增加sql名称作为前缀，加#号，如: "sql_sugg#name", 表示是sql_sugg的name字段的值
		 * @param mcs
		 * @return
		 */
		private ModelColumn findModelColumn(String colName, ArrayList<ModelColumn> mcs) throws Exception{
			String[] ss=colName.split("#");
			boolean isPdtColumn=(ss.length==1);
			for(ModelColumn mc: mcs){
//				logger.debug("compare "+ mc.col+", sql="+mc.sql+" with "+ colName+"(idpdtcolumn="+ isPdtColumn+")+ss[0]="+ ss[0]+",ss[1]="+(ss.length>1? ss[1]:""));
				if(mc.isPdtColumn!=isPdtColumn){
					continue;
				}
				if(mc.isColor){
					return mc;
				}
				if(isPdtColumn){
					if( mc.col.equals(colName)) return mc;
				}else {
					if(mc.col.equals(ss[1]) && ss[0].equals(mc.sql)) return mc;
				}
				
			}
			throw new NDSException("view of "+ colName+"in model"+"@b2bedit-found@");
		}
	}
	/**
	 * 每一个可见行, 到商品款色级别, 不含asi级别的字段定义
	 *
	 */
	public class ViewRow{
		
		public ViewDefine view;
		public Color color;
		public ProductMatrix matrix;
		/**
		 * 准备好的数据行
		 */
		public JSONArray data;
		private String image;
		/**
		 * 
		 * @param view
		 * @param color
		 * @param matrix
		 */
		public ViewRow(ViewDefine view, Color color,ProductMatrix matrix){
			this.view=view;
			this.color=color;
			this.matrix=matrix;
			data=new JSONArray();
			for(ViewColumn vc:view.columns){
				if(vc.isPreSize||vc.isSize) continue;
				ArrayList<ModelColumn> cols=vc.cols;
				if(cols.size()>1){
					JSONArray v=new JSONArray();
					for(int i=0;i<cols.size();i++) v.put(JSONObject.NULL);
					data.put(v);
				}else{
					data.put(JSONObject.NULL);
				}
			}
		}
		/**
		 * 设置图片
		 * @param img
		 */
		public void setProductImage(String img){
			image=img;
		}
		/**
		 * 当前款色的图片
		 * @return
		 */
		public String getProductImage(){
			return image;
		}
		
		/**
		 * 商品id
		 * @return
		 */
		public int getProductId(){
			return matrix.getProductIds().get(0);
		}
		/**
		 * 色号
		 * @return
		 */
		public String getColorCode(){
			return color.getCode();
		}
		/**
		 * 设置单元格的值，将完成格式化，依据mc.fmt定义
		 * @param mc
		 * @param value
		 */
		public void setCell(ModelColumn mc, Object value) throws Exception{
			Object oldValue=data.get( mc.viewColumnIndex);
			if(oldValue instanceof JSONArray){
				((JSONArray)oldValue).put(mc.viewColumnItemIndex, value);
			}else{
				if(mc.viewColumnItemIndex>0) throw new NDSException("mc has item，but old data not array");
				data.put(mc.viewColumnIndex, value);
			}
		}
		
		
	}
	
	/**
	 * 存放每条ad_sql的查询结果和对应的数据列位置
	 */
	class SQLForColumn{
		String sqlName;
		ArrayList<Integer> columnIndex=new ArrayList();
		boolean asiLevel;
		
		// asi级别: key: pdtid, value: asiLevel {key: asi, value: jsonobj of that asi
		// 非asi: key pdtid+"_"+cc(colorcode), row of that 款色
		HashMap<String, JSONObject> pdtRows=new HashMap();
		/**
		 * 
		 * @param name ad_sql.name
		 * @param isASI
		 */
		public SQLForColumn(String name, boolean isASI){
			sqlName=name;
			asiLevel=isASI;
		}
		
		/**
		 * 非asi模式下的值获取
		 * @param pdtId
		 * @param colorCode 颜色
		 * @param key 
		 * @param format 可能为空
		 * @return null 如果未找到
		 */
		public Object getPdtValue(int pdtId,String colorCode, String key, Format format){
			JSONObject one=pdtRows.get(pdtId+"_"+colorCode);
			if(one==null) return null;
			Object value= one.opt(key);
			if(format!=null && value!=null) value=format.format(value);
			return value;
		}
		/**
		 * 获取所有pdt的asi，针对指定key的值
		 * @param key
		 * @param format maybe null
		 * @return {pdt_asi: value}
		 */
		public JSONObject getASIValuesByKey(String key, Format format) throws JSONException{
			JSONObject ret=new JSONObject();
			for(String pdtId:pdtRows.keySet()){
				JSONObject asiObj=pdtRows.get(pdtId);
				for(Iterator it=asiObj.keys();it.hasNext();){
					String asi=(String)it.next();
					JSONObject value=asiObj.getJSONObject(asi);
					Object v=value.opt(key);
					if(v!=null){
						if(format!=null) v=format.format(v);
						ret.put(pdtId+"_"+ asi, v);
					}else{
						if(format!=null) v=format.format(v);
						ret.put(pdtId+"_"+ asi, "");
					}
				}
			}
			return ret;
		}
		
		/**
		 * asi模式下的值获取
		 * @param pdtId
		 * @param asi
		 * @param key
		 * @param format maybe null
		 * @return null 如果未找到
		 */
		public Object getPdtValue(int pdtId, int asi, String key, Format format){
			JSONObject one=pdtRows.get(pdtId);
			if(one==null) return null;
			JSONObject asiObj= one.optJSONObject(String.valueOf(asi));
			if(asiObj==null) return null;
			Object v= asiObj.opt(key);
			if(v!=null && format!=null) v=format.format(v);
			return v;
		}
		
		/**
		 * 知道每个sql结果对应的column的位置
		 * @param colIdx 在model里定义的位置
		 */
		public void addColumn(int colIdx){
			columnIndex.add(colIdx);
		}
		/**
		 * 添加结果集
		 * @param rows
		 * @throws Exception
		 */
		public void addRows(JSONArray rows) throws Exception{
			for(int i=0;i<rows.length();i++){
				addRow(rows.getJSONObject(i));
			}
		}
		/**
		 * 针对查询到的行记录进行处理, 如果不是asi的，行对象必须具有2个属性：pdtid, cc(colorcode), 如果是asi级别的查询，无需cc字段，但需要有asi字段
		 * 注意非asi的查询都是到款色级别
		 * @param row {pdtid,cc,asi, xxx}
		 * {"pdtid":121,"suggqty":38,"asi":1404,"saleqty":37,"stockqty":36}
		 * {"pdtid":121,"asi":355,"qty":7}
		 * @throws Exception
		 */
		public void addRow(JSONObject row) throws Exception{
			int pdtId=row.optInt("pdtid",-1);
			if(pdtId==-1) throw new NDSException("ad_sql#"+sqlName+"@b2bedit-define@"+"@b2bedit-must@"+" col#pdtid");
			if(asiLevel){
				int asi=row.optInt("asi",-1);
				if(asi==-1)throw new NDSException("ad_sql#"+sqlName+"@b2bedit-define@"+"@b2bedit-must@"+" col#asi");
				JSONObject prow=pdtRows.get(String.valueOf(pdtId));
				if(prow==null) {
					prow=new JSONObject();
					pdtRows.put(String.valueOf(pdtId), prow);
				}
				prow.put(String.valueOf(asi), row);
			}else{
				String cc=row.optString("cc");
				if(Validator.isNull(cc)) throw new NDSException("ad_sql#"+sqlName+"@b2bedit-define@"+"@b2bedit-must@"+" col#cc");
				pdtRows.put(pdtId+"_"+cc, row);
			}
		}
	}
	
	
	/**
	 * 初始化矩阵构建器，主要是获得getSheet的支持
	 * @param jo 客户端传来的请求
	 * @param event 事件
	 * @param vc 上下午
	 * @param jedis
	 * @param conn
	 * @throws Exception
	 */
	public  void init(UserObj user, JSONObject jo, DefaultWebEvent event,VelocityContext vc,Jedis jedis, Connection conn) throws Exception{
		
		this.conn=conn;
		this.event=event;
		this.vc=vc;
		this.usr=user;
		this.jedis=jedis;
		this.engine=QueryEngine.getInstance();
		this.manager=TableManager.getInstance();
		this.conf = jo;
		gridConf=(JSONObject)PhoneController.getInstance().getValueFromADSQLAsJSON("grid:"+conf.optString("table")+":meta", null, conn);
		if(gridConf==null) throw new NDSException("@b2bedit-config@"+"ad_sql#grid:"+conf.optString("table")+":meta"+"@b2bedit-found@");
		
	}
	/**
	 * 根据客户端传来的参数，后台直接执行存储过程即可
	 * 
	 * @param jo{rowData:[{"0":"裙子",asi:{"2":"123_34_45677"},pdtId,box}]}
	 * @return jo{rowData:[{"0":"裙子",asi:{"2":"123_34_45677"},pdtId,box}],sum:{}}
	 * @param rowData 处理后的数据，根据配码或者散码模式的不同，分别对相应的asi级别的数据做清零操作，或者不做操作（当商品不存在该asi）
	 * @param  sum grid下面合计总量
	 * @throws Exception 
	 */
	@Admin(mail="li.shuhao@lifecycle.cn")
	public Object gridDelete(JSONObject jo) throws Exception{
		
		ArrayList<ModelColumn>  model=createModel(gridConf.getJSONArray("model"));
		//[{title, cols, css, issize, ispresize, width, options}]
		ViewDefine view=new ViewDefine(gridConf.getJSONArray("view"), model, gridConf.opt("options"));
		//在model中找到含有key为sumsql的ad_sql做数据的计算，当然还要拿到尺码所在索引来判断，所计算的sumsql的计算索引，就是返回给前台的field所在的key
		JSONArray rowData = jo.optJSONArray("rowData");
		if(rowData.length()==0) throw new NDSException("@b2bedit-delete@");
		
		int actId = jo.optInt("actId",-1);
		int sizeIndex = view.getSizeColumnIndexInGridView();//找到尺码组所在起始索引
		int maxSizeCount = WebController.getInstance().getMaxSizeCount(usr.getMarketId(), jedis, conn);//拿到尺码组最大的尺码列数
		for(int i = 0;i < rowData.length();i++){
			JSONObject data = rowData.optJSONObject(i);
			JSONObject asi = data.optJSONObject("asi");
			for(int k = 0;k < maxSizeCount;k++){
				if(view.preSizeColumnIndex > -1){
					if(Validator.isNull(asi.optString(String.valueOf(sizeIndex+(k*2+1)))))
						data.put(String.valueOf(sizeIndex+(k*2+1)), "");
					else
						data.put(String.valueOf(sizeIndex+(k*2+1)), "0");
				}
				else{
					if(Validator.isNull(asi.optString(String.valueOf(sizeIndex+k))))
						data.put(String.valueOf(sizeIndex+k), "");
					else
						data.put(String.valueOf(sizeIndex+k), "0");
				}
			}
			for(int k = 0;k < model.size();k++){
				ModelColumn mc = model.get(k);
				String sql = mc.sql;
				//计算列不为空的时候，我们就可以拿到该key为sql中的数据
				if(Validator.isNotNull(sql) && mc.asiLevel==false/*目前不支持asi级别的字段的重新计算*/){
					if(view.preSizeColumnIndex > -1){
						if(Validator.isNotNull(mc.fmt))
							data.put(String.valueOf(mc.viewColumnIndex+maxSizeCount*2), new DecimalFormat(mc.fmt).format(0));
						else
							data.put(String.valueOf(mc.viewColumnIndex+maxSizeCount*2), "0");
					}
					else{
						if(Validator.isNotNull(mc.fmt))
							data.put(String.valueOf(mc.viewColumnIndex+maxSizeCount), new DecimalFormat(mc.fmt).format(0));
						else
							data.put(String.valueOf(mc.viewColumnIndex+maxSizeCount), "0");
					}
				}
			}
			vc.put("userid", usr.getId());
			vc.put("pdtid", data.getInt("pdtId"));
			vc.put("asiid", -1);
			vc.put("actid", actId);
			vc.put("qty", 0);
			vc.put("marketid", usr.getMarketId());
			executeProcedureOrMerge();
		}
		jo.put("sum",  computeSumLineData(gridConf.optString("sumline_sql"),gridConf.optJSONArray("sumline")));
		
		clearBroRedisCashe(jo);
		
		return jo;
	}
	/**
	 * 退货单清楚redis缓存
	 * 
	 */
	private void clearBroRedisCashe(JSONObject jo){
		//退货单清除redis缓存
		if(jo.optString("table").equals("bro")){
			String key = new String();
			key = "bro:"+jo.optInt("id");
			if(jedis.exists(key)){
				jedis.del(key);
			}
		}
		
	}
	/**
	 * 
	 * 根据前台在界面做修改，我们在后台做即时保存，并返回前台修改过的计算值	
	 *   
	 * @throws Exception
	 * @param pdtRow {"cmd":"b2b.grid.save","key":"457_1619","cnt":"start":"actid":,"cc":,"cashekey":,"qty":}
	 * key:$pdtId_$asiId
	 * cashKey:前台传给后台，拿到所有商品来调整grid下面的合计量的缓存key
	 * qty:客户修改后的值
	 * cnt:当前页有多少条
	 * start:从多少条开始，我其实也没懂
	 * cc:颜色字段
	 * @return JSONObject
	 * {comm:[  
	 * 				{key: ,value:}..
	 *  ],sum[123,345,1111]}
	 *  comm:普通行数据
	 *  sum:grid下面的计算总量
	 *  key:前台所对应的field
	 *  value:对应单元格的值
	 * @throws Exception
	 * @return 
	 * {
	  "comm": [
	    {
	      "key": "9",
	      "value": "457"
	    },
	    {
	      "key": "10",
	      "value": "￥228043"
	    },
	    {
	      "key": "11",
	      "value": "￥228043"
	    }
	  ],
	  "sum": {
	    "sumamt":"￥16,378.00",
	    "sumqty":"325,000",
	    "sumamtlist":"￥20,378.00"
	  }
		key ：为前台对应字段索引，value为input值
		smm：代表grid下面的总计量, key: 关键名称， value: 值，注意值已经format过了
	 */
	@Admin(mail = "li.shuhao@lifecycle.cn")
	public JSONObject gridSave(JSONObject pdtRow) throws Exception{
		
		//完成订单的更新
		mergeData(pdtRow);
		
		String pdtIdAndAsiId = pdtRow.getString("key");
		int pdtId = Integer.parseInt(pdtIdAndAsiId.split("_")[0]);
		int asiId = Integer.parseInt(pdtIdAndAsiId.split("_")[1]);
		String colorCode = pdtRow.getString("cc");
		//model*:[{col, sql, desc, func, key, sum,sumsql, fmt, css }]
		ArrayList<ModelColumn>  model=createModel(gridConf.getJSONArray("model"));
		//[{title, cols, css, issize, ispresize, width, options}]
		ViewDefine view=new ViewDefine(gridConf.getJSONArray("view"), model, gridConf.opt("options"));
		//在model中找到含有key为sumsql的ad_sql做数据的计算，当然还要拿到尺码所在索引来判断，所计算的sumsql的计算索引，就是返回给前台的field所在的key
		
		int sizeIndex = view.getSizeColumnIndexInGridView();//找到尺码组所在起始索引
		int maxSizeCount = WebController.getInstance().getMaxSizeCount(usr.getMarketId(), jedis, conn);//拿到尺码组最大的尺码列数
		HashMap<String, SQLForColumn> sqlColumns = setupSQLColumns(model, new JSONArray().put(pdtId), false);
		
		JSONArray comData = new JSONArray(); //key: view column index, value: formatted data
		for(int i = 0;i < model.size();i++){
			ModelColumn mc = model.get(i);
			String sql = mc.sql;
			JSONObject jo;
			//计算列不为空的时候，我们就可以拿到该key为sql中的数据
			if(Validator.isNotNull(sql) && mc.asiLevel==false/*目前不支持asi级别的字段的重新计算*/){
				jo = new JSONObject();
				if(i < sizeIndex){
					jo.put("key",mc.viewColumnIndex+"");
				}else{
					if(view.preSizeColumnIndex > -1)
						jo.put("key",(mc.viewColumnIndex+maxSizeCount*2)+"");
					else
						jo.put("key",(mc.viewColumnIndex+maxSizeCount)+"");
				}
				jo.put("value", sqlColumns.get(mc.sql).getPdtValue(pdtId, colorCode, mc.col,mc.format));
				comData.put(jo);
			}
		}
		//计算grid下面合计总量的值
	
		
		JSONObject result = new JSONObject();
		result.put("comm", comData);
		result.put("sum",  computeSumLineData(gridConf.optString("sumline_sql"),gridConf.optJSONArray("sumline")));
		
		//退货单清除redis缓存
		clearBroRedisCashe(pdtRow);
		
		return result;
	}
	/**
	 * 计算合计行的值， 适配online_edit_grid_sumline的定义， online_edit_grid_sumline的格式:
	 * "合计数量:<b> [[sumqty]] </b>, 合计金额:<b> [[sumamt]]</b>, 合计零售额: <b>[[sumamtlist]]</b>"
	 * [[key]]的定义就是model column.col 定义 
	 * @param model
	 * @return key 需要满足: online_edit_grid_sumline 中的key， value：已经格式化的值
	 * {
	    "sumamt":"￥16,378.00",
	    "sumqty":"325,000",
	    "sumamtlist":"￥20,378.00"
	  }
	 * @throws Exception
	 */
	protected JSONObject computeSumLineData(String ad_sql_line,JSONArray sumTemplate) throws Exception{
		JSONObject result = new JSONObject();
		
		if(sumTemplate==null) throw new NDSException("@b2bedit-model@");
		SQLWithParams swp = PhoneController.getInstance().parseADSQL(ad_sql_line, vc, conn);
		if(Validator.isNull(swp.getSQL())) throw new NDSException("@b2bedit-expression@");
		JSONObject sum = QueryEngine.getInstance().doQueryObject(swp.getSQL(),swp.getParams(),conn);
		for(int i = 0;i < sumTemplate.length();i++){
			JSONObject jo = sumTemplate.getJSONObject(i);
			String value = jo.optString("value");
			String fmt = jo.optString("fmt");
			if(Validator.isNotNull(value)){
				if(Validator.isNotNull(fmt))
					result.put(value, new DecimalFormat(fmt).format(sum.optDouble(value)));
				else
					result.put(value, sum.optDouble(value));
			}else{
				continue;
			}
		}
		return result;
	}
	
	/**
	 * velocity需要插入的值为$userid,用户id $pdtid,商品id $asiid,ASIid $bprmtid,活动id $qty,数量 
	 * @return
	 * @throws Exception 
	 */
	public void mergeData(JSONObject pdtRow) throws Exception{
		String pdtIdAndAsiId = pdtRow.getString("key");
		Pattern pattern = Pattern.compile("(\\d+)_(\\d+)");
		Matcher matcher = pattern.matcher(pdtIdAndAsiId);
		if(!matcher.find()) 
			throw new NDSException("@b2bedit-coun@");
		int actId = pdtRow.optInt("actId",-1);//活动id
		int pdtId = Integer.parseInt(pdtIdAndAsiId.split("_")[0]);//商品id
		int asiId = Integer.parseInt(pdtIdAndAsiId.split("_")[1]);//ASIid
		int qty = Integer.parseInt(pdtRow.optString("qty","0").trim());//去掉空格
		
		/**
		 * both需求，配置中如果存在某个正则，则对qty进行判断，不满足捞出警告信息
		 * 例：qty为偶数
		 */
		JSONObject warnPattern = gridConf.optJSONObject("qtypattern");
		if(warnPattern!=null){
			if(Validator.isNotNull(warnPattern.optString("pattern"))){
				boolean isPattern = Pattern.matches(warnPattern.optString("pattern"), String.valueOf(qty));
				if(!isPattern) throw new NDSException(warnPattern.optString("warn")); 
			}
		}

		
		HashSet<Integer> asis= getAvaiableASIs(pdtId, actId);
		if(!asis.contains(asiId)){
			throw new NDSException("@b2bedit-asi@");
		}
		int sizeFactor = 1;
		ProductMatrixLoader loader = new ProductMatrixLoader(jedis, conn);
		ProductMatrix mat = loader.getSimpleProductMatrix(pdtId);
		JSONArray asi=mat.getASIArrays();
		ArrayList<Integer> sizeFactors=mat.getSizeFactors();
		for(int i=0;i<asi.length();i++){
			//i is for color
			JSONArray row=asi.getJSONArray(i);
			for(int k=0;k<row.length();k++){
				//k is for size
				int as=row.optInt(k,-1);
				if(as == asiId){
					sizeFactor = sizeFactors.get(k);
				} 
			}
		}
		/*
		 * 这是木槿项目开始的下单倍数，比如单价是按支定义的，界面上中包规格是14，则下单1即为14，2就是28，数据库存储仍然按支数
		 */
		//现在关于qty的倍数逻辑全部交给DB
		/*		
		 * JSONObject po=PhoneUtils.fetchObjectAllColumns(TableManager.getInstance().getTable("pdt"), pdtId, conn, jedis);
		 * int packQty = po.optInt("packqty",1);
		 * if(packQty<=0)packQty=1;;
		 * 
		 * qty= qty*packQty*sizeFactor;
		 */

		//在vc中已经有所有传人的参数，包括id, 即bro.id之类的内容
		//拿到oracle merge的sql语句，执行
		//String sql= PhoneController.getInstance().getValueFromADSQL("grid_merge_b_cart", conn);
		vc.put("uid", usr.getId());
		vc.put("pdtid", pdtId);
		vc.put("asiid", asiId);
		vc.put("actid", actId);
		vc.put("qty", qty);
		vc.put("marketid", usr.getMarketId());
		
		//执行存储过程来对数据库进行qty的更新
		executeProcedureOrMerge();
	}
	/**
	 * to Array
	 * @param params
	 * @return
	 */
	private ArrayList toList(Object[] params) {
		@SuppressWarnings("rawtypes")
		ArrayList al=new ArrayList();
		for(Object o: params) al.add(o);
		return al;
	}
	/**
	 * 执行存储过程来对数据库进行qty的更新
	 * 
	 * @param pdtRow 数据行
	 * @param pdtId 商品Id
	 * @param actId 活动Id
	 * @throws Exception
	 */
	private void executeProcedureOrMerge() throws Exception{
		//从save_proc读取参数，如果设置，表示全部由存储过程完成计算
		String sqlName = this.gridConf.optString("save_proc");
		// 由于DB自定义异常报错信息，直接直接抛给前端
		try {
			if (Validator.isNotNull(sqlName)) {
				SQLWithParams sqlParam = PhoneController.getInstance().parseADSQL(sqlName, vc, conn);
				engine.executeStoredProcedure(sqlParam.getSQL(), toList(sqlParam.getParams()), false, conn);
			} else {
				throw new NDSException("@b2bedit-config@"+"save_proc"+"@b2bedit-found@");
			}
		} catch (Exception e) {
			throw new NDSException(e.getMessage());
		}
	}
	/**
	 * 得到所有可用的asiId 
	 * 
	 * @param pdtId 商品id
	 * @param actId 活动id
	 * @return
	 * @throws Exception
	 */
	private HashSet<Integer> getAvaiableASIs(int pdtId, int actId) throws Exception{
		
		HashMap<Integer, HashSet> pdtAsis=new HashMap();
		//key: asi 可以下单的asi
		HashSet<Integer> alaiableAsis= pdtAsis.get(pdtId);
		if(alaiableAsis!=null) return alaiableAsis;
		
		alaiableAsis=new HashSet();
		//获取可下单的asi, 需要基于当前活动, row: [asi], 与标准矩阵不同的是：不同的活动，不同的市场，商品仍然有不同
		JSONArray alaiableAsiArray;
		vc.put("pdtid", pdtId);
		vc.put("marketid", usr.getMarketId());
		if(actId==-1) alaiableAsiArray=PhoneController.getInstance().getDataArrayByADSQL("pdt_asi_list", vc, conn, false);
		else alaiableAsiArray=PhoneController.getInstance().getDataArrayByADSQL("pdt_act_asi_list", vc, conn, false);
		for(int i=0;i<alaiableAsiArray.length();i++) alaiableAsis.add(alaiableAsiArray.getInt(i));
		
		if(alaiableAsis.size()==0) throw new NDSException("@b2bedit-order@"+ pdtId);
		
		pdtAsis.put(pdtId,  alaiableAsis);
		
		return alaiableAsis;
	}
}
