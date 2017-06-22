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
h1. user_address 用户地址作废

> {cmd:"b2b.addr.get",obj}
 
 * @author yfzhu
 *
 */
@Admin(mail="yfzhu@lifecycle.cn")
public class AddrVoid extends ObjectVoid {
	/**
	 * 额外的处理, 在BD存储过程前
	 * @param table
	 * @param editObj
	 * @throws Exception
	 */
	protected void beforeVoid(Table table, JSONObject editObj) throws Exception{
		editObj.put("modifierid", usr.getId());
	}
	/**
	 * 判定obj对象，属于指定的table的记录，是否可被当前操作用户访问
	 * 如果表定义了com权限，表示仅允许内部员工访问，如果当前用户的comid与业务数据不一致，将报错 如果定义了admin, 表示仅允许管理员访问
	 * 
	 * @param table
	 * @param obj
	 * @throws Exception
	 *             如果没有权限就报错
	 */
	protected void checkTableWritePermission(Table table, JSONObject obj) throws Exception {
		int id=this.getInt(obj, "id");
		JSONObject redisObj=PhoneUtils.getRedisObj("addr", id, "id,user_id",  conn, jedis);
		if(redisObj==null) throw new NDSException("意外: 地址不存在:"+ id);
		int uid=redisObj.optInt("user_id", -1);
		if(uid!=usr.getId()) {
			logger.error("try to void other's addr: "+ uid+", current uid="+ usr.getId());
			throw new NDSException("只能更新本人的地址");
		}
	}
}













