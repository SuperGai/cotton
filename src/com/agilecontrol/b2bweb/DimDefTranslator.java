package com.agilecontrol.b2bweb;

import org.apache.velocity.VelocityContext;
import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.util.*;

import com.agilecontrol.nea.core.query.QueryEngine;
import com.agilecontrol.nea.core.velocity.VelocityUtils;
import com.agilecontrol.phone.Admin;
import com.agilecontrol.phone.LanguageManager;
import com.agilecontrol.phone.PhoneController;

/**
 * 
 * 商品属性名称的翻译表，单例模式，缓存于java
 * 
 * 商品属性定义翻译结构：M_DIMDEF_TRANS(m_dimdef_id, b_language_id, attributename)
 * 
 * m_dimdef.dimflag 中对应了dimX的定义, 例如，想知道 dim3 对应的英文，方法是：
 * 
 * getTranslateName(name="dim3", langid=3//en)
 * 
 * 
 * @author yfzhu
 *
 */
@Admin(mail="yfzhu@lifecycle.cn")
public class DimDefTranslator {
	private static Logger logger = LoggerFactory.getLogger(DimDefTranslator.class);
	private static DimDefTranslator instance=null;
	/**
	 * key: dimflag.lowercase+"."+ langid, value: M_DIMdef_TRANS.name
	 */
	private Hashtable<String, String> dict;
	
	
	private DimDefTranslator(){
		dict=new Hashtable();
	}
	
	/**
	 * 将所有的属性翻译缓存到内部hash表
	 * 
	 */
	private void init(Connection conn) throws Exception{
		int defaultLangId=LanguageManager.getInstance().getDefaultLangId();
		/**
		 * 目标：找到所有属性名称对应的翻译，如果翻译语言未定义，使用默认的语言id和在m_dimdef上的描述
		 * 主要语言客户可以不做翻译，翻译的内容需要在clear的时候一并清空
		 */
		VelocityContext vc=VelocityUtils.createContext();
		vc.put("defaultlangid",defaultLangId);
		/**
		 * select d.dimflag, 1 langid, d.name from m_dimdef d
union
SELECT d.dimflag, t.b_language_id langid, t.attribname NAME
FROM   m_dimdef_trans t, m_dimdef d
WHERE  t.m_dimdef_id = d.id and t.b_language_id<>1

		 */
		JSONArray ja=PhoneController.getInstance().getDataArrayByADSQL("dimdef_langs", vc, conn, false);
		for(int i=0;i<ja.length();i++){
			JSONArray row=ja.getJSONArray(i);
			String dimFlag=row.getString(0).toLowerCase();
			int langId=row.getInt(1);
			String name=row.getString(2);
			if(langId==-1){
				//using default langid
				langId=defaultLangId;
			}
			dict.put(dimFlag+"."+langId, name);
		}
	}
	/**
	 * 获取dim对应language的翻译名称
	 * @param dim, m_dimdef.dimflag
	 * @param langId 语言id 
	 * @param conn 默认的连接
	 * @return 翻译好的名称
	 */
	public String getTranslateName(String dim, int langId,  Connection conn) throws Exception{
		if(dict.isEmpty()){
			init(conn);
		}
		return dict.get(dim.toLowerCase()+"."+ langId);
	}
	/**
	 * 清空备份，重新载入数据库翻译内容，当数据库翻译变更的时候
	 */
	public void clear(Connection conn) {
		dict.clear();
		//这里不做重载，用的时候再读取
	}
	
	public static DimDefTranslator getInstance(){
		if(instance==null){
			instance=new DimDefTranslator();
		}
		return instance;
	}
}
