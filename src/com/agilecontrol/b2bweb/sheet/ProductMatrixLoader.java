package com.agilecontrol.b2bweb.sheet;

import java.io.StringWriter;
import java.sql.Connection;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;

import com.agilecontrol.nea.core.schema.TableManager;
import com.agilecontrol.nea.core.query.QueryEngine;
import com.agilecontrol.nea.core.util.ConfigValues;
import com.agilecontrol.nea.core.velocity.VelocityUtils;
import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.nea.util.Validator;
import com.agilecontrol.phone.PhoneConfig;
import com.agilecontrol.phone.PhoneUtils;

/**
 * ��Ʒ���������
 * 
 * �����Ʒ������Ҫ���������������ʶ���Ϊ����ʵ��
 * 
 * @author yfzhu
 *
 */
public class ProductMatrixLoader {
	private static Logger logger = LoggerFactory.getLogger(ProductMatrixLoader.class);

	private Jedis jedis;
	private Connection conn;
	
	public ProductMatrixLoader(Jedis jedis, Connection conn) {
		this.jedis=jedis;
		this.conn=conn;
	}
	/**
	 * for any buyer, not only itself.
	 * ע���������������ĳ�������ǲ������ڵ�ǰ���ֵ��ڴ����, �����Ʒ��ĳ������û�У������ĳ��뽫�������ı����������ֳ������Աȣ�
	 * 
	 * @param pdtId
	 * @param usrId users.id 
	 * @return
	 * @throws Exception
	 */
	public float[] getProductSizeRatios(int pdtId, long usrId) throws Exception{
		JSONObject cmap=(JSONObject)TableManager.getInstance().getTable("b_size_ratio").getJSONProp("cmap");
		if(cmap==null) throw new NDSException("Not specified cmap in b_size_alloc props");
		String[] columns=new String[4];
		
		columns[0]=cmap.optString("column1");
		if(Validator.isNull(columns[0])) columns[0]="null";
		columns[1]=cmap.optString("column2");
		if(Validator.isNull(columns[1])) columns[1]="null";
		columns[2]=cmap.optString("column3");
		if(Validator.isNull(columns[2])) columns[2]="null";
		columns[3]=cmap.optString("column4");
		if(Validator.isNull(columns[3])) columns[3]="null";
		
		String sql="select "+ columns[0]+","+ columns[1]+","+columns[2]+","+columns[3] +" from v_pdtdims where pdtid=?";
		JSONArray pdtColumns= QueryEngine.getInstance().doQueryJSONArray(sql, new Object[]{pdtId},conn);
		pdtColumns= pdtColumns.getJSONArray(0);

//		JSONArray pdtColumns=new JSONArray();
//		JSONObject pdtObj=FairController.getInstance().getFair(fairId).getProduct(pdtId);
//		
//		for(int i=0;i<4;i++) {
//			String v= pdtObj.getString("column"+(i+1));
//			if(Validator.isNull(v)) v="*";
//			pdtColumns.put( v ); 
//		}
		
		ProductMatrix pdtm=this.getProductMatrix(pdtId);
		int sizeCount=pdtm.getSizes().size();
		//loop over b_size_ratio
		StringBuilder sb=new StringBuilder("select id, id name,column1,column2,column3,column4");
		for(int i=0;i<sizeCount;i++){
			sb.append(",ratio").append(i+1);
		}
		sb.append(" from b_size_ratio where isactive='Y' and user_id=? and (column1=? or column1 is null) and (column2=? or column2 is null) and (column3=? or column3 is null) and (column4=? or column4 is null)" +
				" order by column4,column3,column2,column1 asc"); // null �ں��棬�����ݵ�column1,..column4��ǰ��
		sql= sb.toString();
		
		
		float[] ratios= new float[sizeCount]; 
		JSONArray row=null;
		boolean ratioFound=false;
		
		long fbuyerId= usrId;
		while(!ratioFound){
			JSONArray crs= QueryEngine.getInstance().doQueryJSONArray(sql,new Object[]{fbuyerId,
					pdtColumns.getString(0),pdtColumns.getString(1),pdtColumns.getString(2),pdtColumns.getString(3) }, conn);
			if(crs.length()>0){
				row=crs.getJSONArray(0);
				for(int k=0;k<sizeCount;k++){
					double ratio= row.optDouble(6+k, 0);
					ratios[k]=(float)ratio;
				}
				ratioFound=true;
				break;
			}
			
			if(!ratioFound && PhoneConfig.USING_PARENT_TEMPLATES){
				JSONObject uobj=PhoneUtils.getRedisObj("usr", usrId,  conn, jedis);
				fbuyerId= uobj.optInt("manager_id", -1); //  QueryEngine.getInstance().doQueryInt("select manager_id from users where id=?", new Object[]{fbuyerId}, conn);
				if(fbuyerId<0) break;
			}
		}
		//check pdtSizeRatios should sum to 100
		if(!ratioFound){
			//if(raiseObjectNotFoundException)throw new ObjectNotFoundException("��ǰ��Ʒ("+pdtId+")����ģ��δ���ã����������Ա");
			//2013.10.11 just use first size
			ratios[0]=100;
		}//throw new NDSException("��ǰ��Ʒ("+pdtId+")����ģ��δ���ã����������Ա");
		float qsm=0;
		for(int i=0;i< ratios.length;i++) qsm+=ratios[i];
		if(qsm==0) ratios[0]=100;// throw new NDSException("@contact-your-administrator@");
		
		if(Math.round(qsm)!=100){
			for(int i=0;i< ratios.length;i++) ratios[i]= ratios[i]*100.0f/ qsm;
		}
		return  ratios;
	}		
	/**
	 * ��ratio�����ٴδ������ȱ�֤ ���ص�ratio֮��Ϊ100����������Ʒ������ɫ��������ָ�����룬����Ŀ��ratioֵ��Ϊ0
	 * @param ratios ������ pdtMatrix.size ����һ��, ��Ϊ100�� 
	 * ������Ҫ����ȫ��(ֻ��1����������)ratio��һ��Ԫ�ؾ���100����Ӧ���ҵ�availableSizes�е�һ������0��λ�ã��ƶ�ratio[0]�����λ���ϡ�
	 * @param asis û�е�asiλ��Ϊnull, �о���Integer
	 * @return
	 */
	public float[] clearNoneExistsSizeRatios(float[] ratios, JSONArray asis){
		try{
			float[] nes=new float[asis.length()];
			if(ratios[0]==100f){
				//ȫ��
				for(int i=0;i<nes.length;i++){
					if(! asis.isNull(i)){ nes[i]=ratios[0]; break;}
				}
			}else{
				
				float sum=0;
				for(int i=0;i<nes.length;i++){
					if( asis.isNull(i) ) nes[i]=0;
					else {
						if(ratios.length-1>=i)nes[i]=ratios[i];
						else nes[i]=0;
					}
					sum+= nes[i];
				}
				for(int i=0;i< nes.length;i++) nes[i]= nes[i]*100f/sum;
			}
			return nes;
		}catch(RuntimeException tx){
			logger.error("fail to handle ratio:"+ Arrays.toString(ratios)+", asis:"+ asis);
			throw tx;
		}
	}
	/**
	 * Get color array
	 * ���ν�Ҫ����ɫ��������Ҫ���Ӽ۸���Ϣ
	 * @param pdtId
	 * @param colorAttributeId
	 * @param conn
	 * @return format:
	 *  [{id: color_id(m_attributevalue.id), 
	 *    value: (m_attributevalue.value), desc :color description(default:m_attributevalue.name)}]
	 * @throws Exception
	 */
	private JSONArray getColors(int pdtId, int colorAttributeId, Connection conn) throws Exception{
		//color, you can set such as v.value || '-' || v.name
		String colorDescColumn= ConfigValues.get("fair.matrix.color.column", "v.name");
		//���������?����һ������pdtId���������Թ�����������ɫ����ļ۸�
		if(!colorDescColumn.contains("?")){
			JSONArray colors= QueryEngine.getInstance().doQueryObjectArray("select v.id, v.value, "+ colorDescColumn +" description from m_attributevalue v where v.isactive='Y' and v.m_attribute_id=? and exists("+
					"select 1 from m_product_alias a, m_attributesetinstance asi where a.isactive='Y' and a.m_product_id=? AND asi.id=a.m_attributesetinstance_id and asi.value1_code= v.value  )order by to_number(martixcol),v.value",
					new Object[]{colorAttributeId,pdtId },conn);
			return colors;
		}else{
			// colorDescColumn ��ʽ: get_pdtclr_desc(?,v.id), ?:=m_product_id
			JSONArray colors= QueryEngine.getInstance().doQueryObjectArray("select v.id, v.value, "+ colorDescColumn +" description from m_attributevalue v where v.isactive='Y' and v.m_attribute_id=? and exists("+
					"select 1 from m_product_alias a, m_attributesetinstance asi where a.isactive='Y' and a.m_product_id=? AND asi.id=a.m_attributesetinstance_id and asi.value1_code= v.value  )order by to_number(martixcol),v.value",
					new Object[]{pdtId, colorAttributeId,pdtId },conn);
			return colors;
		}
	}
	/**
	 * Not care about model or full color define, just pdt's def

{
	sizes: ['23', '24', '25','26', '27'], 
	sizenotes: ['23', '24', '25','26', '27'], --Ӣ������
	sizefactors:[1,1,1,1,1] -- ����ϵ��
	colors: [ {n:'green' , s:[0,1,1,1,0], c:'01', g:"pdt001_s_01_001.jpg"}],
	pdtids: [int]
	asis:[][]
}

	 * @param pdtId
	 * @return
	 * @throws Exception
	 */
	protected ProductMatrix loadSimpleProductMatrix(int pdtId) throws Exception{
		VelocityContext vc=VelocityUtils.createContext();

		JSONObject pdtObj=QueryEngine.getInstance().doQueryObject(
				"select m_attributeset_id, name,value,flowno,m_sizegroup_id,stylename,pricelist,shortname,fabcode from m_product where id=?", new Object[]{pdtId}, conn);
		int asId= pdtObj.getInt("m_attributeset_id");
		String name= pdtObj.getString("name");
		
		//261
		int colorAttributeId=QueryEngine.getInstance().doQueryInt(
				"select a.id from m_attribute a, m_attributeuse u where a.isactive='Y' and a.ATTRIBUTEVALUETYPE='L' and a.id=u.m_attribute_id and u.m_attributeset_id=? and clrsize=1", new Object[]{asId},conn);
		//262
		int sizeAttributeId=QueryEngine.getInstance().doQueryInt(
				"select a.id from m_attribute a, m_attributeuse u where a.isactive='Y' and a.ATTRIBUTEVALUETYPE='L' and a.id=u.m_attribute_id and u.m_attributeset_id=? and clrsize=2", new Object[]{asId},conn);
		if(asId==-1 || colorAttributeId==-1 ||sizeAttributeId==-1 ) throw new NDSException("Error attibute definition for product id="+ pdtId);
		//asi
		JSONArray asi= QueryEngine.getInstance().doQueryJSONArray("select asi.value1_id, asi.value2_id, asi.id from m_product_alias a, m_attributesetinstance asi where a.isactive='Y' and a.m_product_id=? AND asi.id=a.m_attributesetinstance_id",
				new Object[]{pdtId },conn);
		//key: color id +"."+ size id, value asi.id
		HashMap<String, Integer> asiHash=new HashMap<String,Integer>();
		for(int i=0;i<asi.length();i++){
			JSONArray row= asi.getJSONArray(i);
			asiHash.put(row.get(0)+"."+ row.get(1), row.getInt(2));
		}

		JSONObject jo=new JSONObject();
		//size
		/**
		 * yfzhu 20120717 ��triumph����Ʒ�ƣ�������12����ʵ��̫�࣬�����Զ���û�еĳ����ɾ�����������������ˣ��Ͳ���֧�ֻ��ڳ���ģ�������
		 * 
		 */
		boolean isThrinkSizeGroup= ConfigValues.get("fair.sizegroup.thrink", true);
	
		JSONArray sizes;
//		if(!isThrinkSizeGroup) sizes= QueryEngine.getInstance().doQueryObjectArray("select v.id, v.value, v.description name,v.description note,v.factor from m_attributevalue v where v.isactive='Y' and v.m_attribute_id=? order by to_number(martixcol),v.value",
//					new Object[]{sizeAttributeId },conn);
//		else
			sizes= QueryEngine.getInstance().doQueryObjectArray("select v.id, v.value, v.description name,v.description note,v.factor from m_attributevalue v where v.isactive='Y' and v.m_attribute_id=? and exists("+
				"select 1 from m_product_alias a, m_attributesetinstance asi where a.isactive='Y' and a.m_product_id=? AND asi.id=a.m_attributesetinstance_id and asi.value2_code= v.value  ) order by to_number(martixcol),v.value",
					new Object[]{sizeAttributeId,pdtId },conn);
		
		JSONArray size=new JSONArray();
		JSONArray sizeNotes=new JSONArray();
		JSONArray sizeFactors=new JSONArray();
		for(int i=0;i<sizes.length();i++){
			JSONObject row= sizes.getJSONObject(i);
			size.put(row.getString("name"));//name of size
			sizeNotes.put(row.getString("note"));//Ӣ������ of size
			int factor=row.optInt("factor",1);
			if(factor<1)factor=1;
			sizeFactors.put(factor);
			
		}
		jo.put("sizes", size);
		jo.put("sizenotes", sizeNotes);
		jo.put("sizefactors", sizeFactors);
		/*[{id: color_id(m_attributevalue.id), 
		     value: (m_attributevalue.value), desc :color description(default:m_attributevalue.name)}]
		     */
		JSONArray colors= getColors(pdtId, colorAttributeId, conn);
		JSONArray color=new JSONArray();
		JSONArray asiArray=new JSONArray();
		
		JSONArray pdtIds=new JSONArray();//added 2013.8.18 ͳһģʽ������ģ�ؿȫɫ���
		for(int i=0;i<colors.length();i++){
			pdtIds.put(pdtId);
			JSONObject row= colors.getJSONObject(i);
//				logger.debug("row="+ row);
			JSONObject clr=new JSONObject();
			int clrId= row.getInt("id");
			
			vc.put("pdt", pdtObj);
			vc.put("color", row);
			StringWriter output = new StringWriter();
			Velocity.evaluate(vc, output, VelocityUtils.class.getName(), PhoneConfig.SIMPLE_MATRIX_ROW_DESC);
			
			clr.put("n", output.toString());// will show on matrix, just as show, not for feedback to server
			//clr.put("n", row.getString("desc"));// will show on matrix, just as show, not for feedback to server
			String code=row.getString("value");
			clr.put("c", code);
			
			JSONArray s=new JSONArray();
			JSONArray asiRow=new JSONArray();
			for(int j=0;j< sizes.length();j++){
				int sizeId= sizes.getJSONObject(j).getInt("id");
				Integer asiId= asiHash.get(clrId+"."+ sizeId);
				s.put(asiId==null? 0:1);
				asiRow.put(asiId);
			}
			clr.put("s", s);
			color.put(clr);
			asiArray.put(asiRow);
		}
		jo.put("colors", color);
		jo.put("pdtids", pdtIds);//����ģʽ�£�ÿ�е���ɫ����Ҫ���ö�Ӧ��pdtid
		jo.put("asis", asiArray);
		
		return new ProductMatrix(jo);
			
		
	}
	/**

	��ȡ��Ʒ����ɫ, ����, ģ��Ķ���,   
	{
	size: ['23', '24', '25','26', '27'],
	factor:[1,12,144,1,1] ���ǳ���ϵ�� - m.attributevalue.factor�ֶΣ���԰ٻ���Ʒ����ͬ�ĵ�λ��Ϊ�����ʱ������Ϊ�µ�������
	���磺��һ�����λ��֧���ڶ������λΪ����14֧�������ʱ����Ϊ1�������̨����14�� 
	color: [ {n:'green' , s:[0,1,1,1,0], c:'01'}], n for name, s for sizes, c for code, g for graph(thumbnail)
	pdtids:[13,24] --����Ǻϲ�������ʾ��ģʽ�����г�ÿһ�ж�Ӧ��pdtid, ����ǵ�pdtid�ľ��󣨱���lily,ckģʽ�����������������
	asis:[][]
	}
		 * @param pdtId
		 * @return
		 * @throws Exception
		 */
	public ProductMatrix  getProductMatrix(int pdtId) throws Exception{
		String val= jedis.get("pdt:"+pdtId+":sheet");
		ProductMatrix mat=null;
		if(Validator.isNull(val)){
			//will also write to jedis
			mat=loadProductMatrix(pdtId);
		}else{
			mat=new ProductMatrix(new JSONObject(val));
		}
		return mat;
	}	
	/**
	 * ��һ��Ʒ���󣬲����ǿ�ɫģʽ�µĶ����Ʒ�ϲ�Ϊһ����������
	 * @param pdtId
	 * @return {
	sizes: ['23', '24', '25','26', '27'], 
	sizenotes: ['23', '24', '25','26', '27'], --Ӣ������
	sizefactors:[1,1,1,1,1] -- ����ϵ��
	colors: [ {n:'green' , s:[0,1,1,1,0], c:'01', g:"pdt001_s_01_001.jpg"}],
	pdtids: [int]
	asis:[][]
}
	 * @throws Exception
	 */
	public ProductMatrix  getSimpleProductMatrix(int pdtId) throws Exception{
		String val= jedis.get("pdt:"+pdtId+":simplesheet");
		ProductMatrix mat=null;
		if(Validator.isNull(val)){
			//will also write to jedis
			mat=loadSimpleProductMatrix(pdtId);
			jedis.set("pdt:"+ pdtId+":simplesheet", mat.toJSONObject().toString());
		}else{
			mat=new ProductMatrix(new JSONObject(val));
		}
		return mat;
	}	
	/**
	 * Update productMatrixDefs and productMatrixASI
	 * write to redis key: "pdt:$pdtid:sheet", value: string of {def,asi}
	 * @param pdtId
	 * @throws Exception
	 */
	protected ProductMatrix loadProductMatrix(int pdtId) throws Exception{
		//�ܵ��ô˷�����ʾ����jedis��
//		JSONArray pdt=QueryEngine.getInstance().doQueryJSONArray("select m_attributeset_id, name from m_product where id=?", new Object[]{pdtId}, conn).getJSONArray(0);
//		int asId= pdt.getInt(0);
//		String name= pdt.getString(1);
		
		ProductMatrix mat=null;
		if(PhoneConfig.FULL_COLOR_MATRIX_BY_STYLE && this.isBelongToMultipleColorStyle(pdtId)){
				mat=loadFullColorMatrix(pdtId);
		}else
			mat= getSimpleProductMatrix(pdtId);
		//write to redis
		String matStr=mat.toJSONObject().toString();
		jedis.set("pdt:"+ pdtId+":sheet", matStr);
		
		return mat;	
	}
	
