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
 * ������ʾ�õ�view�Ķ���
 * 
 * @author yfzhu
 *
 */
public class GridViewDefine {
	private static Logger logger = LoggerFactory.getLogger(GridViewDefine.class);
	/**
	 * �ж��壬��������м���Ԫ�أ�ÿ��Ԫ�طֱ��css
*view*: [{title, cols, css, issize, ispresize, width, options}]

> *title* grid �ı�����
> *cols* 2����ʽ: string���� string array, ��: "no", ��["no","price"], ע�����ĳ��sql����Ӧ���ֶκ�pdt�ֶ�����һ�£��ֶ�����Ҫ����sql������Ϊǰ׺����#�ţ���: "sql_sugg#name", ��ʾ��sql_sugg��name�ֶε�ֵ
> *css* �����е���ʾЧ����ע��model�����ÿ��colҲ��һ��css�����1��ֻ��1���ֶΣ�������������
> *issize* boolean, �Ƿ���뿪ʼ�У�����ʵ��������ϵͳ�Զ�����
> *ispresize* boolean, ��ǰ�еĶ����Ƿ��ǳ�����أ���ζ��ÿ�������ж����������ǰ׺
> *width*  int �еĿ��, in pixel
> *options* �ͻ��˿��Խ��ܵ��������ã�����Ҫ���������е���ʾ���Ե�

	 */
	private class ColumnDefine{
		public String title;
		public ArrayList<String> titles;//��Զ������������,Ŀǰûʲô����
		public String css;
		public int width;
		public JSONObject options;
		public ArrayList<ColumnItem> subColumns;//����item
		public boolean isEditable;//�Ƿ�����༭
		public boolean isSize;//�Ƿ������
		public boolean isPreSize;//�Ƿ��ǳ����е�ǰ׺
		/**
		 * Ŀǰ��2��ֵ��Column.STRING, Column.NUMBER
		 */
		public int type;
		/**
		 * �ͻ�����Ҫ��ag-grid��ʽ����
		 * @return
		 */
		//��ʱ��������
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
	 * ��view���е�����Ŀ
	 *
	 */
	private class ColumnItem{
		public String desc;
		public String css;
		public int type;
	}
	
	private ArrayList<ColumnDefine> colDefs;//��̨�����ת����ǰ̨�ܹ�ʶ���meta����
	private int maxSizeNum;
	private int sizeIndex;
	/**
	 * [
	 *  [38,S,28] (��0�У�,
	 *  [39,M,29]����1�У�,
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
	 * ����View�Ķ��崴���ͻ��������ʽ
	 * @param view
	 * @param sizeColumns ��ʽ: 
	 * 38,39,40,41,42,43
     * S,M,L,XL,XXL,XXXL
	 * 28,30,32,34,36
	 * �ѳ�����Ŵ����
	 * [
	 *  [38,S,28] (��0�У�,
	 *  [39,M,28]����1�У�,
	 *  [40,L,30]...
	 * ]
	 * 
	 * @param maxSizeNum �������������
	 * @param sizeIndex ��������view�е�����λ�ã�0��ʼ
	 * @param gridConf ԭʼ��online_edit_grid���������л�ȡsumline��options�ȶ��壬���ؿͻ���
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
	 * ��ʼ��
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
			cd.titles=null;//TODO lishuhao Ŀǰûʲô����
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
	 * �������߱༭�����ͷ����
	 * 
	 * @return
	 * @throws Exception
	 */
	@Admin(mail = "li.shuhao@lifecycle.cn")
	public JSONObject toJSONObjet() throws Exception {
		JSONObject model_head = new JSONObject();//���ص��ܶ���
		JSONObject dataDefine = new JSONObject();//���ݵĿ��Ի��е�css����
		JSONObject presizeDefine = new JSONObject();//ǰ׺����
		JSONArray ja = new JSONArray();
		ColumnDefine sizeDefine = null;
		ColumnDefine preDefine = null;
		int linewrap = 1;
		//�ҵ�ǰ׺�ο��ж���ͳ��붨��
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
				//�ο��ж���ȫ��Ψһ����Ҫ������
				presizeDefine.put("pre_css", pre_css);//css���ַ�����ʽ���ظ�ǰ̨������֮����";"���
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
					//������ǰ׺�ο���
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
					//����ǰ׺�ο��У���װ��һ����ǰ׺�ο���+���붩�����Ķ���
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
				
				//�����Ԫ���ж������ݣ������ʽ���ݸ�ǰ��
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
		//����ǰ̨�����ȫ��options��չ��������һЩ�޸ģ�����freezecol�ֶεĶ��壬���ظ���ǰ̨�ܹ�ֱ���õ�����ʽ��options�������ֶθ�ʽ����
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
	 * ���ȱʡ��һЩcss��$css;padding-left:2px
	 * @param css
	 * @param padding �Ƿ���Ҫpadding-left
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
	 * ���ȱʡ��һЩcss: {padding-left:2px}
	 * @param css 
	 * @param padding �Ƿ���Ҫpadding-left
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
