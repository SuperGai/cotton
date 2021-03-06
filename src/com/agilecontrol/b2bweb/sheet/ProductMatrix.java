package com.agilecontrol.b2bweb.sheet;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.agilecontrol.nea.core.util.ConfigValues;
import com.swabunga.spell.engine.GenericTransformator;

/**
{
	size: ['23', '24', '25','26', '27'],  - m_attibute.name
	sizenotes:[String] - note就是m_attribute.description字段 
	sizefactors:[int]  -  规格系数，m_attribute.factor
	color: [ {n:'green' , s:[0,1,1,1,0], c:'01', n for name, s for sizes, c for code
			],
	pdtids:[13,24] 每一行对应的pdtid
	asis: [row][column] 每个元素是对应行列的asi, null 表示当前cell没有asi
}
 */
public class ProductMatrix{
	JSONArray sizeNotes;//尺码的英文描述， string
	JSONArray sizeFactors;//尺码系数，支，包，箱等 int
	JSONArray sizes;
	JSONArray colors;
	JSONArray pdtIds;
	/**
	 * JSONAray matrix[][], elements are asi id (Integer) for that cell, null if cell has no asi, matrix is just the same as ui
	 */
	JSONArray asiArray;
	
	
	/**
	 * 当要求单品矩阵下单时，在线编辑启用原来不缩进那一套asi尺码组
	 * @author LeeSh 2017-03-03
	 */
	JSONArray onlineEditSizeNotes;//尺码的英文描述， string 在线编辑
	JSONArray onlineEditSizeFactors;//尺码系数，支，包，箱等 int 在线编辑
	JSONArray onlineEditSizes; //在线编辑
	JSONArray onlineEditAsiArray;

	/**
	 * 
	 * @param compose {sizes, colors, pdtids, asis,sizenotes, sizefactors,onlineditsizes,onlineditsizenotes,onlineditsizefactors,onlineEditAsis}
	 * sizeobjs - [string] - 其中note就是m_attribute.description字段
	 */
	public ProductMatrix(JSONObject compose){
		sizeNotes=compose.optJSONArray("sizenotes");
		sizeFactors=compose.optJSONArray("sizefactors");
		sizes=compose.optJSONArray("sizes");
		colors=compose.optJSONArray("colors");
		pdtIds=compose.optJSONArray("pdtids");
		asiArray=compose.optJSONArray("asis");
		onlineEditSizes = compose.optJSONArray("onlineditsizes");
		onlineEditSizeNotes = compose.optJSONArray("onlineditsizenotes");
		onlineEditSizeFactors = compose.optJSONArray("onlineditsizefactors");
		onlineEditAsiArray = compose.optJSONArray("onlineditasis");
		 
	}
	
	/**
	 * 
	 * @return [int] - note就是m_attribute.factor字段，放置规格系数
	 */
	public ArrayList<Integer> getSizeFactors(){
		ArrayList<Integer> ss=new ArrayList();
		for(int i=0;i<sizeFactors.length();i++) ss.add(sizeFactors.optInt(i,1));
		return ss;
	}
	/**
	 * 
	 * @return [int] - note就是m_attribute.factor字段，放置规格系数
	 */
	public ArrayList<Integer> getOnlineEditSizeFactors(){
		ArrayList<Integer> ss=new ArrayList();
		if(onlineEditSizeFactors == null) return getSizeFactors();
		for(int i=0;i<onlineEditSizeFactors.length();i++) ss.add(onlineEditSizeFactors.optInt(i,1));
		return ss;
	}
	/**
	 * 
	 * @return [String] - note就是m_attribute.description字段，放置英文描述
	 */
	public ArrayList<String> getSizeNotes(){
		ArrayList<String> ss=new ArrayList();
		for(int i=0;i<sizeNotes.length();i++) ss.add(sizeNotes.optString(i));
		return ss;
	}
	/**
	 * 尺码名称，m_attributevalue.name 对应商品的尺码属性
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
	 * @return [][] 按行按列的asi值
	 * elements are asi id (Integer) for that cell, null if cell has no asi,
	 * 
	 */
	public JSONArray getASIArrays(){
		return asiArray;
	}
	/**
	 * 
	 * @return [][] 按行按列的asi值
	 * elements are asi id (Integer) for that cell, null if cell has no asi,
	 * 
	 */
	public JSONArray getOnlineEditAsiArray() {
		if(ConfigValues.get("fair.sizegroup.thrink", false))
			return onlineEditAsiArray;
		else
			return asiArray;
	}
	
	/**
	 * 每行对应的pdtid
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
		jo.put("onlineditsizes", onlineEditSizes);
		jo.put("onlineditsizenotes", onlineEditSizeNotes);
		jo.put("onlineditsizefactors", onlineEditSizeFactors);
		jo.put("onlineditasis", onlineEditAsiArray);
		return jo;
	}
}