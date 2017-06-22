package com.agilecontrol.b2b.cmd;

import org.json.JSONObject;

import com.agilecontrol.b2b.schema.Table;
import com.agilecontrol.nea.util.Validator;
import com.agilecontrol.phone.Admin;
import com.agilecontrol.phone.CmdHandler;
import com.agilecontrol.phone.CmdResult;
import com.agilecontrol.phone.PhoneController;
import com.agilecontrol.phone.PhoneUtils;

/**
 * 用户设置
 * @author chenmengqi
 *
 */
@Admin(mail="chen.mengqi@lifecycle.cn")
public class UsrSet extends CmdHandler{

	@Override
	public CmdResult execute(JSONObject jo) throws Exception {
		Table table=manager.getTable("pref");
		String tableName=table.getName();
		
		JSONObject obj= getObject(jo,"obj");
		long objectId=obj.optLong("id",-1);
		JSONObject ro = null;
		if(objectId == -1){

			//重构可编辑字段
			JSONObject editObj=this.createEditObject(table, obj,true, true/*chekNull*/);
			//权限判定
			checkTableReadPermission(table, editObj);
			//id
			objectId=PhoneController.getInstance().getNextId(table.getName(), conn);
			editObj.put("id", objectId);
			
			//创建数据库对象
			PhoneUtils.insertTable(tableName, editObj, conn);
			//存储过程 (objId, empId)
			String dbproc=table.getProcAC();
			execProc(dbproc, objectId);


			//创建redis 对象， ro是redis 缓存对象
			ro=PhoneUtils.addRedisObj(objectId, table, createLuaArgObj(objectId), conn, jedis);
		}else{
			//重构可编辑字段
			JSONObject editObj=this.createEditObject(table, obj,false,false);
			editObj.put("id", objectId);
			//权限判定
			checkTableReadPermission(table, editObj);
			
			PhoneUtils.modifyTable(tableName, editObj, conn);
			//存储过程 (objId, empId)
			String dbproc=table.getProcAM();
			execProc(dbproc, objectId);


			//创建redis 对象
			ro=PhoneUtils.modifyRedisObj(objectId, table, createLuaArgObj(objectId), conn, jedis);
		}
		
		CmdResult res=new CmdResult( ro);
		return res;
	}

}
