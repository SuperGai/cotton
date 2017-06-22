package com.agilecontrol.b2bweb.cmd;

import org.json.JSONArray;
import org.json.JSONObject;

import com.agilecontrol.b2b.cmd.ObjectVoid;
import com.agilecontrol.b2b.cmd.Search;
import com.agilecontrol.b2b.schema.Table;
import com.agilecontrol.b2bweb.WebController;
import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.nea.util.Tools;
import com.agilecontrol.nea.util.Validator;
import com.agilecontrol.phone.Admin;
import com.agilecontrol.phone.CmdHandler;
import com.agilecontrol.phone.CmdResult;
import com.agilecontrol.phone.PhoneController;
import com.agilecontrol.phone.PhoneUtils;

/**
h1. user_address �û���ַ����

> {cmd:"b2b.addr.get",obj}
 
 * @author yfzhu
 *
 */
@Admin(mail="yfzhu@lifecycle.cn")
public class AddrVoid extends ObjectVoid {
	/**
	 * ����Ĵ���, ��BD�洢����ǰ
	 * @param table
	 * @param editObj
	 * @throws Exception
	 */
	protected void beforeVoid(Table table, JSONObject editObj) throws Exception{
		editObj.put("modifierid", usr.getId());
	}
	/**
	 * �ж�obj��������ָ����table�ļ�¼���Ƿ�ɱ���ǰ�����û�����
	 * ���������comȨ�ޣ���ʾ�������ڲ�Ա�����ʣ������ǰ�û���comid��ҵ�����ݲ�һ�£������� ���������admin, ��ʾ���������Ա����
	 * 
	 * @param table
	 * @param obj
	 * @throws Exception
	 *             ���û��Ȩ�޾ͱ���
	 */
	protected void checkTableWritePermission(Table table, JSONObject obj) throws Exception {
		int id=this.getInt(obj, "id");
		JSONObject redisObj=PhoneUtils.getRedisObj("addr", id, "id,user_id",  conn, jedis);
		if(redisObj==null) throw new NDSException("����: ��ַ������:"+ id);
		int uid=redisObj.optInt("user_id", -1);
		if(uid!=usr.getId()) {
			logger.error("try to void other's addr: "+ uid+", current uid="+ usr.getId());
			throw new NDSException("ֻ�ܸ��±��˵ĵ�ַ");
		}
	}
}













