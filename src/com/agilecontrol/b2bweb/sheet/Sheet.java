package com.agilecontrol.b2bweb.sheet;

import java.util.*;

import org.json.*;

import com.agilecontrol.phone.Admin;

/**
 * Sheet for matrix, ��������ģ�壬����ȶ����ɻ��棬�����ͻ��������ݡ�
 * @author yfzhu
 *
 */
@Admin(mail="yfzhu@lifecycle.cn")
public class Sheet {
	private int rows, cols, fixedCols=1/*Ĭ�ϵĹ̶���*/,fixedRows = 1/*Ĭ�ϵĹ̶���*/;
	private HashSet<Cell> cells;
	private JSONArray  asserts;//elements: {r,err}, r for rule, err for message
	
	public Sheet(){
		cells=new HashSet();
		asserts=new JSONArray();
	}
	
	public void addCell(Cell cell){
		cells.add(cell);
	}
	
	public void setRowHeights(JSONArray heights){
		this.rowHeights=heights;
	}
	public void setColumnWidths(JSONArray widths){
		this.colWidths=widths;
	}
	/**
	 * �̶������������ǰ2����Ҫ�̶���������2
	 * @param cnt 
	 */
	public void setFixedColumns(int cnt){
		this.fixedCols=cnt;
	}
	/**
	 * �̶������������ǰ2����Ҫ�̶���������2*/
	public void setFixedRows(int cnt){
		this.fixedRows = cnt;
	}
	private JSONArray rowHeights,colWidths;
	/**
	 * {
rows:11,
cols:10,
cells:  { "x:y": { t:"f", f:"c(3,1)+c(3,2))",  d:"#0.0%",  k:"1434"}, "x2:y2:":{}},
assert:"c(13,2)==c(14,2)",
}
	 * @return
	 * @throws Exception
	 */
	public JSONObject toJSONObject() throws Exception{
		JSONObject jo=new JSONObject();
		jo.put("rows",rows);
		jo.put("cols", cols);
		if(this.fixedCols>1 || this.fixedRows > 1) jo.put("lt", "c("+fixedRows+","+ fixedCols+")");
		
		if(rowHeights!=null){
			jo.put("rowsz", rowHeights);
		}
		if(colWidths!=null){
			jo.put("colsz", colWidths);
		}
		JSONObject cs=new JSONObject();
		for(Cell cell: cells){
			cs.put(cell.getCellIndex(), cell.getCellObject());
		}
		jo.put("cells", cs);
		jo.put("asserts", asserts);
		
		return jo;
	}

	public int getRows() {
		return rows;
	}

	public void setRows(int rows) {
		this.rows = rows;
	}

	public int getCols() {
		return cols;
	}

	public void setCols(int cols) {
		this.cols = cols;
	}

	public void addAssert(String assertString, String error) throws Exception{
		JSONObject ast=new JSONObject();
		ast.put("r", assertString);
		ast.put("err", error);
		asserts.put(ast);
		
	}
}
