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
 ����ָ������֧���Զ����������:
 
 {table*, obj*}
 table - table.name
 obj - {��Ŀɱ༭����, ��Ҫ����Ϊ�ֶο�д������ֶβ���Ϊ�գ�����������}
  
 * @author yfzhu
 *
 */
public class ObjectAdd extends CmdHandler {
	/**
	 * ����Ĵ���, ��AC�洢���̺�
	 * @param table
	 * @param editObj д�����ݿ�Ŀɱ༭�ֶ�ֵ
	 * @throws Exception
	 */
	protected void postDBAC(Table table, JSONObject editObj) throws Exception{
		
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
		
		JSONObject obj= getObject(jo,"obj");

		Table table=findTable(jo,"Add");
		String tableName=table.getName();

		//�ع��ɱ༭�ֶ�
		JSONObject editObj=this.createEditObject(table, obj,true, true/*chekNull*/);
		//Ȩ���ж�
		checkTableReadPermission(table, editObj);
		//id
		long objectId=PhoneController.getInstance().getNextId(table.getName(), conn);
		editObj.put("id", objectId);
		
		//�������ݿ����, ����Ҫά������ǩ����
		reviseTagColumnValue(objectId,table,editObj);
		PhoneUtils.insertTable(tableName, editObj, conn);
		//�洢���� (objId, empId)
		String dbproc=table.getProcAC();
		execProc(dbproc, objectId);

		postDBAC(table,editObj);
		
		CmdResult res = null;
		try {
			//����redis ���� ro��redis �������
			JSONObject ro=PhoneUtils.addRedisObj(objectId, table, createLuaArgObj(objectId), conn, jedis);
			//�ٴκ��ж��⴦��
			postAction(table, editObj,ro);
			res=new CmdResult( reviseColumnsOfJSONTypeValue(ro, table));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return res;
	}
	
}
