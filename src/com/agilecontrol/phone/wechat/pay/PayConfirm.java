package com.agilecontrol.phone.wechat.pay;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONObject;

import com.agilecontrol.nea.core.util.ConfigValues;
import com.agilecontrol.phone.Admin;
import com.agilecontrol.phone.CmdHandler;
import com.agilecontrol.phone.CmdResult;
import com.agilecontrol.phone.wechat.pay.util.GetWxOrderno;
import com.agilecontrol.phone.wechat.pay.util.RequestHandler;
import com.agilecontrol.phone.wechat.pay.util.Sha1Util;
import com.agilecontrol.phone.wechat.pay.util.TenpayUtil;
/**
 * 微信调用JSSDK支付授权
 * URL: /servlets/binserv/Phone
 * MiscCmd: {
 *     "cmd": "com.agilecontrol.phone.wechat.pay.PayConfirm",
 *     "money": "",
 * }
 * Result: {
 *     "timestamp": "TIMESTAMP"
 *     "nonceStr": "NONCESTR",
 *     "package": "PACKAGE",
 *     "paySign": "PAYSIGN"
 * }
 * 
 * @author cw 
 * 
 */
@Admin(mail="wang.cun@lifecycle.cn")
public class PayConfirm extends CmdHandler{

	
	private String appid = "wx721253a1f71930eb";
	private String appsecret = "9dcfa66b9ef377a39b663de01b07adf6";
	private String mch_id = "1330570501";
	private String partnerkey = "MX72CmmJ9u1H2fShniX1cFIvHewuKuGO";
	@Override
	public CmdResult execute(JSONObject jo) throws Exception {
		
		//appid = ConfigValues.get("wechat.appid");
		//appsecret = ConfigValues.get("wechat.secret");
		//partnerkey = ConfigValues.get("wechat.partnerkey");
		//mch_id = ConfigValues.get("wechat.mch_id");
		//String money = jo.getString("money");
		//从页面获取数据
		String money = "1";
		String spbill_create_ip  = getHttpRequest().getRemoteAddr();
		if(spbill_create_ip.startsWith("0:0:0:0:0:0:0:1")){
			spbill_create_ip = "127.0.0.1";
		}
		String body = jo.getString("body");
		String attach = jo.getString("attach");
		String out_trade_no = jo.getString("out_trade_no");
		
		String openId  = usr.getOpenId();
		//String openId = "o2q_gv7TuZ_pkRDMETfTvlhQr4gw";//用oath授权得到的openid
		String currTime = TenpayUtil.getCurrTime();
		String strTime = currTime.substring(8, currTime.length());
		String strRandom = TenpayUtil.buildRandom(4) + "";
		String strReq = strTime + strRandom;
		String device_info = "";
		String nonce_str = strReq;
		String notify_url = "http://www.1688mj.com/servlets/binserv/WxPayUrl";
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
				+ "<body><![CDATA[" + body + "]]></body>"
				+ "<attach>"+attach+"</attach>"
				+ "<out_trade_no>"
				+ out_trade_no
				+ "</out_trade_no>"
				+"<total_fee>"
				+ money
				+ "</total_fee>"
				+"<spbill_create_ip>" + spbill_create_ip + "</spbill_create_ip>"
				+ "<notify_url>" + notify_url + "</notify_url>"
				+ "<trade_type>" + trade_type + "</trade_type>" + "<openid>"
				+ openid + "</openid>" + "</xml>";
		System.out.println("xml = " + xml);
		String allParameters = "";
		try {
			allParameters = reqHandler.genPackage(packageParams);
		} catch (Exception e) {
			e.printStackTrace();
		}
		String createOrderURL = "https://api.mch.weixin.qq.com/pay/unifiedorder";
		Map<String, Object> dataMap2 = new HashMap<String, Object>();
		String prepay_id = "";
		try {
			prepay_id = new GetWxOrderno().getPayNo(createOrderURL, xml);
			if (prepay_id.equals("")) {
				System.out.println("统一支付接口获取预支付订单出错");
			}
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		System.out.println("prepay_id:"+prepay_id);
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
		
		return new CmdResult(json);
	
	}
	
	protected HttpServletRequest getHttpRequest() {
		return event.getContext().getHttpServletRequest();
	}

}
