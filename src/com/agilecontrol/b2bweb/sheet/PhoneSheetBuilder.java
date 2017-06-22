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
h1. 面向手机版的标准矩阵构造

h2.输入

> {cmd:"b2b.pdt.sheet", pdtid, actid, readonly, isphone}
*isphone* - true

h2. 输出

> {pdtid,value1, value2,pdt,vname, template}

举例如下：
<pre>
{
	pdtid:11,
	value1:[ 
		{
			name:'黑色' , value2:[0,1,1,1,0], code:'01', pdtid:12, 
			note:[null, [{n:'库存', v:12}, {n:'可维持天数', v:4}]],
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
	vname:['颜色', '尺寸'],
	notedesc:['库存','可维持天数'],
	qtydesc:"数量",
	template: {unit:12, ratio:[0.1, 0.2,0.3,0.4], func:'poisson'}
}
</pre>

*pdtid* - 当前主商品id, int 对应m_product.id
*value1* - 一级属性，对应服装里的颜色，或搭配商品名称，具体名称由vname[0] 指定. jsonobj
>	name: - 颜色显示名称, string
>	value2: - boolean[] 对应value2 数组，哪些value2的值允许输入, 0 表示不允许，1表示允许
>	code: - 颜色代码, string，目前没有作用
>	pdtid: - 商品id, int, 在搭配或款色模式下，不同的商品显示在当前页面。在切换value1的时候，需要更改商品图片，编号和价格等
>	note: - 备注, {n,v}[][], 优先于note2属性，用于在数量边上显示备注信息，如库存，销量等信息。n-名称，v-显示值。
>	note2: -备注，string[]， 在数量输入框边上的备注，若设置了note,将无需再识别note2。note2是html代码段
>	key: - 输入键值, string[]， 与value2长度一致，value2元素为0的，这里需要为空。对应当前颜色下的不同尺码的上传key，参见SaveSheet
>	value: - 当前值, number[], 与value2长度一致，value2元素为0的，这里需要为空  
*value2* - 二级属性，对应服装的尺码。服装中单一尺码的情况下，需要考虑在绘制界面的时候，将尺码画到第一层去
*pdt* - 商品情况, {}, key是pdtid, value是pdt的标准属性{id,no,note,price,mainpic}
*vname* - 属性描述, string[2], value1和value2分别对应的名称
*notedesc* -  note的各个属性的描述
*qtydesc* - 数量的描述（翻译）
*template* - 订货模板，可选jsonobj
>	unit - 1手的数量，结合ratio可以方便下单，在1手的计算方法里作为汇总数 取自：B_SIZE_ALLOC.unitqty
>	ratio- double[]，与value2对应，对应每个尺码的占比, 取自：B_SIZE_ALLOC.user_id=?, 
>	func - 算法名称，目前仅支持'poisson'，即帕松算法, 用于决定四舍五入的规则

h2. 帕松算法

服装类商品在尺码上的分布呈现帕松形态，即中部的尺码最多，两边依次减少。
采用取整而非四舍五入（也一样会总量不一致，计算更复杂）的方式，合计总量会和预计总量不一致。
这时需要将不足的量按优先序，从正中的尺码开始向两边放，注意是一个个的放。目标是尽可能保证放置完的尺码比例与理想尺码比例间的均方差最小。

 * 
 * @author yfzhu
 *
 */
public class PhoneSheetBuilder extends SheetBuilder {
	
	protected JSONObject ret;
	/**
	 * 构建完成的矩阵定义
	 * @return
	 * 对于phone版的sheet，{pdtid,value1, value2,pdt,vname, template}
	 */
	public JSONObject getSheet() throws Exception{

		return ret;
	}
	
	/**
	 * 创建一个新的jsonobject,只有需要的属性key
	 * @param obj
	 * @param keys string[]
	 * @return 新的对象
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
	 * 构建矩阵结果，包括数据部分
	 * @throws Exception
	 */
	public void build() throws Exception{
		ret=new JSONObject();

		//捞取所有定制的属性还给客户端，除了class，items，color_column
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
		ret.put("qtydesc", "数量");
		
		int defaultLangId=LanguageManager.getInstance().getDefaultLangId();
		ProductMatrixLoader loader=new ProductMatrixLoader(jedis,conn);
		/**
		 * 全矩阵
		 */
		ProductMatrix matrix=loader.getProductMatrix(pdtId);
		ArrayList<Integer> pdtIds=matrix.getProductIds();
		ArrayList<String> sizes=matrix.getSizes();
		ArrayList<String> sizeNotes=matrix.getSizeNotes();
		ArrayList<Integer> sizeFactors=matrix.getSizeFactors();//尺码系数，面向文具版，尺码是支包件这种形式
		ArrayList<Color> colors= matrix.getColors(); //=pdtIds.legnth
		JSONArray asiArray=matrix.getASIArrays(); // 2维数组
		/*
		 * 需要为每个pdtId, 确认其数据内容，matrix 有可能有多个不同的pdtid
		 * key: pdt.id,value:
		 *  {items, asis} items - [hashmap<asi, value>] ,items 的行顺序与itemsConf一致, asis - hashset<asi>
		 * name 对应 itemsConf 每行 desc的
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
				//去note字段，如果note字段为空，提示未维护, note字段即m_attribute.description
				sizeDesc=sizeNotes.get(i);
				if(Validator.isNull(sizeDesc)) sizeDesc="Need m_size.description";
			}
			value2.put(sizeDesc);
		}
		ret.put("value2", value2);
		JSONArray vname= conf.optJSONArray("vname");
		if(vname==null) {
			vname=new JSONArray();
			vname.put("颜色");
			vname.put("尺码");
		}
		ret.put("vname",vname);
		
		//item: row idx in sheet of qty
		ArrayList<Integer> qtyRows=new ArrayList();
		
		//key: pdt.id, value:$pdt对象，是为了实现配置 color_column 指定的多个值， 通过$pdt和$color来获取定义
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
				JSONObject po=PhoneUtils.fetchObjectAllColumns(pdtTable, pdtId,  conn, jedis);//需要拿到packqty，mask=00
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
//	                name:'黑色' , value2:[0,1,1,1,0], code:'01', pdtid:12, 
//	                note:[null, [{n:'库存', v:'12件'}, {n:'可维持天数', v:'4天'}]],
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
			//pdtObject- {items, asis} items - [hashmap<asi, value>] ,items 的行顺序与itemsConf一致, asis - hashset<asi>
			HashMap<String,Object> pdtObject= pdtObjects.get(pdtId);
			JSONObject redisPdt=redisPdts.get(pdtId);
			/*
			 * 这是木槿项目开始的下单倍数，比如单价是按支定义的，界面上中包规格是14，则下单1即为14，2就是28，数据库存储仍然按支数
			 */
			int packQty=redisPdt.optInt("packqty", 1);
			if(packQty<=0)packQty=1;
			
//			logger.debug("pdtId="+ pdtId+", pdtObj="+ pdtObject);
			//row: by item, key: asi, value: value of that item
			ArrayList<HashMap<Integer, Object>> items=(ArrayList<HashMap<Integer, Object>>) pdtObject.get("items");
			//asis that can input，这是根据市场查询得出的数据，是否此asi在这个市场或活动里有
			HashSet<Integer> asis=(HashSet<Integer>)pdtObject.get("asis");
			Color color=colors.get(i);
			//elements are asi of each size position, null if has no
			//注意这个元素还需要结合当前活动或市场，即根据asis再做一次判定
			JSONArray pdtASIArray= asiArray.getJSONArray(i);
			
			/**
			 * key: asi, value: asi对应的价格，{d, p} d - description, p - 价格3元数组或单一价格
			 */
			HashMap<Integer, Object> priceItem=items.get(this.getPriceItemIdx());
			
			String colorDef=colorColumnDef.getString(0);//取出第一个颜色描述说明作为当前颜色名称
			StringWriter output = new StringWriter();
			colorVC.put("color", color);
			colorVC.put("pdt",redisPdt);
			Velocity.evaluate(colorVC, output, VelocityUtils.class.getName(), colorDef);
			String colorValue=output.toString();			

			pdtValue.put("name", colorValue);//color.getName());
			pdtValue.put("code", color.getCode());
			pdtValue.put("pdtid", pdtId);
			JSONArray ss=color.getSizes();
			
			// 每个色下面有多个项目
			JSONArray notes=new JSONArray();//note:[null/*size1*/, [{n:'库存', v:'12件'}, {n:'可维持天数', v:'4天'}]/*size2*/],
			for(int n=0;n<ss.length();n++) notes.put(new JSONArray());//blank array for each size
			pdtValue.put("note", notes);
			String[] keys=new String[ss.length()];//[null, 'p11_12', 'p11_13', 'p11_14', null]
			Integer[] values=new Integer[ss.length()];//[null, 14, 0, 0, null]
			
			//添加这些项目，注意排除掉qty列
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
				
				//项目名称
				String itemDesc=itemConf.optString("desc");
				String lang=itemConf.optString("lang");
				if(Validator.isNotNull(lang)) itemDesc=lang;
				//尺码
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
					Object value= itemValues.get(asi); // 2种情况，非price字段，是结果值，price字段，{d,p}  d - description, 用于界面描述，p - price 价格区间3元数组或单一价格
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
						if(ps==null) throw new NDSException("意外: asi="+asi +"没有对应价格(pdtid="+pdtId +")");
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
				value2JSON.put(k==null? 0: ss.getInt(x));//key若不存在，value2就必定为0，表示尺码没有。key不存在可能是由于商品不在当前市场
			}
			pdtValue.put("key", keyJSON);
			pdtValue.put("value2", value2JSON);//[0,1,1,0]  表示是否有这个尺码, 前提是key存在
			
			JSONArray valueJSON=new JSONArray();
			for(Integer k: values) valueJSON.put(k==null?JSONObject.NULL:k);
			pdtValue.put("value", valueJSON);
			/**
			value1:[ 
		        {
		            name:'黑色' , value2:[0,1,1,1,0], code:'01', pdtid:12, 
		            note:[null, [{n:'库存', v:'12件'}, {n:'可维持天数', v:'4天'}]],
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
		//是否具有尺码模板
		ret.put("ratio", PhoneConfig.USING_SIZE_RATIOS);
	}
	
}