	/**
	 * Check if product belong to style which has more then one product of specific color
	 * @param mainPdtId
	 * @param conn
	 * @return
	 * @throws Exception
	 */
	public boolean isBelongToMultipleColorStyle(int mainPdtId)throws Exception{
		boolean b= QueryEngine.getInstance().doQueryInt("select count(*) from m_product where stylename=(select stylename from m_product where id=?)",
				new Object[]{mainPdtId},conn)>1;
		return b;
	}
	
	/**
	 * Load full color matrix define and asi 
	 * @throws Exception
	 */
	protected ProductMatrix loadFullColorMatrix(int mainPdtId)throws Exception{
		// ģʽ1����֤��ǰ��ɫ�ڵ�һ��
		// 2013.3.11 bug: satchi ����ɫģʽ������ͬһ���Aɫ�ڶ������Bɫ���ڣ���Ҫ�����Ʒ������ͬһ����������, Ҫ������Ƿ�ɾ����
		// 2013.9.19 ���ҿ����ڶ���������������
		//2015.8.3 �������show_pdtdel_matrix������ɾ���ھ����е���ʾ
		JSONArray pdts= QueryEngine.getInstance().doQueryJSONArray(
				"select p.id, p.m_sizegroup_id,0 porder, p.name from m_product p where p.id=? union all " +
				"select p.id, p.m_sizegroup_id,1 porder, p.name from m_product p where p.id<>? and" +
				" p.stylename=(select stylename from m_product where id=?) order by porder,name",
				new Object[]{mainPdtId,mainPdtId,mainPdtId}, conn);
		// ģʽ2������ɫ˳��
//		JSONArray pdts= QueryEngine.getInstance().doQueryJSONArray(
//				"select p.id, p.m_sizegroup_id from m_product p where p.stylename=(select stylename from m_product where id=?) order by p.name",
//				new Object[]{mainPdtId}, conn);
		JSONObject fullMatrixDef= new JSONObject();
		JSONArray fullMatrixASI = new JSONArray();
		HashSet<Integer> sizeGroups=new HashSet();
		JSONArray fullSize=new JSONArray();
		JSONArray fullSizeNotes=new JSONArray();
		JSONArray fullSizeFactors=new JSONArray();
		JSONArray fullColor=new JSONArray();
		JSONArray pdtIds= new JSONArray();
		int maxSizeCount=0;
		
		for(int i=0;i<pdts.length();i++){
			JSONArray row= pdts.getJSONArray(i);
			int pdtId=row.getInt(0);
			
			int sizeGroupId= row.getInt(1);
			//String pdtName= row.getString(2);
			
			ProductMatrix mat= getSimpleProductMatrix(pdtId);
			JSONArray ja= mat.asiArray;// getProductMatrixASI(pdtId);
			
			if(!sizeGroups.contains(sizeGroupId)){
				//add size to new size
				JSONArray pdtSize=mat.sizes;
				JSONArray pdtSizeNotes=mat.sizeNotes;
				JSONArray pdtSizeFactors=mat.sizeFactors;
				extendSizeArray(fullSize, pdtSize);
				extendSizeArray(fullSizeNotes, pdtSizeNotes);
				extendSizeArray(fullSizeFactors, pdtSizeFactors);
				sizeGroups.add(sizeGroupId);
			}
			JSONArray color= mat.colors;
			for(int j=0;j<color.length();j++){
				JSONObject clr= color.getJSONObject(j);
				JSONObject mclr= new JSONObject(clr.toString());//clone
				//��Ϊ��ͬһ���������������
				mclr.put("n",  mclr.getString("n"));
				fullColor.put(mclr);
			}
			//asi
			for(int j=0;j<ja.length();j++){
				JSONArray jr=new JSONArray( ja.getJSONArray(j).toString());
				fullMatrixASI.put(jr);
				pdtIds.put(pdtId);
			}
			
		}
		//change all color.s, add  zeros
		for(int i=0;i<fullColor.length();i++){
			JSONObject mclr= fullColor.getJSONObject(i);
			JSONArray ss= mclr.getJSONArray("s");
			for(int j=ss.length();j< fullSize.length();j++ )ss.put(0);
		}
		Integer nullInt= null;
		for(int i=0;i<fullMatrixASI.length();i++){
			JSONArray asis= fullMatrixASI.getJSONArray(i);
			for(int j=asis.length();j< fullSize.length();j++ )asis.put(nullInt);
		}
		fullMatrixDef.put("colors",fullColor);
		fullMatrixDef.put("sizes",fullSize);
		fullMatrixDef.put("sizenotes",fullSizeNotes);
		fullMatrixDef.put("sizefactors",fullSizeFactors);
		fullMatrixDef.put("pdtids", pdtIds);// ��֪ÿһ�е�pdtid
		fullMatrixDef.put("asis", fullMatrixASI);// ��֪ÿһ�е�pdtid
		ProductMatrix mat=new ProductMatrix(fullMatrixDef);
		return mat;//jedis.set("pdt:"+ pdtId+":sheet", mat.toJSONObject().toString());
		
	}	
	/**
	 * Exetend to contain the new size as new row
	 * @param masterSize
	 * @param pdtSize
	 * @throws Exception
	 */
	private void extendSizeArray(JSONArray masterSize, JSONArray pdtSize) throws Exception{
		if(masterSize.length()==0){
			for(int i=0;i<pdtSize.length();i++)masterSize.put(pdtSize.get(i));
			return;
		}
		int mlen=masterSize.length();
		int plen= pdtSize.length();
		if(mlen<plen){
			for(int i=mlen;i< plen;i++){
				masterSize.put(" ");
			}
		}
		
		// ÿ��һ�������飬��ʽ��
		// 36,37,38,39 - shoes
		// xl,x ,l ,xxl - clothes
		for(int i=0;i< pdtSize.length();i++){
			masterSize.put(i, masterSize.getString(i)+"\n"+ pdtSize.getString(i));
		}
		for(int i=pdtSize.length();i< masterSize.length();i++){
			masterSize.put(i,masterSize.getString(i)+"\n ");
		}
		
	}
}
