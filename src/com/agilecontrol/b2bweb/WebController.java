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
 * ������
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
	 * ��ȡָ���г�����ĳ�����ĳ�������, ��ͬ�г���������Ʒ����ܴ󣨹��ŵ���ʾ������
	 * Ϊ������ܣ�����ȡ����Ϣ���浽 b2b:mkt:$marketid:sg (hset) {maxsize, sizes)
	 * ����ɾ��ʱ������������ M_ALIAS_ERP ��ɾ�ĵ�ʱ�򣬼� M_ALIAS_ERP����չ���� rkey
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
	 * ��ȡָ���г������г����������, ��ͬ�г���������Ʒ����ܴ󣨹��ŵ���ʾ������
	 * Ϊ������ܣ�����ȡ����Ϣ���浽 b2b:mkt:$marketid:sg (hset) {maxsize, sizes)
	 * ����ɾ��ʱ������������ M_ALIAS_ERP ��ɾ�ĵ�ʱ�򣬼� M_ALIAS_ERP����չ���� rkey
	 * @param marketId
	 * @param jedis
	 * @param conn
	 * @return 
	 * 38,39,40,41,42,43
     * S,M,L,XL,XXL,XXXL
	 * 28,30,32,34,36
	 * �ѳ�����Ŵ����
	 * [
	 *  [38,S,28] (��0�У�,
	 *  [39,M,28]����1�У�,
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
	 * �����г����ڵĳ�������Ϣ��д��redis:
	 * b2b:mkt:$marketid:sg (hset) {maxsize, sizes)
	 * @param marketId
	 * @param jedis
	 * @param conn
	 * @return {maxsize, sizes} ����, maxsize: int, sizes: json array
	 * @throws Exception
	 */
	private JSONObject initSizeGroup(int marketId, Jedis jedis, Connection conn) throws Exception{
		VelocityContext vc = VelocityUtils.createContext();
		vc.put("marketid", marketId);
		 /*
		 *  38,39,40,41,42,43
	     * S,M,L,XL,XXL,XXXL
		 * 28,30,32,34,36
		 * �ѳ�����Ŵ����
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
	 * ����M_PRODUCT_TRANS����ֶ��滻��pdt����ֶΣ�ֱ�Ӹ���pdt��Ŀǰ���������ֶΣ�
	 * note,mainpic,tags
	 * Ȼ������г�����b_mk_pdt���¼۸�������ֶ�
	 * mainpic,price,qty_min,limit_qty
	 * 
	 * @param pdt
	 * @param langId b_language.id
	 * @param marketId b_market.id
	 * @param vc
	 * @param jedis
	 * @param conn
	 * @return M_PRODUCT_TRANS.id��Ӧ�Ķ��������и�picture�ֶΣ��������ⲿ��Ҫ�ģ�����Ʒ�����dtls)
	 * @throws Exception
	 */
	public JSONObject replacePdtValues(JSONObject pdt, int langId,int marketId, VelocityContext vc, Jedis jedis, Connection conn) throws Exception{
		long objectId=pdt.getLong("id");
		//�����Է��룬����M_PRODUCT_TRANS
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
		//�г���Ʒ�л�ȡ�۸�����ȵ���Ϣ,B_MK_PDT
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
			//������Ȼ����ʾ��ƷʧЧ
//			if(!"Y".equals( objMarket.optString("isactive"))) throw new NDSException("��Ʒδ�ڵ�ǰ�г��ϼ�");
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
