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
h1. TagList 查询全部标签 

标签表是为TagColumn而设计，TagColumn的介绍见 [[TableManager_]]

h3. 条件

* 列出以下标签表: pdt_tag, sup_tag, cust_tag

h3. 操作

从redis读取当前针对指定商家可用的所有标签，包括他自定义的标签，和平台级别的标签

* mj:pdt_tag - hash key: name, value: id 平台级标签
* mj:pdt_tag_s - list, pdt_tag.id 平台级标签全部列表
* com:$comid:pdt_tag - hash  key: name, value:id 商家级标签
* com:$comid:pdt_tag_s - list,  pdt_tag.id 商家级标签全部列表

h3. 输入

> {table}
* table - String 表名，目前支持 pdt_tag, sup_tag, cust_tag

h3. 输出

> {"$table_s":[{Tag}]}
* Tag - json对象 {id,name,is_hq}
** id - long id
** name - string 标签名
** hq - boolean 是否平台级, true 表示是  hq- headquarter

如 {pdt_tag_s: [{id: 1, name:"A货",hq:true}]}

 *
 *@author yfzhu@lifecycle.cn
 */
public class TagList extends CmdHandler {

	/*
	 * 查询商品表中属于该商家的所有商品集合
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
			if("N".equals(tag.optString("en", "Y")))continue;//不显示到客户端
			ja.put(tag);
		}
		
		
		JSONObject res=new JSONObject();
		res.put(tableName+"_s", ja);
		
		return new CmdResult(res);
	}

}
