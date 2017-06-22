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
 * ��Ʒ���Եķ��������ģʽ��������java
 * 
 * ��Ʒ���Է���ṹ��M_DIM_TRANS(m_dim_id, b_language_id, attributename)
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
	 * key: b_language_id, value: key: M_DIM_TRANS.attributename.tolowercase()Ϊ�˲����ִ�Сд, value:m_dim_id
	 */
	private Hashtable<Integer, Hashtable<String, Integer>> dictNames;
	
	private DimTranslator(){
		dict=new Hashtable();
		dictNames=new Hashtable();
	}
	
	/**
	 * �����е����Է��뻺�浽�ڲ�hash��
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
	 * ����dim�����ƣ�ָ�����ԣ��ҵ���Ӧ��id����������ڣ�����-1
	 * @param langId ����
	 * @param name ����ֵ�� �����ִ�Сд
	 * @param conn
	 * @return -1 ���δ�ҵ�
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
		Integer id=trans.get(name.toLowerCase());//Ϊ�˲����ִ�Сд
		if(id==null) id=-1;
		return id;
	}
	/**
	 * ��ȡdimId��Ӧlanguage�ķ�������
	 * @param dimId
	 * @param langId ����id 
	 * @param defaultName ��lang�ķ��벻���ڵ�ʱ���������ֵ
	 * @return ����õ�����
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
	 * ��ȡdimId��Ӧlanguage�ķ�������
	 * @param dimId
	 * @param lang 
	 * @param defaultName ��lang�ķ��벻���ڵ�ʱ���������ֵ
	 * @return ����õ�����
	 */
	public String getTranslateName(int dimId, String lang, String defaultName, Connection conn) throws Exception{
		return getTranslateName(dimId, LanguageManager.getInstance().getLanguageId(lang), defaultName,conn);
		
	}
	/**
	 * ��ձ��ݣ������������ݿⷭ�����ݣ������ݿⷭ������ʱ��
	 */
	public void clear(Connection conn) {
		dict.clear();
		dictNames.clear();
		//���ﲻ�����أ��õ�ʱ���ٶ�ȡ
	}
	
	public static DimTranslator getInstance(){
		if(instance==null){
			instance=new DimTranslator();
		}
		return instance;
	}
}
