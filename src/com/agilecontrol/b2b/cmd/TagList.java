package com.agilecontrol.b2b.cmd;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;

import com.agilecontrol.b2b.query.SearchResult;
import com.agilecontrol.b2b.schema.Column;
import com.agilecontrol.phone.CmdHandler;
import com.agilecontrol.phone.CmdResult;

/**
 * 
h1. TagList ��ѯȫ����ǩ 

��ǩ����ΪTagColumn����ƣ�TagColumn�Ľ��ܼ� [[TableManager_]]

h3. ����

* �г����±�ǩ��: pdt_tag, sup_tag, cust_tag

h3. ����

��redis��ȡ��ǰ���ָ���̼ҿ��õ����б�ǩ���������Զ���ı�ǩ����ƽ̨����ı�ǩ

* mj:pdt_tag - hash key: name, value: id ƽ̨����ǩ
* mj:pdt_tag_s - list, pdt_tag.id ƽ̨����ǩȫ���б�
* com:$comid:pdt_tag - hash  key: name, value:id �̼Ҽ���ǩ
* com:$comid:pdt_tag_s - list,  pdt_tag.id �̼Ҽ���ǩȫ���б�

h3. ����

> {table}
* table - String ������Ŀǰ֧�� pdt_tag, sup_tag, cust_tag

h3. ���

> {"$table_s":[{Tag}]}
* Tag - json���� {id,name,is_hq}
** id - long id
** name - string ��ǩ��
** hq - boolean �Ƿ�ƽ̨��, true ��ʾ��  hq- headquarter

�� {pdt_tag_s: [{id: 1, name:"A��",hq:true}]}

 *
 *@author yfzhu@lifecycle.cn
 */
public class TagList extends CmdHandler {

	/*
	 * ��ѯ��Ʒ�������ڸ��̼ҵ�������Ʒ����
	 */
	@Override
	public CmdResult execute(JSONObject jo) throws Exception {
		
		String tableName=this.getString(jo, "table");
		ArrayList<Long> tagIds=this.getTagList(tableName, false);
		JSONArray ja=new JSONArray();
		
		ArrayList<Column> cols=manager.getTable(tableName).getColumns("id,name,com_id,en");
		for(Long objectId: tagIds){
			JSONObject tag=this.fetchObject(objectId, tableName,cols, null);
			long comId= tag.optLong("com_id", -1);
			
			tag.put("hq",comId!=usr.getComId());
			tag.remove("com_id");
			if("N".equals(tag.optString("en", "Y")))continue;//����ʾ���ͻ���
			ja.put(tag);
		}
		
		
		JSONObject res=new JSONObject();
		res.put(tableName+"_s", ja);
		
		return new CmdResult(res);
	}

}
