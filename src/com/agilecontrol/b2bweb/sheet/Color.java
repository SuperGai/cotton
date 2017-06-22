package com.agilecontrol.b2bweb.sheet;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;
/**
 * 颜色信息，适配ProductMatrix使用
 * 
 * @author yfzhu
 *
 */
public class Color{
	/**
	 * 名称
	 */
	String name;
	/**
	 * 代码
	 */
	String code;
	/**
	 * 当前色的每个尺码位是否可以输入
	 */
	ArrayList<Boolean> sizes;
	
	/**
	 * {n:'green' , s:[0,1,1,1,0], c:'01', n for name, s for sizes, c for code
	 * @param clr
	 */
	public  Color(JSONObject clr){
		name=clr.optString("n");
		code=clr.optString("c");
		JSONArray ss= clr.optJSONArray("s");
		sizes=new ArrayList();
		for(int i=0;i<ss.length();i++) sizes.add(ss.optInt(i,0)==1);
		
	}
	/**
	 * 
	 * @return [0,1,1,1,0]
	 */
	public JSONArray getSizes(){
		JSONArray ss=new JSONArray();
		for(Boolean b: sizes) ss.put(b?1:0);
		return ss;
	}
	/**
	 * color.name
	 * @return
	 */
	public String getName(){
		return name;
	}
	/**
	 * color.code
	 * @return
	 */
	public String getCode(){
		return code;
	}
	/**
	 * 判定当前颜色指定尺码位置是否有sku
	 * @param idx start from 0
	 * @return
	 */
	public boolean isAvailableSize(int idx){
		if(idx>= sizes.size()) return false;
		return sizes.get(idx);
	}
	
	public String toString(){
		return code;
	}
	
}