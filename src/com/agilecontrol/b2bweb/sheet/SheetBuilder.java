package com.agilecontrol.b2bweb.sheet;

import java.io.StringWriter;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.json.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;

import com.agilecontrol.b2b.schema.TableManager;
import com.agilecontrol.b2bweb.cmd.GetSheet;
import com.agilecontrol.nea.core.control.event.DefaultWebEvent;
import com.agilecontrol.nea.core.query.QueryEngine;
import com.agilecontrol.nea.core.util.ConfigValues;
import com.agilecontrol.nea.core.velocity.VelocityUtils;
import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.nea.util.Validator;
import com.agilecontrol.phone.PhoneConfig;
import com.agilecontrol.phone.PhoneController;
import com.agilecontrol.phone.PhoneUtils;
import com.agilecontrol.phone.UserObj;

/**
 * 
 * 构造各种界面要求的矩阵，参见:
 * 
 * com.agilecontrol.b2bweb.GetSheet
 * 
 * @author yfzhu
 *
 */
public abstract class SheetBuilder {
	protected Logger logger = LoggerFactory.getLogger(getClass());
	
	protected int pdtId,actId,storeId;
	protected boolean readOnly;
	protected JSONObject conf;// ad_sql#sheet_conf
	

	protected UserObj usr;
	/**
	 * 标配的jedis连接，和conn一样，需要主动关闭
	 */
	protected Jedis jedis;

	protected DefaultWebEvent event;
	protected Connection conn;
	protected QueryEngine engine;
	protected TableManager manager;
	protected VelocityContext vc;
	
	/**
	 * 初始化矩阵构建器，主要是获得getSheet的支持
	 * @param jo 客户端传来的请求
	 * @param conf ad_sql#sheet_conf
	 * @param event 事件
	 * @param vc 上下午
	 * @param jedis
	 * @param conn
	 * @throws Exception
	 */
	public  void init(UserObj user, JSONObject jo, JSONObject conf,DefaultWebEvent event,VelocityContext vc,Jedis jedis, Connection conn) throws Exception{
		
		
		pdtId= jo.getInt( "pdtid");
		actId=jo.optInt("actid", -1);			
		storeId=jo.optInt("storeid",-1);	
		readOnly=jo.optBoolean("readonly",false);
		this.conf=conf;
		
		this.conn=conn;
		this.event=event;
		this.vc=vc;
		this.usr=user;
		this.jedis=jedis;
		this.engine=QueryEngine.getInstance();
		this.manager=TableManager.getInstance();
		
		JSONArray itemsConf=conf.getJSONArray("items");
		
		for(int i=0;i<itemsConf.length();i++){
			JSONObject itemConf=itemsConf.getJSONObject(i);
			String itemKey=itemConf.optString("key");
			boolean isPriceItem="price".equals(itemKey);
			if(isPriceItem){
				priceItemIdx=i;
				break;
			}
		}
		if(priceItemIdx==-1) throw new NDSException("配置错误，ad_sql#sheet_conf需要price item");
		
	}
	
	private int priceItemIdx=-1;
	/**
	 * 获取在conf.items中定义的key=price的item的序号
	 * 注意即便定义了price 行，如果当前商品+活动是固定价格，系统也不会添加价格行到矩阵上
	 * @return 必定存在
	 */
	protected int getPriceItemIdx(){
		return priceItemIdx;
	}
	
	/**
	 * 构建矩阵结果，包括数据部分
	 * @throws Exception
	 */
	public abstract void build() throws Exception;
	/**
	 * 构建完成的矩阵定义
	 * @return
	 * 对于pc版的sheet, {def,values, type:="skusheet"},
	 * 对于phone版的sheet，{pdtid,value1, value2,pdt,vname, template, type:="phonesheet"}
	 */
	public abstract JSONObject getSheet() throws Exception;
	
	
	
	/**
	 * 加载单个pdtId对应的矩阵数据
	 * @param pdtId
	 * @param itemConf - ad_sql#sheet_conf#items
	 * @return {items, asis} items - [hashmap<asi, value>] ,items 的行顺序与itemsConf一致, asis - hashset<asi>
	 *  items里的hashmap的value定义：对于普通item，就直接是显示值，比如库存值；对于key 是price的item，value是jsonobject: "d" - 为界面显示，"p" 是价格，有两种情况：
	 *  单一价格：p 就是浮点数，阶梯价格， p 是数组，元素是3个倍数，每组3个元素分别对于最小值(含），最大值（含)，价格, 在画矩阵的时候，需要代入公式:
	 *  amt(price, cell)
	 *   
	 * @throws Exception
	 */
	protected HashMap<String ,Object> loadPdtObject(int pdtId, JSONArray itemsConf) throws Exception{
		vc.put("pdtid", pdtId);
		if(storeId!=-1){
			vc.put("storeid", storeId);
		}
		ArrayList<HashMap<Integer, Object>> items=new ArrayList();		// 每行： {asi, value}

		for(int i=0;i<itemsConf.length();i++){
			JSONObject itemconf=itemsConf.getJSONObject(i);
			String sql=itemconf.getString("sql");
			String key=itemconf.optString("key");
			boolean isPriceItem= "price".equals(key);
			HashMap<Integer, Object> vals=new HashMap();
			//sql format:select asi asi, xxx value from xxx where user_id=$uid and $pdtid=m_product_id
			//如果是price列，第二个元素是显示值，还有第3个元素：price， 是具体的price值，或阶梯价js区间定义:[0,400,50,401,1000,30,1001,99999,20]
			//将构造到sheet中： amt([0,400,50,401,1000,30,1001,99999,20], cell(3,4))
			JSONArray rows= PhoneController.getInstance().getDataArrayByADSQL(sql, vc, conn, false);
			for(int j=0;j<rows.length();j++){
				JSONArray one=rows.getJSONArray(j);
				int asi=one.getInt(0);
				String value=one.optString(1);
				if(isPriceItem){
					JSONObject val=new JSONObject();
					val.put("p", one.optString(2));
					val.put("d", value);
					vals.put(asi, val);
				}else{
					if(Validator.isNotNull(value)) vals.put(asi, value); //为空就不给了
				}
			}
			
			
			items.add(vals);
			
		}
		//key: asi 可以下单的asi
		HashSet<Integer> alaiableAsis =new HashSet();
		//获取可下单的asi, 需要基于当前活动, row: [asi], 与标准矩阵不同的是：不同的活动，不同的市场，商品仍然有不同
		JSONArray alaiableAsiArray;
		if(actId==-1) alaiableAsiArray=PhoneController.getInstance().getDataArrayByADSQL("pdt_asi_list", vc, conn, false);
		else alaiableAsiArray=PhoneController.getInstance().getDataArrayByADSQL("pdt_act_asi_list", vc, conn, false);
		for(int i=0;i<alaiableAsiArray.length();i++) alaiableAsis.add(alaiableAsiArray.getInt(i));
		
		HashMap<String, Object> pdtObj= new HashMap();
		pdtObj.put("items", items);
		pdtObj.put("asis", alaiableAsis);
		return pdtObj;
	}
	
	
	/**
	 * 
	 * @param ja [1,2,3]
	 * @param packQty if 2
	 * @return [2,4,6]
	 * @throws Exception
	 */
	protected String multiple(String jaStr, int packQty) throws Exception{
		JSONArray ja=new JSONArray(jaStr);
		JSONArray ret=new JSONArray();
		for(int i=0;i<ja.length();i++){
			double d=ja.getDouble(i); 
			if(d>0) d=d*packQty;
			ret.put(d);
		}
		return ret.toString();
	}
	
}
