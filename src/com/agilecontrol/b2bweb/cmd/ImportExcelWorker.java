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
 * �����ϴ���xls �ļ�
 * ��ʽ������	 ��Ʒ������Ϣ�ֶ�..., ����1,2,3
 * ���裺
 * ��ȡ����: �ؼ��ֶ�: ���ţ�����λ��
 * {
 *   columns:{ pdt:{col:1},skuqty:{col:5},err:{col:14}, value1:{col:x} },
 *   startrow: 1,
 *   end_proc: proc1,
 *   start_proc: proc2
 *   
 * }
 * ��Excel, ��ȡ����
 * ���д���:
 *  ��ȡÿ�У�����ÿ���ֶΣ�����ÿ���ֶ����ݣ����д���, ���б�עΪ��ɫ������������Ϣд��errָ������
 * ��ɺ�:
 *  ����ִ�д洢���� end_proc 
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
		boolean isDefined;//�Ƿ��ж�������ֶ�
		Object fixedValue;//�����û��ֶΣ������Ǹ��㶨ֵ���ڴ����ã��Ա���
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
	private ColumnDef pdtCol, skuQtyCol, errCol, colorCol/*��ɫģʽ�µ�ɫ��λ��*/; 
	/**
	 * ��ʹ��POI����excel��ʱ�������˱Ƚϳ������֣���Ȼexcel�������øõ�Ԫ�����ı����͵ģ�����POI��cell�����;ͻ�����������
	 * �������������Ƿ�С����ʹ��cell.getNumbericCellValue() ȥ��ȡֵ��ʱ�򣬻�õ�һ��double�����ҵ����ȴ�һ���ʱ����ɿ�ѧ��������ʽ
	 * ��ô��ȡ�����Ԫ���ԭʼ�����ݣ�����ʵ��һ��double��ôת����������������
	 * ʹ��DecimalFormat�����double�����˸�ʽ�������ʹ��format������õ�String��������Ҫ��ֵ��
	 */
	private DecimalFormat df = new DecimalFormat("0");  //��ʽ��cell��numeric

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
	 * ��ѯ��Ʒ��id
	 */
	private PreparedStatement pstmt_pdt_id= null;
	/**
	�������Ʒ�����Ƿ����ƥ��2�Σ�ak2��������Զ���Ĳ�ѯ����ֻ��ƥ��һ��
	 */
	private boolean pdtHashAK2=false;
	/**
	 * ���������ʱ��Ҫִ�еĴ洢����
	 */
	private String startProc=null,endProc=null;
	/**
	 * Excel ��ȡ����ʼ�У���0��ʼ����
	 */
	private int startRow=1;
	private String fileName;//file name without xls
	
	
	private Locale locale;
	
	public ImportExcelWorker(){
	}
	
	/**
	 * ��Excel, ��ȡ���ж�����+�û���ɾ��������ϸ��û�ж�����Ҫ����
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
				"update b_cart i set qty=?, price_list=null, price=null,modifieddate=sysdate where i.user_id="+usr.getId()+" and i.m_product_id=? and i.m_attributesetinstance_id=? and b_market_id="+usr.getMarketId()+" and��B_PRMT_ID=? or (B_PRMT_ID is null and -1=?) ��");
		
		//Ĭ�ϵ�sql������: market_id, $input(ak), $input(ak2)
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
			pdtHashAK2=false;// cart_impxls_pdtid ��֧��ak2
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
        font.setFontName("����");
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
        font.setFontName("����");
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
	 * У��ÿ�У�Ŀǰ��֧�ֻ(actid)����
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
			//��ɫ������ģʽ
			cell=row.getCell(colorCol.col);
			colorCode=getString(cell);;
		}
		
		ProductMatrix matrix=loader.getSimpleProductMatrix(pdtId);//��ɫ
		//����sizeFactors
		ArrayList<Integer> sizeFactors=matrix.getSizeFactors();
		JSONObject redisPdt=PhoneUtils.fetchObjectAllColumns(pdtTable, pdtId, conn, jedis);//��Ҫ�õ�packqty��mask=00
		/*
		 * ����ľ����Ŀ��ʼ���µ����������絥���ǰ�֧����ģ��������а������14�����µ�1��Ϊ14��2����28�����ݿ�洢��Ȼ��֧��
		 */
		int packQty=redisPdt.optInt("packqty", 1);
		if(packQty<=0)packQty=1;
		
		int colorPdtId=-1;
		ArrayList<Color> colors=matrix.getColors();
		JSONArray asis=null; //��Ч��asi��Integer[],  null ��ʾû��asi
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
			if(matrix.getColors().size()!=1) throw new NDSException("Need colorcode,��Ҫ��ɫ����");
			colorPdtId=matrix.getProductIds().get(0);
			asis=matrix.getASIArrays().getJSONArray(0);
			
		}
		for(int i=0;i<asis.length();i++){
			int asiId= asis.optInt(i,-1);
			double asiQty=0;
			cell=row.getCell( skuQtyCol.col+i);
			if(cell!=null) asiQty= cell.getNumericCellValue();
			if(asiId==-1){
				//asi not exists, that's normal, 2014.11.8 ��Ҫ��ֹ����
				if(asiQty!=0) throw new NDSException(pdt+" @size-position@:"+ (i+1)+", @not-allow-input@, @your-input@:"+ asiQty);//pdt+"�ڵ�"+(i+1)+"������λ������������ֻ��д0�����գ���ǰ������"+asiQty+"");
			}else{
				if(asiQty>=0){
					if(colorPdtId!= pdtId) throw new NDSException("����:"+colorCode+"��Ӧ����Ʒid���ǵ�ǰ��Ʒ");
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
	 * @param qty ����Excel�����������x ��װ���ľ�ȣ� x ����ϵ�������⣩
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
	 * Ҫ�������>0
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
	 * ���ǰ�涼��ȷ����ִ�����Ĵ洢����
	 * @throws Exception
	 */
	protected void finishImport()throws Exception{
		if(Validator.isNull(this.endProc)) return ;
		ArrayList params=new ArrayList();
		params.add(usr.getId());
		engine.executeStoredProcedure(endProc,params , false, conn);
		
	}
	/**
	 * ׼����ʼ���룬��Ҫʶ��ִ�д洢����startProc
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
		sb.append("<b>"+"@handled-total-of@").append( getTotalRows()).append("@handled-middle-msg@"+ errorLines.size()+"@datas-error@"+"��").append("</b><br/>");
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
		if(startRow<0) throw new NDSException("�������startrow��Ҫ>=0");
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
