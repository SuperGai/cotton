package com.agilecontrol.b2bweb.cmd;

import java.util.*;

import org.json.*;

import com.agilecontrol.b2b.schema.Column;
import com.agilecontrol.b2bweb.WebController;
import com.agilecontrol.b2bweb.grid.GridBuilder;
import com.agilecontrol.b2bweb.grid.GridViewDefine;
import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.nea.util.Tools;
import com.agilecontrol.nea.util.Validator;
import com.agilecontrol.phone.CmdResult;
import com.agilecontrol.phone.PhoneController;
import com.agilecontrol.phone.PhoneUtils;
import com.agilecontrol.phone.PhoneController.SQLWithParams;

/**

h1. 面向Grid提供搜索结果内容

在 "b2b.pdt.search"命令基础上扩展，支持kvs的数据选择， kvs的定义来自于 ad_sql#online_edit_kvs ：[{key, desc, filtersql, valuesql, default}] 
从而支持多样的自定义商品集合过滤选择，传统在b2b.pdt.search 中的actid, isfav， iscart都通过配置为特殊的kv来解决。

与 "b2b.pdt.search" 不同的输出结果，是面向grid客户端所需要的。

> {cmd:"b2b.grid.search", dims, cat, pdtsearch, kvs, meta}

*dims* - jsonobj, key 是当前选中的column, value 是column对应的id, 举例: {dim3: 12, dim4:2}
*cat* - 当前用户选择的三层分类定义，每个cat有最多n个dimid组成，按b2b:cat:tree_conf定义构造
*pdtsearch* 搜索条件，面向翻译表
*kvs* 界面选择的过滤设置, jsonobj, key 为ad_sql#online_edit_kvs的key，value是界面选择的值，boolean（checkbox）或具体值(select)
*meta* boolean 是否获取meta信息，即只有gridviewdefine, 

 * 
 * 
 * @author yfzhu
 *
 */
public class GridSearch extends PdtSearch {
	/**
	 * 原始的请求
	 */
	private JSONObject requestObj;
	
	private GridBuilder builder;

