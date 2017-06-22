package com.agilecontrol.b2bweb.cmd;

import java.util.HashMap;
import java.util.Iterator;

import org.json.JSONObject;

import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.nea.util.Validator;
import com.agilecontrol.phone.CmdResult;
import com.agilecontrol.phone.PhoneController;
import com.agilecontrol.phone.PhoneController.SQLWithParams;

/**
 * 
h1. 获取商品属性清单(针对B2B增加过滤条件，满足左边dims的显示条件)

h2. 输入

> {cmd:"b2b.grid.dimlist", selected, actid, isfav, cat, pdtsearch,table,id}

*selected* - jsonobj, key 是当前选中的column, value 是column对应的id, 举例: {dim3: 12, dim4:2}
*actid* - int 活动id, 默认-1，是否基于指定的活动进行属性过滤
*isfav* - boolean 是否面向收藏夹进行属性过滤，默认false, true的时候不读取actid
*cat* - 当前用户选择的三层分类定义，每个cat有最多n个dimid组成，按b2b:cat:tree_conf定义构造
*pdtsearch* 搜索条件，面向翻译表

h2. 输出

> [{column,desc, values}]

*column* - string，当前字段的名称, 作为key回传服务器
*desc* - string 当前字段的描述，显示在界面上
*values* - [{k:v}] 数组，每个元素都是一个单一属性的对象，属性的key是值id，value是值的显示描述，例如

<pre>
[
   {column:"dim3", desc:"季节", values:  [{1:"春"}, {2:"夏"}, {3:"冬"}]},
   {column:"dim4", desc:"价格段", values:  [{1:"~100"}, {2:"200~1000"}, {3:"1000~"}]}
   {column:"dim14", desc:"品类", values:  []}
]   
</pre>  

h2. 操作说明

默认selected 为空的时候，系统将返回全部的可选属性列表。selected对应的字段不会在进行处理

配置 ad_sql#b2b:dim_conf进行属性配置，结构["dimname"]，举例:
> ["dim14", "dim3", "dim5"]
表示将使用商品表的dim14,dim3,dim5字段进行dim显示，如果其中的key出现在selected中，将不做处理。否则就需要针对当前selected
的内容进行过滤（还需要结合cat，isfav，actid）
 * 
 * 
 * @author lsh
 */
public class GridDimList extends DimList {

	@Override
	protected HashMap<String, Object> reviseSearchCondition(JSONObject jo) throws Exception {
		
		for(Iterator it=jo.keys();it.hasNext();){
			String key=(String)it.next();
			Object v=jo.get(key);
			vc.put(key, v);
		}
		
		HashMap<String, Object> map=new HashMap<String, Object>();
		
		if(Validator.isNull(jo.optString("table"))) throw new NDSException("@b2bedit-config@"+"ad_sql#grid:"+jo.optString("table")+":meta"+"@b2bedit-found@");
		
		JSONObject gridConf=(JSONObject)PhoneController.getInstance().getValueFromADSQLAsJSON("grid:"+jo.optString("table")+":meta", null, conn);
		
		if(gridConf==null) throw new NDSException("@b2bedit-config@"+"ad_sql#grid:"+jo.optString("table")+":meta"+"@b2bedit-found@");
		
		String conf = gridConf.optString("dimfilter_sql");
		if(Validator.isNotNull(conf)){
			SQLWithParams swp = PhoneController.getInstance().parseADSQL(conf, vc, conn);
			map.put(swp.getSQL(),swp.getParams());
		}
		return map;
	}

	@Override
	public CmdResult execute(JSONObject jo) throws Exception {
		return super.execute(jo);
	}
}
