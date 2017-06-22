package com.agilecontrol.b2bweb.sheet;

import org.json.JSONObject;

import com.agilecontrol.phone.Admin;


/**
 * For matrix definition and handling
 * 
 * @author yfzhu
 *
 */
@Admin(mail="yfzhu@lifecycle.cn")
public class Cell {
	//2位小数
	public final static Format FORMAT_DOUBLE=new Format(0, "#0.00");
	public final static Format FORMAT_PERCENT=new Format(1, "#0.00%");
	//4位小数
	public final static Format FORMAT_DOUBLE4=new Format(2, "#0.0000");
	
	public final static String TYPE_FOMULA="f";
	public final static String TYPE_INTEGER="i";
	public final static String TYPE_DOUBLE="d";
	public final static String TYPE_STRING="s";
	public final static String TYPE_CHECK="c";// 意思是checkmark，显示效果为（空白／打勾），数值为（0／1）
	public final static String TYPE_ESTRING="t";
	
	//"x:y": { t:"f", f:"cell(3,1)+cell(3,2))"
	private int row,col;
	/**
	 * f - formula
	 * i - int
	 * d - double
	 * v - view only
	 */
	private String type;
	
	private String key;
	
	private Format format;
	
	private String formula;
	
	private Object value;
	
	private Boolean editable;
	
	static class Format{
		
		private String formatString;
		private int key;
		
		public Format(int key, String fmt ){
			this.key=key;
			this.formatString=fmt;
		}
		public int key(){
			return key;
		}
		
		public String toString(){
			return formatString;
		}
		
	}
	
	/**
	 * 公式类型、固定值类型
	 * @param row
	 * @param col
	 * @param type
	 * @param formula
	 * @param value 默认值
	 */
	public Cell(int row, int col, String type, String formula,Object value){
		this.row=row;
		this.col=col;
		this.type=type;
		this.formula=formula;
		this.value=value;
	}
	/**
	 * 输入且上传类型
	 * @param row
	 * @param col
	 * @param type
	 * @param key
	 */
	public Cell(int row, int col, String type, String key){
		this.row=row;
		this.col=col;
		this.type=type;
		this.key=key;
	}
	/**
	 * Double input cell, allow edit/upload
	 * @param row
	 * @param col
	 * @param key
	 * @return
	 */
	public static Cell createDoubleInput(int row, int col, String key){
		return (new Cell(row,col,Cell.TYPE_DOUBLE, key));
	}
	/**
	 * Integer input cell, allow edit/upload
	 * @param row
	 * @param col
	 * @param key
	 * @return
	 */
	public static Cell createIntegerInput(int row, int col, String key){
		return (new Cell(row,col,Cell.TYPE_INTEGER, key));
	}
	/**
	 * 可以打钩
	 * @param row
	 * @param col
	 * @param key
	 * @return
	 */
	public static Cell createCheckInput(int row, int col, String key){
		return (new Cell(row,col,Cell.TYPE_CHECK, key));
	}
	/**
	 * Formula cell and disable input 
	 * @param row
	 * @param col
	 * @param formula
	 * @return
	 */
	public static Cell createFomula(int row, int col, String formula){
		return (new Cell(row,col,Cell.TYPE_FOMULA, formula, null)).disable();
	}
	/**
	 * Ratio % formula cell and disable input 
	 * @param row
	 * @param col
	 * @param formula
	 * @return
	 */
	public static Cell createRatioFormula(int row, int col, String formula){
		Cell cell= (new Cell(row,col,Cell.TYPE_FOMULA,formula, null)).disable();
		cell.setFormat(Cell.FORMAT_PERCENT);
		return cell;
	}
	/**
	 * Double cell in format of ratio
	 * @param row
	 * @param col
	 * @param key
	 * @return
	 */
	public static Cell createRatioKey(int row, int col, String key){
		Cell cell= (new Cell(row,col,Cell.TYPE_DOUBLE, key)).disable();
		cell.setFormat(Cell.FORMAT_PERCENT);
		return cell;
	}
	
	/**
	 * Integer cell
	 * @param row
	 * @param col
	 * @param key
	 * @return
	 */
	public static Cell createIntegerKey(int row, int col, String key){
		Cell cell= (new Cell(row,col,Cell.TYPE_INTEGER, key)).disable();
		return cell;
	}
	

	/**
	 * Double cell in format 
	 * @param row
	 * @param col
	 * @param key
	 * @return
	 */
	public static Cell createDoubleKey(int row, int col, String key){
		Cell cell= (new Cell(row,col,Cell.TYPE_DOUBLE, key)).disable();
		return cell;
	}

	/**
	 * String cell and disable input
	 * @param row
	 * @param col
	 * @param text
	 * @return
	 */
	public static Cell createText(int row, int col, String text){
		return (new Cell(row,col,Cell.TYPE_STRING,null, text)).disable();
	}
	
	public static Cell createStringInput(int row , int col ,String key)
	{
		return (new Cell(row,col,Cell.TYPE_ESTRING, key));
	}

	
	public int getRow() {
		return row;
	}

	public Cell setRow(int row) {
		this.row = row;
		return this;
	}

	public int getCol() {
		return col;
	}

	public Cell setCol(int col) {
		this.col = col;
		return this;
	}

	public String getType() {
		return type;
	}

	public Cell setType(String type) {
		this.type = type;
		return this;
	}
	
	public Cell disable(){
		this.editable=Boolean.FALSE;
		return this;
	}
	/**
	 * key will be used for uploading
	 * @return
	 */
	public String getKey() {
		return key;
	}
	/**
	 * key will be used for uploading
	 * @param key
	 */
	public Cell setKey(String key) {
		this.key = key;
		return this;
	}

	public Format getFormat() {
		return format;
	}

	public Cell setFormat(Format format) {
		this.format = format;
		return this;
	}
	/**
	 * Index in sheet object
	 * @return cell.row+":"+cell.col
	 */
	public String getCellIndex(){
		return row+":"+col;
	}
	/**
	 * Object in sheet object
	 * @return  { t:"f", f:"cell(3,1)+cell(3,2))",  d:"#0.0%",  k:"1434", e:false}
	 */
	public JSONObject getCellObject() throws Exception{
		JSONObject jo=new JSONObject();
		jo.put("t", type);
		if(format!=null)jo.put("d", format);
		if(key!=null) jo.put("k",key);
		if(formula!=null)jo.put("f", formula);
		if(value!=null)jo.put("v", value);
		if(editable!=null)jo.put("e", editable.booleanValue());
		return jo;
	}
	/**
	 * Default value of the cell
	 * @return
	 */
	public Object getValue() {
		return value;
	}
	
	public Cell setValue(Object value) {
		this.value = value;
		return this;
	}
}