	/**
	 * 获取指定key对应的json定义
	 * @param key 
	 * @param kvDef ad_sql#online_edit_kvs
	 * @return null 如果key 未定义
	 * @throws Exception
	 */
	private JSONObject getKVDefine(String key, JSONArray kvDef) throws Exception{
		for(int i=0;i<kvDef.length();i++){
			JSONObject jo=kvDef.getJSONObject(i);
			String k=jo.getString("key");
			if(k.equals(key)) return jo;
		}
		return null;
	}
	/**
	 * to Array
	 * @param params
	 * @return
	 */
	private ArrayList toList(Object[] params) {
		ArrayList al=new ArrayList();
		for(Object o: params) al.add(o);
		return al;
	}
	/**
	 * 获取grid定义
	 * @return 
	 * @throws Exception
	 */
	private JSONObject getGridMetaDefine() throws Exception{
		GridViewDefine gvd=builder.getViewDefine();
		return gvd.toJSONObjet();
	}
	/**
	 *表头数据和表头定义
	 h1.输入
	 
	 {cmd:"b2b.grid.search",meta,table,id}
	 
	 h2. 输出
	 
  	 {columns:[{childDesc:[{editable,field,headerName,width}],desc,isSize},dataDefine,linewrap,options,preDefine:{pre_css,pre_desc},sizeGroup,sumline:[{fmt,name,value}],buttons}
	 
	 *meta* -true返回表头定义，false返回表头数据
	 
	 （表头定义）
	 *colums* -表头定义
	 *cellStyle* -当前列的样式定义
	 *desc* -表头描述
	 *editable* -当前列的数据是否可以编辑
     *field* -前端交互的索引
     *isSize* -是否是尺码列
     *width* -当前列的宽度
     *childDesc* -尺码列与参考列的表头定义 如果配置信息具有参考列，则把尺码列和参考列的表头合并
     *dataDefine* -当前列的数据多行时的样式定义
     *linewrap* -grid cell级别的高度
     *options* -扩展定义
     *preDefine* -参考列的定义 
     *pre_css* -参考列的样式定义
     *pre_desc* -参考列的描述 
     *sizeGroup* -前端定义
     *sumline* -grid总计量得定义模板
     *isPreSize* -是否是前缀参考列
     *isEnter* -非尺码列（包含前缀参考列）的cell级别的数据是否是多行
     *buttons* -根据配置满足不同模式下的多功能按钮
     *headerName* -存在前缀参考列时，在合并单元头下面的描述               例：如下
     ==============================================
     =       S（desc）               =		M（desc）	  =
     ==============================================
     =       44（desc）            =			45（desc）	  =
     ==============================================
     =headerName=headerName= 	参考         =		订量	  =
     ==============================================
    
     h2. 输出
     
           （表头数据）
	 *cachekey* -缓存
     *cnt* -当前数据数量
     *rowData* -表头数据
     *asi* -尺码列的唯一标识
	 *cc* -颜色标识
     *pdtId* -商品id
     *pic* -图片地址
     *start* -
     *sum* -grid下面的合计行数据
     *total* -满足当前过滤条件的商品数量
     
    
	 * @return JSONObject
	 * @throws Exception 
	 * @throws Exception
	 */
	public CmdResult execute(JSONObject jo) throws Exception {
		logger.debug("marketId:"+usr.getMarketId()+"-"+usr.getId());
		requestObj=jo;
		builder=new GridBuilder();
		if(Validator.isNull(jo.optString("table"))) throw new NDSException("@b2bedit-config@"+"ad_sql#grid:"+jo.optString("table")+":online_edit_kvs"+"@b2bedit-found@");
		//将所有传人的参数都放置在vc中供调用
		for(Iterator it=jo.keys();it.hasNext();){
			String key=(String)it.next();
			Object v=jo.get(key);
			vc.put(key, v);
		}
		
		builder.init(usr, requestObj,  event, vc, jedis, conn);

		if(jo.optBoolean("meta", false)){
			return new CmdResult(getGridMetaDefine());
		}
		return super.execute(jo);
	}
	/**
	 * 对搜索的SearchResult.toJSON对象进行改装，以符合客户端要求
	 * @param ret redis SearchResult.toJSON，直接再次编辑. 传入的格式由于在PdtSearch中含有idonly的控制，故仅仅有id
	 * sample
	 * ret:{"total":438,"b_mk_pdt_s":[473,472...],"cnt":54,"cachekey":"list:b_mk_pdt:893:438:list:eNkdT1jsQF274DBrMxLC9Q","start":0}
	 * @return {"total":438,"cnt":54,"cachekey":"list:b_mk_pdt:893:438:list:eNkdT1jsQF274DBrMxLC9Q","start":0, rowData, sum}
	 * rowData: 
	 * sum: {
	    "sumamt":"￥16,378.00",
	    "sumqty":"325,000",
	    "sumamtlist":"￥20,378.00"
	  }
	 * @throws Exception
	 */
	protected void postAction(JSONObject ret) throws Exception{
		JSONArray ids=(JSONArray) ret.remove("b_mk_pdt_s");
		
		//return rowData, sum
		JSONObject gbView=builder.getViewData(ids);
		for(Iterator it=gbView.keys();it.hasNext();) {
			String key=(String)it.next();
			ret.put(key, gbView.get(key));
		}
		
	}
	/**
	 * 处理以下过滤条件：属性过滤，活动，收藏，pdtsearch, 需要增加对kvs的识别
	 * 
	 * @param jo 相比PdtSearch, 增加了kvs
	 * @return 额外的查询条件，将增加到查询where语句中
	 * key: 是要补充到sql where 部分的clause语句内容，比如 "emp_id=?"
	 * value 是问号对应的实际值，如果value是java.util.List，将允许多值代替？号
	 */
	protected HashMap<String, Object> reviseSearchCondition(JSONObject jo) throws Exception{
		HashMap<String, Object> map=super.reviseSearchCondition(jo);
		vc.put("marketid", usr.getMarketId());
		
		JSONObject kvs=jo.optJSONObject("kvs");
		if(kvs!=null && kvs.length()>0){
			JSONArray kvDefs=(JSONArray) PhoneController.getInstance().getValueFromADSQLAsJSON("grid:online_edit_kvs", conn);
			for(Iterator it=kvs.keys();it.hasNext();){
				String key=(String)it.next();
				JSONObject kvDef=getKVDefine(key, kvDefs);
				if(kvDef==null) {
					logger.debug("key "+ key+" not defined in ad_sql#online_edit_kvs");
					continue;
				}
				if(kvDef.isNull("valuesql")){
					//boolean
					if(Tools.getBoolean(kvs.opt(key), false)==false)continue;
				}else{
					//select, 将返回的结果补充作为变量代入filtersql, 如 actid->$actid
					vc.put(key, kvs.opt(key));
				}
				String filterSQL=kvDef.getString("filtersql");//ad_sql#name
				
				SQLWithParams spa= PhoneController.getInstance().parseADSQL(filterSQL, vc, conn);
				map.put("exists("+ spa.getSQL()+")", toList( spa.getParams()));
			}
		}
		//读取meta 的配置：pdtfilter_sql： 举例“grid:bro:sql:pdtfilter”， 是对商品的额外过滤条件，比如退货单的商品清单必须是退货明细商品
		JSONObject gridConf=builder.getGridConf();
		String pdtFilterSQLName= gridConf.optString("pdtfilter_sql");
		if(Validator.isNotNull(pdtFilterSQLName)){
			SQLWithParams sqlParams=PhoneController.getInstance().parseADSQL(pdtFilterSQLName, vc, conn);
			map.put(sqlParams.getSQL(), toList(sqlParams.getParams()));
		}
		
		
		return map;
	}
}
