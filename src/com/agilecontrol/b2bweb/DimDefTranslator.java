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
 * ��Ʒ�������Ƶķ��������ģʽ��������java
 * 
 * ��Ʒ���Զ��巭��ṹ��M_DIMDEF_TRANS(m_dimdef_id, b_language_id, attributename)
 * 
 * m_dimdef.dimflag �ж�Ӧ��dimX�Ķ���, ���磬��֪�� dim3 ��Ӧ��Ӣ�ģ������ǣ�
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
	 * �����е����Է��뻺�浽�ڲ�hash��
	 * 
	 */
	private void init(Connection conn) throws Exception{
		int defaultLangId=LanguageManager.getInstance().getDefaultLangId();
		/**
		 * Ŀ�꣺�ҵ������������ƶ�Ӧ�ķ��룬�����������δ���壬ʹ��Ĭ�ϵ�����id����m_dimdef�ϵ�����
		 * ��Ҫ���Կͻ����Բ������룬�����������Ҫ��clear��ʱ��һ�����
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
	 * ��ȡdim��Ӧlanguage�ķ�������
	 * @param dim, m_dimdef.dimflag
	 * @param langId ����id 
	 * @param conn Ĭ�ϵ�����
	 * @return ����õ�����
	 */
	public String getTranslateName(String dim, int langId,  Connection conn) throws Exception{
		if(dict.isEmpty()){
			init(conn);
		}
		return dict.get(dim.toLowerCase()+"."+ langId);
	}
	/**
	 * ��ձ��ݣ������������ݿⷭ�����ݣ������ݿⷭ������ʱ��
	 */
	public void clear(Connection conn) {
		dict.clear();
		//���ﲻ�����أ��õ�ʱ���ٶ�ȡ
	}
	
	public static DimDefTranslator getInstance(){
		if(instance==null){
			instance=new DimDefTranslator();
		}
		return instance;
	}
}
