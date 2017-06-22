package com.agilecontrol.b2bweb.grid;

import java.util.*;

import org.json.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.Connection;
import com.agilecontrol.b2bweb.WebController;
import com.agilecontrol.b2bweb.grid.GridBuilder.ModelColumn;
import com.agilecontrol.b2bweb.grid.GridBuilder.ViewColumn;
import com.agilecontrol.b2bweb.grid.GridBuilder.ViewDefine;
import com.agilecontrol.b2bweb.grid.GridBuilder.ViewRow;
import com.agilecontrol.b2bweb.sheet.Color;
import com.agilecontrol.b2bweb.sheet.ProductMatrix;
import com.agilecontrol.nea.core.query.QueryEngine;
import com.agilecontrol.nea.core.query.QueryException;
import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.nea.util.Validator;
import com.agilecontrol.phone.Admin;
import com.agilecontrol.phone.PhoneController;

/**
 * 
 * 界面显示用的view的定义
 * 
 * @author yfzhu
 *
 */
public class GridViewDefine {
	private static Logger logger = LoggerFactory.getLogger(GridViewDefine.class);
	/**
	 * 列定义，比如此列有几个元素，每个元素分别的css
*view*: [{title, cols, css, issize, ispresize, width, options}]

> *title* grid 的标题行
> *cols* 2种形式: string，或 string array, 如: "no", 或["no","price"], 注意如果某个sql语句对应的字段和pdt字段名称一致，字段名需要增加sql名称作为前缀，加#号，如: "sql_sugg#name", 表示是sql_sugg的name字段的值
> *css* 决定列的显示效果，注意model上面的每个col也有一个css，如果1列只有1个字段，可以任意设置
> *issize* boolean, 是否尺码开始列，尺码实际列数由系统自动生成
> *ispresize* boolean, 当前列的定义是否是尺码相关，意味着每个尺码列都有这个列做前缀
> *width*  int 列的宽度, in pixel
> *options* 客户端可以接受的其他配置，比如要设置特殊列的显示属性等

	 */
	private class ColumnDefine{
		public String title;
		public ArrayList<String> titles;//针对多个尺码组的情况,目前没什么意义
		public String css;
		public int width;
		public JSONObject options;
		public ArrayList<ColumnItem> subColumns;//数据item
		public boolean isEditable;//是否允许编辑
		public boolean isSize;//是否尺码列
		public boolean isPreSize;//是否是尺码列的前缀
		/**
		 * 目前就2种值：Column.STRING, Column.NUMBER
		 */
		public int type;
		/**
		 * 客户端需要的ag-grid格式定义
		 * @return
		 */
		//暂时先舍弃掉
/*		public JSONObject toJSONObject() throws Exception{
			JSONObject jo = new JSONObject();
			jo.put("model_css", css);
			jo.put("model_title",title);
			jo.put("type", type);
			jo.put("isEditable", isEditable);
			jo.put("options", options);
			jo.put("width", width);
			jo.put("view_define", subColumns);
			jo.put("size_title", titles);
			jo.put("isSize", isSize);
			jo.put("isPre", isPreSize);
			return jo;
		}*/
	}
	/**
	 * 在view列中的子项目
	 *
	 */
	private class ColumnItem{
		public String desc;
		public String css;
		public int type;
	}
	
	private ArrayList<ColumnDefine> colDefs;//后台定义的转换成前台能够识别的meta定义
	private int maxSizeNum;
	private int sizeIndex;
	/**
	 * [
	 *  [38,S,28] (第0列）,
	 *  [39,M,29]（第1列）,
	 *  [40,L,30]...
	 * ]	 * 
	 */
	private JSONArray sizeColumns;
	/**
	 * ad_sql#online_edit_grid
	 */
	private JSONObject gridConf;
	
