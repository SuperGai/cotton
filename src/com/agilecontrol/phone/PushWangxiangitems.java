package com.agilecontrol.phone;
import java.net.URL;
import javax.xml.namespace.QName;
import javax.xml.rpc.ParameterMode;
import org.apache.axis.Constants;
import org.apache.axis.client.Call;
import org.apache.axis.client.Service;
import org.apache.axis.encoding.XMLType;
import java.util.*;
import java.io.ByteArrayInputStream;
import java.io.StringBufferInputStream;
import java.math.BigDecimal;
import java.rmi.RemoteException;
import java.sql.Connection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.servlet.http.Cookie;
import com.agilecontrol.b2b.schema.TableManager;
import com.agilecontrol.nea.core.control.ejb.Command;
import com.agilecontrol.nea.core.control.ejb.command.imgupload.FairConfig;
import com.agilecontrol.nea.core.control.event.DefaultWebEvent;
import com.agilecontrol.nea.core.control.util.ValueHolder;
import com.agilecontrol.nea.core.control.web.UserWebImpl;
import com.agilecontrol.nea.core.control.web.WebUtils;
import com.agilecontrol.nea.core.io.PluginLifecycleListener;
import com.agilecontrol.nea.core.process.SvrProcess;
import com.agilecontrol.nea.core.query.QueryEngine;
import com.agilecontrol.nea.core.query.QueryException;
import com.agilecontrol.nea.core.query.QueryUtils;
import com.agilecontrol.nea.core.query.SPResult;
import com.agilecontrol.phone.UserObj;
import com.agilecontrol.phone.util.JsonObjtoObj;
import com.agilecontrol.wanxiangcheng.EsalesSoap;
import com.agilecontrol.wanxiangcheng.EsalesSoapProxy;
import com.agilecontrol.wanxiangcheng.Esaleshdr;
import com.agilecontrol.wanxiangcheng.Esalesitem;
import com.agilecontrol.wanxiangcheng.Esalestender;
import com.agilecontrol.wanxiangcheng.Postesalescreaterequest;
import com.agilecontrol.wanxiangcheng.Postesalescreateresponse;
import com.agilecontrol.wanxiangcheng.Requestheader;
import com.agilecontrol.wanxiangcheng.Responseheader;
import com.strangeberry.jmdns.tools.Main;
import com.sun.org.apache.xml.internal.resolver.helpers.Debug;
import com.agilecontrol.nea.core.rest.SipStatus;
import com.agilecontrol.nea.core.util.CookieKeys;
import com.agilecontrol.nea.core.util.MessagesHolder;
import com.agilecontrol.nea.core.velocity.VelocityUtils;
import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.nea.util.NDSRuntimeException;
import com.agilecontrol.nea.util.Tools;
import com.agilecontrol.nea.util.Validator;
import org.apache.axis.utils.NetworkUtils;
import org.apache.velocity.VelocityContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.python.modules.thread.thread;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;

/**
 * 
 * @author gaigai
 * @time 2017/9/12
 * 推送订单信息给万象城,一天推送一次
 */
@Admin(mail="xuwj@cottonshop.com")
public class PushWangxiangitems  extends SvrProcess  {
	static Logger logger = LoggerFactory.getLogger(PushWangxiangitems.class);
	protected static Connection conn;
	protected static QueryEngine engine;
	protected static VelocityContext vc;
	@Override
	protected String doIt() throws Exception {
		String info=pushData();	
		return info;
	}

	@Override
	protected void prepare() {
		// TODO Auto-generated method stub
		
	}
	
