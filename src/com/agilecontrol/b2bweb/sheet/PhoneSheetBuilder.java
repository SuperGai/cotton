package com.agilecontrol.b2bweb.sheet;

import java.io.StringWriter;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.json.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;

import com.agilecontrol.b2b.schema.Table;
import com.agilecontrol.b2b.schema.Column;
import com.agilecontrol.b2b.schema.TableManager;
import com.agilecontrol.b2bweb.WebController;
import com.agilecontrol.b2bweb.cmd.GetSheet;
import com.agilecontrol.nea.core.control.event.DefaultWebEvent;
import com.agilecontrol.nea.core.query.QueryEngine;
import com.agilecontrol.nea.core.velocity.VelocityUtils;
import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.nea.util.Tools;
import com.agilecontrol.nea.util.Validator;
import com.agilecontrol.phone.LanguageManager;
import com.agilecontrol.phone.PhoneConfig;
import com.agilecontrol.phone.PhoneController;
import com.agilecontrol.phone.PhoneUtils;
import com.agilecontrol.phone.UserObj;

/**
 * 
h1. �����ֻ���ı�׼������

h2.����

> {cmd:"b2b.pdt.sheet", pdtid, actid, readonly, isphone}
*isphone* - true

h2. ���

> {pdtid,value1, value2,pdt,vname, template}

�������£�
<pre>
{
	pdtid:11,
	value1:[ 
		{
			name:'��ɫ' , value2:[0,1,1,1,0], code:'01', pdtid:12, 
			note:[null, [{n:'���', v:12}, {n:'��ά������', v:4}]],
			note2: [''],
			key:[null, 'p11_12', 'p11_13', 'p11_14', null],
			value:[null, 14, 0, 0, null]
		}
	],
	value2: ['23', '24', '25','26', '27'],
	pdt:{
		12: {id:12, no:'A001', mainpic:'', note:'', price:''},
		11: {id:11, no:'A002', mainpic:'', note:'', price:''}
	},
	vname:['��ɫ', '�ߴ�'],
	notedesc:['���','��ά������'],
	qtydesc:"����",
	template: {unit:12, ratio:[0.1, 0.2,0.3,0.4], func:'poisson'}
}
</pre>

*pdtid* - ��ǰ����Ʒid, int ��Ӧm_product.id
*value1* - һ�����ԣ���Ӧ��װ�����ɫ���������Ʒ���ƣ�����������vname[0] ָ��. jsonobj
>	name: - ��ɫ��ʾ����, string
>	value2: - boolean[] ��Ӧvalue2 ���飬��Щvalue2��ֵ��������, 0 ��ʾ������1��ʾ����
>	code: - ��ɫ����, string��Ŀǰû������
>	pdtid: - ��Ʒid, int, �ڴ�����ɫģʽ�£���ͬ����Ʒ��ʾ�ڵ�ǰҳ�档���л�value1��ʱ����Ҫ������ƷͼƬ����źͼ۸��
>	note: - ��ע, {n,v}[][], ������note2���ԣ�����������������ʾ��ע��Ϣ�����棬��������Ϣ��n-���ƣ�v-��ʾֵ��
>	note2: -��ע��string[]�� �������������ϵı�ע����������note,��������ʶ��note2��note2��html�����
>	key: - �����ֵ, string[]�� ��value2����һ�£�value2Ԫ��Ϊ0�ģ�������ҪΪ�ա���Ӧ��ǰ��ɫ�µĲ�ͬ������ϴ�key���μ�SaveSheet
>	value: - ��ǰֵ, number[], ��value2����һ�£�value2Ԫ��Ϊ0�ģ�������ҪΪ��  
*value2* - �������ԣ���Ӧ��װ�ĳ��롣��װ�е�һ���������£���Ҫ�����ڻ��ƽ����ʱ�򣬽����뻭����һ��ȥ
*pdt* - ��Ʒ���, {}, key��pdtid, value��pdt�ı�׼����{id,no,note,price,mainpic}
*vname* - ��������, string[2], value1��value2�ֱ��Ӧ������
*notedesc* -  note�ĸ������Ե�����
*qtydesc* - ���������������룩
*template* - ����ģ�壬��ѡjsonobj
>	unit - 1�ֵ����������ratio���Է����µ�����1�ֵļ��㷽������Ϊ������ ȡ�ԣ�B_SIZE_ALLOC.unitqty
>	ratio- double[]����value2��Ӧ����Ӧÿ�������ռ��, ȡ�ԣ�B_SIZE_ALLOC.user_id=?, 
>	func - �㷨���ƣ�Ŀǰ��֧��'poisson'���������㷨, ���ھ�����������Ĺ���

h2. �����㷨

��װ����Ʒ�ڳ����ϵķֲ�����������̬�����в��ĳ�����࣬�������μ��١�
����ȡ�������������루Ҳһ����������һ�£���������ӣ��ķ�ʽ���ϼ��������Ԥ��������һ�¡�
��ʱ��Ҫ����������������򣬴����еĳ��뿪ʼ�����߷ţ�ע����һ�����ķš�Ŀ���Ǿ����ܱ�֤������ĳ��������������������ľ�������С��

 * 
 * @author yfzhu
 *
 */
