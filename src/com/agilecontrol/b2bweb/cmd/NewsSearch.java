package com.agilecontrol.b2bweb.cmd;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONObject;

import com.agilecontrol.b2b.cmd.Search;
import com.agilecontrol.b2b.query.SearchResult;
import com.agilecontrol.b2b.schema.Column;
import com.agilecontrol.b2b.schema.Table;
import com.agilecontrol.b2bweb.WebController;
import com.agilecontrol.b2bweb.cmd.DimList.DimValue;
import com.agilecontrol.nea.core.util.MessagesHolder;
import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.nea.util.Tools;
import com.agilecontrol.nea.util.Validator;
import com.agilecontrol.phone.Admin;
import com.agilecontrol.phone.CmdHandler;
import com.agilecontrol.phone.CmdResult;
import com.agilecontrol.phone.LanguageManager;
import com.agilecontrol.phone.PhoneConfig;
import com.agilecontrol.phone.PhoneController;
import com.agilecontrol.phone.PhoneUtils;

/**
 ����Search, ÿ����Ʒ������Ҫ��ȡ�����M_PRODUCT_TRANS
 ���з���

> {cmd:"b2b.news.search", ������������, doctype}

*doctype* - string, �����ڲ����� rule �����ƶ�|notes �ڲ�֪ͨ|company ��˾����|industry ��ҵ����|hotspot ��ҳ��̬�ȵ�|declaration ���Ź���|latest ���¶�̬|other ����

 �Աȱ�׼��search������
 {
    table, exprs, querystr,  maxcnt,pagesize, mask, usepref, orderby
 }
 * @author wu.qiong
 *
 */
public class NewsSearch extends Search {
	
	private String doctype;
	public CmdResult execute(JSONObject jo) throws Exception {
		doctype = jo.optString("doctype","");
		String cacheKey=jo.optString("cachekey");
		if(Validator.isNotNull(cacheKey) && jedis.exists(cacheKey) /*������key timeoutҲҪ���²�*/){
			jo.put("table","news");//cache key wil reload from this table's pk records
		}
		return super.execute(jo);
	}
	/**
	 * �������¹�����������������
	 * @param jo ԭʼ�Ĳ�ѯ�������п����ڴ˵ر��ع�
	 * @return ����Ĳ�ѯ�����������ӵ���ѯwhere�����
	 * key: ��Ҫ���䵽sql where ���ֵ�clause������ݣ����� "emp_id=?"
	 * value ���ʺŶ�Ӧ��ʵ��ֵ�����value��java.util.List���������ֵ���棿��
	 */
	protected HashMap<String, Object> reviseSearchCondition(JSONObject jo) throws Exception{
		HashMap<String, Object> map=new HashMap<String, Object>();
		StringBuilder sb=new StringBuilder();
		Table table=manager.getTable("news");
		ArrayList params=new ArrayList();
		String scs = (String) table.getJSONProp("search_on");
			if (Validator.isNotNull(scs)) {
				String[] scss = scs.split(",");
				Column cl = table.getColumn(scss[0]);
				if (cl == null){
					throw new NDSException(table + ".search_on��չ�����е��ֶ�δ����:" + scss[0]);
				}else{
					sb=new StringBuilder(" lower( ");
					sb.append(cl.getRealName()).append(") = ?");
				}
				params.add(doctype);

			} else{
				throw new NDSException("��Ҫ����" + table + "��search_on��չ����");
			}
			map.put(sb.toString(), params);
		return map;
	}
}
