package com.agilecontrol.b2bweb;

import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.util.*;

import com.agilecontrol.nea.core.query.QueryEngine;
import com.agilecontrol.phone.Admin;
import com.agilecontrol.phone.LanguageManager;

/**
 * 
 * 商品属性的翻译表，单例模式，缓存于java
 * 
 * 商品属性翻译结构：M_DIM_TRANS(m_dim_id, b_language_id, attributename)
 * 
 * 
 * 
 * 
 * @author yfzhu
 *
 */
@Admin(mail="yfzhu@lifecycle.cn")
public class DimTranslator {
	private static Logger logger = LoggerFactory.getLogger(DimTranslator.class);
	private static DimTranslator instance=null;
	/**
	 * key: b_language_id, value: key: m_dim_id, value: M_DIM_TRANS.attributename
	 */
	private Hashtable<Integer, Hashtable<Integer, String>> dict;
	
	/**
	 * key: b_language_id, value: key: M_DIM_TRANS.attributename.tolowercase()为了不区分大小写, value:m_dim_id
	 */
	private Hashtable<Integer, Hashtable<String, Integer>> dictNames;
	
	private DimTranslator(){
		dict=new Hashtable();
		dictNames=new Hashtable();
	}
	
	/**
	 * 将所有的属性翻译缓存到内部hash表
	 * 
	 */
	private void init(Connection conn) throws Exception{
		JSONArray ja=QueryEngine.getInstance().doQueryJSONArray("select m_dim_id, b_language_id, attribname from m_dim_trans union select id,"+
				LanguageManager.getInstance().getDefaultLangId()+",attribname from m_dim",  null, conn);
		for(int i=0;i<ja.length();i++){
			JSONArray row=ja.getJSONArray(i);
			int dimId=row.getInt(0);
			int langId=row.getInt(1);
			String name=row.getString(2);
			Hashtable ht=dict.get(langId);
			if(ht==null){
				ht=new Hashtable();
				dict.put(langId, ht);
				
			}
			ht.put(dimId, name);
			
			
			ht=dictNames.get(langId);
			if(ht==null){
				ht=new Hashtable();
				dictNames.put(langId, ht);
			}
			ht.put( name.toLowerCase(),dimId);
		}
	}
	/**
	 * 根据dim的名称，指定语言，找到对应的id，如果不存在，返回-1
	 * @param langId 语言
	 * @param name 属性值， 不区分大小写
	 * @param conn
	 * @return -1 如果未找到
	 * @throws Exception
	 */
	public int getDimId(int langId, String name, Connection conn) throws Exception{
		if(dictNames.isEmpty()){
			init(conn);
		}
		Hashtable<String,Integer> trans=dictNames.get(langId);
		if(trans==null){
			logger.error("Fail to load from langid="+ langId+" of name="+name);
		}
		Integer id=trans.get(name.toLowerCase());//为了不区分大小写
		if(id==null) id=-1;
		return id;
	}
	/**
	 * 获取dimId对应language的翻译名称
	 * @param dimId
	 * @param langId 语言id 
	 * @param defaultName 当lang的翻译不存在的时候，所用替代值
	 * @return 翻译好的名称
	 */
	public String getTranslateName(int dimId, int langId, String defaultName, Connection conn) throws Exception{
		if(dict.isEmpty()){
			init(conn);
		}
		Hashtable<Integer, String> trans=dict.get(langId);
		String name=null;
		if(trans!=null){
			name=trans.get(dimId);
		}
		return name==null?defaultName:name;
	}
	/**
	 * 获取dimId对应language的翻译名称
	 * @param dimId
	 * @param lang 
	 * @param defaultName 当lang的翻译不存在的时候，所用替代值
	 * @return 翻译好的名称
	 */
	public String getTranslateName(int dimId, String lang, String defaultName, Connection conn) throws Exception{
		return getTranslateName(dimId, LanguageManager.getInstance().getLanguageId(lang), defaultName,conn);
		
	}
	/**
	 * 清空备份，重新载入数据库翻译内容，当数据库翻译变更的时候
	 */
	public void clear(Connection conn) {
		dict.clear();
		dictNames.clear();
		//这里不做重载，用的时候再读取
	}
	
	public static DimTranslator getInstance(){
		if(instance==null){
			instance=new DimTranslator();
		}
		return instance;
	}
}
