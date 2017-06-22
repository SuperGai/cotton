package com.agilecontrol.b2b.schema;

/**
 * 具有扩展属性
 * 
如果exprs 里有tag类型的字段，找到tag表，需要进行多选匹配, 客户端传来的是id array 
		  什么是tag类型字段:  pdt表和ptag表，pdt.tag 字段的扩展定义: 
		  
		  {tagtable: { tb:"ptag", tag_column:"tag_id", main_column:"pdt_id", storetb:"pdt_tag" }}
		  
		  其中: tb: tag关系表，tag_column: tag表上用来匹配客户端传来的id array的字段，main_column：tag表上用来关联主表的字段
		  storetb: 用来存储tag定义的表
		  tb 表的标准格式: [id, pdt_id, tag_id], storetb的标准格式: [id,name,com_id]

 * @author yfzhu
 *
 */
public class TagColumn extends Column{
	
	private String tagTable;
	private String tagColumn;
	private String tagMainColumn;
	private String tagStoreTable;
	/**
	 * 关系表，比如 ptag, stag 
	 * @return the tagTable
	 */
	public String getTagTable() {
		return tagTable;
	}
	/**
	 * 关系表，比如 ptag, stag 
	 * @param tagTable the tagTable to set
	 */
	public void setTagTable(String tagTable) {
		this.tagTable = tagTable;
	}
	/**
	 * 指向存储tag的表的字段，这是在关系表上的字段，比如: tag_id
	 * @return the tagColumn
	 */
	public String getTagColumn() {
		return tagColumn;
	}
	/**
	 * 指向存储tag的表的字段，这是在关系表上的字段，比如: tag_id
	 * @param tagColumn the tagColumn to set
	 */
	public void setTagColumn(String tagColumn) {
		this.tagColumn = tagColumn;
	}
	/**
	 * @return 在关系表上指向主表的字段，比如 pdt_id
	 */
	public String getTagMainColumn() {
		return tagMainColumn;
	}
	/**
	 * @param 在关系表上指向主表的字段，比如 pdt_id
	 */
	public void setTagMainColumn(String mainTableColumn) {
		this.tagMainColumn = mainTableColumn;
	}
	/**
	 * 
	 * @return 存储标签的表名，比如pdt_tag
	 */
	public String getTagStoreTable() {
		return tagStoreTable;
	}
	/**
	 * 存储标签的表名，比如pdt_tag
	 * @param storeTable the storeTable to set
	 */
	public void setTagStoreTable(String storeTable) {
		this.tagStoreTable = storeTable;
	}
	
}
