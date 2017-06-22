package com.agilecontrol.b2b.cmd;

import org.apache.velocity.VelocityContext;
import org.json.*;

import java.util.*;

import com.agilecontrol.b2b.schema.*;
import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.nea.util.Tools;
import com.agilecontrol.nea.util.Validator;
import com.agilecontrol.phone.CmdHandler;
import com.agilecontrol.phone.CmdResult;
import com.agilecontrol.phone.PhoneController;
import com.agilecontrol.phone.PhoneUtils;

/**
 * 
 ���ָ������֧���Զ����������:
 table: String ���ڱ�����: usr, com
 id: long ����id
 items: boolean | string, 
 	false(Ĭ��), ��������ϸ��
 	true: �����е���ϸ������meta�Ķ�����
 	"table1,table2" ָ������ϸ���ö��ŷָ� 
 
 
  ����ȡ�����ļ� ad_sql#table:<table>:meta ����ʽ
  Table
  {
  	name, dk:[column1, column2], perm
  	cols:[{Column}]
  }
  name - string ����
  dk - ������string, �� [String],  ��ʾ��ǰ����Ϊ�����ʱ����Ҫ������ʾ���ֶ�
  perm - [String] Ȩ�ޣ���com�� ��ʾֻ��com�ڵ�Ա���ɼ���Ҫ��ǰuser��comid=��ǰ��¼��com_id�ֶ�, "admin"��ʾ��ǰ�û���Ҫ�ǹ���Ա���
  
  Column
  {
  	name, fktable, type, edit
  }
  name - string �ֶε����ݿ���
  fktable - String fk������ƣ���fktable is not null ���ֶζ��󻯣���ֱ�������ֶ�����Ӧ��������
  type - String "string","long", "time", "datenumber"
  edit - boolean �Ƿ������޸ģ�����insert/update, Ĭ��Ϊtrue
  null - boolean �Ƿ�����Ϊ�գ�Ĭ��true
  
  ����:
  {key: value}
  key: ��ǰ�����ͨ�ֶ����� value�������ݿ�ֵ
  ����ǵ�ǰ���fk���͵��ֶΣ�value ��fk�ֶζ�Ӧ���������ʾ����ɵ�object������: {id,name,img}
  ���������items����ʾ��
  �����ǰitem��1:1 ���������Զ�����ʽ��ʾvalue��name Ϊrefby�����name
  ���item��1:m����������array��ʽ��ʾvalue���ṹ:[{}] Ԫ�ض�����ָ������б���ʾ�ֶε�����
  
  
  
  
  {cnt:2000, start:0, range: 20, cachekey:"list:$table:$uid:$uuid", data:[{}]} 
  ����cnt���2000������ʾ��ѯ���û���ݣ�ֻ�ǵ���Ϊֹ
  start ��ǰ��ʼ��idx, range: ��ǰ��������������cachekey: ���ڻ�ȡ��ǰ��ѯ�����е�����, cachekey��Ӧ�Ĳ�ѯ�����ڲ�ѯֹͣʹ�õ�30���Ӻ�ʧЧ���ͻ��˽�������Ҫ���¹����ѯ
  data: ��ָ������б���ʾ�ֶε�����
  ��ҳ��ѯ����һ��ҳ�����󷽷���: 
  {cmd:GetList, cachekey:"list:$table:$uid:$uuid", start: xxx, range: 20 }�� ϵͳ��ͨ��cachekey��֤�û��ͻ�����ڱ�
  ����:
  {cnt:2000, start:0, range: 20, cachekey:"list:$table:$uid:$uuid", data:[{}]}  
  
  ����redis���棺"list:$table:$uid:$uuid" �� list of id
  
 * @author yfzhu
 *
 */
public class ObjectGet extends CmdHandler {
	
	/**
	 * redis��ɻ�����º�Ĵ���
	 * @param table
	 * @param retObj ����������ģ������ؿͻ��˵Ķ��󣬿����ع�, �����Ѿ���װ����Ķ���
	 * @throws Exception
	 */
	protected void postAction(Table table, JSONObject retObj) throws Exception{
		
	}
	

	/**
	 * ����򵥶����ȡ�󣬷��㿪��У��Ȩ��
	 * @param table
	 * @param fetchObj ���������ǵ�ǰ��¼չ�� 
	 * @throws Exception
	 */
	protected void checkPermission(Table table, JSONObject fetchObj) throws Exception{
		
	}
	
	@Override
	public CmdResult execute(JSONObject jo) throws Exception {

		Table table=findTable(jo,"Get");
		String tableName=table.getName();

		long objectId=getLong(jo,"id");
		JSONObject obj=null;
		Object items=jo.opt("items");
		if(items!=null){
			if(items instanceof Boolean){
				obj=fetchObject(objectId, tableName, (Boolean)items);
			}else if(items instanceof JSONArray){
				String[] its=new String[((JSONArray)items).length()];
				for(int i=0;i<its.length;i++) its[i]=((JSONArray) items).getString(i);
				obj=fetchObject(objectId, tableName,its);
			}else{
				String[] its=items.toString().split(",");
				obj=fetchObject(objectId, tableName,its);
			}
		}else{
			obj=fetchObject(objectId, tableName,null);
		}
		//read from ad_sql#table:$table:assemble
		JSONArray conf=(JSONArray)PhoneController.getInstance().getValueFromADSQLAsJSON("table:"+ tableName+":assemble:obj",new JSONArray(), conn);
		this.assembleObject(obj, conf, manager.getTable(tableName));
		
		checkPermission(table, obj);
		
		postAction(table, obj);
		CmdResult res=new CmdResult(reviseColumnsOfJSONTypeValue(obj, table) );
		return res;
	}
	
	
}
