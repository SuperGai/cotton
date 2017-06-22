package com.agilecontrol.phone.wechat.pay;

import java.util.SortedMap;
import java.util.TreeMap;

import me.chanjar.weixin.common.util.RandomUtils;

import org.json.JSONObject;

import com.agilecontrol.phone.Admin;
import com.agilecontrol.phone.wechat.pay.util.GetWxOrderno;
import com.agilecontrol.phone.wechat.pay.util.RequestHandler;
import com.agilecontrol.phone.wechat.pay.util.Sha1Util;
/**
 * ���ɷ����֧���������
 * ����:{
 * 	  body:"",
 * 	  money:"",
 *    attach:"",
 *    docno:"",
 *    ip:"",
 * }
 * 
 * ���:{
     "appId":"";
	 "partnerId":"";
	 "prepayId":"";
	 "package","";
	 "nonceStr", "";
	 "timeStamp", "";
	 "sign", "";
 * }
 * @author cw
 *
 */
@Admin(mail="wang.cun@lifecycle.cn")
public class AppPayParamter {

	
	private static String appid = "wxed4204412f02b48d"; 
	private static String appsecret ="43b8501ac8745c5a43c8d04df7b4f2b9" ;
	private static String partnerkey = "MX72CmmJ9u1H2fShniX1cFIvHewuKuGO";
	private static String mch_id = "1334558501";

	public static  JSONObject createParamters(JSONObject jo) throws Exception{
		
		//����ַ���
		String nonce_str = RandomUtils.getRandomStr();
		//���붩�����
		String body = jo.getString("body");
		//���븽������
		String attach = jo.getString("attach");
		//�����̻�������
		String out_trade_no = jo.getString("docno");
		//��������ּ�
		String money = jo.getString("money");
		
		String spbill_create_ip  = jo.getString("ip");
		if(spbill_create_ip.startsWith("0:0:")){
			spbill_create_ip = "127.0.0.1";
		}
		String notify_url = "http://www.1688mj.com/servlets/binserv/WxPayUrl";
		
		/*
		 * ����ʱ����cookie:debug=x��ͨ��nginx�ж�cookie_debug��ת�����󵽲��Է�������
		 * ΢��֧����Ȩʱ��΢�ŷ���������portal�������󣬸����󲻴�cookie:debug������֧��û���ڲ��Է�������ɡ�
		 * ͨ����֧������ʱ���ûص�url:WxPayUrl?debug=x����nginx�м���$arg_debug=x�����ת����
		 * By zhangbh on 20160623
		 */
		String debug = jo.optString("debug", null);
		if (debug != null) {
			notify_url += "?debug=" + debug;
		}
		
		String trade_type = "APP";
		
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
		
		RequestHandler reqHandler = new RequestHandler(null, null);
		reqHandler.init(appid, appsecret, partnerkey);
		
		String sign = reqHandler.createSign(packageParams);
		String xml = "<xml>" 
		        + "<appid>" + appid + "</appid>" 
				+ "<mch_id>"+ mch_id + "</mch_id>"
		        + "<nonce_str>" + nonce_str+ "</nonce_str>"
				+ "<sign><![CDATA[" + sign + "]]></sign>"
				+ "<body><![CDATA[" + body + "]]></body>"
				+ "<attach>"+attach+"</attach>"
				+ "<out_trade_no>"+ out_trade_no+ "</out_trade_no>"
				+ "<total_fee>"+ money+ "</total_fee>"
				+ "<spbill_create_ip>" + spbill_create_ip + "</spbill_create_ip>"
				+ "<notify_url>" + notify_url + "</notify_url>"
				+ "<trade_type>" + trade_type + "</trade_type>" 
				+ "</xml>";
		String allParameters = "";
		try {
			allParameters = reqHandler.genPackage(packageParams);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		String createOrderURL = "https://api.mch.weixin.qq.com/pay/unifiedorder";
		String prepay_id = "";
		try {
			prepay_id = new GetWxOrderno().getPayNo(createOrderURL, xml);
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		
		SortedMap<String, String> finalpackage = new TreeMap<String, String>();
		
		String appid2 = appid;
		String partnerid = mch_id;
		String prepayid = prepay_id;
		String packages = "Sign=WXPay";
		String nonceStr2 = nonce_str;
		String timestamp = Sha1Util.getTimeStamp();
		
		finalpackage.put("appid", appid2);
		finalpackage.put("partnerid", partnerid);
		finalpackage.put("prepayid", prepayid);
		finalpackage.put("package", packages);
		finalpackage.put("noncestr", nonceStr2);
		finalpackage.put("timestamp", timestamp);
		String finalsign = reqHandler.createSign(finalpackage);
		
		JSONObject json = new JSONObject();
		json.put("appId", appid2);
		json.put("partnerId", partnerid);
		json.put("prepayId", prepayid);
		json.put("package", packages);
		json.put("nonceStr", nonceStr2);
		json.put("timeStamp", timestamp);
		json.put("sign", finalsign);
		
		return json;
	}
}
