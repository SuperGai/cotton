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
 
参照Search, 面向"b_bro"即退货单表。

> {cmd:"b2b.bro.search", exprs:{"b_bfo_id":12}, table:"bro"}


h2. 返回
> [{id, billdate,tot_qty,tot_amt, docno, bfo_id:{docno,id}}]

*pdt* - 选出退货单中的一个商品进行图片展示，商品信息：{id,no,note,mainpic,color,size}
*bfo_id* - 所属订单，对象化，取订单的列表字段内容展示


 * @author wu.qiong
 *
 */
public class BroSearch extends Search {
	
	/**
	 * 将添加商品一个
	 * 
	 * 对搜索的SearchResult.toJSON对象进行改装，以符合客户端要求
	 * @param ret redis SearchResult.toJSON，直接再次编辑
	 * 
	 * @throws Exception
	 */
	protected void postAction(JSONObject ret) throws Exception{
		JSONArray ja=ret.getJSONArray("bro_s");
		//bfo_pdts, select 的部分：id，note,no,mainpic,color,size， 变量只有一个 $ids,  直接放 in (?)
		StringBuilder sb=new StringBuilder();
		
		HashMap<Integer, JSONObject> rOrders=new HashMap();//key: orderid, value: order obj
		if(ja.length()>0){
			for(int i=0;i<ja.length();i++){
				JSONObject one=ja.getJSONObject(i);
				int id=one.getInt("id");
				sb.append(id).append(";");// sql 参数velocity解析的时候，是将,当做分隔符处理的，不能在这里使用,所以用分号，sql语句里陈松也会基于分号转换
				rOrders.put(id, one);
			}
			sb.deleteCharAt(sb.length()-1);
		}else{
			return;
		}
		vc.put("ids",sb.toString());
		JSONArray pdts=PhoneController.getInstance().getDataArrayByADSQL("bro_pdts", vc, conn, true	);
		for(int i=0;i<pdts.length();i++){
			JSONObject pdt= pdts.getJSONObject(i);
			WebController.getInstance().replacePdtValues(pdt, usr, vc, jedis, conn);
			int rorderId=pdt.getInt("rorderid");
			JSONObject rorderObj= rOrders.get(rorderId);
			if(rorderObj!=null) rorderObj.put("pdt", pdt);
		}
	}
	/**
	 * 对输入的查询条件进行重构，以支持特殊的查询请求
	 * @param jo 原始的查询条件，有可能在此地被重构
	 * @return 额外的查询条件，将增加到查询where语句中
	 * key: 是要补充到sql where 部分的clause语句内容，比如 "emp_id=?"
	 * value 是问号对应的实际值，目前key仅支持一个问号比如对应上面的value= 当前emp的id
	 * 如果value是java.util.List，将允许多值代替？号
	 */
	protected HashMap<String, Object> reviseSearchCondition(JSONObject jo) throws Exception{
		HashMap<String,Object> data=new HashMap();
		data.put("ownerid=?", usr.getId());
		
		String querystr=jo.optString("pdtsearch");
		//作为退货订单号搜索
		if (Validator.isNotNull(querystr) && querystr.toLowerCase().startsWith("bro") ) {
			data.put("bro.docno=?", querystr.toUpperCase());
			return data;
		}
		StringBuilder sb=new StringBuilder();
		ArrayList params=new ArrayList();
		
		//dims
		if(usr.getLangId()!=LanguageManager.getInstance().getDefaultLangId()){
			sb=new StringBuilder("exists(select 1 from b_broitem i, m_product d, M_PRODUCT_TRANS r where i.b_bro_id=bro.id and i.m_product_id=d.id and r.m_product_id=d.id and r.b_language_id=?");
			params.add(usr.getLangId());
		}else{
			sb=new StringBuilder("exists(select 1 from b_broitem i,m_product d where i.b_bro_id=bro.id and i.m_product_id=d.id");
		}
		// search
		Table table=manager.getTable("pdt");
		// fuzzy search
		if (Validator.isNotNull(querystr)) {
			querystr = querystr.toLowerCase();
			
			//搜索内容忽略大小写
			sb.append(" and (");
			String scs = (String) table.getJSONProp("search_on");
			if (Validator.isNotNull(scs)) {
				String[] scss = scs.split(",");
				sb.append("(");
				boolean isFirst = true;
				for (String cname : scss) {
					Column cl = table.getColumn(cname);
					if (cl == null)
						throw new NDSException(table + ".search_on扩展属性中的字段未定义:" + cname);

					if (!isFirst)   
						sb.append(" or ");
					if (cl.getFkTable() == null) {
						//name 字段例外，需要读取M_PRODUCT_TRANS 
						if(usr.getLangId()!=LanguageManager.getInstance().getDefaultLangId() && cname.equals("note")){
							sb.append(" lower(r.value) like ?");
						}else{
							sb.append(" lower(d.").append(cl.getRealName()).append(") like ?");
						}
						params.add("%" + querystr + "%");
					} else {
						Table fkt = cl.getFkTable();
						String sdk = null;
						for (Column dk : fkt.getDisplayKeys()) {
							if (!"id".equals(dk.getName())) {
								sdk = dk.getName();
								break;
							}
						}
						if (sdk == null)
							throw new NDSException(table + ".search_on扩展属性中的字段:" + cname + "的FK表的dks无id以外的字段");
						sb.append(" exists(select 1 from " + fkt + " where " + fkt + ".id=d." + cl.getName()
								+ " and " + fkt + "." + sdk + " like ?)");
						params.add("%" + querystr + "%");

					}
					isFirst = false;
				}
				sb.append(")");

			} else
				throw new NDSException("需要配置" + table + "的search_on扩展属性");
			
			//
			sb.append(")");
			sb.append(")");
			data.put(sb.toString(), params);
		}		
		
		
		return data;
	}

}
