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
import com.agilecontrol.phone.PhoneController;
import com.agilecontrol.phone.PhoneUtils;
import com.agilecontrol.phone.UserObj;

/**
 * 
 * 标准矩阵构造
 * 返回的sheet.values: key="p"+$pdtid+"_"+$asi, value=输入的qty
 * 
 * 增加 color_column - jsonarray of string, 这是指颜色列的值配置。
 * 由于有多个item，颜色列针对一个商品也可占据多行，比如item如果有3个，则颜色列首行显示货号，
 * 第二行显示名称，第三行显示颜色等。配置为：["$pdt.no", "$pdt.name", "$pdt.colros"]；
 * 如果只有下单item，则可以考虑将多个商品属性配置在一个单元格里显示：如 ["$pdt.no $pdt.colors"], 
 * 这里的$pdt是在redis缓存中的商品对象，上面的对象属性可以通过 ad_sql#table:pdt:meta 中设置column来获取。
 * 如果配置的color_column个数超过items的个数，将报错
 * 
 * @author yfzhu
 *
 */
public class SKUSheetBuilder extends SheetBuilder {
	/**
	 * 构建完成的矩阵定义, 针对Pc版本，phone版不需要
	 */
	protected Sheet sheet;
	/**
	 * 构建完成的矩阵的数据定义
	 */
	protected JSONObject values;

