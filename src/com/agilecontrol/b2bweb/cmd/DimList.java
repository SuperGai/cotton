package com.agilecontrol.b2bweb.cmd;

import java.sql.Connection;
import java.util.*;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.aop.framework.HashMapCachingAdvisorChainFactory;

import com.agilecontrol.b2b.schema.Column;
import com.agilecontrol.b2b.schema.Table;
import com.agilecontrol.b2bweb.DimDefTranslator;
import com.agilecontrol.b2bweb.DimTranslator;
import com.agilecontrol.b2bweb.cmd.PdtSearch.DimValue;
import com.agilecontrol.nea.core.query.QueryEngine;
import com.agilecontrol.nea.core.query.QueryException;
import com.agilecontrol.nea.core.util.MessagesHolder;
import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.nea.util.Tools;
import com.agilecontrol.nea.util.Validator;
import com.agilecontrol.phone.Admin;
import com.agilecontrol.phone.CmdHandler;
import com.agilecontrol.phone.CmdResult;
import com.agilecontrol.phone.LanguageManager;
import com.agilecontrol.phone.PhoneConfig;
import com.agilecontrol.phone.PhoneController;
import com.agilecontrol.phone.UserObj;

import redis.clients.jedis.Jedis;

/**

h1. 获取商品属性清单

h2. 输入

> {cmd:"b2b.dim.list", selected, actid, isfav, cat, pdtsearch}

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

2016-12-12 @author lsh
针对b2b不同模式，增加子类 *DimList，满足不同需求

 * @author yfzhu
 *
 */
@Admin(mail="yfzhu@lifecycle.cn")
public class DimList extends CmdHandler {
	
	private boolean isFav=false;
	private boolean isCart=false;//是否在购物车中搜索

