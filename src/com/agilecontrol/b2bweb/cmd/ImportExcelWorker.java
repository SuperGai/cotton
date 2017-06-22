package com.agilecontrol.b2bweb.cmd;

import org.json.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.math.BigDecimal;
import java.sql.*;
import java.text.DecimalFormat;
import java.util.*;
import java.util.Date;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import redis.clients.jedis.Jedis;

import com.agilecontrol.b2bweb.sheet.Color;
import com.agilecontrol.b2bweb.sheet.ProductMatrix;
import com.agilecontrol.b2bweb.sheet.ProductMatrixLoader;
import com.agilecontrol.nea.core.control.web.UserWebImpl;
import com.agilecontrol.nea.core.control.web.WebUtils;
import com.agilecontrol.nea.core.query.QueryEngine;
import com.agilecontrol.nea.core.query.QueryException;
import com.agilecontrol.nea.core.query.QueryUtils;
import com.agilecontrol.nea.core.schema.ClientManager;
import com.agilecontrol.nea.core.schema.SQLTypes;
import com.agilecontrol.nea.core.schema.Table;
import com.agilecontrol.nea.core.schema.TableManager;
import com.agilecontrol.nea.core.schema.TriggerHolder.VersionedTrigger;
import com.agilecontrol.nea.core.util.MessagesHolder;
import com.agilecontrol.nea.util.FileUtils;
import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.nea.util.StringUtils;
import com.agilecontrol.nea.util.Tools;
import com.agilecontrol.nea.util.Validator;
import com.agilecontrol.phone.LanguageManager;
import com.agilecontrol.phone.PhoneController;
import com.agilecontrol.phone.PhoneUtils;
import com.agilecontrol.phone.UserObj;
/**
 * 处理上传的xls 文件
 * 格式：货号	 商品其他信息字段..., 尺码1,2,3
 * 步骤：
 * 获取定义: 关键字段: 货号，尺码位置
 * {
 *   columns:{ pdt:{col:1},skuqty:{col:5},err:{col:14}, value1:{col:x} },
 *   startrow: 1,
 *   end_proc: proc1,
 *   start_proc: proc2
 *   
 * }
 * 打开Excel, 读取首行
 * 逐行处理:
 *  读取每行，处理每个字段，处理每个字段内容，若有错误, 整行标注为红色，并讲错误信息写在err指定列上
 * 完成后:
 *  整单执行存储过程 end_proc 
 *  
 * 2016.10.30 init 
 *   
 * @author yfzhu
 *
 */
public class ImportExcelWorker {
	private static final Logger logger = LoggerFactory.getLogger(ImportExcelWorker.class);
	
	class ColumnDef{
		String name;
		boolean isNull;
		Object defaultValue;
		int col;// in excel
		boolean isDefined;//是否有定义这个字段
		Object fixedValue;//比如用户字段，必须是个恒定值，在此设置，以便检查
		String fkSQL;
		HashMap<String, Integer> fkIds;//key: cell.value, value: Integer 
		public  ColumnDef(String n,JSONObject def) throws Exception{
			name=n;
			if(def!=null){
				isNull=def.optBoolean("isnull", false);
				defaultValue= def.opt("default");
				col=def.getInt("col");
				isDefined=true;
			}else{
				isNull=true;
				col=-1;
				defaultValue=null;
				isDefined=false;
			}
		}
	}
	private ColumnDef pdtCol, skuQtyCol, errCol, colorCol/*多色模式下的色的位置*/; 
	/**
	 * 当使用POI处理excel的时候，遇到了比较长的数字，虽然excel里面设置该单元格是文本类型的，但是POI的cell的类型就会变成数字类型
	 * 而且无论数字是否小数，使用cell.getNumbericCellValue() 去获取值的时候，会得到一个double，而且当长度大一点的时候会变成科学计数法形式
	 * 那么获取这个单元格的原始的数据，就其实是一个double怎么转换成整数的问题了
	 * 使用DecimalFormat对这个double进行了格式话，随后使用format方法获得的String就是你想要的值了
	 */
	private DecimalFormat df = new DecimalFormat("0");  //格式化cell的numeric

	private Sheet sheet;
	private CellStyle errorRowStyle,errorCellStyle,successRowStyle;
	
	private ProductMatrixLoader loader;
	private Jedis jedis;
	private Connection conn;
	com.agilecontrol.b2b.schema.Table pdtTable;
	private UserObj usr;
	
	private QueryEngine engine;
	
