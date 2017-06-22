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
 保存指定对象，支持自定义符合类型:
 
 {table*, obj*}
 table - table.name
 obj - {表的可编辑属性, 需要定义为字段可写，如果字段不能为空，还将额外检测}
  
 * @author yfzhu
 *
 */
public class ObjectAdd extends CmdHandler {
	/**
	 * 额外的处理, 在AC存储过程后
	 * @param table
	 * @param editObj 写入数据库的可编辑字段值
	 * @throws Exception
	 */
	protected void postDBAC(Table table, JSONObject editObj) throws Exception{
		
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
		
		JSONObject obj= getObject(jo,"obj");

		Table table=findTable(jo,"Add");
		String tableName=table.getName();

		//重构可编辑字段
		JSONObject editObj=this.createEditObject(table, obj,true, true/*chekNull*/);
		//权限判定
		checkTableReadPermission(table, editObj);
		//id
		long objectId=PhoneController.getInstance().getNextId(table.getName(), conn);
		editObj.put("id", objectId);
		
		//创建数据库对象, 首先要维护到标签内容
		reviseTagColumnValue(objectId,table,editObj);
		PhoneUtils.insertTable(tableName, editObj, conn);
		//存储过程 (objId, empId)
		String dbproc=table.getProcAC();
		execProc(dbproc, objectId);

		postDBAC(table,editObj);
		
		CmdResult res = null;
		try {
			//创建redis 对象， ro是redis 缓存对象
			JSONObject ro=PhoneUtils.addRedisObj(objectId, table, createLuaArgObj(objectId), conn, jedis);
			//再次呼叫额外处理
			postAction(table, editObj,ro);
			res=new CmdResult( reviseColumnsOfJSONTypeValue(ro, table));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return res;
	}
	
}
