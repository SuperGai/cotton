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
 * ��׼������
 * ���ص�sheet.values: key="p"+$pdtid+"_"+$asi, value=�����qty
 * 
 * ���� color_column - jsonarray of string, ����ָ��ɫ�е�ֵ���á�
 * �����ж��item����ɫ�����һ����ƷҲ��ռ�ݶ��У�����item�����3��������ɫ��������ʾ���ţ�
 * �ڶ�����ʾ���ƣ���������ʾ��ɫ�ȡ�����Ϊ��["$pdt.no", "$pdt.name", "$pdt.colros"]��
 * ���ֻ���µ�item������Կ��ǽ������Ʒ����������һ����Ԫ������ʾ���� ["$pdt.no $pdt.colors"], 
 * �����$pdt����redis�����е���Ʒ��������Ķ������Կ���ͨ�� ad_sql#table:pdt:meta ������column����ȡ��
 * ������õ�color_column��������items�ĸ�����������
 * 
 * @author yfzhu
 *
 */
public class SKUSheetBuilder extends SheetBuilder {
	/**
	 * ������ɵľ�����, ���Pc�汾��phone�治��Ҫ
	 */
	protected Sheet sheet;
	/**
	 * ������ɵľ�������ݶ���
	 */
	protected JSONObject values;

	/**
	 * ������ɵľ�����
	 * @return
	 * ����pc���sheet, {def,values},
	 * ����phone���sheet��{pdtid,value1, value2,pdt,vname, template}
	 */
	public JSONObject getSheet() throws Exception{
		JSONObject ret=new JSONObject();
		ret.put("def", sheet.toJSONObject());
		ret.put("values", values);
		//��ȡ���ж��Ƶ����Ի����ͻ��ˣ�����class��items��color_column
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
	 * �������������������ݲ���
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
		boolean hasOnlyOneItem=(itemCount==1);//�Ƿ���ж���������ǣ�����ʾ����Ŀ�� ��
		
		int defaultLangId=LanguageManager.getInstance().getDefaultLangId();
		/**
		 * ȫ����
		 */
		ProductMatrixLoader loader=new ProductMatrixLoader(jedis,conn);
		ProductMatrix matrix=loader.getProductMatrix(pdtId);
		ArrayList<Integer> pdtIds=matrix.getProductIds();
		ArrayList<String> sizes=matrix.getSizes();
		ArrayList<String> sizeNotes=matrix.getSizeNotes();
		ArrayList<Integer> sizeFactors=matrix.getSizeFactors();//����ϵ���������ľ߰棬������֧����������ʽ
		ArrayList<Color> colors= matrix.getColors(); //=pdtIds.legnth
		JSONArray asiArray=matrix.getASIArrays(); // 2ά����
//		logger.debug("##############pdtid="+pdtId+": pdtIds="+ Tools.toString(pdtIds, ",")+
//				" sizes="+ Tools.toString(sizes, ",")+ " colors="+
//				Tools.toString(colors, ",")+" asis="+asiArray);
		
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
				//ȥnote�ֶΣ����note�ֶ�Ϊ�գ���ʾδά��, note�ֶμ�m_attribute.description
				sizeDesc=sizeNotes.get(i);
				if(Validator.isNull(sizeDesc)) sizeDesc="Need m_size.description";
			}
			sheet.addCell(new Cell(row,col++,Cell.TYPE_STRING, null,sizeDesc));
		}
		sheet.addCell(new Cell(row,col++,Cell.TYPE_STRING, null,"@sheet_totqty@"));
		sheet.addCell(new Cell(row,col++,Cell.TYPE_STRING, null,"@sheet_totamt@"));
		
		
		//item: row idx in sheet of qty
		ArrayList<Integer> qtyRows=new ArrayList();
		
		//key: pdt.id, value:$pdt������Ϊ��ʵ������ color_column ָ���Ķ��ֵ�� ͨ��$pdt��$color����ȡ����
		HashMap<Integer,JSONObject> redisPdts=new HashMap();
		Table pdtTable=manager.getTable("pdt");
		for(int i=0;i< pdtIds.size();i++){
			int pdtId= pdtIds.get(i);
			JSONObject po=PhoneUtils.fetchObjectAllColumns(pdtTable, pdtId, conn, jedis);//��Ҫ�õ�packqty��mask=00
			WebController.getInstance().replacePdtValues(po, usr, vc, jedis, conn);
			redisPdts.put(pdtId, po);
		}
		VelocityContext colorVC = VelocityUtils.createContext();

		//for each color row
		for(int i=0;i< pdtIds.size();i++){
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
			
			// ÿ��ɫ�����ж����Ŀ
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
				
				//�Ƿ񹻷���ɫ�ֶ�����
				if(j<= colorColumnDef.length()-1){
					//ȡ����ɫ�ֶζ���
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
				//��Ŀ����
				if(!hasOnlyOneItem){
					String itemDesc=itemConf.optString("desc");
					String lang=itemConf.optString("lang");
					if(Validator.isNotNull(lang)) itemDesc=lang;
					sheet.addCell(Cell.createText(row, col++, itemDesc));
				}
				//����
				
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
					Object value= itemValues.get(asi); // 2���������price�ֶΣ��ǽ��ֵ��price�ֶΣ�{d,p}  d - description, ���ڽ���������p - price �۸�����3Ԫ�����һ�۸�
					if(isQtyItem){
						String key="p"+pdtId+"_"+asi;//for client input
						cell=Cell.createIntegerInput(row,col+k, key );
						if(this.readOnly) cell.disable();
						sheet.addCell(cell);
						int sizeFactor=sizeFactors.get(k);
						if(sizeFactor<1) throw new NDSException("����:size.factor=0");
						int dbQty= Tools.getInt(value, -1);
						if(dbQty>0)
							values.put(key, dbQty/(packQty*sizeFactor));
						
						rowSumQty.append("c("+ row+","+ (col+k)+")*"+(packQty*sizeFactor)+"+");
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
					//����С�ƺͽ��С��
					if(rowSumQty.length()>0)rowSumQty.deleteCharAt(rowSumQty.length()-1);
					if(rowSumAmt.length()>0)rowSumAmt.deleteCharAt(rowSumAmt.length()-1);
					sheet.addCell(Cell.createFomula(row, col++, rowSumQty.toString()));
					sheet.addCell(Cell.createFomula(row, col, rowSumAmt.toString()));
				}
			}
			
		}
//		logger.debug("qtyrows="+ qtyRows);
		// �ϼ���
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