public class PhoneSheetBuilder extends SheetBuilder {
	
	protected JSONObject ret;
	/**
	 * ������ɵľ�����
	 * @return
	 * ����phone���sheet��{pdtid,value1, value2,pdt,vname, template}
	 */
	public JSONObject getSheet() throws Exception{

		return ret;
	}
	
	/**
	 * ����һ���µ�jsonobject,ֻ����Ҫ������key
	 * @param obj
	 * @param keys string[]
	 * @return �µĶ���
	 */
	private JSONObject filterKeys(JSONObject obj, ArrayList<String> keys) throws Exception{
		JSONObject one=new JSONObject();
		for(String key: keys){
			Object value=obj.opt(key);
			if(value!=null)one.put(key, value);
		}
		return one;
	}

	/**
	 * �������������������ݲ���
	 * @throws Exception
	 */
	public void build() throws Exception{
		ret=new JSONObject();

		//��ȡ���ж��Ƶ����Ի����ͻ��ˣ�����class��items��color_column
		for(Iterator it=conf.keys();it.hasNext();){
			String key=(String)it.next();
			if(key.equals("class") || key.equals("items") || key.equals("color_column")){
				continue;
			}
			ret.put(key, conf.get(key));
		}
		
		ret.put("type", "phonesheet");
		ret.put("pdtid", this.pdtId);
		
		vc.put("actid", this.actId);
		vc.put("uid", this.usr.getId());
		vc.put("marketid", this.usr.getMarketId());
		
		JSONArray itemsConf=conf.getJSONArray("items");
		JSONArray noteDescs=new JSONArray();
		//notedesc
		for(int j=0;j<itemsConf.length();j++){
			JSONObject itemConf=itemsConf.getJSONObject(j);
			if("qty".equals(itemConf.optString("key")))continue;//not qty item desc
			String itemDesc=itemConf.optString("desc");
			String lang=itemConf.optString("lang");
			if(Validator.isNotNull(lang)) itemDesc=lang;
			noteDescs.put( itemDesc);
		}
		ret.put("notedesc", noteDescs);
		ret.put("qtydesc", "����");
		
		int defaultLangId=LanguageManager.getInstance().getDefaultLangId();
		ProductMatrixLoader loader=new ProductMatrixLoader(jedis,conn);
		/**
		 * ȫ����
		 */
		ProductMatrix matrix=loader.getProductMatrix(pdtId);
		ArrayList<Integer> pdtIds=matrix.getProductIds();
		ArrayList<String> sizes=matrix.getSizes();
		ArrayList<String> sizeNotes=matrix.getSizeNotes();
		ArrayList<Integer> sizeFactors=matrix.getSizeFactors();//����ϵ���������ľ߰棬������֧����������ʽ
		ArrayList<Color> colors= matrix.getColors(); //=pdtIds.legnth
		JSONArray asiArray=matrix.getASIArrays(); // 2ά����
		/*
		 * ��ҪΪÿ��pdtId, ȷ�����������ݣ�matrix �п����ж����ͬ��pdtid
		 * key: pdt.id,value:
		 *  {items, asis} items - [hashmap<asi, value>] ,items ����˳����itemsConfһ��, asis - hashset<asi>
		 * name ��Ӧ itemsConf ÿ�� desc��
		 */
		HashMap<Integer, HashMap> pdtObjects=new HashMap();
		for(Integer pdtId: pdtIds){
			if(pdtObjects.containsKey(pdtId)) continue;
			HashMap<String, Object> pdtObj=this.loadPdtObject(pdtId,itemsConf);
			pdtObjects.put(pdtId, pdtObj);
		}
		JSONArray colorColumnDef=conf.getJSONArray("color_column");
		JSONArray value2=new JSONArray();
		String sizeDesc;
		for(int i=0;i< sizes.size();i++){
			if(defaultLangId== this.usr.getLangId()) sizeDesc=sizes.get(i);
			else{
				//ȥnote�ֶΣ����note�ֶ�Ϊ�գ���ʾδά��, note�ֶμ�m_attribute.description
				sizeDesc=sizeNotes.get(i);
				if(Validator.isNull(sizeDesc)) sizeDesc="Need m_size.description";
			}
			value2.put(sizeDesc);
		}
		ret.put("value2", value2);
		JSONArray vname= conf.optJSONArray("vname");
		if(vname==null) {
			vname=new JSONArray();
			vname.put("��ɫ");
			vname.put("����");
		}
		ret.put("vname",vname);
		
		//item: row idx in sheet of qty
		ArrayList<Integer> qtyRows=new ArrayList();
		
		//key: pdt.id, value:$pdt������Ϊ��ʵ������ color_column ָ���Ķ��ֵ�� ͨ��$pdt��$color����ȡ����
		HashMap<Integer,JSONObject> redisPdts=new HashMap();
		JSONObject retPdts=new JSONObject(); //for ret, key: pdtid, value: redisObj
		Table pdtTable=manager.getTable("pdt");
		ArrayList<String> pdtListColumns=new ArrayList();
		for(Column col: pdtTable.getColumnsInListView()){
			pdtListColumns.add(col.getName()); 
		}
		for(int i=0;i< pdtIds.size();i++){
			int pdtId= pdtIds.get(i);
			if(!redisPdts.containsKey(pdtId)){
				JSONObject po=PhoneUtils.fetchObjectAllColumns(pdtTable, pdtId,  conn, jedis);//��Ҫ�õ�packqty��mask=00
				WebController.getInstance().replacePdtValues(po, usr.getLangId(), usr.getMarketId(), vc, jedis, conn);
				redisPdts.put(pdtId, po);
				//only columns in listview
				retPdts.put(String.valueOf(pdtId), filterKeys(po, pdtListColumns));
			}
		}
		ret.put("pdt", retPdts);
		
		VelocityContext colorVC = VelocityUtils.createContext();
		
		JSONArray value1=new JSONArray();
//	    value1:[ 
//	            {
//	                name:'��ɫ' , value2:[0,1,1,1,0], code:'01', pdtid:12, 
//	                note:[null, [{n:'���', v:'12��'}, {n:'��ά������', v:'4��'}]],
//	                note2: [''],
//	                key:[null, 'p11_12', 'p11_13', 'p11_14', null],
//	                value:[null, 14, 0, 0, null]
//	            }
//	        ]
		//for each color row
		StringBuilder sumQty=new StringBuilder();
		StringBuilder sumAmt=new StringBuilder();
		
		for(int i=0;i< pdtIds.size();i++){
			JSONObject pdtValue=new JSONObject();
			
			int pdtId= pdtIds.get(i);
			//pdtObject- {items, asis} items - [hashmap<asi, value>] ,items ����˳����itemsConfһ��, asis - hashset<asi>
			HashMap<String,Object> pdtObject= pdtObjects.get(pdtId);
			JSONObject redisPdt=redisPdts.get(pdtId);
			/*
			 * ����ľ����Ŀ��ʼ���µ����������絥���ǰ�֧����ģ��������а������14�����µ�1��Ϊ14��2����28�����ݿ�洢��Ȼ��֧��
			 */
			int packQty=redisPdt.optInt("packqty", 1);
			if(packQty<=0)packQty=1;
			
//			logger.debug("pdtId="+ pdtId+", pdtObj="+ pdtObject);
			//row: by item, key: asi, value: value of that item
			ArrayList<HashMap<Integer, Object>> items=(ArrayList<HashMap<Integer, Object>>) pdtObject.get("items");
			//asis that can input�����Ǹ����г���ѯ�ó������ݣ��Ƿ��asi������г�������
			HashSet<Integer> asis=(HashSet<Integer>)pdtObject.get("asis");
			Color color=colors.get(i);
			//elements are asi of each size position, null if has no
			//ע�����Ԫ�ػ���Ҫ��ϵ�ǰ����г���������asis����һ���ж�
			JSONArray pdtASIArray= asiArray.getJSONArray(i);
			
			/**
			 * key: asi, value: asi��Ӧ�ļ۸�{d, p} d - description, p - �۸�3Ԫ�����һ�۸�
			 */
			HashMap<Integer, Object> priceItem=items.get(this.getPriceItemIdx());
			
			String colorDef=colorColumnDef.getString(0);//ȡ����һ����ɫ����˵����Ϊ��ǰ��ɫ����
			StringWriter output = new StringWriter();
			colorVC.put("color", color);
			colorVC.put("pdt",redisPdt);
			Velocity.evaluate(colorVC, output, VelocityUtils.class.getName(), colorDef);
			String colorValue=output.toString();			

			pdtValue.put("name", colorValue);//color.getName());
			pdtValue.put("code", color.getCode());
			pdtValue.put("pdtid", pdtId);
			JSONArray ss=color.getSizes();
			
			// ÿ��ɫ�����ж����Ŀ
			JSONArray notes=new JSONArray();//note:[null/*size1*/, [{n:'���', v:'12��'}, {n:'��ά������', v:'4��'}]/*size2*/],
			for(int n=0;n<ss.length();n++) notes.put(new JSONArray());//blank array for each size
			pdtValue.put("note", notes);
			String[] keys=new String[ss.length()];//[null, 'p11_12', 'p11_13', 'p11_14', null]
			Integer[] values=new Integer[ss.length()];//[null, 14, 0, 0, null]
			
			//�����Щ��Ŀ��ע���ų���qty��
			for(int j=0;j<itemsConf.length();j++){
				JSONObject itemConf=itemsConf.getJSONObject(j);
				String itemKey=itemConf.optString("key");
				boolean isQtyItem="qty".equals(itemKey);
				boolean isPriceItem="price".equals(itemKey);
//				logger.debug("itemconf("+j+")="+ itemConf+" of pdt id="+ pdtId+" where i="+i+", isQtyItem="+isQtyItem);

				if(itemConf.optBoolean("hide", false)){
//					logger.debug("skip "+itemKey +" row since it's hide");
					continue;
				}
				
				//��Ŀ����
				String itemDesc=itemConf.optString("desc");
				String lang=itemConf.optString("lang");
				if(Validator.isNotNull(lang)) itemDesc=lang;
				//����
				HashMap<Integer, Object> itemValues=items.get(j);
				
				
				for(int k=0;k<sizes.size();k++){
					boolean hasASI= color.isAvailableSize(k);
					if(!hasASI) {
//						logger.debug("not found asi for current color size idx="+ k+", color="+ color.getCode()+", colorsizee="+ Tools.toString( color.sizes,",") );
						continue;
					}
					int asi= pdtASIArray.optInt(k, -1);
					if(asi==-1){
//						logger.debug("not found asi for current color size idx="+ k+", color="+ color.getCode()+", pdtASIArray="+pdtASIArray.toString() );
						continue;
					}
					if(!asis.contains(asi)) {
//						logger.debug("found asi id="+ asi+" of pdtid="+ pdtId+" not in current marketid="+ usr.getMarketId()+", actid="+ actId+", of uid="+ usr.getId());
						continue;
					}
					Object value= itemValues.get(asi); // 2���������price�ֶΣ��ǽ��ֵ��price�ֶΣ�{d,p}  d - description, ���ڽ���������p - price �۸�����3Ԫ�����һ�۸�
					if(isQtyItem){
						String key="p"+pdtId+"_"+asi;//for client input
						keys[k]=key;
						int sizeFactor=sizeFactors.get(k);
						int dbQty= Tools.getInt(value, -1);
						if(dbQty>0){
							values[k]= dbQty/(packQty*sizeFactor);
						}
						sumQty.append("v('"+ key+"')*"+(packQty*sizeFactor)+"+");
						//sumAmt="amt(price, cell)"
						JSONObject ps=(JSONObject)priceItem.get(asi);
						if(ps==null) throw new NDSException("����: asi="+asi +"û�ж�Ӧ�۸�(pdtid="+pdtId +")");
						String p=ps.getString("p");
						if((packQty*sizeFactor)>1){
							if(p.startsWith("[")){
								p=multiple(p,(packQty*sizeFactor));
							}else{
								p=String.valueOf(Double.parseDouble(p) * (packQty*sizeFactor));
							}
						}
						sumAmt.append("amt("+p+",v('"+ key+"'))+");
						
						
					}else if(isPriceItem){
						String ps=((JSONObject)value).getString("d");
						JSONObject oneNote=new JSONObject();
						oneNote.put("n", itemDesc);
						oneNote.put("v", ps);
						notes.getJSONArray(k).put(oneNote);
						
					}else{
						JSONObject oneNote=new JSONObject();
						oneNote.put("n", itemDesc);
						oneNote.put("v", value==null?"":value.toString());
						notes.getJSONArray(k).put(oneNote);
					}
					
				}//end sizes
				
				
			}
			JSONArray keyJSON=new JSONArray();
			JSONArray value2JSON=new JSONArray();
			for(int x=0;x<keys.length;x++) {
				String k=keys[x];
				keyJSON.put(k==null?JSONObject.NULL:k);
				value2JSON.put(k==null? 0: ss.getInt(x));//key�������ڣ�value2�ͱض�Ϊ0����ʾ����û�С�key�����ڿ�����������Ʒ���ڵ�ǰ�г�
			}
			pdtValue.put("key", keyJSON);
			pdtValue.put("value2", value2JSON);//[0,1,1,0]  ��ʾ�Ƿ����������, ǰ����key����
			
			JSONArray valueJSON=new JSONArray();
			for(Integer k: values) valueJSON.put(k==null?JSONObject.NULL:k);
			pdtValue.put("value", valueJSON);
			/**
			value1:[ 
		        {
		            name:'��ɫ' , value2:[0,1,1,1,0], code:'01', pdtid:12, 
		            note:[null, [{n:'���', v:'12��'}, {n:'��ά������', v:'4��'}]],
		            note2: [''],
		            key:[null, 'p11_12', 'p11_13', 'p11_14', null],
		            value:[null, 14, 0, 0, null]
		        }
	    	]
			 */
			value1.put(pdtValue);
		}
		
		ret.put("value1", value1);
		if(sumAmt.length()>0){
			sumAmt.deleteCharAt(sumAmt.length()-1);
			ret.put("sumamt", sumAmt.toString());
		}
		if(sumQty.length()>0){
			sumQty.deleteCharAt(sumQty.length()-1);
			ret.put("sumqty", sumQty.toString());
		}
		//�Ƿ���г���ģ��
		ret.put("ratio", PhoneConfig.USING_SIZE_RATIOS);
	}
	
}
