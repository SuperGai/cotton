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
 * �������߹��ﳵ��񻯱༭�����ṩ��������
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
	 * �����jedis���ӣ���connһ������Ҫ�����ر�
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
	 * View�Ķ���
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

����grid, ����������Ӧpdtid, Ȼ��ƴ�Ӵ��м�¼

ad_sql#online_edit_grid, ��Ϊ����(model)�������ʾ(view)����, ��ʾ��������Ϊ1�п����ж���ֶ�
> {model, view, options, sumline}
*model*:[{col, sql, desc, func, key, sum,sumsql, fmt, css, asilevel }]
*view*: [{title, cols, css, issize, ispresize, width, options}]

model����˵��
> *col* string�� col�ɶ����ֵ�����е�table:pdt:meta���ֶ��������ֶ�������ʽ����"no", ���ʹ��pdt���ԣ���Ҫ����sql�������������ȡ��sql�����У�����sql�����ֶ�����Ϊcol�����ƣ���"qty", ϵͳ��ʶ��sql��䶨�����sql��Ӧ���ж����м�����Ӧֵ
> *sql* string,����ad_sql�����ƣ����ǵ�cols �����ж��и�����ͬsql��䣬��������Ҫ��֤����ɱ����ã�sql����п���ʹ�õı���������kv˵��һ��
> *desc* string, ֧��@xxx@��ʽ�Ķ����Է��룬��Ӧpdt����ֶο��Բ��ṩ����ϵͳ�Զ����룬����realname���Զ�λ��ad_column�����
> *func* string, ����ֶ��Ǽ����У��ڴ��趨���㹫ʽ����ʽ�п��Ժ��������У���key����ʶ���� "price*qty"
> *key* string�� ����Ϊ�գ���ǰ�����е�keyֵ����������func������
> *sum*  boolean, default false. ��ǰ���Ƿ���Ҫ�ڵײ��ϼƣ�ϵͳ��Ҫ�������еĵ�ǰ�кϼ�����������Ǽ����У������ṩsumsql
> *sumsql* string, ad_sql.name�� ��ʽ select xxx from yyy , ��Ȼ���ñ��������������sumsql������sum
> *fmt* ���ݵĸ�ʽ�����colΪarray��fmtҲ������array���趨ÿ�еĸ�ʽ
> *css* ������ǰ�ֶε���ʾЧ����ע��view�����css������еģ���1����������ж��col
> *asilevel* boolean �Ƿ���asi��������ݣ�asi��������ݣ� sql�ĸ�ʽ�� select pdtid, asi, xxx from xxx 

view����˵��
> *title* grid �ı�����
> *cols* 2����ʽ: string���� string array, ��: "no", ��["no","price"], ע�����ĳ��sql����Ӧ���ֶκ�pdt�ֶ�����һ�£��ֶ�����Ҫ����sql������Ϊǰ׺����#�ţ���: "sql_sugg#name", ��ʾ��sql_sugg��name�ֶε�ֵ
> *css* �����е���ʾЧ����ע��model�����ÿ��colҲ��һ��css�����1��ֻ��1���ֶΣ�������������
> *issize* boolean, �Ƿ���뿪ʼ�У�����ʵ��������ϵͳ�Զ�����
> *ispresize* boolean, ��ǰ�еĶ����Ƿ��ǳ�����أ���ζ��ÿ�������ж����������ǰ׺
> *width*  int �еĿ��, in pixel
> *options* �ͻ��˿��Խ��ܵ��������ã�����Ҫ���������е���ʾ���Ե�

