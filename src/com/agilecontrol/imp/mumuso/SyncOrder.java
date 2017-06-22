package com.agilecontrol.imp.mumuso;


import java.net.URL;
import java.sql.Connection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.agilecontrol.nea.core.monitor.ObjectActionEvent;
import com.agilecontrol.nea.core.query.QueryEngine;
import com.agilecontrol.nea.core.query.SPResult;
import com.agilecontrol.phone.PhoneConfig;


/**
 * post data to service
 * 
 * @param e : 订单id（e.getObjId();）
 * @return
 * @throws Exception
 */
public class SyncOrder implements Runnable {
	private static final Logger logger = LoggerFactory.getLogger(SyncOrder.class);

	private int orderId;
	/**
	 * 
	 * @param orderId 订单id
	 */
	public SyncOrder(int orderId){
		this.orderId=orderId;
	}
	/**
	 * 订单主表数据
	 * 
	 * @param id
	 * @return
	 * @throws Exception
	 */
	private JSONObject postHeadOrder(int id, Connection conn) throws Exception {

		String sql = QueryEngine.getInstance().doQueryString(
						"select value from ad_sql where name='bfo_info' ",
						new Object[] {}, conn).toString();
		JSONObject ja = QueryEngine.getInstance().doQueryObject(sql,new Object[] { id });
		SimpleDateFormat sd = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
		String ocrdate = sd.format(new Date());
		JSONObject jo = new JSONObject();

		jo.put("uuid", ja.getString("uuid"));
		jo.put("senderCode", ja.getString("sendercode"));
		jo.put("senderWrh", ja.getString("senderwrh"));
		jo.put("receiverCode", ja.getString("receivercode"));
		jo.put("contactor", ja.getString("contactor"));
		jo.put("phoneNumber", ja.getString("phonenumber"));
		jo.put("deliverAddress", ja.getString("deliveraddress"));
		jo.put("remark", ja.getString("remark"));
		jo.put("ocrDate", ocrdate);
		jo.put("filler", ja.getString("filler"));
		jo.put("seller", ja.getString("seller"));
		
		return jo;

	}

	/**
	 * 订单子表上数据
	 * 
	 * @param id
	 * @return
	 * @throws Exception
	 */
	private JSONArray postWithOutOrder(int id, Connection conn) throws Exception {

		String sql = QueryEngine.getInstance().doQueryString(
						"select value from ad_sql where name='bfoitem_info' ",
						new Object[] {}, conn).toString();

		JSONArray ja = QueryEngine.getInstance().doQueryObjectArray(sql,new Object[] { id });
		JSONArray js = new JSONArray();
		
		for (int i = 0; i < ja.length(); i++) {
			JSONObject jo = new JSONObject();
			jo.put("skuId", ja.getJSONObject(i).getString("name"));
			jo.put("qty", ja.getJSONObject(i).getDouble("qty"));
			jo.put("price", ja.getJSONObject(i).getDouble("price"));
			js.put(jo);
		}
		return js;
	}
	
	public void run() {
		
		Connection conn =null;
		try {
			QueryEngine engine=QueryEngine.getInstance();
			conn=engine.getConnection();

			if(0== engine.doQueryInt("select nvl(echocode,-1) from b_bfo where id=?", new Object[]{orderId}, conn)){
				logger.warn("已经传输成功了 orderid="+ orderId);
				return;
			}
			send(conn);
			
		}catch(Throwable tx){
			logger.error("Fail to send:", tx);
			try{
				QueryEngine.getInstance().executeUpdate("update B_BFO set echoCode=?,echoMessage=? where id =? ", new Object[]{1, tx.getMessage(), orderId});
			}catch(Throwable tt){
				logger.error("Fail to update orderid="+orderId, tx);
			}
		}finally {
			try{if(conn!=null)conn.close();}catch(Throwable tx){}
		}
	}

	private  void send(Connection conn)  throws Exception{

		/**
		 * 根据sql包装成json数据
		 */
		JSONObject data = new JSONObject();
		
		String url = PhoneConfig.ORDER_SENDER_URL;
		CloseableHttpClient httpclient = HttpClientBuilder.create().build();

		URL urlObj=new URL(url);
		/**
		 * 采用HTTP Basic Authentication 进行身份认证。
		 */
		HttpHost targetHost = new HttpHost(urlObj.getHost(), urlObj.getPort(), urlObj.getProtocol());
		
		CredentialsProvider credsProvider = new BasicCredentialsProvider();
		
		credsProvider.setCredentials(new AuthScope(targetHost.getHostName(),targetHost.getPort()), 
				                     new UsernamePasswordCredentials(PhoneConfig.ORDER_SENDER_LOGIN, PhoneConfig.ORDER_SENDER_PASSWORD));// 服务端的用户名和密码
		
		AuthCache authCache = new BasicAuthCache();

		BasicScheme basicAuth = new BasicScheme();

		authCache.put(targetHost, basicAuth);

		HttpClientContext context = HttpClientContext.create();
		context.setCredentialsProvider(credsProvider);
		context.setAuthCache(authCache);


		// 取数据
		data = postHeadOrder(orderId, conn);
		data.put("products", postWithOutOrder(orderId, conn));
		

		/**
		 * 将数据传送远端
		 */
		HttpPost httppost = new HttpPost(url);

		// 设置请求和传输超时时间
		RequestConfig requestConfig = RequestConfig.custom()
				.setSocketTimeout(600000).setConnectTimeout(600000).build();

		httppost.setConfig(requestConfig);
		// post head
		httppost.setHeader("Content-Type", "application/json;charset=utf-8");
		httppost.setHeader("Accept", "application/json;charset=utf-8");

		// post body
		StringEntity se = new StringEntity(data.toString(), "UTF-8");
		httppost.setEntity(se);

		/**
		 * 从远端响应信息
		 */
		CloseableHttpResponse response = httpclient.execute(targetHost,httppost, context);
		try {
			String result = IOUtils.toString(response.getEntity().getContent(),"UTF-8");
			
			logger.debug("result="+ result);
			
			JSONObject js = new JSONObject(result);

			String sql = "update B_BFO set echoCode=?,echoMessage=?,uuid=? where id =?";

			QueryEngine.getInstance().executeUpdate(sql,new Object[] { 
					                js.optString("echoCode"),
									js.optString("echoMessage"),
									js.optString("uuid") ,orderId},conn);

//			if (js.optString("echoCode") == "1") {
//				throw new Exception(js.optString("echoMessage"));
//			} else {
//				return new SPResult(js.toString());
//			}

		} finally {
			response.close();
		}
		
	}

}