	private Connection conn;
	/**
	 * 根据View的定义创建客户端所需格式
	 * @param view
	 * @param sizeColumns 格式: 
	 * 38,39,40,41,42,43
     * S,M,L,XL,XXL,XXXL
	 * 28,30,32,34,36
	 * 把尺码横排处理成
	 * [
	 *  [38,S,28] (第0列）,
	 *  [39,M,28]（第1列）,
	 *  [40,L,30]...
	 * ]
	 * 
	 * @param maxSizeNum 最大尺码组的数量
	 * @param sizeIndex 尺码组在view中的索引位置，0开始
	 * @param gridConf 原始的online_edit_grid，将从其中获取sumline，options等定义，返回客户端
	 * @throws Exception
	 */
	public GridViewDefine(ViewDefine view,JSONArray sizeColumns, JSONObject gridConf ,Connection conn) throws Exception{
		this.sizeColumns=sizeColumns;
		this.maxSizeNum = sizeColumns.length();
		this.sizeIndex = view.getSizeColumnIndexInGridView();
		this.gridConf=gridConf;
		this.conn = conn;
		init(view);
		
	}
	/**
	 * 初始化
	 * @param view
	 * @throws Exception
	 */
	private void init(ViewDefine view) throws Exception{
		colDefs=new ArrayList();
		for (ViewColumn vc: view.columns){
			ColumnDefine cd=new ColumnDefine();
			cd.css=vc.css;
			cd.isEditable=vc.isSize;
			cd.isPreSize=vc.isPreSize;
			cd.isSize=vc.isSize;
			cd.options=vc.options;
			cd.title=vc.title;
			cd.titles=null;//TODO lishuhao 目前没什么意义
			cd.width= vc.width;
			
			cd.subColumns=new  ArrayList<ColumnItem>();
			for(ModelColumn mc: vc.cols){
				ColumnItem ci=new ColumnItem();
				ci.css=mc.css;
				ci.desc=mc.desc;
				ci.type=mc.getType();
				cd.subColumns.add(ci);
			}
			
			colDefs.add(cd);
		}
	}
	/**
	 * 返回在线编辑界面表头定义
	 * 
	 * @return
	 * @throws Exception
	 */
	@Admin(mail = "li.shuhao@lifecycle.cn")
	public JSONObject toJSONObjet() throws Exception {
		JSONObject model_head = new JSONObject();//返回的总定义
		JSONObject dataDefine = new JSONObject();//数据的可以换行的css定义
		JSONObject presizeDefine = new JSONObject();//前缀定义
		JSONArray ja = new JSONArray();
		ColumnDefine sizeDefine = null;
		ColumnDefine preDefine = null;
		int linewrap = 1;
		//找到前缀参考列定义和尺码定义
		for(int i = 0;i < colDefs.size();i++){
			ColumnDefine cd = colDefs.get(i);
			
			if(cd.isPreSize){
				preDefine = cd;
				JSONArray pre_css = new JSONArray();
				JSONArray pre_desc = new JSONArray();
				for(int k = 0;k < cd.subColumns.size();k++){
					ColumnItem ci = cd.subColumns.get(k);
					pre_css.put(addFixedToStringCss(ci.css,true));
					pre_desc.put(ci.desc);
				}
				if(pre_css.length() > linewrap) linewrap = pre_css.length();
				//参考列定义全局唯一，需要单独给
				presizeDefine.put("pre_css", pre_css);//css以字符串形式返回给前台，属性之间以";"间隔
				presizeDefine.put("pre_desc", pre_desc);
				continue;
			}
			if (cd.isSize) {
				sizeDefine = cd;
				continue;
			}
		}
		
		
		int j = 0;
		for (int i = 0; i < colDefs.size(); i++) {
			ColumnDefine cd = colDefs.get(i);
			JSONObject jo;
			if(cd.isPreSize) continue;
			if (j == sizeIndex) {
				JSONArray sizeDesc = sizeColumns;
				for(int k = 0;k < sizeColumns.length();k++){
					jo = new JSONObject();
					StringBuilder stb = new StringBuilder();
					for(int l = 0;l < sizeDesc.getJSONArray(k).length();l++){
						stb.append(sizeDesc.getJSONArray(k).get(l)+",");
					}
					jo.put("desc", stb.deleteCharAt(stb.length()-1));
					jo.put("options", sizeDefine.options);
					jo.put("isSize", true);
					
//					jo.put("type", sizeDefine.type);
					JSONArray childDesc = new JSONArray();
					//不含有前缀参考列
					if(preDefine==null){
						JSONObject sdesc = new JSONObject();
						sdesc.put("isPresize", false);
						sdesc.put("editable", true);
						sdesc.put("headerName", sizeDefine.title);
						sdesc.put("field", String.valueOf(k + sizeIndex));
						sdesc.put("width", sizeDefine.width);
						sdesc.put("cellStyle", addFixedToJSONCss(sizeDefine.css,false));

						j++;
						
						childDesc.put(sdesc);
					}
					//含有前缀参考列，封装成一个（前缀参考列+尺码订量）的对象
					else{
						JSONObject pdesc = new JSONObject();
						pdesc.put("isPresize", true);
						pdesc.put("editable", false);
						pdesc.put("headerName", preDefine.title);
						pdesc.put("field", String.valueOf((k*2) + sizeIndex));
						pdesc.put("width", preDefine.width);
						pdesc.put("cellStyle", addFixedToJSONCss(preDefine.css,false));
						j++;
						
						JSONObject sdesc = new JSONObject();
						sdesc.put("isPresize", false);
						sdesc.put("editable", true);
						sdesc.put("headerName", sizeDefine.title);
						sdesc.put("field", String.valueOf((k*2+1) + sizeIndex));
						sdesc.put("width", sizeDefine.width);
						sdesc.put("cellStyle", addFixedToJSONCss(sizeDefine.css,false));
						j++;
						
						childDesc.put(pdesc);
						childDesc.put(sdesc);
					}
					jo.put("childDesc", childDesc);
					
					ja.put(jo);
				}

			} else {
				

				
				jo = new JSONObject();
				jo.put("editable", cd.isEditable);
				jo.put("desc", cd.title);
				jo.put("options", cd.options);
				jo.put("cellStyle", addFixedToJSONCss(cd.css,true));
				jo.put("width", cd.width);
				jo.put("field", String.valueOf(j));
				jo.put("isSize", false);
				jo.put("childDesc", "");
				
				JSONArray data_css = new JSONArray();
				int css_length = cd.subColumns.size();
				
				//如果单元格还有多行数据，则把样式传递给前端
				if(css_length > 1) {
					jo.put("isEnter", true);
					for (int k = 0; k < cd.subColumns.size(); k++) {
						ColumnItem ci = cd.subColumns.get(k);
						data_css.put(addFixedToStringCss(ci.css, true));
					}
					dataDefine.put(j + "_css", data_css);
					if(css_length > linewrap) linewrap = css_length; 
				}
				
				j++;
				ja.put(jo);
			}
		}
		model_head.put("columns", ja);
		model_head.put("dataDefine", dataDefine);
		model_head.put("preDefine", presizeDefine);
		model_head.put("sizeGroup", 1);
		model_head.put("linewrap", linewrap);
		model_head.put("buttons", gridConf.optJSONArray("buttons"));
		
		//gloable options and sumline
		//根据前台需求对全局options扩展定义做出一些修改，对于freezecol字段的定义，返回给以前台能够直接拿到的形式，options其他的字段格式不变
		JSONObject options=this.gridConf.optJSONObject("options");
		JSONArray opt = new JSONArray();
		if(!options.isNull("freezecol")){
			int index = options.optInt("freezecol");
			if(index!=0){
				for(int i = 0;i < index;i++){
					opt.put(String.valueOf(i));
				}
				options.put("freezecol", opt);
				model_head.put("options", options);
			}else{
				options.put("freezecol", "");
				model_head.put("options",options);
			}
		}
		
		//sumline template
		JSONArray sumline=gridConf.optJSONArray("sumline");
		if(Validator.isNull("sumline")) throw new NDSException("@b2bedit-config@"+"ad_sql#grid:$table:online_edit_kvs#sumline"+"@b2bedit-found@");
		model_head.put("sumline", sumline);
		
		return model_head;
	}
	/**
	 * 添加缺省的一些css：$css;padding-left:2px
	 * @param css
	 * @param padding 是否需要padding-left
	 * @return
	 * @throws Exception
	 */
	private String addFixedToStringCss(String css,boolean padding) throws Exception{
		StringBuilder newCss = new StringBuilder();
		if(css == null) css = new String();
		if(padding&&!css.contains("padding-left")){
			if(css.endsWith(";")||css.endsWith(",")) newCss.append(css+"padding-left");
			else {
				if(css.length()==0)
					newCss.append("padding-left");
				else
					newCss.append(";padding-left");
			}
			return newCss.toString().replaceAll(",", ";");
		}else
			return css.replaceAll(",", ";");
	}
	/**
	 * 添加缺省的一些css: {padding-left:2px}
	 * @param css 
	 * @param padding 是否需要padding-left
	 * @return
	 * @throws Exception
	 */
	private JSONObject addFixedToJSONCss(String css, boolean padding) throws Exception{
		JSONObject cssObj;
		try{
			if(css==null) cssObj= new JSONObject();
			else if(css.startsWith("{")) cssObj=new JSONObject(css);
			else cssObj=new JSONObject("{"+css+"}");
		}catch(Throwable tx){
			logger.error("Fail to parse "+ css +" as jsonbj:"+ tx.getLocalizedMessage(), tx);
			cssObj=new JSONObject();
		}
		if(padding && Validator.isNull( cssObj.optString("padding-left"))){
			cssObj.put("padding-left", "2px");
		}
		return cssObj;
	}
}
