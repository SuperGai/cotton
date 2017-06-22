package com.agilecontrol.phone.wechat.pay;

import java.math.BigDecimal;
import java.math.BigInteger;
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
 *     "cmd": "com.agilecontrol.phone.wechat.auth.pay.AppPayConfirm",
 *     "money": "",
 *     "body":"",
 *     "attach":"",
 *     "out_trade_no":""
 * }
 * Result: {
        "appId": ,
		"partnerId": ,
		"prepayId": ,
		"package": ,
		"nonceStr": ,
		"timeStamp": ,
		"sign" : 
 * }
 * 
 * @author cw 
 * 
 */
@Admin(mail="wang.cun@lifecycle.cn")
public class AppPayConfirm extends CmdHandler{

	
	private String appid = ConfigValues.get("app.appid","wxb37306c06a123501");;
	private String appsecret = ConfigValues.get("app.appsecret","6ae436be88cd792584717c57963efe98"); 
	private String partnerkey = ConfigValues.get("app.partnerkey","MX72CmmJ9u1H2fShniX1cFIvHewuKuGO");
	private String mch_id = ConfigValues.get("app.mch_id","1329351601");
	@Override
	public CmdResult execute(JSONObject jo) throws Exception {
		//应用ID
		//appid = ConfigValues.get("app.appid","wxb37306c06a123501");
		//应用Sercret
		//appsecret =  ConfigValues.get("app.appid");
		//商户号		
		//mch_id = ConfigValues.get("app.mch_id");
		
		//随机字符串
		String currTime = TenpayUtil.getCurrTime();
		String strTime = currTime.substring(8, currTime.length());
		String strRandom = TenpayUtil.buildRandom(4) + "";
		String strReq = strTime + strRandom;
		//设备号可以为空 String device_info = "";
		String nonce_str = strReq;
		
		String body = jo.getString("body");
		
		//附加数据
		String attach = jo.getString("attach");
		//String attach = "1";
		//String orderNo = appid + Sha1Util.getTimeStamp();
		//String out_trade_no = orderNo;
		//商户订单号
		String out_trade_no = jo.getString("out_trade_no");
		//金额，按分计
		String money = jo.getString("money");
		
		/*BigDecimal bdf = new BigDecimal(moneyf);
		BigDecimal bds = new BigDecimal(100);
		String money = bdf.multiply(bds).toBigInteger().toString();*/
		//终端ip
		//String spbill_create_ip = jo.optString("ip");
		String spbill_create_ip  = getHttpRequest().getRemoteAddr();
		if(spbill_create_ip.startsWith("0:0:")){
			spbill_create_ip = "127.0.0.1";
		}
		//服务号Secret
		//appsecret = ConfigValues.get("wechat.secret");
		//商户的key
		//partnerkey = ConfigValues.get("wechat.partnerkey");
		
        //String openId = usr.getOpenId();
		//String openid = openId;
		
		//body = "报表";
		
		
		String notify_url = "http://www.1688mj.com/servlets/binserv/WxPayUrl";
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
		//packageParams.put("openid", openid);
		RequestHandler reqHandler = new RequestHandler(null, null);
		//reqHandler.init(appid, appsecret, partnerkey);
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
		logger.debug("xml = " + xml);
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
		logger.debug("prepay_id:"+prepay_id);
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
		
		return new CmdResult(json);
	
	}
	
	protected HttpServletRequest getHttpRequest() {
		return event.getContext().getHttpServletRequest();
	}

}
