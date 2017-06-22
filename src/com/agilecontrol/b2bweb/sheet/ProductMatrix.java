package com.agilecontrol.b2bweb.sheet;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
{
	size: ['23', '24', '25','26', '27'],  - m_attibute.name
	sizenotes:[String] - note����m_attribute.description�ֶ� 
	sizefactors:[int]  -  ���ϵ����m_attribute.factor
	color: [ {n:'green' , s:[0,1,1,1,0], c:'01', n for name, s for sizes, c for code
			],
	pdtids:[13,24] ÿһ�ж�Ӧ��pdtid
	asis: [row][column] ÿ��Ԫ���Ƕ�Ӧ���е�asi, null ��ʾ��ǰcellû��asi
}
 */
public class ProductMatrix{
	JSONArray sizeNotes;//�����Ӣ�������� string
	JSONArray sizeFactors;//����ϵ����֧��������� int
	JSONArray sizes;
	JSONArray colors;
	JSONArray pdtIds;
	/**
	 * JSONAray matrix[][], elements are asi id (Integer) for that cell, null if cell has no asi, matrix is just the same as ui
	 */
	JSONArray asiArray;
	
	/**
	 * 
	 * @param compose {sizes, colors, pdtids, asis,sizenotes, sizefactors}
	 * sizeobjs - [string] - ����note����m_attribute.description�ֶ�
	 */
	public ProductMatrix(JSONObject compose){
		sizeNotes=compose.optJSONArray("sizenotes");
		sizeFactors=compose.optJSONArray("sizefactors");
		sizes=compose.optJSONArray("sizes");
		colors=compose.optJSONArray("colors");
		pdtIds=compose.optJSONArray("pdtids");
		asiArray=compose.optJSONArray("asis");
	}
	
	/**
	 * 
	 * @return [int] - note����m_attribute.factor�ֶΣ����ù��ϵ��
	 */
	public ArrayList<Integer> getSizeFactors(){
		ArrayList<Integer> ss=new ArrayList();
		for(int i=0;i<sizeFactors.length();i++) ss.add(sizeFactors.optInt(i,1));
		return ss;
	}
	/**
	 * 
	 * @return [String] - note����m_attribute.description�ֶΣ�����Ӣ������
	 */
	public ArrayList<String> getSizeNotes(){
		ArrayList<String> ss=new ArrayList();
		for(int i=0;i<sizeNotes.length();i++) ss.add(sizeNotes.optString(i));
		return ss;
	}
	/**
	 * �������ƣ�m_attributevalue.name ��Ӧ��Ʒ�ĳ�������
	 * @return ['23', '24', '25','26', '27']
	 */
	public ArrayList<String> getSizes(){
		ArrayList<String> ss=new ArrayList();
		for(int i=0;i<sizes.length();i++) ss.add(sizes.optString(i));
		return ss;
	}
	/**
	 * 
	 * @return {code,name, sizes}
	 */
	public ArrayList<Color> getColors(){
		ArrayList<Color> cs=new ArrayList();
		for(int i=0;i<colors.length();i++) {
			Color color=new Color(colors.optJSONObject(i));
			cs.add(color);
		}
		return cs;
	}
	/**
	 * 
	 * @return [{code,name, sizes}]
	 */
	public JSONArray getObjColors(){
		return colors;
	}
	/**
	 * 
	 * @return [][] ���а��е�asiֵ
	 * elements are asi id (Integer) for that cell, null if cell has no asi,
	 * 
	 */
	public JSONArray getASIArrays(){
		return asiArray;
	}
	/**
	 * ÿ�ж�Ӧ��pdtid
	 * @return
	 */
	public ArrayList<Integer> getProductIds(){
		ArrayList<Integer> ss=new ArrayList();
		for(int i=0;i<pdtIds.length();i++) ss.add(pdtIds.optInt(i));
		return ss;
	}
	
	public JSONObject toJSONObject() throws JSONException {
		JSONObject jo=new JSONObject();
		jo.put("sizes", sizes);
		jo.put("colors", colors);
		jo.put("pdtids", pdtIds);
		jo.put("asis", asiArray);
		jo.put("sizenotes", sizeNotes);
		jo.put("sizefactors", sizeFactors);
		return jo;
	}
}