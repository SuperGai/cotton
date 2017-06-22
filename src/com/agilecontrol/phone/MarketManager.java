package com.agilecontrol.phone;
import org.json.*;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.agilecontrol.b2bweb.DimTranslator;
import com.agilecontrol.nea.core.query.QueryEngine;

/**
 * 将b_market缓存于java内存 
 * 
 * @author yfzhu
 *
 */
@Admin(mail="yfzhu@lifecycle.cn")
public class MarketManager {
	private static Logger logger = LoggerFactory.getLogger(DimTranslator.class);
	private static MarketManager instance=null;
	/**
	 * b_market
	 * 
	 */
	public class Market{
		private int id;
		private String name;
		private String description;
		private String currency;
		/**
		 * b_market.id
		 * @return
		 */
		public int getId() {
			return id;
		}
		/**
		 * 市场代码
		 * @return
		 */
		public String getName() {
			return name;
		}
		/**
		 * 市场描述
		 * @return
		 */
		public String getDescription() {
			return description;
		}
		/**
		 * 货币符号
		 * @return
		 */
		public String getCurrency() {
			return currency;
		}
	}
	/**
	 * key: b_market.id , value: b_market
	 *  
	 */
	private Hashtable<Integer, Market> marketIds;
	/**
	 * key: b_market.name , value: b_market
	 *  
	 */
	private Hashtable<String, Market> marketNames;
	

	private MarketManager(){
		marketIds=new Hashtable();
		marketNames=new Hashtable();
	}

	/**
	 * 返回所有的b_market.id
	 * @return
	 */
	public Collection<Market> getAllMarkets(){
		return marketIds.values();
	}
	/**
	 * 根据名称找到对应的id，返回-1如果没找到
	 * @param name
	 * 
	 */
	public int getMaketId(String name){
		Market mkt= marketNames.get(name);
		int id;
		if(mkt==null)id=-1;
		id=mkt.id;
		return id;
	}
	
	/**
	 * 根据id找到对应的name，返回null如果没找到
	 * @param mktId
	 */
	public String getMarketName(int mktId){
		Market mkt= marketIds.get(mktId);
		if(mkt==null) return null;
		return mkt.getName();
	}
	
	/**
	 * 根据id找到对应的description，返回null如果没找到
	 * @param mktId
	 */
	public String getMarketDesc(int mktId){
		Market mkt= marketIds.get(mktId);
		if(mkt==null) return null;
		return mkt.getDescription();
	}
	
	/**
	 * 根据id找到对应的symbal，返回null如果没找到
	 * @param mktId
	 */
	public String getCurrency(int mktId){
		Market mkt= marketIds.get(mktId);
		if(mkt==null) return null;
		return mkt.getCurrency();
	}
	
	/**
	 * 清空数据，重新载入数据库翻译内容
	 */
	public void clear(Connection conn) throws Exception {
		marketIds.clear();
		marketNames.clear();
		
		JSONArray rows=QueryEngine.getInstance().doQueryObjectArray(
				"select m.id, m.name, m.description, c.symbol from b_market m, b_currency c where c.id(+)=m.b_currency_id ", null, conn);
		for(int i=0;i<rows.length();i++){
			JSONObject one=rows.getJSONObject(i);
			Market mkt=new Market();
			mkt.id=one.getInt("id");
			mkt.name=one.getString("name");
			mkt.description=one.optString("description");
			mkt.currency=one.getString("symbol");
			marketIds.put(mkt.id, mkt);
			marketNames.put(mkt.name, mkt);
		}
		
	}
	
	public static MarketManager getInstance(){
		if(instance==null){

			MarketManager one=new MarketManager();
			Connection conn=null;
			try{
				conn=QueryEngine.getInstance().getConnection();
				one.clear(conn);
			}catch(Throwable tx){
				logger.error("fail to load langs", tx);
			}finally{
				try{conn.close();}catch(Exception ex){}
			}
			instance=one;
		}
		return instance;
	}
}
