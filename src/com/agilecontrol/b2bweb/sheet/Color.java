package com.agilecontrol.b2bweb.sheet;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;
/**
 * ��ɫ��Ϣ������ProductMatrixʹ��
 * 
 * @author yfzhu
 *
 */
public class Color{
	/**
	 * ����
	 */
	String name;
	/**
	 * ����
	 */
	String code;
	/**
	 * ��ǰɫ��ÿ������λ�Ƿ��������
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
	 * �ж���ǰ��ɫָ������λ���Ƿ���sku
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