	private PreparedStatement pstmt_cart_insert= null;
	private PreparedStatement pstmt_cart_update= null;
	/**
	 * 查询商品的id
	 */
	private PreparedStatement pstmt_pdt_id= null;
	/**
	输入的商品名称是否可以匹配2次（ak2）如果是自定义的查询，则只能匹配一次
	 */
	private boolean pdtHashAK2=false;
	/**
	 * 启动和完成时需要执行的存储过程
	 */
	private String startProc=null,endProc=null;
	/**
	 * Excel 读取的起始行，从0开始计算
	 */
	private int startRow=1;
	private String fileName;//file name without xls
	
	
	private Locale locale;
	
	public ImportExcelWorker(){
	}
	
	/**
	 * 打开Excel, 读取首行订货会+用户，删除订单明细，没有订单需要创建
	 * @throws Exception
	 */
	protected void initOrder() throws Exception{
		this.prepareImport();

		//m_product_id,m_attributesetinstance_id,actid, qty	
		pstmt_cart_insert = conn.prepareStatement("insert into b_cart i(id, m_product_id, m_attributesetinstance_id, " +
				"user_id, ownerid, modifierid, creationdate,modifieddate,isactive,B_PRMT_ID,qty,b_market_id,ad_client_id)"
				+ " values(get_sequences('b_cart'),?, ?,"+usr.getId()+","+usr.getId()+","+usr.getId()+",sysdate,sysdate,'Y',?,?,"+usr.getMarketId()+","+ ClientManager.getInstance().getDefaultClientId()+")");
		//qty,m_product_id,m_attributesetinstance_id,actid,actid
		pstmt_cart_update = conn.prepareStatement(
				"update b_cart i set qty=?, price_list=null, price=null,modifieddate=sysdate where i.user_id="+usr.getId()+" and i.m_product_id=? and i.m_attributesetinstance_id=? and b_market_id="+usr.getMarketId()+" and（B_PRMT_ID=? or (B_PRMT_ID is null and -1=?) ）");
		
		//默认的sql语句参数: market_id, $input(ak), $input(ak2)
		Table table=TableManager.getInstance().getTable("m_product");
		StringBuilder defaultPdtSQL=new StringBuilder("select p.id from m_product p, b_mk_pdt m where m.isactive='Y' and m.m_product_id=p.id and m.b_market_id=? and p.isactive='Y' and (p."+table.getAlternateKey().getName()+"=?");
		if(table.getAlternateKey2()!=null){
			defaultPdtSQL.append(" or p.").append(table.getAlternateKey2().getName()).append("=?");
			pdtHashAK2=true;
		}
		defaultPdtSQL.append(")");
		
		String pdtsql=PhoneController.getInstance().getValueFromADSQL("cart_impxls_pdtid",conn);
		if(Validator.isNull(pdtsql)) pdtsql=defaultPdtSQL.toString();
		else{
			pdtHashAK2=false;// cart_impxls_pdtid 不支持ak2
		}
		//market_id,$input(ak), $input(ak2=true)
		pstmt_pdt_id=conn.prepareStatement(pdtsql);
		this.pstmt_pdt_id.setInt(1, usr.getMarketId());
	}
	
	
	protected CellStyle createSuccessCellStyle(Workbook wb){
        CellStyle style = wb.createCellStyle();
        // Create a new font and alter it.
        Font font = wb.createFont();
        font.setFontHeightInPoints((short)10);
        font.setFontName("宋体");
        font.setColor((short) IndexedColors.BLACK.getIndex());
        // Fonts are set into a style so create a new one to use.
        style.setFont(font);
        
        //style.setFillBackgroundColor((short) IndexedColors.RED.getIndex());
        return style;
    }
	protected CellStyle createErrorCellStyle(Workbook wb){
        CellStyle style = wb.createCellStyle();
        // Create a new font and alter it.
        Font font = wb.createFont();
        font.setFontHeightInPoints((short)10);
        font.setFontName("宋体");
        font.setColor((short) IndexedColors.RED.getIndex());
        // Fonts are set into a style so create a new one to use.
        style.setFont(font);
        
        //style.setFillBackgroundColor((short) IndexedColors.RED.getIndex());
        return style;
    }
	protected CellStyle createErrorRowStyle(Workbook wb){
		
		return createErrorCellStyle(wb);
    }
	/**
	 * Write to errColumn, mark all line to read
	 * @param row
	 * @param err
	 * @throws Exception
	 */
	protected void writeError(Row row, String err) throws Exception{
		addError(row.getRowNum()+1, err);
		
		Cell cell=row.getCell((int)errCol.col,org.apache.poi.ss.usermodel.Row.CREATE_NULL_AS_BLANK);
		if(cell==null){
			cell=row.createCell(errCol.col);
		}
		cell.setCellValue( err);
        cell.setCellStyle(errorCellStyle);
        //whole row red
        for(int i=0;i< errCol.col;i++){
        	cell=row.getCell(i);
        	if(cell!=null)cell.setCellStyle(this.errorRowStyle);
        }
		
	}
	protected void writeSuccess(Row row) throws Exception{
        //whole row red
        for(int i=0;i<= errCol.col;i++){
        	Cell cell=row.getCell(i);
        	if(cell!=null)cell.setCellStyle(this.successRowStyle);
        }
        Cell cell=row.getCell((int)errCol.col);
        if(cell!=null)cell.setCellValue("");
	}
	
