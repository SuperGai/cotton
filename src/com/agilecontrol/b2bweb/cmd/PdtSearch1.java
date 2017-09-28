package com.agilecontrol.b2bweb.cmd;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.velocity.VelocityContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.agilecontrol.b2b.cmd.Search;
import com.agilecontrol.b2b.schema.Column;
import com.agilecontrol.b2b.schema.Table;
import com.agilecontrol.b2b.schema.TableManager;
import com.agilecontrol.b2bweb.WebController;
import com.agilecontrol.nea.core.util.MessagesHolder;
import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.nea.util.Tools;
import com.agilecontrol.nea.util.Validator;
import com.agilecontrol.phone.Admin;
import com.agilecontrol.phone.CmdResult;
import com.agilecontrol.phone.LanguageManager;
import com.agilecontrol.phone.PhoneConfig;
import com.agilecontrol.phone.PhoneController;
import com.agilecontrol.phone.PhoneUtils;

/**
 参照Search, 每个商品数据需要读取翻译表M_PRODUCT_TRANS
 进行翻译

> {cmd:"b2b.pdt.search", 常规搜索条件, dims, actid, isfav, types, cat,  pdtsearch, iscart}

*dims* - jsonobj, key 是当前选中的column, value 是column对应的id, 举例: {dim3: 12, dim4:2}
*actid* - int 活动id, 默认-1，是否基于指定的活动进行属性过滤
*isfav* - boolean 是否面向收藏夹进行属性过滤，默认false, true的时候不读取actid
*types* -- array [{type:"",values:[]},{type:"",value:}] type -- 筛选类型 value -- 需要添加到sql中的值 ,
*		  若没有value值,将省略或为 null  type类型在ad_sql#pdtsearchConf 配置中
*cat* - 当前用户选择的三层分类定义，每个cat有最多n个dimid组成，按b2b:cat:tree_conf定义构造
*pdtsearch* 搜索条件，面向翻译表
*iscart* - 是否基于购物车搜索

 对比标准的search方法，
 {
    table, exprs, querystr,  maxcnt,pagesize, mask, usepref, orderby
 }
 
 2017.1.19 支持按用户读取商品的折扣价格，为考虑性能，先对redis中的pprice进行命中测试，批量从数据库中获取，然后在
 WebController.getInstance().replacePdtValues中读取redis内存值，来提高效率
 redis key: usr:{uid}:pprice (hset) , field: {pdtid}, value: pprice(float) 
 
 * @author yfzhu
 *
 */
