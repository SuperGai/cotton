package com.agilecontrol.b2bweb.cmd;


import org.json.JSONArray;
import org.json.JSONObject;

import com.agilecontrol.b2bweb.WebController;
import com.agilecontrol.nea.core.query.QueryEngine;
import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.nea.util.Validator;
import com.agilecontrol.phone.Admin;
import com.agilecontrol.phone.CmdHandler;
import com.agilecontrol.phone.CmdResult;
import com.agilecontrol.phone.PhoneController;

/**


h1. 获取商品分类下的前几名的商品

h2. 场景

首页或列表

h2. 输入

> {cmd:"b2b.cat.get", cat, withsubcat, subcatcnt, pdtcnt}

*dim* - 商品属性，来自CatTree的输出结果，例如"13.34" 表dim1的id是13，且dim2的id是34的分类定义 

*withsubcat* - 是否显示下级分类 boolean default false
*subcatcnt* - 下级分类个数, int，default 3
*pdtcnt* - 将要显示的每个分类中的商品数量， int，default 10


h2. 输出

当withsubcat=false的时候，

> [{pdt}]

当withsubcat=true,

> [{cat}]

pdt 对象定义
> {pdtid,note,no,mainPic,price,tags}
*pdtid* - int pdtid
*note* - string 商品备注 
*no* - string 商品编号
*mainPic* -string 商品图片
*price* - double 显示的价格
*tags* - jsonarray of string 商品标签，如: ["爆款","范冰冰同款"]

cat 对象定义
> {dim,name,pic,content}

*id* - m_dim.id 
*title* - string 分类名称, m_dim.desc
*img* - string 分类图片
*content* - [{pdt}]  商品列表

h3. 实现

获得指定分类的下级分类的方法

加载CatTree，读取 "b2b:cat:tree:$marketname:$lang:dim", 根据客户端的dim值，找到value即list of subcat dim obj {dim,name,pic},
读取后，还需要根据

如何获取dim下的商品列表
redis 缓存: key: "b2b:cat:pdts:$marketname:$dimid", value: list of pdtid 
$dimid - "132.34" 这样的形式，是cat的dimid，可解析为对应于 ad_sql#b2b:cat:tree_conf 中定义dim层级，比如: ["dim1", "dim3", "dim14"] 的各个值，从而可以构造对应的sql语句
*pdts* - 在当前商品分类下的pdt id array，order by B_MK_PDT.orderno

{pdt} 缓存设计

redis逐行读取pdtid, 然后从利用redis obj来获取定义，key: "pdt:$id", value: {pdt list columns}


 * @author yfzhu
 *
 */
@Admin(mail="yfzhu@lifecycle.cn")
public class CatGet extends CatTree {
	
	/**
	 * @param jo - {cat:"14.4", withsubcat:true, subcatcnt:6, pdtcnt:12}
	 */
	public CmdResult execute(JSONObject jo) throws Exception {
		//得到ad_sql的层级，来与cat中的dimid匹配
		String ad_sql = "b2b:cat:tree_conf";//获得ad_sql
		JSONArray level = (JSONArray)PhoneController.getInstance().getValueFromADSQLAsJSON(ad_sql, conn);
		//加载cattree
		getAllDim((usr.getMarketId()), (usr.getLangId()));
		//得到pdtint的默认值，返回相应数目的商品jsonobject
		int pdtcnt = jo.optInt("pdtcnt",12);
		
		String[] str = jo.optString("cat").split("\\.");//获取所给cat的各个层级的值
		//判断dim层级是否与cat匹配，不匹配无法拼凑所给sql
		if(level.length() < str.length){
			throw new NDSException("dim层次不符与请求不符"); 
		}
		
		//根据所给dimid拼凑成条件sql
		StringBuilder pdimid = new StringBuilder();
		for(int i = 0;i < str.length;i++){
			if(i==0){
				pdimid.append("p.m_"+level.getString(i)+"_id = "+str[i]);
			}
			else{
				pdimid.append(" and p.m_"+level.getString(i)+"_id = "+str[i]);
			}
		}
		String sql = "select"
				+ " p.id from b_mk_pdt m, m_product p "
				+ "where p.id = m.m_product_id "
				+ "and m.b_market_id = ? "
				+ "and "+pdimid+" "
						+ "and m.isactive = 'Y'  "
						+ "order by m.orderno desc";
		logger.debug("sql语句是:"+sql);
		JSONArray ja = QueryEngine.getInstance().doQueryObjectArray(sql, new Object[]{usr.getMarketId()},conn);
		//获得到一个含有字段为p.id的JSONObject的JSONArray，遍历，通过方法生成一个满足需求的JSONObject，然后放入索要返回的array中去
		JSONArray newarray = new JSONArray();
		//对于默认需求商品的数量，如果需求小于生成的数量，直接遍历需求，反之，直接遍历所有已生成的pdt
		if(pdtcnt < ja.length()){
			for(int j = 0;j < pdtcnt;j++){
				String pid = new String();
				pid = ja.getJSONObject(j).optString("id");
				JSONObject obj = fetchObject(Long.valueOf(pid), "pdt",false);
				JSONObject objTrans = WebController.getInstance().replacePdtValues(obj, usr, vc, jedis, conn);
				newarray.put(objTrans);
			}
		}else{
			for(int j = 0;j < ja.length();j++){
				String pid = new String();
				pid = ja.getJSONObject(j).optString("id");
				JSONObject obj = fetchObject(Long.valueOf(pid), "pdt",false);
				JSONObject objTrans = WebController.getInstance().replacePdtValues(obj, usr, vc, jedis, conn);
				newarray.put(objTrans);
			}
		}
		  
		return new CmdResult(newarray);
	}
	
}