	/**
	 * parse value of cell to datenumber (8)
	 * @param cell
	 * @return -1 if not found a valid date cell
	 * @throws Exception
	 */
	protected int parseToDate(Cell cell) throws Exception{
		int dateInt=-1;
		int type = cell.getCellType();
		switch (type){
			
			case Cell.CELL_TYPE_NUMERIC:
				CellStyle style = cell.getCellStyle();
				if (DateUtil.isCellDateFormatted(cell)){
					double numericValue = cell.getNumericCellValue();
					Date date = DateUtil.getJavaDate(numericValue);
					long tzOffset = TimeZone.getDefault().getOffset(date.getTime());
					date = new Date(date.getTime() + tzOffset);
					dateInt=  Tools.getInt(((java.text.SimpleDateFormat)QueryUtils.dateNumberFormatter.get()).format( date), -1);
					
//					logger.debug("Parsing df date " + numericValue+ " to "+ dateInt);
				}else{
					double numericValue = cell.getNumericCellValue();
					dateInt= (int)numericValue;
//					logger.debug("Parsing num date " + cell.getNumericCellValue()+ " to "+ dateInt);
					
				}
				break;
			case Cell.CELL_TYPE_STRING:
				dateInt= Tools.getInt(cell.getRichStringCellValue().getString(),-1);
//				logger.debug("Parsing str date " +cell.getRichStringCellValue().getString()+ " to "+ dateInt);
				break;
			default:
				dateInt= -1;
//				logger.debug("unknown format " +type+ " of date cell");
				
		}
		
		return dateInt;
				
	}
	
	private String getString(Cell cell){
		String s;
		if(cell.getCellType()==Cell.CELL_TYPE_STRING){
			s= String.valueOf(cell.getStringCellValue());
		}else if(cell.getCellType()==Cell.CELL_TYPE_NUMERIC){
			s= df.format( cell.getNumericCellValue());
		}else{
			s=null;
		}
		return s;
	}
	
