package com.agilecontrol.phone.wechat.pay.util;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.agilecontrol.nea.core.control.web.binhandler.GetFile;
import com.agilecontrol.phone.wechat.pay.util.http.HttpClientConnectionManager;


public class GetWxOrderno {
	public static DefaultHttpClient httpclient;
	public static String getPayNo(String url, String xmlParam) {
	Logger logger= LoggerFactory.getLogger(GetWxOrderno.class);	 
		httpclient = new DefaultHttpClient();
		httpclient = (DefaultHttpClient) HttpClientConnectionManager
				.getSSLInstance(httpclient);
		DefaultHttpClient client = new DefaultHttpClient();
		client.getParams().setParameter(ClientPNames.ALLOW_CIRCULAR_REDIRECTS,
				true);
		HttpPost httpost = HttpClientConnectionManager.getPostMethod(url);
		String prepay_id = "";
		try {
			httpost.setEntity(new StringEntity(xmlParam, "UTF-8"));
			HttpResponse response = httpclient.execute(httpost);
			String jsonStr = EntityUtils
					.toString(response.getEntity(), "UTF-8");
			Map<String, Object> dataMap = new HashMap<String, Object>();
			logger.debug(jsonStr);

			if (jsonStr.indexOf("FAIL") != -1) {
				return prepay_id;
			}
			Map map = doXMLParse(jsonStr);
			String return_code = (String) map.get("return_code");
			prepay_id = (String) map.get("prepay_id");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return prepay_id;
	}
	
	
	//订单查询接口
	  public static Map getOrderNo(String url,String xmlParam){
		  Logger logger= LoggerFactory.getLogger(GetWxOrderno.class);	
		  DefaultHttpClient client = new DefaultHttpClient();
		  client.getParams().setParameter(ClientPNames.ALLOW_CIRCULAR_REDIRECTS, true);
		  HttpPost httpost= HttpClientConnectionManager.getPostMethod(url);
		  String trade_state = "";
		  Map map = new HashMap<String, Object>();
		try {
			 httpost.setEntity(new StringEntity(xmlParam, "UTF-8"));
			 HttpResponse response = httpclient.execute(httpost);
			 String jsonStr = EntityUtils.toString(response.getEntity(), "UTF-8");
			 logger.debug("checkAppPayResult==========="+jsonStr);
			 Map<String, Object> dataMap = new HashMap<String, Object>();
			 map = doXMLParse(jsonStr);
			 if(jsonStr.indexOf("FAIL")!=-1){
			    	return map;
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return map;
	  }

	
	public static Map doXMLParse(String strxml) throws Exception {
		if (null == strxml || "".equals(strxml)) {
			return null;
		}

		Map m = new HashMap();
		InputStream in = String2Inputstream(strxml);
		SAXBuilder builder = new SAXBuilder();
		Document doc = builder.build(in);
		Element root = doc.getRootElement();
		List list = root.getChildren();
		Iterator it = list.iterator();
		while (it.hasNext()) {
			Element e = (Element) it.next();
			String k = e.getName();
			String v = "";
			List children = e.getChildren();
			if (children.isEmpty()) {
				v = e.getTextNormalize();
			} else {
				v = getChildrenText(children);
			}

			m.put(k, v);
		}

		
		in.close();

		return m;
	}

	
	public static String getChildrenText(List children) {
		StringBuffer sb = new StringBuffer();
		if (!children.isEmpty()) {
			Iterator it = children.iterator();
			while (it.hasNext()) {
				Element e = (Element) it.next();
				String name = e.getName();
				String value = e.getTextNormalize();
				List list = e.getChildren();
				sb.append("<" + name + ">");
				if (!list.isEmpty()) {
					sb.append(getChildrenText(list));
				}
				sb.append(value);
				sb.append("</" + name + ">");
			}
		}

		return sb.toString();
	}

	public static InputStream String2Inputstream(String str) {
		return new ByteArrayInputStream(str.getBytes());
	}

}