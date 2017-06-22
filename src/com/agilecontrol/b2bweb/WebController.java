package com.agilecontrol.b2bweb;

import java.util.*;

import org.apache.velocity.VelocityContext;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;

import java.sql.Connection;

import com.agilecontrol.b2b.schema.Table;
import com.agilecontrol.b2b.schema.TableManager;
import com.agilecontrol.nea.core.query.QueryEngine;
import com.agilecontrol.nea.core.velocity.VelocityUtils;
import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.nea.util.Tools;
import com.agilecontrol.nea.util.Validator;
import com.agilecontrol.phone.PhoneController;
import com.agilecontrol.phone.PhoneUtils;

/**
 * 工具类
 * 
 * @author yfzhu
 *
 */
public class WebController {
	
	private static Logger logger = LoggerFactory.getLogger(WebController.class);
	private static WebController instance=null;
	
	private WebController(){
		
	}
	/**
	 * 获取指定市场中最长的尺码组的尺码数量, 不同市场售卖的商品区别很大（贯信的演示环境）
	 * 为提高性能，将获取的信息缓存到 b2b:mkt:$marketid:sg (hset) {maxsize, sizes)
	 * 缓存删除时机：分销条码 M_ALIAS_ERP 增删改的时候，见 M_ALIAS_ERP的扩展属性 rkey
	 * @param marketId b_market.id
	 * @param jedis
	 * @param conn
	 * @return
	 * @throws Exception
	 */
	public int getMaxSizeCount(int marketId, Jedis jedis, Connection conn) throws Exception{
		
		int maxSize=Tools.getInt( jedis.hget("b2b:mkt:"+marketId+":sg", "maxsize"), -1);
		if(maxSize==-1){
			JSONObject sg=initSizeGroup(marketId, jedis,conn);
			maxSize= sg.getInt("maxsize");
		}
		return maxSize;
	}
	/**
	 * 获取指定市场中所有尺码组的排列, 不同市场售卖的商品区别很大（贯信的演示环境）
	 * 为提高性能，将获取的信息缓存到 b2b:mkt:$marketid:sg (hset) {maxsize, sizes)
	 * 缓存删除时机：分销条码 M_ALIAS_ERP 增删改的时候，见 M_ALIAS_ERP的扩展属性 rkey
	 * @param marketId
	 * @param jedis
	 * @param conn
	 * @return 
	 * 38,39,40,41,42,43
     * S,M,L,XL,XXL,XXXL
	 * 28,30,32,34,36
	 * 把尺码横排处理成
	 * [
	 *  [38,S,28] (第0列）,
	 *  [39,M,28]（第1列）,
	 *  [40,L,30]...
	 * ]
	 * 
	 * @throws Exception
	 */
	public JSONArray getSizeGroups(int marketId, Jedis jedis, Connection conn) throws Exception{
		String key="b2b:mkt:"+marketId+":sg";
		if(!jedis.hexists(key, "sizes")){
			JSONObject sg=initSizeGroup(marketId, jedis,conn);
			return sg.getJSONArray("sizes");
		}
		String sz=jedis.hget(key, "sizes");
		return new JSONArray(sz);
	}
	/**
	 * 创建市场对于的尺码组信息，写入redis:
	 * b2b:mkt:$marketid:sg (hset) {maxsize, sizes)
	 * @param marketId
	 * @param jedis
	 * @param conn
	 * @return {maxsize, sizes} 其中, maxsize: int, sizes: json array
	 * @throws Exception
	 */
	private JSONObject initSizeGroup(int marketId, Jedis jedis, Connection conn) throws Exception{
		VelocityContext vc = VelocityUtils.createContext();
		vc.put("marketid", marketId);
		 /*
		 *  38,39,40,41,42,43
	     * S,M,L,XL,XXL,XXXL
		 * 28,30,32,34,36
		 * 把尺码横排处理成
		 * [
		 *  [38,S,28],
		 *  [39,M,28],
		 *  [40,L,30]...
		 *  ]
		*/
		JSONArray size=(JSONArray)PhoneController.getInstance().getDataArrayByADSQL("b2b_mkt_sizegroup", vc, conn, false);
		JSONArray ja = new JSONArray();
		int maxSize = size.getString(0).split(",").length;
		
		for (int i = 0; i < maxSize; i++) {
			JSONArray desc = new JSONArray();
			for (int j = 0; j < size.length(); j++) {
				String[] str = size.getString(j).split(",");
				try {
					desc.put(str[i]);
				} catch (ArrayIndexOutOfBoundsException e) {
					desc.put(".");
				}
			}
			ja.put(desc);
		}
		String key="b2b:mkt:"+marketId+":sg";
		jedis.hset(key, "maxsize"	, String.valueOf(maxSize));
		jedis.hset(key, "sizes"	, ja.toString());
		
		JSONObject sg=new JSONObject();
		sg.put("maxsize", maxSize);
		sg.put("sizes", ja);
		
		return sg;
	}
	
