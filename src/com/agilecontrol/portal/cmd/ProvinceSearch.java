package com.agilecontrol.portal.cmd;

import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONObject;

import com.agilecontrol.b2b.cmd.Search;
import com.agilecontrol.b2b.schema.Column;
import com.agilecontrol.b2b.schema.Table;
import com.agilecontrol.nea.core.query.QueryEngine;
import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.nea.util.Tools;
import com.agilecontrol.nea.util.Validator;
import com.agilecontrol.phone.CmdResult;
import com.agilecontrol.phone.PhoneUtils;
import com.agilecontrol.phone.UserObj;

/**
 ����Search, ÿ����Ʒ������Ҫ��ȡ�����M_PRODUCT_TRANS
 ���з���

> {cmd:"b2b.province.search", ������������, c_country_id}

*c_country_id* - number,����id

 �Աȱ�׼��search������
 {
    table, exprs, querystr,  maxcnt,pagesize, mask, usepref, orderby
 }
 * @author stao
 *
 */
public class ProvinceSearch extends Search {
	
	private int c_country_id;
	public CmdResult execute(JSONObject jo) throws Exception {
		if (null == usr || null == usr.getName()) {
			int rootUserId=Tools.getInt(QueryEngine.getInstance().doQueryOne("select id from users where name='root'"),893);
			JSONObject usrjo = PhoneUtils.getRedisObj("usr", rootUserId, conn, jedis);
			usr=new UserObj(usrjo);
		}
		c_country_id = jo.optInt("c_country_id",-1);
		String cacheKey=jo.optString("cachekey");
		if(Validator.isNotNull(cacheKey) && jedis.exists(cacheKey) /*������key timeoutҲҪ���²�*/){
			jo.put("table","province");//cache key wil reload from this table's pk records
		}
		return super.execute(jo);
	}
	/**
	 * �������¹�������������id
	 * @param jo ԭʼ�Ĳ�ѯ�������п����ڴ˵ر��ع�
	 * @return ����Ĳ�ѯ�����������ӵ���ѯwhere�����
	 * key: ��Ҫ���䵽sql where ���ֵ�clause������ݣ����� "c_country_id=?"
	 * value ���ʺŶ�Ӧ��ʵ��ֵ�����value��java.util.List���������ֵ���棿��
	 */
	protected HashMap<String, Object> reviseSearchCondition(JSONObject jo) throws Exception{
		HashMap<String, Object> map=new HashMap<String, Object>();
		StringBuilder sb=new StringBuilder();
		Table table=manager.getTable("province");
		ArrayList params=new ArrayList();
		
		String scs = (String) table.getJSONProp("search_on");
		if (Validator.isNotNull(scs)) {
			String[] scss = scs.split(",");
			Column cl = table.getColumn(scss[0]);
			if (cl == null){
				throw new NDSException(table + ".search_on��չ�����е��ֶ�δ����:" + scss[0]);
			}else{
				
				if( c_country_id != -1){
					sb.append(cl.getRealName() + " = ?");
					params.add(c_country_id);
				}
			}

		} else{
			throw new NDSException("��Ҫ����" + table + "��search_on��չ����");
		}
			map.put(sb.toString(), params);
		return map;
	}
	
	/* (non-Javadoc)
	 * @see com.agilecontrol.phone.CmdHandler#allowGuest()
	 */
	@Override
	public boolean allowGuest() {
		return true;
	}
}
