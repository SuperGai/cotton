package com.agilecontrol.portal.cmd;

import org.json.JSONObject;

import com.agilecontrol.b2b.cmd.Search;
import com.agilecontrol.nea.core.query.QueryEngine;
import com.agilecontrol.nea.util.Tools;
import com.agilecontrol.nea.util.Validator;
import com.agilecontrol.phone.CmdResult;
import com.agilecontrol.phone.PhoneUtils;
import com.agilecontrol.phone.UserObj;

/**
 ����Search, ÿ����Ʒ������Ҫ��ȡ�����M_PRODUCT_TRANS
 ���з���

> {cmd:"b2b.country.search", ������������}

 �Աȱ�׼��search������
 {
    table, exprs, querystr,  maxcnt,pagesize, mask, usepref, orderby
 }
 * @author stao
 *
 */
public class CountrySearch extends Search {
	
	public CmdResult execute(JSONObject jo) throws Exception {
		if (null == usr || null == usr.getName()) {
			int rootUserId=Tools.getInt(QueryEngine.getInstance().doQueryOne("select id from users where name='root'"),893);
			JSONObject usrjo = PhoneUtils.getRedisObj("usr", rootUserId, conn, jedis);
			usr=new UserObj(usrjo);
		}
		String cacheKey=jo.optString("cachekey");
		if(Validator.isNotNull(cacheKey) && jedis.exists(cacheKey) /*������key timeoutҲҪ���²�*/){
			jo.put("table","country");//cache key wil reload from this table's pk records
		}
		return super.execute(jo);
	}
	
	/* (non-Javadoc)
	 * @see com.agilecontrol.phone.CmdHandler#allowGuest()
	 */
	@Override
	public boolean allowGuest() {
		return true;
	}
}
