package com.agilecontrol.b2bweb.cmd;

import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONObject;

import com.agilecontrol.b2b.cmd.ObjectGet;
import com.agilecontrol.b2b.cmd.Search;
import com.agilecontrol.b2b.schema.Table;
import com.agilecontrol.b2bweb.WebController;
import com.agilecontrol.nea.core.schema.ClientManager;
import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.nea.util.Tools;
import com.agilecontrol.nea.util.Validator;
import com.agilecontrol.phone.Admin;
import com.agilecontrol.phone.CmdHandler;
import com.agilecontrol.phone.CmdResult;
import com.agilecontrol.phone.PhoneController;
import com.agilecontrol.phone.PhoneUtils;
import com.agilecontrol.phone.PhoneController.SQLWithParams;

/**

h1. ��ȡ���߱༭����Ķ���

h2. ����

> {cmd:"b2b.grid.meta", key, canchange}

*canchange* �Ƿ������޸�key��Ӧ��ѡ��ֵ��ȫ�ֿ���
*key*  ����ad_sql#online_edit_kvs�����key�������ж������Ӧ��Ĭ��ֵ���������ﴫ���ȥ������ҪĬ�����ûid=101���Ҳ������޸ģ�
> {cmd:"b2b.grid.meta", "acts": 101, canchange: false}

h2. ���

>{kvs: [{key, desc, values, type, default }]}

*key* - �ͻ��˺ͷ�������ʾ�Ĺؼ���
*desc* - �ͻ��˽�����ʾ����
*values* - array of array,  ����v1,v2���� ���е�һ���ǽ���ɼ����������ڶ����Ǻ�̨���ܵ�value, ����acts�������ǻ���ƣ��ڶ�����act.id
*type* - string: ��ѡ��"checkbox"|"select"
*default* - ȱʡֵ��������ã��ͻ���Ӧ�ý���ӦֵĬ��ѡ�У���Ӧcheckbox�࣬defaultֵ��boolean��0,1
*canchange* - boolean �Ƿ������޸ģ����false����ʾ��ǰѡ����޸�

h2. ����

kvs�Ķ����ad_sql#online_edit_kvs ��ȡ

kv������Ŀ����: ad_sql#online_edit_kvs, kv ���ڽ������ѡ���������ʽ��checkbox����select, checkbox��ʾѡ�л�ѡ�С�select�����ƻѡ���ģʽ��select��ʽ��������values��Ӧsql

[{key, desc, filtersql, valuesql, default}]

*key* - �ͻ��˺ͷ�������ʾ�Ĺؼ���
*desc* - �ͻ��˽�����ʾ����
*filtersql* - ��ƴ�ӵ���Ʒ��������е�sql���� ad_sql��name��ָ����Ӧsql��䣬sql���ĸ�ʽ�� select 1 from xxx where xxx.pdtid=b_mk_pdt.id and xxx.ownerid=?, ֧�ֵģ���Ӧ�ı�����

��ӦB2B��Ŀ
$usrid

���ڶ�������Ŀ
$funitid
$fairid
$usrid

*valuesql* - ���ɽ���ѡ���sql��������ã������ʹ��selectģʽ��sql��ʽ�淶��select description,value from xxxx, ���е�һ���ǽ���ɼ����������ڶ����Ǻ�̨���ܵ�value, ����acts�������ǻ���ƣ��ڶ�����act.id
*default* - ȱʡֵ�����Բ����ã���Ӧcheckbox�࣬defaultֵ��boolean��0,1



 * @author yfzhu
 *
 */
@Admin(mail="yfzhu@lifecycle.cn")
public class GridMeta extends ObjectGet {
	
	/**
	 * 
	 */
	public CmdResult execute(JSONObject jo) throws Exception {
		
		boolean canChange=jo.optBoolean("canchange", true);
		vc.put("marketid", usr.getMarketId());
		
		//2016-12-9 lsh
		if(Validator.isNull(jo.optString("table"))) throw new NDSException("@b2bedit-config@"+"ad_sql#grid:"+jo.optString("table")+":online_edit_kvs"+"@b2bedit-found@");
		
		JSONArray kvDefs=(JSONArray) PhoneController.getInstance().getValueFromADSQLAsJSON("grid:"+jo.optString("table")+":online_edit_kvs", conn);
		JSONArray kvs=new JSONArray();
		for(int i=0;i<kvDefs.length();i++){
			//{key, desc, filtersql, valuesql, default}
			JSONObject kvDef=kvDefs.getJSONObject(i);
			//{key, desc, values, type, default }
			JSONObject kv=new JSONObject();
			String key=kvDef.getString("key");
			kv.put("key", key);
			kv.put("desc", kvDef.getString("desc"));
			String defaultValue= jo.optString(key);
			String valueSQL=kvDef.optString("valuesql");//ad_sql#name
			if(Validator.isNull(valueSQL)){
				kv.put("type", "checkbox");
				
			}else{
				//���е�һ���ǽ���ɼ����������ڶ����Ǻ�̨���ܵ�value
				vc.put("uid", usr.getId());
				vc.put("marketid", usr.getMarketId());
				JSONArray values=(JSONArray)PhoneController.getInstance().getDataArrayByADSQL(valueSQL, vc, conn, false);
				kv.put("type", "select");
				kv.put("values", values);
			}
			//ȡĬ��ֵ
			if(Validator.isNotNull(defaultValue))kv.put("default", defaultValue);
			if(!canChange) kv.put("canchange", false);
			kvs.put(kv);
		}
		
		
		JSONObject obj=new JSONObject();
		obj.put("kvs", kvs);
		
		return new CmdResult(obj);
	}
}













