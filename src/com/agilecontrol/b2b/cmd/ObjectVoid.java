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
 ͣ�ö��󣬲�����ɾ������en�ֶ�����ΪN, �������Ӧ��db��redis����
 
 {table*, id, obj}
 table - table.name
 id ����id
 obj ��id ��ѡ����ȡ��һ����ֶν����ж�
  
 * @author yfzhu
 *
 */
public class ObjectVoid extends CmdHandler {
	/**
	 * ����Ĵ���, ��BD�洢����ǰ
	 * @param table
	 * @param editObj
	 * @throws Exception
	 */
	protected void beforeVoid(Table table, JSONObject editObj) throws Exception{
		
	}
	/**
	 * redis��ɻ�����º�Ĵ���
	 * @param table
	 * @param editObj db�ı������д�����ݿ�Ĳ���
	 * @param redisObj redis ���صĻ�����󣬽����пͻ��ˣ����Խ����ٴα༭
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
		//�ع��ɱ༭�ֶ�
		JSONObject editObj=this.createEditObject(table, obj,false,false);
		editObj.put("id", objectId);
		//Ȩ���ж�
		checkTableWritePermission(table, editObj);
		
		//���ݿ����
		beforeVoid(table, editObj);
		//update ������
		PhoneUtils.voidTable(tableName, editObj, conn);
		//�洢���� (objId, empId)
		String dbproc=table.getProcBD();
		execProc(dbproc, objectId);

		CmdResult res = null;
		try {
			//����redis ����
			JSONObject ro=PhoneUtils.voidRedisObj(objectId, table, createLuaArgObj(objectId), conn, jedis);
			
			//�ٴκ��ж��⴦��
			postAction(table, editObj,ro);
			
			res=new CmdResult( reviseColumnsOfJSONTypeValue(ro, table));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return res;
	}
	
}
