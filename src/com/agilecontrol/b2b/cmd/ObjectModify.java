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
public class ObjectModify extends CmdHandler {
	/**
	 * ����Ĵ���, ��Update/AM�洢����ǰ
	 * @param table
	 * @param editObj
	 * @throws Exception
	 */
	protected void beforeModify(Table table, JSONObject editObj) throws Exception{
		
	}
	/**
	 * ����Ĵ���, ��AM�洢���̺�
	 * @param table
	 * @param editObj
	 * @throws Exception
	 */
	protected void afterModify(Table table, JSONObject editObj) throws Exception{
		
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
		Table table=findTable(jo,"Modify");
		String tableName=table.getName();
		JSONObject obj= getObject(jo,"obj");

		//id
		long objectId=getLong(obj,"id");
		
		//�ع��ɱ༭�ֶ�
		JSONObject editObj=this.createEditObject(table, obj,false,false);
		editObj.put("id", objectId);
		//Ȩ���ж�
		checkTableReadPermission(table, editObj);
		//���ݿ����
		reviseTagColumnValue(objectId,table,editObj);
		beforeModify(table, editObj);
		PhoneUtils.modifyTable(tableName, editObj,PhoneUtils.getRedisObj(tableName, objectId, conn, jedis), conn);
		//�洢���� (objId, empId)
		String dbproc=table.getProcAM();
		execProc(dbproc, objectId);
		afterModify(table,editObj);
		
		CmdResult res = null;
		try {
			//����redis ����
			JSONObject ro=PhoneUtils.modifyRedisObj(objectId, table, createLuaArgObj(objectId), conn, jedis);
			//�ٴκ��ж��⴦��
			postAction(table, editObj,ro);
			res=new CmdResult( reviseColumnsOfJSONTypeValue(ro, table));
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return res;
	}
	
}
