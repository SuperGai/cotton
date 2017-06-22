package com.agilecontrol.b2b.cmd;

import org.apache.velocity.VelocityContext;
import org.json.*;

import java.util.*;

import com.agilecontrol.b2b.schema.*;
import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.nea.util.Validator;
import com.agilecontrol.phone.CmdHandler;
import com.agilecontrol.phone.CmdResult;
import com.agilecontrol.phone.PhoneConfig;
import com.agilecontrol.phone.PhoneController;
import com.agilecontrol.phone.PhoneUtils;

/**
 * 
 停用对象，不是真删除，将en字段设置为N, 另外检查对应的db和redis更新
 
 {table*, id, obj}
 table - table.name
 id 对象id
 obj 和id 两选，仅取第一层的字段进行判定
  
 * @author yfzhu
 *
 */
public class ObjectVoid extends CmdHandler {
	/**
	 * 额外的处理, 在BD存储过程前
	 * @param table
	 * @param editObj
	 * @throws Exception
	 */
	protected void beforeVoid(Table table, JSONObject editObj) throws Exception{
		
	}
	/**
	 * redis完成缓存更新后的处理
	 * @param table
	 * @param editObj db的保存对象，写入数据库的部分
	 * @param redisObj redis 返回的缓存对象，将换行客户端，可以进行再次编辑
	 * @throws Exception
	 */
	protected void postAction(Table table, JSONObject editObj,JSONObject redisObj) throws Exception{
		
	}
	
	@Override
	public CmdResult execute(JSONObject jo) throws Exception {
		Table table=findTable(jo,"Void");
		String tableName=table.getName();
		JSONObject obj= jo.optJSONObject("obj");
		
		long objectId;
		if(obj==null){
			objectId=getLong(jo,"id");
			obj=PhoneUtils.getRedisObj(tableName, objectId, conn, jedis);
		}else{
			//id
			objectId=getLong(obj,"id");
		}
		//重构可编辑字段
		JSONObject editObj=this.createEditObject(table, obj,false,false);
		editObj.put("id", objectId);
		//权限判定
		checkTableWritePermission(table, editObj);
		
		//数据库对象
		beforeVoid(table, editObj);
		//update 语句而已
		PhoneUtils.voidTable(tableName, editObj, conn);
		//存储过程 (objId, empId)
		String dbproc=table.getProcBD();
		execProc(dbproc, objectId);

		CmdResult res = null;
		try {
			//创建redis 对象
			JSONObject ro=PhoneUtils.voidRedisObj(objectId, table, createLuaArgObj(objectId), conn, jedis);
			
			//再次呼叫额外处理
			postAction(table, editObj,ro);
			
			res=new CmdResult( reviseColumnsOfJSONTypeValue(ro, table));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return res;
	}
	
}