	/**
	 * 根据M_PRODUCT_TRANS里的字段替换掉pdt里的字段，直接更新pdt，目前更新以下字段：
	 * note,mainpic,tags
	 * 然后根据市场定义b_mk_pdt更新价格和以下字段
	 * mainpic,price,qty_min,limit_qty
	 * 
	 * @param pdt
	 * @param langId b_language.id
	 * @param marketId b_market.id
	 * @param vc
	 * @param jedis
	 * @param conn
	 * @return M_PRODUCT_TRANS.id对应的对象，上面有个picture字段，可能是外部需要的（主商品界面的dtls)
	 * @throws Exception
	 */
	public JSONObject replacePdtValues(JSONObject pdt, int langId,int marketId, VelocityContext vc, Jedis jedis, Connection conn) throws Exception{
		long objectId=pdt.getLong("id");
		//多语言翻译，来自M_PRODUCT_TRANS
		String langKey="pdt:"+objectId+":lang";
		QueryEngine engine=QueryEngine.getInstance();
		JSONObject langPdt=null;
		int langObjId;
		if(jedis.hexists(langKey,String.valueOf(langId) )){
			langObjId=Tools.getInt( jedis.hget(langKey,String.valueOf(langId)), -1);
		}else{
			//read into key
			langObjId=engine.doQueryInt("select id from M_PRODUCT_TRANS where m_product_id=? and B_LANGUAGE_ID=?", new Object[]{objectId,langId }, conn);
			jedis.hset(langKey, String.valueOf(langId),String.valueOf( langObjId));
			
		}
//		logger.debug(langKey+"="+ langObjId);
		if(langObjId!=-1){
			Table table=TableManager.getInstance().getTable( "pdt_trans");
			langPdt =PhoneUtils.getRedisObj("pdt_trans", langObjId, table.getColumnsInObjectView(), conn,jedis);
		}
//		logger.debug("pdt_trans:"+langObjId +"="+ langPdt);
		String val;

		if(langPdt!=null){
			val= langPdt.optString("note");if(Validator.isNotNull(val))pdt.put("note", val);//value
			val= langPdt.optString("mainpic");if(Validator.isNotNull(val))pdt.put("mainpic", val);//imageurl
			val= langPdt.optString("tags");if(Validator.isNotNull(val))pdt.put("tags", val); 
			val= langPdt.optString("dtls");if(Validator.isNotNull(val))pdt.put("dtls", val);//pdtdtls
		}
//		logger.debug("pdt:"+objectId +"="+ pdt);
		//市场商品中获取价格，区间等的信息,B_MK_PDT
		String mktKey="pdt:"+objectId+":mkt";
		JSONObject objMarket=null;
		int mktObjId;
		if(jedis.hexists(mktKey,String.valueOf(marketId) )){
			mktObjId=Tools.getInt( jedis.hget(mktKey,String.valueOf(marketId)), -1);
		}else{
			//read into key
			mktObjId=engine.doQueryInt("select id from B_MK_PDT where m_product_id=? and B_MARKET_ID=?", new Object[]{objectId,marketId }, conn);
			jedis.hset(mktKey, String.valueOf(marketId),String.valueOf( mktObjId));
		}
		if(mktObjId!=-1){
			Table table=TableManager.getInstance().getTable( "b_mk_pdt");
			objMarket = PhoneUtils.getRedisObj("b_mk_pdt", mktObjId, table.getColumnsInObjectView(), conn,jedis); 
		}
//		logger.debug("b_mk_pdt:"+mktObjId +"="+ objMarket);
		if(objMarket!=null){
			//后面自然会提示商品失效
//			if(!"Y".equals( objMarket.optString("isactive"))) throw new NDSException("商品未在当前市场上架");
			double num;
			val= objMarket.optString("mainpic");if(Validator.isNotNull(val))pdt.put("mainpic", val);
			pdt.put("price", objMarket.optDouble("price", 0));
			pdt.put("qty_min", objMarket.optDouble("qty_min", 0));
			pdt.put("limit_qty", objMarket.optDouble("limit_qty", 0));
		}
//		logger.debug("pdt:"+objectId +"="+ pdt);
		return pdt;
	}	
	public static WebController getInstance(){
		if(instance==null){
			instance=new WebController();
		}
		return instance;
	}
}