	/**
	 * 校验每行，目前不支持活动(actid)导入
	 * @param row
	 * @throws Exception
	 */
	protected void handleRow(Row row) throws Exception{
		Cell cell;
		
		int pdtId;
		cell= row.getCell(pdtCol.col);
		String pdt= getString(cell);
		if(Validator.isNull(pdt)) throw new NDSException("@Article-No-null@");
		pdt=pdt.trim();
		pdtId = findProduct(pdt);
		if(pdtId <0) throw new NDSException("@Article-No-unexist@:"+pdt);
		
		String colorCode=null;
		if(this.colorCol!=null){
			//多色，即款模式
			cell=row.getCell(colorCol.col);
			colorCode=getString(cell);;
		}
		
		ProductMatrix matrix=loader.getSimpleProductMatrix(pdtId);//多色
		//生成sizeFactors
		ArrayList<Integer> sizeFactors=matrix.getSizeFactors();
		JSONObject redisPdt=PhoneUtils.fetchObjectAllColumns(pdtTable, pdtId, conn, jedis);//需要拿到packqty，mask=00
		/*
		 * 这是木槿项目开始的下单倍数，比如单价是按支定义的，界面上中包规格是14，则下单1即为14，2就是28，数据库存储仍然按支数
		 */
		int packQty=redisPdt.optInt("packqty", 1);
		if(packQty<=0)packQty=1;
		
		int colorPdtId=-1;
		ArrayList<Color> colors=matrix.getColors();
		JSONArray asis=null; //有效的asi，Integer[],  null 表示没有asi
		if(Validator.isNotNull(colorCode)){
			boolean found=false;
			for(int i=0;i<colors.size();i++){
				Color color=colors.get(i);
				if(color.getCode().equals(colorCode)){
					found=true;
					colorPdtId=matrix.getProductIds().get(i);
					asis=matrix.getASIArrays().getJSONArray(i);
					break;
				}
			}
			if(!found) throw new NDSException("Error:"+ colorCode+",colors="+ Tools.toString(colors, ","));
		}else{
			if(matrix.getColors().size()!=1) throw new NDSException("Need colorcode,需要颜色代码");
			colorPdtId=matrix.getProductIds().get(0);
			asis=matrix.getASIArrays().getJSONArray(0);
			
		}
		for(int i=0;i<asis.length();i++){
			int asiId= asis.optInt(i,-1);
			double asiQty=0;
			cell=row.getCell( skuQtyCol.col+i);
			if(cell!=null) asiQty= cell.getNumericCellValue();
			if(asiId==-1){
				//asi not exists, that's normal, 2014.11.8 需要禁止输入
				if(asiQty!=0) throw new NDSException(pdt+" @size-position@:"+ (i+1)+", @not-allow-input@, @your-input@:"+ asiQty);//pdt+"在第"+(i+1)+"个尺码位不允许下量，只能写0或留空，当前输入了"+asiQty+"");
			}else{
				if(asiQty>=0){
					if(colorPdtId!= pdtId) throw new NDSException("意外:"+colorCode+"对应的商品id不是当前商品");
					int sizeFactor= sizeFactors.get(i);
					savePdtQty( colorPdtId, asiId, asiQty*packQty*sizeFactor);
				}
				else if(asiQty<0) throw new NDSException("@size-number-limit@");
			}
		}
			
		
		
	}
	/**
	 * @param pdtId
	 * @param asiId
	 * @param qty 这是Excel里输入的数量x 包装规格（木槿） x 尺码系数（晨光）
	 * @throws Exception
	 */
	protected void savePdtQty( int pdtId, int asiId, double qty) throws Exception{
		
		//insert : m_product_id,m_attributesetinstance_id,actid, qty	
		//update : qty,m_product_id,m_attributesetinstance_id,actid,actid

		pstmt_cart_update.setDouble(1, qty);
		pstmt_cart_update.setInt(2, pdtId);
		pstmt_cart_update.setInt(3, asiId);
		pstmt_cart_update.setInt(4,-1);
		pstmt_cart_update.setInt(5, -1);
		if(pstmt_cart_update.executeUpdate()==0){
			//insert
			pstmt_cart_insert.setInt(1, pdtId);
			pstmt_cart_insert.setInt(2, asiId);
			pstmt_cart_insert.setNull(3,  java.sql.Types.NUMERIC);
			pstmt_cart_insert.setDouble(4, qty);
			pstmt_cart_insert.executeUpdate();
		}

	}	
	/**
	 * 要求必须是>0
	 * @param qty
	 * @throws Exception
	 */
	protected void checkQtyValid(int qty) throws Exception{
		if(qty<0)throw new NDSException("@not-allow-negative@"+":"+ qty);
	}

	/**
	 * Find product by name
	 * @param name
	 * @return -1 if not found
	 * @throws Exception
	 */
	protected int findProduct(String name) throws Exception{
		
		this.pstmt_pdt_id.setString(2, name);
		if(this.pdtHashAK2)this.pstmt_pdt_id.setString(3, name);
		ResultSet rs=null;
		try{
			rs=pstmt_pdt_id.executeQuery();
			if(rs.next()){
				return rs.getInt(1);
			}
		}finally{
			if(rs!=null)rs.close();
		}
		return -1;
	}

	/**
	 * 如果前面都正确，将执行最后的存储过程
	 * @throws Exception
	 */
	protected void finishImport()throws Exception{
		if(Validator.isNull(this.endProc)) return ;
		ArrayList params=new ArrayList();
		params.add(usr.getId());
		engine.executeStoredProcedure(endProc,params , false, conn);
		
	}
	/**
	 * 准备开始导入，需要识别并执行存储过程startProc
	 * @throws Exception
	 */
	protected void prepareImport()throws Exception{
		if(Validator.isNull(this.startProc)) return ;
		ArrayList params=new ArrayList();
		params.add(usr.getId());
		engine.executeStoredProcedure(startProc,params , false, conn);
		
	}
	
	
	protected void closeAll() {
		try{
			if( this.pstmt_pdt_id!=null && !pstmt_pdt_id.isClosed()) try{pstmt_pdt_id.close();}catch(Throwable t){};
			if( this.pstmt_cart_insert!=null && !pstmt_cart_insert.isClosed()) try{pstmt_cart_insert.close();}catch(Throwable t){};
			if( this.pstmt_cart_update!=null && !pstmt_cart_update.isClosed()) try{pstmt_cart_update.close();}catch(Throwable t){};
			
		}catch(Throwable tx){
			logger.error("Fail to close", tx);
		}
	}
	