@Admin(mail="yfzhu@lifecycle.cn")
public class PdtSearch1 extends Search {
	private boolean isFav=false;
	private int actId=-1;
	private boolean isCart=false;//是否在购物车中搜索
	private JSONArray types =null;// 过滤条件组
	private String blank;//'空白'的语言翻译
	class DimValue{
		String dim; //eg: dim15
		int id; // 当前选择的值，eg: 13，即 m_dim15_id=13
		public String toString(){
			return "{dim:"+dim+",id:"+id+"}";
		}
	}
	private ArrayList<DimValue> dimNodes;
	/**
	 * 根据传人的cat分解出dim的值, 并将界面选择的dim也一并加入
	 * @param cat - 形式如"12.145.8438", 分别对应 b2b:cat:tree_conf 的每个dim的配置
	 * @param selectedDims - format  {dim3: 12, dim4:2}
	 * @throws Exception
	 */
	private ArrayList<DimValue> initCatNodes(String cat,JSONObject selectedDims ) throws Exception{
		ArrayList dimNodes=new ArrayList();
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
		logger.debug("dimNodes:"+ Tools.toString(dimNodes, ","));
		return dimNodes;
	}
	/**
	 * 处理以下过滤条件：属性过滤，活动，收藏，pdtsearch
	 * 
	 * 对输入的查询条件进行重构，以支持特殊的查询请求
	 * @param jo 原始的查询条件，有可能在此地被重构
	 * @return 额外的查询条件，将增加到查询where语句中
	 * key: 是要补充到sql where 部分的clause语句内容，比如 "emp_id=?"
	 * value 是问号对应的实际值，如果value是java.util.List，将允许多值代替？号
	 */
	protected HashMap<String, Object> reviseSearchCondition(JSONObject jo) throws Exception{
		
		//向 vc注入参数和对应值 将会覆盖掉 coverVcParams() 中传入的值,防止uid等值错误注入了
		Table usrTable=TableManager.getInstance().getTable("usr");
		JSONObject usrObj = PhoneUtils.fetchObject(usrTable, usr.getId(), conn, jedis);
		vc.put("1",1);
		vc.put("lang_id", usrObj.getInt("lang_id"));
		vc.put("uid", usr.getId());
		vc.put("market_id",usr.getMarketId());
		vc.put("c_store_id",usrObj.getInt("c_store_id"));
		
		HashMap<String, Object> map=new HashMap<String, Object>();
		//market
		map.put("b_mk_pdt.b_market_id=?", usr.getMarketId());
		map.put("b_mk_pdt.isactive=?", "Y");//需要是上架商品 2016.9.10
		
		StringBuilder sb=new StringBuilder();
		ArrayList params=new ArrayList();
		/**
		 * params为空，表示没有where 参数，但有时isnull这种形式还是需要
		 */
		boolean hasWhereClause=false;
		//dims
		if(usr.getLangId()!=LanguageManager.getInstance().getDefaultLangId()){
			sb=new StringBuilder("exists(select 1 from m_product d, M_PRODUCT_TRANS r where r.m_product_id=d.id and r.b_language_id=? and d.isactive='Y' and d.id=b_mk_pdt.m_product_id");
			params.add(usr.getLangId());
		}else{
			sb=new StringBuilder("exists(select 1 from m_product d where d.isactive='Y' and d.id=b_mk_pdt.m_product_id");
		}
		for( DimValue dv:dimNodes){
			//这里有个特殊值：－2，表示为空, 这是catree里设置的定义
			if(dv.id==-2){
				if(dv.dim.equals(PhoneConfig.PRICE_RANGE_DIM))
					sb.append(" and b_mk_pdt.m_").append(dv.dim).append("_id is null");
				else
					sb.append(" and d.m_").append(dv.dim).append("_id is null");
				hasWhereClause=true;
			}else{
				if(dv.dim.equals(PhoneConfig.PRICE_RANGE_DIM))
					sb.append(" and b_mk_pdt.m_").append(dv.dim).append("_id=?");
				else
					sb.append(" and d.m_").append(dv.dim).append("_id=?");
				params.add(dv.id);
			}
		}
		// search
		Table table=manager.getTable("pdt");
		String querystr=jo.optString("pdtsearch");
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
		}		
		
		sb.append(")");
		if(params.size()>0 || hasWhereClause )map.put(sb.toString(), params);
		
		// 接下来要处理 actid,isfav
		if(actId!=-1){
			map.put("exists(select 1 from b_prmt_pdt a where a.m_product_id=b_mk_pdt.m_product_id and a.b_prmt_id=? and a.isactive='Y')", actId);
		}
		
		if(isFav){
			map.put("exists(select 1 from B_FAVOURITE f where f.m_product_id=b_mk_pdt.m_product_id and f.user_id=? and f.isactive='Y')",usr.getId());
		}
		if(isCart){
			map.put("exists(select 1 from b_cart c where c.b_market_id=b_mk_pdt.b_market_id and c.m_product_id=b_mk_pdt.m_product_id and c.user_id=? and c.isactive='Y')",usr.getId());
		}
		
