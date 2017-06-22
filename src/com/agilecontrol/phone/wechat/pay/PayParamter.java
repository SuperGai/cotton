package com.agilecontrol.phone.wechat.pay;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import me.chanjar.weixin.common.util.RandomUtils;

import org.json.JSONObject;

import com.agilecontrol.phone.Admin;
import com.agilecontrol.phone.wechat.pay.util.GetWxOrderno;
import com.agilecontrol.phone.wechat.pay.util.RequestHandler;
import com.agilecontrol.phone.wechat.pay.util.Sha1Util;

/**
 * 生成服务号支付所需参数
 * 输入:{
 * 	  body:"",
 * 	  money:"",
 *    attach:"",
 *    docno:"",
 *    ip:"",
 *    openid:""
 * }
 * 输出:{
 *    "timestamp":"",
 *	  "Noncestr":"",
 *	  "package":"",
 *	  "paySign":""
 * }
 * 
 * @author cw
 * 
 */
@Admin(mail="wang.cun@lifecycle.cn")
public class PayParamter  {

	/*private static String appid = "wx054589af40758bfd";
	private static String appsecret = "9dcfa66b9ef377a39b663de01b07adf6";
	private static String mch_id = "1311689201";
	private static String partnerkey = "8F6CCADF9FF922482C377D15638DA603";
*/
	
	private static String appid = "wx721253a1f71930eb";
	private static String appsecret = "9281bd1a26ff420ab22b722dcc159b15";
	private static String mch_id = "1330570501";
	private static String partnerkey = "MX72CmmJ9u1H2fShniX1cFIvHewuKuGO";

	public static JSONObject createParamters(JSONObject jo) throws Exception {
		// 随机字符串
		String nonce_str = RandomUtils.getRandomStr();
		// 传入订单简介
		String body = jo.getString("body");
		// 传入附加数据
		String attach = jo.getString("attach");
		// 传入商户订单号
		String out_trade_no = jo.getString("docno");
		// 传入金额，按分计
		String money = jo.getString("money");
		// 传入ip
		String spbill_create_ip = jo.getString("ip");
		if (spbill_create_ip.startsWith("0:0:")) {
			spbill_create_ip = "127.0.0.1";
		}
		String openId = jo.getString("openid");
		
		
		String notify_url = "http://www.1688mj.com/servlets/binserv/WxPayUrl";
		
		/*
		 * 测试时设置cookie:debug=x，通过nginx判断cookie_debug来转发请求到测试服务器。
		 * 微信支付授权时，微信服务器会向portal发送请求，该请求不带cookie:debug，导致支付没法在测试服务器完成。
		 * 通过在支付请求时设置回调url:WxPayUrl?debug=x，在nginx中加入$arg_debug=x来完成转发。
		 * By zhangbh on 20160623
		 */
		String debug = jo.optString("debug", null);
		if (debug != null) {
			notify_url += "?debug=" + debug;
		}
		
		//支付类型，网页支付，APP支付，扫码支付
		String trade_type = "JSAPI";
		String openid = openId;
		SortedMap<String, String> packageParams = new TreeMap<String, String>();
		packageParams.put("appid", appid);
		packageParams.put("mch_id", mch_id);
		packageParams.put("nonce_str", nonce_str);
		packageParams.put("body", body);
		packageParams.put("attach", attach);
		packageParams.put("out_trade_no", out_trade_no);
		packageParams.put("total_fee", money);
		packageParams.put("spbill_create_ip", spbill_create_ip);
		packageParams.put("notify_url", notify_url);
		packageParams.put("trade_type", trade_type);
		packageParams.put("openid", openid);
		RequestHandler reqHandler = new RequestHandler(null, null);
		reqHandler.init(appid, appsecret, partnerkey);

		String sign = reqHandler.createSign(packageParams);
		String xml = "<xml>" + "<appid>" + appid + "</appid>" + "<mch_id>"
				+ mch_id + "</mch_id>" + "<nonce_str>" + nonce_str
				+ "</nonce_str>" + "<sign><![CDATA[" + sign + "]]></sign>"
				+ "<body><![CDATA[" + body + "]]></body>" + "<attach>" + attach
				+ "</attach>" + "<out_trade_no>" + out_trade_no
				+ "</out_trade_no>" + "<total_fee>" + money + "</total_fee>"
				+ "<spbill_create_ip>" + spbill_create_ip
				+ "</spbill_create_ip>" + "<notify_url>" + notify_url
				+ "</notify_url>" + "<trade_type>" + trade_type
				+ "</trade_type>" + "<openid>" + openid + "</openid>"
				+ "</xml>";
		String allParameters = "";
		try {
			allParameters = reqHandler.genPackage(packageParams);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		//微信网页支付参数获取
		String createOrderURL = "https://api.mch.weixin.qq.com/pay/unifiedorder";
		Map<String, Object> dataMap2 = new HashMap<String, Object>();
		String prepay_id = "";
		try {
			prepay_id = new GetWxOrderno().getPayNo(createOrderURL, xml);
		}
		catch (Exception e1) {
			e1.printStackTrace();
		}
		SortedMap<String, String> finalpackage = new TreeMap<String, String>();
		String appid2 = appid;
		String timestamp = Sha1Util.getTimeStamp();
		String nonceStr2 = nonce_str;
		String prepay_id2 = "prepay_id=" + prepay_id;
		String packages = prepay_id2;
		finalpackage.put("appId", appid2);
		finalpackage.put("timeStamp", timestamp);
		finalpackage.put("nonceStr", nonceStr2);
		finalpackage.put("package", packages);
		finalpackage.put("signType", "MD5");
		String finalsign = reqHandler.createSign(finalpackage);

		JSONObject json = new JSONObject();

		json.put("timestamp", timestamp);
		json.put("Noncestr", nonceStr2);
		json.put("package", packages);
		json.put("paySign", finalsign);

		return json;
	}
}
