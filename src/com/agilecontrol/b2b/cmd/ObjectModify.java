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
public class ObjectModify extends CmdHandler {
	/**
	 * 额外的处理, 在Update/AM存储过程前
	 * @param table
	 * @param editObj
	 * @throws Exception
	 */
	protected void beforeModify(Table table, JSONObject editObj) throws Exception{
		
	}
	/**
	 * 额外的处理, 在AM存储过程后
	 * @param table
	 * @param editObj
	 * @throws Exception
	 */
	protected void afterModify(Table table, JSONObject editObj) throws Exception{
		
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
		Table table=findTable(jo,"Modify");
		String tableName=table.getName();
		JSONObject obj= getObject(jo,"obj");

		//id
		long objectId=getLong(obj,"id");
		
		//重构可编辑字段
		JSONObject editObj=this.createEditObject(table, obj,false,false);
		editObj.put("id", objectId);
		//权限判定
		checkTableReadPermission(table, editObj);
		//数据库对象
		reviseTagColumnValue(objectId,table,editObj);
		beforeModify(table, editObj);
		PhoneUtils.modifyTable(tableName, editObj,PhoneUtils.getRedisObj(tableName, objectId, conn, jedis), conn);
		//存储过程 (objId, empId)
		String dbproc=table.getProcAM();
		execProc(dbproc, objectId);
		afterModify(table,editObj);
		
		CmdResult res = null;
		try {
			//创建redis 对象
			JSONObject ro=PhoneUtils.modifyRedisObj(objectId, table, createLuaArgObj(objectId), conn, jedis);
			//再次呼叫额外处理
			postAction(table, editObj,ro);
			res=new CmdResult( reviseColumnsOfJSONTypeValue(ro, table));
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return res;
	}
	
}