		/**
		 * 添加过滤条件 根据 adsql#pdtsearchConf 配置进行额外过滤
		 * add by stao 2017/06/21
		 */
		if(null !=types || types.length() !=0){
			JSONObject adJson =(JSONObject)PhoneController.getInstance().getValueFromADSQLAsJSON( "pdtsearchConf", conn);
			if (null == adJson) {
				throw new NDSException("请检查  ad_sql#pdtsearchConf配置");
			}
			for (int i = 0; i < types.length(); i++) {
				String typeStr = types.getJSONObject(i).optString("type");
				if(Validator.isNull(typeStr)){
					throw new NDSException("客户端 type类型为空 ");
				}
				JSONObject typeObj = adJson.optJSONObject(typeStr);
				if (null == typeObj) {
					throw new NDSException("请检查  ad_sql#pdtsearchConf " + typeStr+ " 配置");
				}
				/*
				 * 根据type下配置的filter 过滤条件进行过滤, 一个type对应一个过滤条件
				 */
				JSONObject filter = typeObj.optJSONObject("filter");
				if (null ==filter ) {
					throw new NDSException("请检查  ad_sql#pdtsearchConf " + typeStr + " filter" + " 配置");
				}
				/**
				 * 将过滤条件添加到map中
				 * 读取 配置上 sql名称查询出该名称所对应的sql语句
				 */
				String sqlname = filter.getString("sqlname");
				String sql = PhoneController.getInstance().getValueFromADSQL(sqlname, conn);
				if(Validator.isNull(sql)){
					throw new NDSException("请检查  ad_sql#pdtsearchConf " + typeStr + " filter  sqlname" + " 配置");
				}
				String paramname =filter.getString("paramname");
				//将 filters中配置的sql语句 和参数 添加到 map中作为过滤条件
				map.put(sql,vc.get(paramname));
			}
		}
		return map;
	}
	
	public CmdResult execute(JSONObject jo) throws Exception {
		String cacheKey=jo.optString("cachekey");
		if(Validator.isNotNull(cacheKey) && jedis.exists(cacheKey) /*需求：若key timeout也要重新查*/){
			jo.put("table","pdt");//cache key wil reload from this table's pk records
		}else{
			jo.put("table","b_mk_pdt");//我们其实是发起了select m_product_id from b_mk_pdt where xxx 这样的语句，所以table是b_mk_pdt
		}
		jo.put("pkcolumn", "m_product_id");// need return b_mk_pdt.m_product_id for search result
		jo.put("idonly", true);// only need m_product_id
		
		if(jo.has("querystr") || jo.has("exprs")) throw new NDSException("不能使用querystr或exprs参数");
		
		//key:dim, value column value
		JSONObject selectedDims= jo.optJSONObject("dims");
		if(selectedDims==null)selectedDims=new JSONObject();
		
		blank=MessagesHolder.getInstance().getMessage(locale, "blank");
		isFav= jo.optBoolean("isfav",false);
		actId= jo.optInt("actid", -1);
		types =jo.optJSONArray("types");
		dimNodes=initCatNodes(jo.optString("cat"),selectedDims );
		isCart=jo.optBoolean("iscart",false);
		
		//覆盖参数将客户端参数保存在vc中
		coverVcParams(types,vc);
		
		return super.execute(jo);
	}
	
	/**
	 * 将客户端参数保存在vc中
	 * @param types 类型数组
	 * @param vc
	 * @throws Exception
	 */
	protected void coverVcParams(JSONArray types,VelocityContext vc) throws Exception {
		if(null == types || types.length() ==0){
			return;
		}
		JSONObject adJson =(JSONObject)PhoneController.getInstance().getValueFromADSQLAsJSON( "pdtsearchConf", conn);
		if (null == adJson) {
			throw new NDSException("请检查  ad_sql#pdtsearchConf配置");
		}
		for (int i = 0; i < types.length(); i++) {
			String typeStr = types.getJSONObject(i).optString("type");
			String value = types.getJSONObject(i).optString("value");
			if (Validator.isNull(value)) {
				continue;
			}
			if(Validator.isNull(typeStr)){
				continue;
			}
			JSONObject typeObj = adJson.optJSONObject(typeStr);
			if (null == typeObj) {
				continue;
			}
			JSONObject filter = typeObj.optJSONObject("filter");
			if (null == filter ) {
				continue;
			}
			String sqlparam =filter.optString("paramname");
			if(Validator.isNull(sqlparam)){
				continue;
			}
			
			//将 客户端传来的值添加到vc中 (防止错误注入值  如 uid=-100, 在 reviseSearchCondition() 中将必要的参数进行覆盖)
			vc.put(sqlparam,value);
		}
	}
	
	/**
	 * 对搜索的SearchResult.toJSON对象进行改装，以符合客户端要求
	 * @param ret redis SearchResult.toJSON，直接再次编辑
	 * sample:
	 *  ret:{"total":438,"b_mk_pdt_s":[473,472...],"cnt":54,"cachekey":"list:b_mk_pdt:893:438:list:eNkdT1jsQF274DBrMxLC9Q","start":0}
	 * @throws Exception
	 */
	protected void postAction(JSONObject ret) throws Exception{
		logger.debug("ret:"+ ret);
		JSONArray ids=(JSONArray) ret.remove("b_mk_pdt_s");
		
		ArrayList<Column> cols=manager.getTable("pdt").getColumnsInListView();
		JSONArray data=PhoneUtils.getRedisObjectArray("pdt", ids, cols, true, conn, jedis);
		
		if(PhoneConfig.LOAD_PPRICE)
			WebController.getInstance().loadUserPPrices(ids, usr, vc, jedis, conn);
		
		for(int i=0;i<data.length();i++){
			JSONObject jo=data.getJSONObject(i);
			//更新
			WebController.getInstance().replacePdtValues(jo, usr, vc, jedis, conn);
			
		}
		
		assmbleExtfileds(data);
		
		ret.put("pdt_s", data);
	}
	/**
	 * 组装扩展字段
	 * @param data 商品列表
	 * @throws Exception 
	 */
	private void assmbleExtfileds(JSONArray data) throws Exception {
		if (null != data && data.length()>0) {
			/**
			 * 组装扩展字段
			 * 将扩展的数据添加到pdt单对象中
			 */
			if(null !=types && types.length()>0){
				for (int i = 0; i < types.length(); i++) {
					String typeStr = types.getJSONObject(i).getString("type");
					String sqlname ="pdtsearchConf:"+ typeStr +":extfields";
					//查询扩展字段,数组为空,则不进行组装,否则将每个元素中的数据组装到pdt对象中(根据pdtid组装)
					JSONArray extArray =PhoneController.getInstance().getDataArrayByADSQL(sqlname, vc, conn,true);
					//JSONArray extArray = engine.doQueryObjectArray(sql, new Object[]{usr.getId()}, conn);
					if (null== extArray || extArray.length() == 0) {
						continue;
					}
					//取出第一个作为代表元素,遍历该元素中的每个属性名称,为组装数据做准备
					JSONObject one =extArray.getJSONObject(0);
					Iterator<String> iter =one.keys();
					ArrayList<String>  paramnames= new ArrayList<String>();
					//将参数名添加到list中
					while (iter.hasNext()) {
						paramnames.add((String) iter.next());
					}
					
					//开始组装数据
					for(int j=0;j<data.length();j++){
						//获取pdtObj中 pdtid
						JSONObject pdtObj=data.getJSONObject(j);
						int pdtid = pdtObj.getInt("pdtid");
						for (int k = 0; k < extArray.length(); k++) {
							JSONObject oneExt =extArray.getJSONObject(k);
							int oneExt_pdtid =oneExt.getInt("pdtid");
							//只有两个pdtid一致时才进行组装
							if (oneExt_pdtid == pdtid) {
								for (int l = 0; l < paramnames.size(); l++) {
									//获取每个参数所对应的值,并组装到pdtObj上
									String value = oneExt.optString(paramnames.get(l),"");
									pdtObj.put(paramnames.get(l),value);
								}
								//组装完后下一个pdtObj继续组装
								break;
							}
						}
					}
				}
			}
		}
		
	}
}
