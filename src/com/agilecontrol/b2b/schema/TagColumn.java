package com.agilecontrol.b2b.schema;

/**
 * ������չ����
 * 
���exprs ����tag���͵��ֶΣ��ҵ�tag����Ҫ���ж�ѡƥ��, �ͻ��˴�������id array 
		  ʲô��tag�����ֶ�:  pdt���ptag��pdt.tag �ֶε���չ����: 
		  
		  {tagtable: { tb:"ptag", tag_column:"tag_id", main_column:"pdt_id", storetb:"pdt_tag" }}
		  
		  ����: tb: tag��ϵ��tag_column: tag��������ƥ��ͻ��˴�����id array���ֶΣ�main_column��tag������������������ֶ�
		  storetb: �����洢tag����ı�
		  tb ��ı�׼��ʽ: [id, pdt_id, tag_id], storetb�ı�׼��ʽ: [id,name,com_id]

 * @author yfzhu
 *
 */
public class TagColumn extends Column{
	
	private String tagTable;
	private String tagColumn;
	private String tagMainColumn;
	private String tagStoreTable;
	/**
	 * ��ϵ������ ptag, stag 
	 * @return the tagTable
	 */
	public String getTagTable() {
		return tagTable;
	}
	/**
	 * ��ϵ������ ptag, stag 
	 * @param tagTable the tagTable to set
	 */
	public void setTagTable(String tagTable) {
		this.tagTable = tagTable;
	}
	/**
	 * ָ��洢tag�ı���ֶΣ������ڹ�ϵ���ϵ��ֶΣ�����: tag_id
	 * @return the tagColumn
	 */
	public String getTagColumn() {
		return tagColumn;
	}
	/**
	 * ָ��洢tag�ı���ֶΣ������ڹ�ϵ���ϵ��ֶΣ�����: tag_id
	 * @param tagColumn the tagColumn to set
	 */
	public void setTagColumn(String tagColumn) {
		this.tagColumn = tagColumn;
	}
	/**
	 * @return �ڹ�ϵ����ָ��������ֶΣ����� pdt_id
	 */
	public String getTagMainColumn() {
		return tagMainColumn;
	}
	/**
	 * @param �ڹ�ϵ����ָ��������ֶΣ����� pdt_id
	 */
	public void setTagMainColumn(String mainTableColumn) {
		this.tagMainColumn = mainTableColumn;
	}
	/**
	 * 
	 * @return �洢��ǩ�ı���������pdt_tag
	 */
	public String getTagStoreTable() {
		return tagStoreTable;
	}
	/**
	 * �洢��ǩ�ı���������pdt_tag
	 * @param storeTable the storeTable to set
	 */
	public void setTagStoreTable(String storeTable) {
		this.tagStoreTable = storeTable;
	}
	
}