	private int actId=-1;
	private String blank;//'空白'的语言翻译
	private String querystr;//"pdtsearch" from web 就是模糊搜索
	private JSONObject config;//前台传来的jo
	class DimValue{
		String dim; //eg: dim15
		int id; // 当前选择的值，eg: 13，即 m_dim15_id=13
	}
	private ArrayList<DimValue> dimNodes;
	/**
	 * 要访问的dim的可选值，需要基于cat,actid,isfav等进行过滤，如果用户输入了搜索的条件完全匹配每个商品分类，将优选此分类，不再基于商品过来，最典型的就是品类的搜索
	 * @param dim 要访问的dim
	 * @param dimNodes 过滤条件,这些是已经确认掉的查询条件
	 * 
	 * @return [{id:value}] format
	 */
	private JSONArray filterDimValues(String dim,ArrayList<DimValue> dimNodes) throws Exception{
		JSONArray values=new JSONArray();

		StringBuilder sb=new StringBuilder();
		ArrayList params=new ArrayList();
		
		boolean isDefaultLang=(usr.getLangId()==LanguageManager.getInstance().getDefaultLangId());
		
		
		/**
		 * sql format: select distinct dim_id, dim from v_pdtdims dims where catdim_id=xxx 
		 * 
		 * 对应价格带属性，dim13的写法是： select distinct dim13.id, dim13.orderno from b_mk_pdt m, v_pdtdims d, m_dim dim13 where m.isactive='Y' and dim13.id=m.m_dim13_id and m.m_product_id=d.pdtid
		 */
		if(PhoneConfig.PRICE_RANGE_DIM.equals(dim)){
			sb.append("select distinct dim13.id dim13_id,dim13.attribname dim13, dim13.orderno dim13_orderno from b_mk_pdt m, v_pdtdims d, m_dim dim13 ");
			//有搜索复杂度会上升很多
			if(Validator.isNotNull(querystr)){
				if(!isDefaultLang){
					sb.append(", m_product_trans r, m_product p where r.m_product_id=d.pdtid and r.b_language_id=? and p.id=d.pdtid and ");
					params.add(usr.getLangId());
				}else{
					sb.append(", m_product p where p.id=d.pdtid and ");
				}
			}else{
				sb.append(" where ");
			}
			sb.append("m.isactive='Y' and dim13.id=m.m_dim13_id and m.m_product_id=d.pdtid ");
		}else{
			sb.append("select distinct d.").append(dim).append("_id,d.").append(dim).append(",d.").append(dim).append("_orderno from v_pdtdims d, b_mk_pdt m ");
			//有搜索复杂度会上升很多
			if(Validator.isNotNull(querystr)){
				if(!isDefaultLang){
					sb.append(", m_product_trans r, m_product p where r.m_product_id=d.pdtid and r.b_language_id=? and p.id=d.pdtid and ");
					params.add(usr.getLangId());
				}else{
					sb.append(", m_product p where p.id=d.pdtid and ");
				}
			}else{
				sb.append(" where ");
			}
			sb.append("m.isactive='Y' and d.").append(dim).append("_id is not null and m.m_product_id=d.pdtid");
		}
		for( DimValue dv:dimNodes){
			if(dv.id==-2){
				if(dv.dim.equals(PhoneConfig.PRICE_RANGE_DIM))
					sb.append(" and m.m_").append(dv.dim).append("_id is null");
				else
					sb.append(" and d.").append(dv.dim).append("_id is null");
			}else{
				if(dv.dim.equals(PhoneConfig.PRICE_RANGE_DIM))
					sb.append(" and m.m_").append(dv.dim).append("_id=?");
				else
					sb.append(" and d.").append(dv.dim).append("_id=?");
				params.add(dv.id);
			}
		}
		//market
		sb.append(" and m.b_market_id=?");
		params.add(usr.getMarketId());
		
		// search
		Table table=manager.getTable("pdt");
		// fuzzy search
		if (Validator.isNotNull(querystr)) {
			/**
			 * 查询字段，定义：String, 为字段名用逗号分隔，如: "docno,cust_id";
			 * 对于fk类型的字段，将默认去查找fk的第一个不是id的dks 字段
			 * 
			 */
			//搜索内容忽略大小写
			sb.append(" and (");
			querystr = querystr.toLowerCase();
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
						if(!isDefaultLang && cname.equals("note")){
							sb.append(" lower(r.value) like ?");
						}else{
							sb.append(" lower(p.").append(cl.getRealName()).append(") like ?");
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
						sb.append(" exists(select 1 from " + fkt + " where " + fkt + ".id=p." + cl.getName()
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
		}		
		
		// 接下来要处理 actid,isfav
		if(actId!=-1){
			sb.append(" and exists(select 1 from b_prmt_pdt a where a.m_product_id=d.pdtid and a.b_prmt_id=? and a.isactive='Y')");
			params.add(actId);
		}
		
		if(isFav){
			sb.append(" and exists(select 1 from B_FAVOURITE f where f.m_product_id=d.pdtid and f.user_id=? and f.isactive='Y')");
			params.add(usr.getId());
		}
		if(isCart){
			sb.append(" and exists(select 1 from b_cart c where c.b_market_id=m.b_market_id and c.m_product_id=m.m_product_id and c.user_id=? and c.isactive='Y')");
			params.add(usr.getId());
		}
		//新增过滤条件，满足B2B不同模式下左边dimlist选项的需求
		HashMap<String, Object> additionalParam = reviseSearchCondition(config);
		
		if (additionalParam != null) {
			for (String key : additionalParam.keySet()) {
				Object value = additionalParam.get(key);
				sb.append(" and ").append(key);
				if(value instanceof List){
					for(Object v: (List)value){
						params.add(v);
					}
				}else if(value instanceof Object[]){
					for(Object v: (Object[])value){
						params.add(v);
					}
					
				}else
					params.add(value);
			}
		}
		
		sb.append(" order by ").append(dim).append("_orderno");
		
		String sql=sb.toString();
		JSONArray rows=engine.doQueryJSONArray(sql, params.toArray(), conn);
		for(int i=0;i<rows.length();i++){
			JSONArray row=rows.getJSONArray(i);
			int id=row.getInt(0);//dim.id
			
			String val=row.optString(1);//dim.name
			if(Validator.isNull(val)) val=blank;
			//translate 
			val=DimTranslator.getInstance().getTranslateName(id, usr.getLangId(), val, conn);
			JSONObject one=new JSONObject().put(String.valueOf(id), val);
			values.put(one);
		}
		return values;
	}
	/**
	 * 根据传人的cat分解出dim的值, 并将界面选择的dim也一并加入
	 * @param cat - 形式如"12.145.8438", 分别对应 b2b:cat:tree_conf 的每个dim的配置
	 * @param selectedDims - format  {dim3: 12, dim4:2}
	 * @throws Exception
	 */
	private ArrayList<DimValue> initCatNodes(String cat,JSONObject selectedDims ) throws Exception{
		ArrayList<DimValue> dimNodes=new ArrayList();
		if(Validator.isNotNull(cat)){
			String[] ids =cat.split("\\.");//获取所给cat的各个层级的值
			JSONArray catDef= (JSONArray)PhoneController.getInstance().getValueFromADSQLAsJSON("b2b:cat:tree_conf", conn);
			for(int i=0;i< ids.length;i++){
				DimValue dv=new DimValue();
				dv.id=Tools.getInt( ids[i],-1);
				if(dv.id==-1){
					logger.warn("not a valid int for "+ cat+"["+ i+"] ");
				}else{
					dv.dim= catDef.getString(i);
					dimNodes.add(dv);
				}
			}
		}
		for(Iterator it=selectedDims.keys();it.hasNext();){
			String dim=(String)it.next();
			DimValue dv=new DimValue();
			dv.dim=dim;
			dv.id= selectedDims.getInt(dim);
			dimNodes.add(dv);
		}
		return dimNodes;
	}
	/**
	 * 增加过滤条件
	 * 
	 * @param jo
	 * @return
	 * @throws Exception
	 */
	protected HashMap<String, Object> reviseSearchCondition(JSONObject jo) throws Exception{
		return null;
	}
	/**
	 * 初始化前台jo
	 */
	private void init(JSONObject jo) {
		this.config = jo;
	}
	@Override
	public CmdResult execute(JSONObject jo) throws Exception {
		
		init(jo);
		//key:dim, value column value
		JSONObject selectedDims= jo.optJSONObject("selected");
		if(selectedDims==null)selectedDims=new JSONObject();
		
		blank=MessagesHolder.getInstance().getMessage(locale, "blank");
		isFav= jo.optBoolean("isfav",false);
		isCart=jo.optBoolean("iscart",false);
		actId= jo.optInt("actid", -1);
		dimNodes=initCatNodes(jo.optString("cat"),selectedDims );
		this.querystr= jo.optString("pdtsearch");
		
		JSONArray conf=(JSONArray)PhoneController.getInstance().getValueFromADSQLAsJSON("b2b:dim_conf", conn, false);
		JSONArray dims=new JSONArray();
		DimDefTranslator ddt=DimDefTranslator.getInstance();
		
		//conf
		
		//客户要求：输入的内容和某商品属性一致时，直接选中此属性，不再针对商品搜索, 类似京东，在搜索框里输入了某品牌名称后，直接就匹配此品牌了。
		//并且其他商品属性的搜索就不再以此作为商品过滤条件
		String querystrDimColumn=null;
		JSONArray querystrDimColumnValues=null;
		for(int i=0;i<conf.length();i++){
			String column=conf.getString(i);
			if(selectedDims.has(column))continue;
			if(Validator.isNotNull(querystr)){
				int dimId=DimTranslator.getInstance().getDimId(usr.getLangId(), querystr, conn);
				if(dimId!=-1){
					querystrDimColumn=column;
					JSONObject one=new JSONObject();
					one.put(String.valueOf(dimId), querystr);
					querystrDimColumnValues=new JSONArray();
					querystrDimColumnValues.put(one);
					querystr=null;//remove
					
					DimValue dv=new DimValue();
					dv.dim=column;
					dv.id=dimId;
					dimNodes.add(dv);
					break;
				}
			}
		}
		
		
		for(int i=0;i<conf.length();i++){
			String column=conf.getString(i);
			if(selectedDims.has(column))continue;
			
			JSONObject row=new JSONObject();
			row.put("column", column);
			String desc= ddt.getTranslateName(column, usr.getLangId(), conn);
			if(desc==null) desc=column;
			row.put("desc", desc);
			if(column.equals(querystrDimColumn)){
				row.put("values", querystrDimColumnValues);
			}else{
				JSONArray values=filterDimValues(column,dimNodes);
				row.put("values",values);
			}
			dims.put(row);
		}
		
		return new CmdResult(dims);
	}
}