	/**
	 * 构建完成的矩阵定义
	 * @return
	 * 对于pc版的sheet, {def,values},
	 * 对于phone版的sheet，{pdtid,value1, value2,pdt,vname, template}
	 */
	public JSONObject getSheet() throws Exception{
		JSONObject ret=new JSONObject();
		ret.put("def", sheet.toJSONObject());
		ret.put("values", values);
		//捞取所有定制的属性还给客户端，除了class，items，color_column
		for(Iterator it=conf.keys();it.hasNext();){
			String key=(String)it.next();
			if(key.equals("class") || key.equals("items") || key.equals("color_column")){
				continue;
			}
			ret.put(key, conf.get(key));
		}
		ret.put("type", "skusheet");
		return ret;
	}
	
	
	/**
	 * 构建矩阵结果，包括数据部分
	 * @throws Exception
	 */
	public void build() throws Exception{
		sheet=new Sheet();
		values=new JSONObject();
		
		
		vc.put("actid", this.actId);
		vc.put("uid", this.usr.getId());
		vc.put("marketid", this.usr.getMarketId());
		
		JSONArray itemsConf=conf.getJSONArray("items");
		int itemCount=0;
		for(int j=0;j<itemsConf.length();j++){
			JSONObject itemConf=itemsConf.getJSONObject(j);
			if(itemConf.optBoolean("hide", false)==false){
				itemCount++;
			}
		}
		boolean hasOnlyOneItem=(itemCount==1);//是否仅有订量，如果是，不显示“项目” 列
		
		int defaultLangId=LanguageManager.getInstance().getDefaultLangId();
		/**
		 * 全矩阵
		 */
		ProductMatrixLoader loader=new ProductMatrixLoader(jedis,conn);
		ProductMatrix matrix=loader.getProductMatrix(pdtId);
		ArrayList<Integer> pdtIds=matrix.getProductIds();
		ArrayList<String> sizes=matrix.getSizes();
		ArrayList<String> sizeNotes=matrix.getSizeNotes();
		ArrayList<Integer> sizeFactors=matrix.getSizeFactors();//尺码系数，面向文具版，尺码是支包件这种形式
		ArrayList<Color> colors= matrix.getColors(); //=pdtIds.legnth
		JSONArray asiArray=matrix.getASIArrays(); // 2维数组
//		logger.debug("##############pdtid="+pdtId+": pdtIds="+ Tools.toString(pdtIds, ",")+
//				" sizes="+ Tools.toString(sizes, ",")+ " colors="+
//				Tools.toString(colors, ",")+" asis="+asiArray);
		
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
		
		int cols= sizes.size()+ (hasOnlyOneItem?3:4);//color, item, totqty, totamt
		sheet.setCols(cols);

		int col=0, row=0;
		Cell cell;
		//head line
		sheet.addCell(new Cell(row,col++,Cell.TYPE_STRING, null,"@sheet_color@"));
		if(!hasOnlyOneItem)sheet.addCell(new Cell(row,col++,Cell.TYPE_STRING, null,"@sheet_item@"));
		String sizeDesc;
		for(int i=0;i< sizes.size();i++){
			if(defaultLangId== this.usr.getLangId()) sizeDesc=sizes.get(i);
			else{
				//去note字段，如果note字段为空，提示未维护, note字段即m_attribute.description
				sizeDesc=sizeNotes.get(i);
				if(Validator.isNull(sizeDesc)) sizeDesc="Need m_size.description";
			}
			sheet.addCell(new Cell(row,col++,Cell.TYPE_STRING, null,sizeDesc));
		}
		sheet.addCell(new Cell(row,col++,Cell.TYPE_STRING, null,"@sheet_totqty@"));
		sheet.addCell(new Cell(row,col++,Cell.TYPE_STRING, null,"@sheet_totamt@"));
		
		
		//item: row idx in sheet of qty
		ArrayList<Integer> qtyRows=new ArrayList();
		
		//key: pdt.id, value:$pdt对象，是为了实现配置 color_column 指定的多个值， 通过$pdt和$color来获取定义
		HashMap<Integer,JSONObject> redisPdts=new HashMap();
		Table pdtTable=manager.getTable("pdt");
		for(int i=0;i< pdtIds.size();i++){
			int pdtId= pdtIds.get(i);
			JSONObject po=PhoneUtils.fetchObjectAllColumns(pdtTable, pdtId, conn, jedis);//需要拿到packqty，mask=00
			WebController.getInstance().replacePdtValues(po, usr, vc, jedis, conn);
			redisPdts.put(pdtId, po);
		}
		VelocityContext colorVC = VelocityUtils.createContext();

		//for each color row
		for(int i=0;i< pdtIds.size();i++){
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
			
			// 每个色下面有多个项目
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
				row++;
				col=0;
				
				if(isQtyItem) qtyRows.add(row);
				
				//是否够放颜色字段内容
				if(j<= colorColumnDef.length()-1){
					//取出颜色字段定义
					String colorDef=colorColumnDef.getString(j);
					StringWriter output = new StringWriter();
					colorVC.put("color", color);
					colorVC.put("pdt",redisPdt);
					Velocity.evaluate(colorVC, output, VelocityUtils.class.getName(), colorDef);
					String colorValue=output.toString();
//					logger.debug("$$$$$$$$$$$$$$$$$$$$$$adding color value: "+colorDef+"="+ colorValue+" of cell("+ row+","+col+")" );
					sheet.addCell(Cell.createText(row, col, colorValue));
				}
				col++;
				//项目名称
				if(!hasOnlyOneItem){
					String itemDesc=itemConf.optString("desc");
					String lang=itemConf.optString("lang");
					if(Validator.isNotNull(lang)) itemDesc=lang;
					sheet.addCell(Cell.createText(row, col++, itemDesc));
				}
				//尺码
				
				HashMap<Integer, Object> itemValues=items.get(j);
				
				StringBuilder rowSumQty=new StringBuilder();
				StringBuilder rowSumAmt=new StringBuilder();
				
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
						cell=Cell.createIntegerInput(row,col+k, key );
						if(this.readOnly) cell.disable();
						sheet.addCell(cell);
						int sizeFactor=sizeFactors.get(k);
						if(sizeFactor<1) throw new NDSException("意外:size.factor=0");
						int dbQty= Tools.getInt(value, -1);
						if(dbQty>0)
							values.put(key, dbQty/(packQty*sizeFactor));
						
						rowSumQty.append("c("+ row+","+ (col+k)+")*"+(packQty*sizeFactor)+"+");
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
						rowSumAmt.append("amt("+p+",c("+ row+","+ (col+k)+"))+");
						
					}else if(isPriceItem){
						String ps=((JSONObject)value).getString("d");
						sheet.addCell(Cell.createText(row, col+k, ps));
					}else{
						if(value==null) value="";
						else if(Validator.isNull(value.toString())) value="";
						sheet.addCell(Cell.createText(row, col+k, value==null?"": value.toString()));
					}
					
				}
				if(isQtyItem){
					col=col+ sizes.size();
					//数量小计和金额小计
					if(rowSumQty.length()>0)rowSumQty.deleteCharAt(rowSumQty.length()-1);
					if(rowSumAmt.length()>0)rowSumAmt.deleteCharAt(rowSumAmt.length()-1);
					sheet.addCell(Cell.createFomula(row, col++, rowSumQty.toString()));
					sheet.addCell(Cell.createFomula(row, col, rowSumAmt.toString()));
				}
			}
			
		}
//		logger.debug("qtyrows="+ qtyRows);
		// 合计行
		row++;
		col=0;
		sheet.addCell(new Cell(row,col++,Cell.TYPE_STRING, null,"@sheet_total@"));
		if(!hasOnlyOneItem)sheet.addCell(new Cell(row,col++,Cell.TYPE_STRING, null,"@sheet_orderqty@"));
		for(int i=0;i<sizes.size()+2;i++){
			StringBuilder sb=new StringBuilder();
			for(int j=0;j<qtyRows.size();j++){
				int qr=qtyRows.get(j);
				sb.append("c("+ qr+","+ (col+i)+")+");
			}
			sb.deleteCharAt(sb.length()-1);
			sheet.addCell(Cell.createFomula(row, col+i, sb.toString()));
		}
		
		sheet.setRows(row+1);
		
		
		
	}
	
}