	private ArrayList<String> errorLines=new ArrayList<String>();
	public String getErrorsHTML(){
		StringBuilder sb=new StringBuilder();
		sb.append("<b>"+"@handled-total-of@").append( getTotalRows()).append("@handled-middle-msg@"+ errorLines.size()+"@datas-error@"+"：").append("</b><br/>");
		for(String line: errorLines)
			sb.append( StringUtils.escapeHTMLTags(line)).append("<br/>");
		File f=new File(fullFilePath);
		
		try{
		sb.append(MessagesHolder.getInstance().getMessage(locale, "download")+"<a href='/servlets/binserv/Download?filename="+
        		java.net.URLEncoder.encode(f.getName(),"UTF-8")+"'>"+"@containing-error-message@"+f.getName()+"</a>");
		}catch(Throwable tx){}
		return MessagesHolder.getInstance().translateMessage(sb.toString(),locale);
	}
	/**
	 * 
	 * @param lineNo start from normal user think
	 * @param errorMsg
	 */
	private void addError(int lineNo, String errorMsg){
		errorLines.add("@the-first-prompt@"+ lineNo+"@lines@"+":"+ errorMsg);
	}
	private int totolRowCount=0;
	
	public int getTotalRows(){
		return totolRowCount;
	}
	private String fullFilePath;
	/**
	 * 
	 * @param file absolute file name
	 * @return true if error found
	 * @throws Exception
	 */
	public boolean handle(String file) throws Exception{
		File f=new File(file);
		this.fullFilePath=file;
		this.fileName= f.getName().substring(0, f.getName().lastIndexOf(".")).trim();
		
		boolean errFound=false;
		InputStream inp = new FileInputStream(file);
	    Workbook wb = WorkbookFactory.create(inp);
	    sheet = wb.getSheetAt(0);
	    errorRowStyle=this.createErrorRowStyle(wb);
	    errorCellStyle=this.createErrorCellStyle(wb);
	    this.successRowStyle=createSuccessCellStyle(wb);
	    try{
		    initOrder();
		    int maxRow=sheet.getLastRowNum();
		    logger.debug("last row num:"+ maxRow);
		    for(int i=1;i<=maxRow;i++){
		    	Row row = sheet.getRow(i);
		    	try{
		    		totolRowCount++;
			    	handleRow(row);
			    	writeSuccess(row);
			    }catch(Throwable tx){
			    	logger.error("Fail to handle row "+ i+" of "+ file, tx);
			    	errFound=true;
			    	String errmsg=WebUtils.getExceptionMessage(tx, locale);
			    	writeError(row,errmsg );
			    }
		    }
		    finishImport();
		    if(errFound){
			    //write back
			    FileOutputStream fileOut = new FileOutputStream(file);
			    wb.write(fileOut);
			    fileOut.close();
		    }       
		    try{inp.close();}catch(Throwable tx){}
		    logger.debug("Handle result for "+file+":"+ (errFound?"fail":"success"));
	    }finally{
	    	closeAll();
	    }
	    return errFound;
	}
	/**
	 * 
	 * @param config {
   columns:{ pdt:{col:1},skuqty:{col:5},err:{col:14} },
   startrow: 1,
   end_proc: proc1,
   start_proc: proc2
}
	 * @param conn
	 * @param owner
	 * @throws Exception
	 */
	public void init(JSONObject config, Connection conn, UserObj owner, Jedis jedis) throws Exception{
		this.conn=conn;
		this.usr=owner;
		this.jedis=jedis;
		loader=new ProductMatrixLoader(jedis,conn);
		pdtTable=com.agilecontrol.b2b.schema.TableManager.getInstance().getTable("pdt");		

		this.startProc=config.optString("start_proc");
		this.endProc=config.optString("end_proc");
		this.startRow=config.optInt("startrow", 1);
		if(startRow<0) throw new NDSException("定义错误，startrow需要>=0");
		engine=QueryEngine.getInstance();
		
		JSONObject columns=config.getJSONObject("columns");
		JSONObject jo=columns.getJSONObject("pdt");
		pdtCol= new ColumnDef("pdt", jo);
		
		jo=columns.getJSONObject("skuqty");
		skuQtyCol= new ColumnDef("skuqty", jo);
		
		jo=columns.getJSONObject("err");
		errCol= new ColumnDef("err", jo);

		jo=columns.optJSONObject("color");
		if(jo!=null)colorCol= new ColumnDef("color", jo);
		
		locale= LanguageManager.getInstance().getLocale(usr.getLangId());
		
	}
	
	
}
