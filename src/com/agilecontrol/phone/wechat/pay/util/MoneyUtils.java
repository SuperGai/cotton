package com.agilecontrol.phone.wechat.pay.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.net.ssl.SSLContext;
import javax.servlet.http.HttpServletRequest;

import net.sf.json.JSONObject;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.Args;
import org.apache.http.util.CharArrayBuffer;
import org.apache.http.util.EntityUtils;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class MoneyUtils {
	private static Logger logger = LoggerFactory.getLogger(MoneyUtils.class);
	private static String appid = "wx721253a1f71930eb";// 应用ID
	private static String appsecret = "9281bd1a26ff420ab22b722dcc159b15";// 引用秘钥
	private static String partner = "1330570501";// 微信支付商户号
	private static String partnerkey = "MX72CmmJ9u1H2fShniX1cFIvHewuKuGO";// 财付通初始密码
	private static String charset = "UTF-8";

	/**
	 * 随机16位数值
	 * 
	 * @return
	 */
	public static String buildRandom() {
		String currTime = TenpayUtil.getCurrTime();
		String strTime = currTime.substring(8, currTime.length());
		int num = 1;
		double random = Math.random();
		if (random < 0.1) {
			random = random + 0.1;
		}
		for (int i = 0; i < 4; i++) {
			num = num * 10;
		}
		return (int) ((random * num)) + strTime;
	}

	/**
	 * 创建md5摘要,规则是:按参数名称a-z排序,遇到空值的参数不参加签名。 sign
	 * 
	 */
	public static String createSign(SortedMap<String, String> packageParams) {
		StringBuffer sb = new StringBuffer();
		Set es = packageParams.entrySet();
		Iterator it = es.iterator();
		while (it.hasNext()) {
			Map.Entry entry = (Map.Entry) it.next();
			String k = (String) entry.getKey();
			String v = (String) entry.getValue();
			if (null != v && !"".equals(v) && !"sign".equals(k)
					&& !"key".equals(k)) {
				sb.append(k + "=" + v + "&");
			}
		}
		sb.append("key=" + partnerkey);
		String sign = MD5Util.MD5Encode(sb.toString(), "UTF-8").toUpperCase();
		//System.out.println("签名:" + sign);
		return sign;

	}

	public static String getOrderNo() {
		String order = partner + new SimpleDateFormat("yyyyMMdd").format(new Date());
		Random r = new Random();
		for (int i = 0; i < 10; i++) {
			order += r.nextInt(9);
		}
		return order;
	}

	final static String KEYSTORE_PASSWORD = "1330570501";

	public static JSONObject doPostStr(String url, String outStr)
			throws Exception {
		KeyStore keyStore = KeyStore.getInstance("PKCS12");
		FileInputStream instream = new FileInputStream(new File("/opt/portal6/apiclient_cert.p12"));
		try {
			keyStore.load(instream, "1330570501".toCharArray());
		}
		finally {
			instream.close();
		}
		SSLContext sslcontext = SSLContexts.custom().loadKeyMaterial(keyStore, "1330570501".toCharArray()).build();
		SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(
				sslcontext, new String[] { "TLSv1" }, null,
				SSLConnectionSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER);
		CloseableHttpClient httpclient = HttpClients.custom().setSSLSocketFactory(sslsf).build();
		try {
			HttpGet httpget = new HttpGet("https://api.mch.weixin.qq.com/mmpaymkttransfers/sendredpack");
			System.out.println("executing request" + httpget.getRequestLine());
			CloseableHttpResponse response = httpclient.execute(httpget);
			try {
				HttpEntity entity = response.getEntity();
				/*System.out.println("----------------------------------------");
				System.out.println(response.getStatusLine());*/
				if (entity != null) {
					System.out.println("Response content length: "
							+ entity.getContentLength());
					BufferedReader bufferedReader = new BufferedReader(
							new InputStreamReader(entity.getContent()));
					String text;
					while ((text = bufferedReader.readLine()) != null) {
						System.out.println(text);
					}
				}
				EntityUtils.consume(entity);
			}
			finally {
				response.close();
			}
		}
		finally {
			httpclient.close();
		}

		DefaultHttpClient client = new DefaultHttpClient();
		HttpPost httpost = new HttpPost(url);
		JSONObject jsonObject = null;
		httpost.setEntity(new StringEntity(outStr, "UTF-8"));
		HttpResponse response = client.execute(httpost);
		String result = EntityUtils.toString(response.getEntity(), "UTF-8");
		jsonObject = JSONObject.fromObject(result);
		return jsonObject;
	}

	public static Map doSendMoney(String url, String data) throws Exception {
		KeyStore keyStore = KeyStore.getInstance("PKCS12");
		//FileInputStream instream = new FileInputStream(new File("D:/apiclient_cert.p12"));////P12文件目录
		FileInputStream instream = new FileInputStream(new File("/opt/portal6b/apiclient_cert.p12"));
		try {
			keyStore.load(instream, "1330570501".toCharArray());
		}
		finally {
			instream.close();
		}
		SSLContext sslcontext = SSLContexts.custom()
				.loadKeyMaterial(keyStore, KEYSTORE_PASSWORD.toCharArray())// 这里也是写密码的
				.build();
		// Allow TLSv1 protocol only
		SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(
				sslcontext, new String[] { "TLSv1" }, null,
				SSLConnectionSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER);
		CloseableHttpClient httpclient = HttpClients.custom()
				.setSSLSocketFactory(sslsf).build();
		try {
			HttpPost httpost = new HttpPost(url);  // 设置响应头信息
			httpost.addHeader("Connection", "keep-alive");
			httpost.addHeader("Accept", "*/*");
			httpost.addHeader("Content-Type","application/x-www-form-urlencoded; charset=UTF-8");
			httpost.addHeader("Host", "api.mch.weixin.qq.com");
			httpost.addHeader("X-Requested-With", "XMLHttpRequest");
			httpost.addHeader("Cache-Control", "max-age=0");
			httpost.addHeader("User-Agent","Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 6.0) ");
			httpost.setEntity(new StringEntity(data, "UTF-8"));
			CloseableHttpResponse response = httpclient.execute(httpost);
			try {
				HttpEntity entity = response.getEntity();
				//String jsonStr = toStringInfo(response.getEntity(), "UTF-8");
				Map map = toMapInfo(response.getEntity(), "UTF-8");
				EntityUtils.consume(entity);
				return map;
			}
			finally {
				response.close();
			}
		}
		finally {
			httpclient.close();
		}
	}
	
	
	public static Map doQueryMoney(String url, String data) throws Exception {
		KeyStore keyStore = KeyStore.getInstance("PKCS12");
		//FileInputStream instream = new FileInputStream(new File("D:/apiclient_cert.p12"));////P12文件目录
		FileInputStream instream = new FileInputStream(new File("/opt/portal6b/apiclient_cert.p12"));
		try {
			keyStore.load(instream, "1330570501".toCharArray());
		}
		finally {
			instream.close();
		}
		SSLContext sslcontext = SSLContexts.custom()
				.loadKeyMaterial(keyStore, KEYSTORE_PASSWORD.toCharArray())// 这里也是写密码的
				.build();
		SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(
				sslcontext, new String[] { "TLSv1" }, null,
				SSLConnectionSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER);
		CloseableHttpClient httpclient = HttpClients.custom()
				.setSSLSocketFactory(sslsf).build();
		try {
			HttpPost httpost = new HttpPost(url);  // 设置响应头信息
			httpost.addHeader("Connection", "keep-alive");
			httpost.addHeader("Accept", "*/*");
			httpost.addHeader("Content-Type","application/x-www-form-urlencoded; charset=UTF-8");
			httpost.addHeader("Host", "api.mch.weixin.qq.com");
			httpost.addHeader("X-Requested-With", "XMLHttpRequest");
			httpost.addHeader("Cache-Control", "max-age=0");
			httpost.addHeader("User-Agent","Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 6.0) ");
			httpost.setEntity(new StringEntity(data, "UTF-8"));
			CloseableHttpResponse response = httpclient.execute(httpost);
			try {
				HttpEntity entity = response.getEntity();
				//String jsonStr = toStringInfo(response.getEntity(), "UTF-8");
				Map map = toMapInfo(response.getEntity(), "UTF-8");
				EntityUtils.consume(entity);
				return map;
			}
			finally {
				response.close();
			}
		}
		finally {
			httpclient.close();
		}
	}

	private static String toStringInfo(HttpEntity entity, String defaultCharset)
			throws Exception, IOException {
		final InputStream instream = entity.getContent();
		if (instream == null) {
			return null;
		}
		try {
			Args.check(entity.getContentLength() <= Integer.MAX_VALUE,
					"HTTP entity too large to be buffered in memory");
			int i = (int) entity.getContentLength();
			if (i < 0) {
				i = 4096;
			}
			Charset charset = null;

			if (charset == null) {
				charset = Charset.forName(defaultCharset);
			}
			if (charset == null) {
				charset = HTTP.DEF_CONTENT_CHARSET;
			}
			final Reader reader = new InputStreamReader(instream, charset);
			final CharArrayBuffer buffer = new CharArrayBuffer(i);
			final char[] tmp = new char[1024];
			int l;
			while ((l = reader.read(tmp)) != -1) {
				buffer.append(tmp, 0, l);
			}
			return buffer.toString();
		}
		finally {
			instream.close();
		}
	}
	
	
	@SuppressWarnings("unused")
	private static Map toMapInfo(HttpEntity entity, String defaultCharset)
			throws Exception, IOException {
		final InputStream ins = entity.getContent();
		if (ins == null) {
			return null;
		}
		
		Map<String, String> map = new HashMap<String, String>();
		SAXReader reader = new SAXReader();
		Document doc = reader.read(ins);
		Element root = doc.getRootElement();
		
		List<Element> list = root.elements();
		
		for(Element e : list){
			map.put(e.getName(), e.getText());
		};
		
		return map;
	}

	public static String createXML(Map<String, String> map) {
		String xml = "<xml>";
		Set<String> set = map.keySet();
		Iterator<String> i = set.iterator();
		while (i.hasNext()) {
			String str = i.next();
			xml += "<" + str + ">" + "<![CDATA[" + map.get(str) + "]]>" + "</"
					+ str + ">";
		}
		xml += "</xml>";
		return xml;
	}
}
