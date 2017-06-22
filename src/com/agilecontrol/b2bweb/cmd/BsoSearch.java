package com.agilecontrol.b2bweb.cmd;

import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONObject;

import com.agilecontrol.b2b.cmd.Search;
import com.agilecontrol.b2b.schema.Column;
import com.agilecontrol.b2b.schema.Table;
import com.agilecontrol.b2bweb.WebController;
import com.agilecontrol.b2bweb.cmd.PdtSearch.DimValue;
import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.nea.util.Validator;
import com.agilecontrol.phone.Admin;
import com.agilecontrol.phone.CmdHandler;
import com.agilecontrol.phone.CmdResult;
import com.agilecontrol.phone.LanguageManager;
import com.agilecontrol.phone.PhoneConfig;
import com.agilecontrol.phone.PhoneController;

/**
 
h1. 发货单搜索 
 
参照Search, 面向"b_bso"即发货单表。目前仅支持基于订单id的搜索

> {cmd:"b2b.bso.search", exprs:{"b_bfo_id":12}, table:"bso"}

h2. 返回
  2016-11-25 修正@author lsh
> [{id, billdate,tot_qty_send, docno, pdt:{},lname, ldocno, bfo_id:{docno,id},l_com_id;{name,code} }]

*pdt* - 选出发货单中的一个商品进行图片展示，商品信息：{id,no,note,mainpic,color,size}
*bfo_id* - 所属订单，对象化，取订单的列表字段内容展示


 * @author yfzhu
 *
 */
@Admin(mail="yfzhu@lifecycle.cn")
public class BsoSearch extends Search {
	
	/**
	 * 将添加商品一个
	 * 
	 * 对搜索的SearchResult.toJSON对象进行改装，以符合客户端要求
	 * @param ret redis SearchResult.toJSON，直接再次编辑
	 * 
	 * @throws Exception
	 */
	protected void postAction(JSONObject ret) throws Exception{
		JSONArray ja=ret.getJSONArray("bso_s");//返回的发货单列表
		//bso_pdts, select 的部分：id，note,no,mainpic,color,size， 变量只有一个 $ids,  直接放 in (?)
		StringBuilder sb=new StringBuilder();
		
		HashMap<Integer, JSONObject> orders=new HashMap();//key: orderid, value: order obj
		if(ja.length()>0){
			for(int i=0;i<ja.length();i++){
				JSONObject one=ja.getJSONObject(i);
				int id=one.getInt("id");
				sb.append(id).append(";");// sql 参数velocity解析的时候，是将,当做分隔符处理的，不能在这里使用,所以用分号，sql语句里陈松也会基于分号转换
				orders.put(id, one);
			}
			sb.deleteCharAt(sb.length()-1);
		}else{
			return;
		}
		vc.put("ids",sb.toString());
		JSONArray pdts=PhoneController.getInstance().getDataArrayByADSQL("bso_pdts", vc, conn, true	);//获得发货单上的商品
		for(int i=0;i<pdts.length();i++){
			JSONObject pdt= pdts.getJSONObject(i);
			WebController.getInstance().replacePdtValues(pdt, usr.getLangId(), usr.getMarketId(), vc, jedis, conn);
			int soId=pdt.getInt("orderid");
			JSONObject orderObj= orders.get(soId);
			if(orderObj!=null) orderObj.put("pdt", pdt);
		}
	}
	
	/**
	 * 当前货单对应的订单的创建人必须是当前操作人
	 * 
	 * @param jo 原始的查询条件，有可能在此地被重构
	 * @return 额外的查询条件，将增加到查询where语句中
	 * key: 是要补充到sql where 部分的clause语句内容，比如 "emp_id=?"
	 * value 是问号对应的实际值，目前key仅支持一个问号比如对应上面的value= 当前emp的id
	 * 如果value是java.util.List，将允许多值代替？号
	 */
	protected HashMap<String, Object> reviseSearchCondition(JSONObject jo) throws Exception{
		HashMap<String,Object> data=new HashMap();
		//当前货单对应的订单的创建人必须是当前操作人
		data.put("exists(select id from b_bfo where b_bfo.id=bso.b_bfo_id and bso.ownerid=?)", usr.getId());
		return data;
	}

}
