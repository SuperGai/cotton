package com.agilecontrol.phone.wechat.pay;

import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.agilecontrol.phone.Admin;
import com.agilecontrol.phone.wechat.pay.util.GetWxOrderno;
import com.agilecontrol.phone.wechat.pay.util.RequestHandler;

import me.chanjar.weixin.common.util.RandomUtils;
import me.chanjar.weixin.common.util.crypto.WxCryptUtil;
import me.chanjar.weixin.mp.api.WxMpInMemoryConfigStorage;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.api.WxMpServiceImpl;
import me.chanjar.weixin.mp.bean.result.WxMpPayResult;

/**
 * 微信查单接口 输入：商家单号 返回： 1.付款成功 { isok:'S', acct_log_id :'', money:'' } 2.付款失败 {
 * isok: 'F', acct_log_id :'' }
 * 
 * @author cw
 *
 */
@Admin(mail="wang.cun@lifecycle.cn")
public class AppCheckPay {

	private static String appid = "wxed4204412f02b48d";
	private static String appsecret = "43b8501ac8745c5a43c8d04df7b4f2b9";
	private static String mch_id = "1334558501";
	private static String partnerkey = "MX72CmmJ9u1H2fShniX1cFIvHewuKuGO";
	private static Logger logger = LoggerFactory.getLogger(AppCheckPay.class);

	public static JSONObject getPayResult(String out_trade_no) throws Exception {
		// 随机字符串
		String nonce_str = RandomUtils.getRandomStr();

		SortedMap<String, String> packageParams = new TreeMap<String, String>();
		packageParams.put("appid", appid);
		packageParams.put("mch_id", mch_id);
		packageParams.put("out_trade_no ", out_trade_no);
		packageParams.put("nonce_str", nonce_str);
		RequestHandler reqHandler = new RequestHandler(null, null);
		reqHandler.init(appid, appsecret, partnerkey);
		String sign = reqHandler.createSign(packageParams);
		// String sign = WxCryptUtil.createSign(packageParams, partnerkey);
		String xml = "<xml>" + "<appid>" + appid + "</appid>" + "<mch_id>"
				+ mch_id + "</mch_id>" + "<out_trade_no>" + out_trade_no
				+ "</out_trade_no>" + "<nonce_str>" + nonce_str
				+ "</nonce_str>" + "<sign><![CDATA[" + sign + "]]></sign>"
				+ "</xml>";

		String allParameters = "";
		try {
			allParameters = reqHandler.genPackage(packageParams);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		String queryOrderURL = "https://api.mch.weixin.qq.com/pay/orderquery";
		// String return_code = null;
		Map map = new GetWxOrderno().getOrderNo(queryOrderURL, xml);
		JSONObject jso = new JSONObject();
		logger.debug("map=" + map);
		try {
			if ("SUCCESS".equals(map.get("trade_state"))) {
				jso.put("result", "S");
				jso.put("money", map.get("total_fee"));
				jso.put("transaction_id", map.get("transaction_id"));
			}
			else {
				jso.put("result", "F");
			}
		}
		catch (Exception e) {
			jso.put("result", "F");
		}
		logger.debug("jso", jso.toString());
		return jso;
	}

}