	public static String pushData() throws Exception {
         int fail=0;
         int success=0;
		List<Postesalescreaterequest> pqs=getData();
		
		Responseheader responseheader=null;
		for (Postesalescreaterequest postesalescreaterequest : pqs) {
			EsalesSoap svc = (EsalesSoap) new EsalesSoapProxy();
			Postesalescreateresponse results = svc.postesalescreate(postesalescreaterequest);
			responseheader=results.getHeader();
			JSONObject result_jo = new JSONObject(responseheader);
			String flag = result_jo.getString("responsecode");
			String msg = result_jo.getString("responsemessage");
			int recCount = result_jo.getInt("updatecount");
			logger.debug(flag+"*******");
			/**
			 * responsecode 是否响应成功
			 */
			if (flag.equals("0")) {
	        	if(recCount==0){
	        		fail++;	        		
	        	}
	        	else{
					success++;
				}
			} 
			
		}
		return "此次同步信息 ,共处理"+pqs.size()+"条,失败"+fail+"条,成功"+success+"条";
	}

   
	public static List<Postesalescreaterequest> getData() throws Exception  {
		List<Postesalescreaterequest> pscr=new ArrayList<>(); 
		conn=QueryEngine.getInstance().getConnection();
		engine=QueryEngine.getInstance();
		JSONObject conf=(JSONObject)PhoneController.getInstance().getValueFromADSQLAsJSON("wanxiangcheng_conf", conn, false);
		Requestheader header = (Requestheader) JsonObjtoObj.json2Object(conf, Requestheader.class);		
		//遍历每天订单集合
		List<Esaleshdr> totals=getEsaleshdr();
        for (Esaleshdr esaleshdr : totals) {
        	Postesalescreaterequest ss=new Postesalescreaterequest();
        	ss.setEsalestotal(esaleshdr);
        	ss.setHeader(header);
    		ss.setEsalesitems(getEsalesitem(esaleshdr.getTxdocno()));	
        	ss.setEsalestenders(getEsalestender(esaleshdr.getTxdocno()));
        	pscr.add(ss);
        }		
		return pscr;	
	}
	
	//获取当前的每天的订单集合然后
    public static List<Esaleshdr> getEsaleshdr() throws Exception{
		JSONArray jsontotal =PhoneController.getInstance().getDataArrayByADSQL("wanxiangcheng_total", vc, conn, true);
		List<Esaleshdr> totals=new ArrayList<>();
		int size=jsontotal.length();
	    for (int i = 0; i < size; i++) {  	  
	     JSONObject jo = jsontotal.getJSONObject(i);
	     Esaleshdr es=(Esaleshdr) JsonObjtoObj.json2Object(jo, Esaleshdr.class);
	    totals.add(es);	
		}
		return totals;
    }
    
    //获取订单的货品信息
    public static Esalesitem[] getEsalesitem(String decno) throws Exception{
    	String pdtsql = PhoneController.getInstance().getValueFromADSQL("wanxiangcheng_salesItem",conn);	
		JSONArray jsontotal = engine.doQueryObjectArray(pdtsql, new Object[]{decno},conn);
		List<Esalesitem> item=new ArrayList<>();
		int size=jsontotal.length();
	    for (int i = 0; i < size; i++) {
	    	JSONObject jo = jsontotal.getJSONObject(i);
	    	Esalesitem ei=(Esalesitem) JsonObjtoObj.json2Object(jo, Esalesitem.class);
	    	item.add(ei);
		}
	    Esalesitem[] items=new Esalesitem[item.size()];
	    for (int i = 0; i < item.size(); i++) {
			items[i]=item.get(i);
		}
		return items;
    }
    
  //获取订单的付款信息
    public static Esalestender[] getEsalestender(String decno) throws Exception{
    	String pdtsql = PhoneController.getInstance().getValueFromADSQL("wanxiangcheng_salestender",conn);	
		JSONArray jsontotal = engine.doQueryObjectArray(pdtsql, new Object[]{decno,decno},conn);
		List<Esalestender> item=new ArrayList<>();
		int size=jsontotal.length();
	    for (int i = 0; i < size; i++) {
	    	JSONObject jo = jsontotal.getJSONObject(i);
	    	Esalestender ei=(Esalestender) JsonObjtoObj.json2Object(jo, Esalestender.class);
	    	item.add(ei);
		}
	    Esalestender[] items=new Esalestender[item.size()];
	    for (int i = 0; i < item.size(); i++) {
			items[i]=item.get(i);
		}
		return items;
    }
    


}