sumline:
ad_sql�����ƣ���ʽ���������ϼ�����:<b> [[sumqty]] </b>, �ϼƽ��:<b> [[sumamt]]</b>, �ϼ����۶�: <b>[[sumamtlist]]</b>������ʹ�ÿͻ��˵�ģ����ʽ�����е�key������modelcolumn.col �Ķ���

	 * @param pdtIds id of m_product.id 
	 * @return {rows, sum}, ����
	 * rows ��ÿ�е����ݸ�ʽ����ʽ��ʽ��TODO ���������
	 * sum: k/v �ṹ���ͻ��˽�����ģ�壬����:"sum": {
	    "sumamt":"��16,378.00",
	    "sumqty":"325,000",
	    "sumamtlist":"��20,378.00"
	  }
	 * @throws Exception
	 */
	public JSONObject getViewData(JSONArray pdtIds) throws Exception {
		//model*:[{col, sql, desc, func, key, sum,sumsql, fmt, css }]
		ArrayList<ModelColumn>  model=createModel(gridConf.getJSONArray("model"));
		//[{title, cols, css, issize, ispresize, width, options}]
		ViewDefine view=new ViewDefine(gridConf.getJSONArray("view"), model, gridConf.opt("options"));
		//if(pdtIds.length()==0) throw new NDSException("��Ʒ�б�Ϊ��");
		//������sql������е���Ϣ
		HashMap<String, SQLForColumn> sqlColumns=setupSQLColumns(model, pdtIds,true);
		//������pdt���Զ�����е���Ϣ
		ArrayList<Column> pdtCols=setupPdtColumns(model);
		//ele are json obj, key is column name
		JSONArray pdtColumnRows=PhoneUtils.getRedisObjectArray("pdt", pdtIds, pdtCols, true, conn, jedis);
		for(int i=0;i<pdtColumnRows.length();i++){
			JSONObject jo=pdtColumnRows.getJSONObject(i);
			//�������еĶ��г�����
			WebController.getInstance().replacePdtValues(jo, usr.getLangId(), usr.getMarketId(), vc, jedis, conn);
		}
		/**
		 * ��model����׼���õ���Ʒ��Ϣ��ɫ����Ϣ����view�Ķ�����
		 */
		ArrayList<ViewRow> pdtColorRows=new ArrayList();
		
		ProductMatrixLoader loader=new ProductMatrixLoader(jedis,conn);
		
		//�������е�pdtmatrix
		ArrayList<ProductMatrix> pdtMatrixes=new ArrayList();
		for(int i=0;i<pdtIds.length();i++){
			int pdtId=pdtIds.getInt(i);
			ProductMatrix matrix=loader.getSimpleProductMatrix(pdtId);
			pdtMatrixes.add(matrix);
		}
		
		//��model�Ķ���Ҫ����װ, ��������Ա�׼�У�������asi������, ����ɫ�뼶���ֶ� 
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
					//������Ҫ����������pdt�ֶμ��ģ���sql�������ݰ�model��˳��д�뵱ǰ��
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
		 * size�н���һ�����ݣ����ǳ����µ���
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
		 * ispresize�������cols��ֵ��key: ${pdtid}_${asi}, value: ��ֵ������view�����cols���ݣ�[] ��ֵ������:
		 * {11_10033:[10, 15, 10.99],11_10034:[9, 12, 10.99]}
		 */
		JSONObject preSize=new JSONObject();
		ArrayList<ModelColumn> mcs=findPreSizeColumns(model,view);
		int arrSize=mcs.size();
		for(int i=0;i<mcs.size();i++){
			mc=mcs.get(i);
			sfc=sqlColumns.get(mc.sql);
			JSONObject sfcObj=sfc.getASIValuesByKey(mc.col,mc.format);
			//���Ԫ�ص�ָ��������λ��
			addToObject(preSize, sfcObj, i, arrSize);
		}
		//�ϲ���Ϊ�ͻ�����Ҫ�ĸ�ʽ
		logger.debug("sizeQtys="+ sizeQtys);
		logger.debug("preSize="+ preSize);
		for(ViewRow vr:pdtColorRows)logger.debug("pdtid="+ vr.getProductId()+", cc="+ vr.getColorCode()+", img="+vr.getProductImage()+":"+ vr.data.toString());
		
		JSONObject ret=new JSONObject();
		ret.put("rowData", mergeProductRows(pdtColorRows,sizeQtys,preSize));
		ret.put("sum", computeSumLineData(gridConf.optString("sumline_sql"),gridConf.optJSONArray("sumline")));
		return ret;
	}
	
	/**
	 * �ҵ�����presize�������ֶ�
	 * @param model
	 * @param view
	 * @return
	 */
	private ArrayList<ModelColumn> findPreSizeColumns(ArrayList<ModelColumn> model, ViewDefine view) throws Exception{
		for(ViewColumn col:view.columns){
			if(col.isPreSize) return col.cols;
		}
		return new ArrayList();//new NDSException("��ǰgrid��view��δ����ispresize��");
	}
	/**
	 * �ҵ�����sizeqty�������ֶ�
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
     * ���壺
     * rowData:grid����
     * 3_field:ǰ׺����Ҫ����ʾֵ
     * pic:ͼƬ
     * asi��ǰ̨֪���Ƿ��cell���Ա༭         
	 * �ϲ���ͨ��Ʒ�к�ɫ����ϢΪ�ͻ�����Ҫ�ĸ�ʽ
	 * @param pdtColorRows  [
	 * 	pdtid=122, cc=998, img=/pdt/m/S1007B8255998.jpg:[["S1007B8255998",199],"����T��",21,4179,4179]
	 * 	pdtid=121, cc=200, img=/pdt/m/S1007B8255200.jpg:[["S1007B8255200",199],"����T��",24,4776,4776]
	 * ]
	 * @param sizeQtys sample: {"122_856":6,"122_94":5,"121_355":7,"122_368":2,"121_1542":8,"121_236":9,"122_1551":1,"122_1073":4,"122_1802":3}
	 * @param preSize sample: {"122_94":[57,58,59],"122_856":[31,32,33],"121_686":[29,30,31],"121_355":[4,5,6],"121_1404":[36,37,38],"121_1542":[39,40,41],"122_368":[97,98,99],"121_236":[100,101,102],"122_1551":[4,5,6],"122_1802":[35,36,37],"122_1073":[3,4,5],"121_516":[4,5,6]}
	 * @return [{pic, asi, "0":[1,2], "1":15.3}] "0" ��ʾ��1�е����ݣ� asi: 
	 * @throws Exception
	 */
	@Admin(mail="li.shuhao@lifecycle.cn")
	private JSONArray mergeProductRows(ArrayList<ViewRow> pdtColorRows, JSONObject sizeQtys,JSONObject preSize) throws Exception{
		JSONArray ja = new JSONArray();
		//��ǰ�û���ʹ�õ���Ʒ�����г��������������
		int maxSizeCount=WebController.getInstance().getMaxSizeCount(usr.getMarketId(), jedis, conn);
		//�����е�λ��
		int sizeIndex=-1;
		int preSizeIndex = -1;
		if(pdtColorRows.size()>0){
			//�ڼ���sizeColumnIndex��ʱ�����ǰ����preSizeColumn�� ԭ�е�sizeColumnIndex��+1��������Ҫȥ��preSizeColumnIndex��Ӱ��
			preSizeIndex = pdtColorRows.get(0).view.preSizeColumnIndex;
			sizeIndex=  pdtColorRows.get(0).view.sizeColumnIndex;
			if(preSizeIndex<sizeIndex && preSizeIndex>-1) sizeIndex--;
			logger.debug("sizeIndex="+ sizeIndex+", maxsizecount="+ maxSizeCount+",preSizeIndex="+preSizeIndex);
		}

		for (int i = 0; i < pdtColorRows.size(); i++) {
			JSONObject jo = new JSONObject();
			JSONObject pdtAndAsiIds = new JSONObject();//���pdt_asi[]
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
	 * ��preSize�����У�ÿ��value����arraySizeָ�����ȵ����飬sfcObj��value����Ϊposλ���ϵ�ֵ
	 * @param preSize ��ʽ {key: [v1,...v(arraySize)]}�� ��: {"11_1234": [1,2,11]}, ����key�ĸ�ʽ��$pdtid_$asi
	 * @param sfcObj  ��ʽ {key: value}�����ܱ�֤ÿ��key��preSize�ж���
	 * @param pos ��sfcObj��value����sfcObj��ָ��posλ��
	 * @param arraySize preSize�������Ԫ�أ��䳤�ȱ�����arraySize
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
	 * ����asi������ֶ�
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
	 * ����model����
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
	 *  online_edit_grid#model��Ԫ�ض���
	 *
	 */
	class ModelColumn{
		String col;
		String sql;
		/**
		 * ��ǰ���Ƿ���pdt��column������sql�е�column
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
		Format format;//����Ϊ��
		/**
		 * �����ڹ���ViewDefine��ʱ���д������λ����Ϣ����ʾ��ViewRow�ĵڼ��У�ע�ⲻ����size/presize�е���Ϣ
		 */
		int viewColumnIndex;
		/**
		 * �����ڹ���ViewDefine��ʱ���д������λ����Ϣ����ʾ��ViewRowָ���еĵڼ���ColumnItem,
		 * ע�ⲻ����size/presize�е���Ϣ
		 */
		int viewColumnItemIndex=0;
		/**
		 * ������ad_sql#model���ȡ
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
		 * �����ֶζ��壬����ж�Ӧ���ֶζ��壬ֱ�Ӷ�ȡ�ֶεĸ�ʽ
		 * ���û�ж�Ӧ�ֶ�(sql)����Ĭ�ϰ�number��ʶ��fmt���塣fmt���Բ��ṩ����Ϊ�ա�
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
						//��fmt������col ���壬��ζ�Ŷ��Ƹ�ʽ������fmt
						if(c.getType().equals(Column.Type.NUMBER)){
							format= new DecimalFormat(fmt);
						}else if(c.getType().equals(Column.Type.DATENUMBER) || c.getType().equals(Column.Type.DATE)){
							format=new SimpleDateFormat(fmt);
						}else{
							logger.debug("not supported type "+ col+" type="+ c.getType().getName());
						}
					}
				}else{
					//c Ϊ�գ�Ĭ�ϵ�fmt ��Ӧdecimal
					if(Validator.isNotNull(fmt)){
						format= new DecimalFormat(fmt);
					}
				}
			}else{
				// ��col����ʶ��fmt Ϊnumber����
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
		 * ���õ�ǰ��������ViewRow�е�λ����Ϣ
		 * @param idx ViewRow�е��к�, ע����Ҫȥ��size�к�presize�еĶ�����У������ǰ���������Ǻ���Ļ�
		 * @param subIdx �������ж��Ԫ�أ�����Ԫ�ص�λ����Ϣ
		 */
		public void setColumnIndexInView(int idx, int subIdx) throws Exception{
			if(viewColumnIndex!=0 ||viewColumnItemIndex!=0 ) throw new NDSException("model of col in view"+"@b2bedit-repeat@");
			viewColumnIndex=idx;
			viewColumnItemIndex=subIdx;
		}
	}
	
	/**
	 * ����model����֪����Ҫ��pdt���ȡ��Щ�ֶΣ�����sql������ֶ�һ�ɲ���Ҫ, �����ٲ���mainpic�ֶ�
	 * �����Ҫȥ����һ���ֶα�����ID�ֶ�
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
		//ͼƬ�ֶα�����
		column=manager.getColumn("pdt", "mainpic");
		cls.add(column);
		return cls;
	}
	
	/**
	 * mc.sumsql ��mc.sql��ͬ���ǣ��ϼ��е�sql�������pdtid list����˲�ѯ����������һ���̶���䡣
	 * ע��sumsql����Ҫ�õ���vc��������ҪԤϰ���ú�
	 * @param model ad_sql#online_edit_grid.model
	 * @return key: sumsql.name, value: �Ѿ�ͳ�ƺõ�jsonobj (����ͳ���еĶ���, key��sumsql��column���ƣ�
	 * @throws Exception
	 */
	private HashMap<String, JSONObject> setupSumSQLColumns(ArrayList<ModelColumn> model) throws Exception{
		/**
		 * ������ʽ�����Ƚ����ֶζ��壬��ȡ����sql���壬����sql��䣬��ȡ��ѯ����������hashmap�� key: 
		 */
		HashMap<String, JSONObject> sqlColumns=new HashMap();//key: ad_sql.name, value sql ��ѯ���
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
	 * ���ڲ��ٵ�column�����ʱ�򣬻Ḵ��sql���Ƚ������׼����������sql��Ӧ�Ľ����������Ա����ÿ��pdt�����л�ȡ��Ӧֵ
	 * @param model ad_sql#online_edit_grid.model
	 * @param pdtIds elements�� int���ǵ�ǰѡ�����Ʒ��id
	 * @param includeASILevel �Ƿ�asi�����ͳ���ֶ�Ҳ���¼��㣬����GridSave��ʱ�򣬲������¼���asi���У������
	 * @return key: sql.name, value: �Ѿ���pdtid���úõĲ�ѯ�������ͨ��getPdtValue��ȡָ����Ʒ�Ķ�Ӧ��ֵ
	 * @throws Exception
	 */
	protected HashMap<String, SQLForColumn> setupSQLColumns(ArrayList<ModelColumn> model,JSONArray pdtIds, boolean includeASILevel) throws Exception{
		/**
		 * ������ʽ�����Ƚ����ֶζ��壬��ȡ����sql���壬����sql��䣬��ȡ��ѯ����������hashmap�� key: 
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
	 * ת����select id from m_product p where id in() or id in()��ʽ����䣬����in ��1000�������ƣ�����Ҫ���Ϊ���in
	 * �������յ�sql�е���ʽ��
wth p as ( $PDTIDSQL$)
select p.id pdtid, s.asi, s.qty
from p, b_cart s where s.m_product_id=p.id and s.user_id=?
	 * @param pdtIds
	 * @return select id from m_product p where id in() or id in()��ʽ
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
	 * view��������ݸ�ʽ
	 *
	 */
	class ViewDefine{
		public ArrayList<ViewColumn> columns;
		/**
		 * �ͻ�����Ҫ������view�������������
		 */
		public Object options;
		
		/**
		 * ��������view�����е�λ�ã������л�
		 */
		private int sizeColumnIndex=-1;
		
		private int preSizeColumnIndex=-1;
		/**
		 * 
		 * @param view  [{title, cols, css, issize, ispresize, width, options}]
		 * @param opts  �ͻ�����Ҫ������view�������������
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
		 * ��������ʾ��grid�е��е���ʼ����
		 * @return
		 */
		public int getSizeColumnIndexInGridView(){
			if(preSizeColumnIndex>-1) sizeColumnIndex--;
			return sizeColumnIndex;
		}
		/**
		 * ��ǰ�����Ƿ���presize��
		 * @return
		 */
		public boolean hasPreSizeColumn(){
			return preSizeColumnIndex>-1;
		}
	}
	/**
	 * view�е��ֶζ���
	 */
	class ViewColumn{
		public String title;
		public String css;
		public ArrayList<ModelColumn> cols;
		public boolean isSize;//��������������1��
		public boolean isPreSize;//����ǰ׺��asi�����������Ϣ�������棬�������ȵȣ��ж�ֵ
		public int width;
		public JSONObject options;
		public int indexInViewDefine;//��viewdefine�е��кţ���0��ʼ
		public boolean isColor;//�Ƿ������ɫ��
		
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
				//�����б�����asi����
				if(isSize&&!mc.asiLevel&&!isPreSize) throw new NDSException("����:"+mc.col+"����asi����");
				//ǰ׺�б�����asi����
				if(isPreSize&&!mc.asiLevel&&!isSize) throw new NDSException("��asi�������"+mc.col+"���ܳ�����ǰ׺�ο���");
				//��ǰ׺�зǳ����б����Ƿ�asi����
				if(!isPreSize&&mc.asiLevel&&!isSize) throw new NDSException("asi�������:"+mc.col+"�����ڷǳ�������������");
				mc.setColumnIndexInView(posInView, cols.size());//��дidx��λ����Ϣ
				cols.add(mc);
				
			}
		}
		/**
		 * ����colName�ҵ�ModelColumn
		 * @param colName ���ĳ��sql����Ӧ���ֶκ�pdt�ֶ�����һ�£��ֶ�����Ҫ����sql������Ϊǰ׺����#�ţ���: "sql_sugg#name", ��ʾ��sql_sugg��name�ֶε�ֵ
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
	 * ÿһ���ɼ���, ����Ʒ��ɫ����, ����asi������ֶζ���
	 *
	 */
	public class ViewRow{
		
		public ViewDefine view;
		public Color color;
		public ProductMatrix matrix;
		/**
		 * ׼���õ�������
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
		 * ����ͼƬ
		 * @param img
		 */
		public void setProductImage(String img){
			image=img;
		}
		/**
		 * ��ǰ��ɫ��ͼƬ
		 * @return
		 */
		public String getProductImage(){
			return image;
		}
		
		/**
		 * ��Ʒid
		 * @return
		 */
		public int getProductId(){
			return matrix.getProductIds().get(0);
		}
		/**
		 * ɫ��
		 * @return
		 */
		public String getColorCode(){
			return color.getCode();
		}
		/**
		 * ���õ�Ԫ���ֵ������ɸ�ʽ��������mc.fmt����
		 * @param mc
		 * @param value
		 */
		public void setCell(ModelColumn mc, Object value) throws Exception{
			Object oldValue=data.get( mc.viewColumnIndex);
			if(oldValue instanceof JSONArray){
				((JSONArray)oldValue).put(mc.viewColumnItemIndex, value);
			}else{
				if(mc.viewColumnItemIndex>0) throw new NDSException("mc has item��but old data not array");
				data.put(mc.viewColumnIndex, value);
			}
		}
		
		
	}
	
	/**
	 * ���ÿ��ad_sql�Ĳ�ѯ����Ͷ�Ӧ��������λ��
	 */
	class SQLForColumn{
		String sqlName;
		ArrayList<Integer> columnIndex=new ArrayList();
		boolean asiLevel;
		
		// asi����: key: pdtid, value: asiLevel {key: asi, value: jsonobj of that asi
		// ��asi: key pdtid+"_"+cc(colorcode), row of that ��ɫ
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
		 * ��asiģʽ�µ�ֵ��ȡ
		 * @param pdtId
		 * @param colorCode ��ɫ
		 * @param key 
		 * @param format ����Ϊ��
		 * @return null ���δ�ҵ�
		 */
		public Object getPdtValue(int pdtId,String colorCode, String key, Format format){
			JSONObject one=pdtRows.get(pdtId+"_"+colorCode);
			if(one==null) return null;
			Object value= one.opt(key);
			if(format!=null && value!=null) value=format.format(value);
			return value;
		}
		/**
		 * ��ȡ����pdt��asi�����ָ��key��ֵ
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
		 * asiģʽ�µ�ֵ��ȡ
		 * @param pdtId
		 * @param asi
		 * @param key
		 * @param format maybe null
		 * @return null ���δ�ҵ�
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
		 * ֪��ÿ��sql�����Ӧ��column��λ��
		 * @param colIdx ��model�ﶨ���λ��
		 */
		public void addColumn(int colIdx){
			columnIndex.add(colIdx);
		}
		/**
		 * ��ӽ����
		 * @param rows
		 * @throws Exception
		 */
		public void addRows(JSONArray rows) throws Exception{
			for(int i=0;i<rows.length();i++){
				addRow(rows.getJSONObject(i));
			}
		}
		/**
		 * ��Բ�ѯ�����м�¼���д���, �������asi�ģ��ж���������2�����ԣ�pdtid, cc(colorcode), �����asi����Ĳ�ѯ������cc�ֶΣ�����Ҫ��asi�ֶ�
		 * ע���asi�Ĳ�ѯ���ǵ���ɫ����
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
	 * ��ʼ�����󹹽�������Ҫ�ǻ��getSheet��֧��
	 * @param jo �ͻ��˴���������
	 * @param event �¼�
	 * @param vc ������
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
	 * ���ݿͻ��˴����Ĳ�������ֱ̨��ִ�д洢���̼���
	 * 
	 * @param jo{rowData:[{"0":"ȹ��",asi:{"2":"123_34_45677"},pdtId,box}]}
	 * @return jo{rowData:[{"0":"ȹ��",asi:{"2":"123_34_45677"},pdtId,box}],sum:{}}
	 * @param rowData ���������ݣ������������ɢ��ģʽ�Ĳ�ͬ���ֱ����Ӧ��asi�����������������������߲�������������Ʒ�����ڸ�asi��
	 * @param  sum grid����ϼ�����
	 * @throws Exception 
	 */
	@Admin(mail="li.shuhao@lifecycle.cn")
	public Object gridDelete(JSONObject jo) throws Exception{
		
		ArrayList<ModelColumn>  model=createModel(gridConf.getJSONArray("model"));
		//[{title, cols, css, issize, ispresize, width, options}]
		ViewDefine view=new ViewDefine(gridConf.getJSONArray("view"), model, gridConf.opt("options"));
		//��model���ҵ�����keyΪsumsql��ad_sql�����ݵļ��㣬��Ȼ��Ҫ�õ����������������жϣ��������sumsql�ļ������������Ƿ��ظ�ǰ̨��field���ڵ�key
		JSONArray rowData = jo.optJSONArray("rowData");
		if(rowData.length()==0) throw new NDSException("@b2bedit-delete@");
		
		int actId = jo.optInt("actId",-1);
		int sizeIndex = view.getSizeColumnIndexInGridView();//�ҵ�������������ʼ����
		int maxSizeCount = WebController.getInstance().getMaxSizeCount(usr.getMarketId(), jedis, conn);//�õ����������ĳ�������
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
				//�����в�Ϊ�յ�ʱ�����ǾͿ����õ���keyΪsql�е�����
				if(Validator.isNotNull(sql) && mc.asiLevel==false/*Ŀǰ��֧��asi������ֶε����¼���*/){
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
	 * �˻������redis����
	 * 
	 */
	private void clearBroRedisCashe(JSONObject jo){
		//�˻������redis����
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
	 * ����ǰ̨�ڽ������޸ģ������ں�̨����ʱ���棬������ǰ̨�޸Ĺ��ļ���ֵ	
	 *   
	 * @throws Exception
	 * @param pdtRow {"cmd":"b2b.grid.save","key":"457_1619","cnt":"start":"actid":,"cc":,"cashekey":,"qty":}
	 * key:$pdtId_$asiId
	 * cashKey:ǰ̨������̨���õ�������Ʒ������grid����ĺϼ����Ļ���key
	 * qty:�ͻ��޸ĺ��ֵ
	 * cnt:��ǰҳ�ж�����
	 * start:�Ӷ�������ʼ������ʵҲû��
	 * cc:��ɫ�ֶ�
	 * @return JSONObject
	 * {comm:[  
	 * 				{key: ,value:}..
	 *  ],sum[123,345,1111]}
	 *  comm:��ͨ������
	 *  sum:grid����ļ�������
	 *  key:ǰ̨����Ӧ��field
	 *  value:��Ӧ��Ԫ���ֵ
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
	      "value": "��228043"
	    },
	    {
	      "key": "11",
	      "value": "��228043"
	    }
	  ],
	  "sum": {
	    "sumamt":"��16,378.00",
	    "sumqty":"325,000",
	    "sumamtlist":"��20,378.00"
	  }
		key ��Ϊǰ̨��Ӧ�ֶ�������valueΪinputֵ
		smm������grid������ܼ���, key: �ؼ����ƣ� value: ֵ��ע��ֵ�Ѿ�format����
	 */
	@Admin(mail = "li.shuhao@lifecycle.cn")
	public JSONObject gridSave(JSONObject pdtRow) throws Exception{
		
		//��ɶ����ĸ���
		mergeData(pdtRow);
		
		String pdtIdAndAsiId = pdtRow.getString("key");
		int pdtId = Integer.parseInt(pdtIdAndAsiId.split("_")[0]);
		int asiId = Integer.parseInt(pdtIdAndAsiId.split("_")[1]);
		String colorCode = pdtRow.getString("cc");
		//model*:[{col, sql, desc, func, key, sum,sumsql, fmt, css }]
		ArrayList<ModelColumn>  model=createModel(gridConf.getJSONArray("model"));
		//[{title, cols, css, issize, ispresize, width, options}]
		ViewDefine view=new ViewDefine(gridConf.getJSONArray("view"), model, gridConf.opt("options"));
		//��model���ҵ�����keyΪsumsql��ad_sql�����ݵļ��㣬��Ȼ��Ҫ�õ����������������жϣ��������sumsql�ļ������������Ƿ��ظ�ǰ̨��field���ڵ�key
		
		int sizeIndex = view.getSizeColumnIndexInGridView();//�ҵ�������������ʼ����
		int maxSizeCount = WebController.getInstance().getMaxSizeCount(usr.getMarketId(), jedis, conn);//�õ����������ĳ�������
		HashMap<String, SQLForColumn> sqlColumns = setupSQLColumns(model, new JSONArray().put(pdtId), false);
		
		JSONArray comData = new JSONArray(); //key: view column index, value: formatted data
		for(int i = 0;i < model.size();i++){
			ModelColumn mc = model.get(i);
			String sql = mc.sql;
			JSONObject jo;
			//�����в�Ϊ�յ�ʱ�����ǾͿ����õ���keyΪsql�е�����
			if(Validator.isNotNull(sql) && mc.asiLevel==false/*Ŀǰ��֧��asi������ֶε����¼���*/){
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
		//����grid����ϼ�������ֵ
	
		
		JSONObject result = new JSONObject();
		result.put("comm", comData);
		result.put("sum",  computeSumLineData(gridConf.optString("sumline_sql"),gridConf.optJSONArray("sumline")));
		
		//�˻������redis����
		clearBroRedisCashe(pdtRow);
		
		return result;
	}
	/**
	 * ����ϼ��е�ֵ�� ����online_edit_grid_sumline�Ķ��壬 online_edit_grid_sumline�ĸ�ʽ:
	 * "�ϼ�����:<b> [[sumqty]] </b>, �ϼƽ��:<b> [[sumamt]]</b>, �ϼ����۶�: <b>[[sumamtlist]]</b>"
	 * [[key]]�Ķ������model column.col ���� 
	 * @param model
	 * @return key ��Ҫ����: online_edit_grid_sumline �е�key�� value���Ѿ���ʽ����ֵ
	 * {
	    "sumamt":"��16,378.00",
	    "sumqty":"325,000",
	    "sumamtlist":"��20,378.00"
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
	 * velocity��Ҫ�����ֵΪ$userid,�û�id $pdtid,��Ʒid $asiid,ASIid $bprmtid,�id $qty,���� 
	 * @return
	 * @throws Exception 
	 */
	public void mergeData(JSONObject pdtRow) throws Exception{
		String pdtIdAndAsiId = pdtRow.getString("key");
		Pattern pattern = Pattern.compile("(\\d+)_(\\d+)");
		Matcher matcher = pattern.matcher(pdtIdAndAsiId);
		if(!matcher.find()) 
			throw new NDSException("@b2bedit-coun@");
		int actId = pdtRow.optInt("actId",-1);//�id
		int pdtId = Integer.parseInt(pdtIdAndAsiId.split("_")[0]);//��Ʒid
		int asiId = Integer.parseInt(pdtIdAndAsiId.split("_")[1]);//ASIid
		int qty = Integer.parseInt(pdtRow.optString("qty","0").trim());//ȥ���ո�
		
		/**
		 * both�����������������ĳ���������qty�����жϣ��������̳�������Ϣ
		 * ����qtyΪż��
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
		 * ����ľ����Ŀ��ʼ���µ����������絥���ǰ�֧����ģ��������а������14�����µ�1��Ϊ14��2����28�����ݿ�洢��Ȼ��֧��
		 */
		//���ڹ���qty�ı����߼�ȫ������DB
		/*		
		 * JSONObject po=PhoneUtils.fetchObjectAllColumns(TableManager.getInstance().getTable("pdt"), pdtId, conn, jedis);
		 * int packQty = po.optInt("packqty",1);
		 * if(packQty<=0)packQty=1;;
		 * 
		 * qty= qty*packQty*sizeFactor;
		 */

		//��vc���Ѿ������д��˵Ĳ���������id, ��bro.id֮�������
		//�õ�oracle merge��sql��䣬ִ��
		//String sql= PhoneController.getInstance().getValueFromADSQL("grid_merge_b_cart", conn);
		vc.put("uid", usr.getId());
		vc.put("pdtid", pdtId);
		vc.put("asiid", asiId);
		vc.put("actid", actId);
		vc.put("qty", qty);
		vc.put("marketid", usr.getMarketId());
		
		//ִ�д洢�����������ݿ����qty�ĸ���
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
	 * ִ�д洢�����������ݿ����qty�ĸ���
	 * 
	 * @param pdtRow ������
	 * @param pdtId ��ƷId
	 * @param actId �Id
	 * @throws Exception
	 */
	private void executeProcedureOrMerge() throws Exception{
		//��save_proc��ȡ������������ã���ʾȫ���ɴ洢������ɼ���
		String sqlName = this.gridConf.optString("save_proc");
		// ����DB�Զ����쳣������Ϣ��ֱ��ֱ���׸�ǰ��
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
	 * �õ����п��õ�asiId 
	 * 
	 * @param pdtId ��Ʒid
	 * @param actId �id
	 * @return
	 * @throws Exception
	 */
	private HashSet<Integer> getAvaiableASIs(int pdtId, int actId) throws Exception{
		
		HashMap<Integer, HashSet> pdtAsis=new HashMap();
		//key: asi �����µ���asi
		HashSet<Integer> alaiableAsis= pdtAsis.get(pdtId);
		if(alaiableAsis!=null) return alaiableAsis;
		
		alaiableAsis=new HashSet();
		//��ȡ���µ���asi, ��Ҫ���ڵ�ǰ�, row: [asi], ���׼����ͬ���ǣ���ͬ�Ļ����ͬ���г�����Ʒ��Ȼ�в�ͬ
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
