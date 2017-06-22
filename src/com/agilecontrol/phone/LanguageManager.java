package com.agilecontrol.phone;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Locale;

import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.agilecontrol.b2bweb.DimTranslator;
import com.agilecontrol.nea.core.query.QueryEngine;

/**
 * ��b_language������java�ڴ� 
 * 
 * @author yfzhu
 *
 */
@Admin(mail="yfzhu@lifecycle.cn")
public class LanguageManager {
	private static Logger logger = LoggerFactory.getLogger(DimTranslator.class);
	private static LanguageManager instance=null;
	/**
	 * ��Ӧ��b_language
	 *
	 */
	private class Lang{
		int id;
		String name, description;
	}
	/**
	 * ֵ̫�٣�ֱ�ӱȶԾ����ˣ�����hash
	 */
	private ArrayList<Lang> langs;

	private LanguageManager(){
		langs=new ArrayList();
	}
	/**
	 * ����langid��ȡ��Ӧ��java locale, Ŀǰ����֧��Locale.ENGLISH��Locale.SIMPLIFIED_CHINESE��Ĭ�ϣ�
	 * @param langId
	 * @return
	 */
	public Locale getLocale(int langId){
		for(Lang lang: langs){
			if(langId==lang.id) {
				if(lang.name.startsWith("en")) return Locale.US;
				else return Locale.SIMPLIFIED_CHINESE;
			}
		} 
		return Locale.SIMPLIFIED_CHINESE;
	}
	/**
	 * ��ȡ��PhoneConfig.LANG_DEFAULT="zh"
	 * @return
	 */
	public String getDefaultLangName(){
		return PhoneConfig.LANG_DEFAULT;
	}
	/**
	 * ��ȡ��PhoneConfig.LANG_DEFAULT="zh"��Ӧ��lang_id
	 * @return
	 */
	public int getDefaultLangId(){
		return getLanguageId(PhoneConfig.LANG_DEFAULT);
	}
	/**
	 * �������е�b_language.id
	 * @return
	 */
	public ArrayList<Integer> getAllLanguageIds(){
		ArrayList<Integer> langIds=new ArrayList();
		for(Lang lang: langs){
			langIds.add(lang.id);
		}
		return langIds;
	}
	/**
	 * ���������ҵ���Ӧ��id������-1���û�ҵ�
	 * @param name
	 */
	public int getLanguageId(String name){
		for(Lang lang: langs){
			if(name.equalsIgnoreCase(lang.name)) return lang.id;
		}
		return -1;
	}
	
	/**
	 * ����id�ҵ���Ӧ��name������null���û�ҵ�
	 * @param name
	 */
	public String getLanguageName(int langId){
		for(Lang lang: langs){
			if(langId==lang.id) return lang.name;
		}
		return null;
	}
	
	/**
	 * ����id�ҵ���Ӧ��name������null���û�ҵ�
	 * @param name
	 */
	public String getLanguageDesc(int langId){
		for(Lang lang: langs){
			if(langId==lang.id) return lang.description;
		}
		return null;
	}
	
	
	/**
	 * ������ݣ������������ݿⷭ������
	 */
	public void clear(Connection conn) throws Exception {
		langs.clear();
		JSONArray ja=QueryEngine.getInstance().doQueryJSONArray("select id,name,description from b_language", null, conn);
		for(int i=0;i<ja.length();i++){
			JSONArray row=ja.getJSONArray(i);
			Lang lang=new Lang();
			lang.id=row.getInt(0);
			lang.name=row.getString(1);
			lang.description=row.getString(2);
			langs.add(lang);
		}
		
	}
	
	public static LanguageManager getInstance(){
		if(instance==null){
			LanguageManager one=new LanguageManager();
			Connection conn=null;
			try{
				conn=QueryEngine.getInstance().getConnection();
				one.clear(conn);
			}catch(Throwable tx){
				logger.error("fail to load langs", tx);
			}finally{
				try{conn.close();}catch(Exception ex){}
			}
			instance=one;

		}
		return instance;
	}
}
