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
 * ������ֽ���Ҫ��ľ��󣬲μ�:
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
	 * �����jedis���ӣ���connһ������Ҫ�����ر�
	 */
	protected Jedis jedis;

	protected DefaultWebEvent event;
	protected Connection conn;
	protected QueryEngine engine;
	protected TableManager manager;
	protected VelocityContext vc;
	
	/**
	 * ��ʼ�����󹹽�������Ҫ�ǻ��getSheet��֧��
	 * @param jo �ͻ��˴���������
	 * @param conf ad_sql#sheet_conf
	 * @param event �¼�
	 * @param vc ������
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
		if(priceItemIdx==-1) throw new NDSException("���ô���ad_sql#sheet_conf��Ҫprice item");
		
	}
	
	private int priceItemIdx=-1;
	/**
	 * ��ȡ��conf.items�ж����key=price��item�����
	 * ע�⼴�㶨����price �У������ǰ��Ʒ+��ǹ̶��۸�ϵͳҲ������Ӽ۸��е�������
	 * @return �ض�����
	 */
	protected int getPriceItemIdx(){
		return priceItemIdx;
	}
	
	/**
	 * �������������������ݲ���
	 * @throws Exception
	 */
	public abstract void build() throws Exception;
	/**
	 * ������ɵľ�����
	 * @return
	 * ����pc���sheet, {def,values, type:="skusheet"},
	 * ����phone���sheet��{pdtid,value1, value2,pdt,vname, template, type:="phonesheet"}
	 */
	public abstract JSONObject getSheet() throws Exception;
	
	
	
	/**
	 * ���ص���pdtId��Ӧ�ľ�������
	 * @param pdtId
	 * @param itemConf - ad_sql#sheet_conf#items
	 * @return {items, asis} items - [hashmap<asi, value>] ,items ����˳����itemsConfһ��, asis - hashset<asi>
	 *  items���hashmap��value���壺������ͨitem����ֱ������ʾֵ��������ֵ������key ��price��item��value��jsonobject: "d" - Ϊ������ʾ��"p" �Ǽ۸������������
	 *  ��һ�۸�p ���Ǹ����������ݼ۸� p �����飬Ԫ����3��������ÿ��3��Ԫ�طֱ������Сֵ(���������ֵ����)���۸�, �ڻ������ʱ����Ҫ���빫ʽ:
	 *  amt(price, cell)
	 *   
	 * @throws Exception
	 */
	protected HashMap<String ,Object> loadPdtObject(int pdtId, JSONArray itemsConf) throws Exception{
		vc.put("pdtid", pdtId);
		if(storeId!=-1){
			vc.put("storeid", storeId);
		}
		ArrayList<HashMap<Integer, Object>> items=new ArrayList();		// ÿ�У� {asi, value}

		for(int i=0;i<itemsConf.length();i++){
			JSONObject itemconf=itemsConf.getJSONObject(i);
			String sql=itemconf.getString("sql");
			String key=itemconf.optString("key");
			boolean isPriceItem= "price".equals(key);
			HashMap<Integer, Object> vals=new HashMap();
			//sql format:select asi asi, xxx value from xxx where user_id=$uid and $pdtid=m_product_id
			//�����price�У��ڶ���Ԫ������ʾֵ�����е�3��Ԫ�أ�price�� �Ǿ����priceֵ������ݼ�js���䶨��:[0,400,50,401,1000,30,1001,99999,20]
			//�����쵽sheet�У� amt([0,400,50,401,1000,30,1001,99999,20], cell(3,4))
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
					if(Validator.isNotNull(value)) vals.put(asi, value); //Ϊ�վͲ�����
				}
			}
			
			
			items.add(vals);
			
		}
		//key: asi �����µ���asi
		HashSet<Integer> alaiableAsis =new HashSet();
		//��ȡ���µ���asi, ��Ҫ���ڵ�ǰ�, row: [asi], ���׼����ͬ���ǣ���ͬ�Ļ����ͬ���г�����Ʒ��Ȼ�в�ͬ
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
