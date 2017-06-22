package com.agilecontrol.phone.wechat.pay;

import me.chanjar.weixin.common.bean.WxJsapiSignature;
import me.chanjar.weixin.common.exception.WxErrorException;
import me.chanjar.weixin.mp.api.WxMpInMemoryConfigStorage;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.api.WxMpServiceImpl;

import org.json.JSONObject;

import com.agilecontrol.nea.core.util.ConfigValues;
import com.agilecontrol.phone.Admin;
import com.agilecontrol.phone.CmdHandler;
import com.agilecontrol.phone.CmdResult;
/**
 * 微信调用JSSDK页面授权
 * URL: /servlets/binserv/Phone
 * MiscCmd: {
 *     "cmd": "com.agilecontrol.phone.wechat.auth.PageConfirm",
 *     "url": "",
 * }
 * Result: {
 *     "appid": "APPID"
 *     "signature": "SIGNATURE",
 *     "timestamp": "TIMESTAMP",
 *     "nonceStr": "NONCESTR"
 * }
 * 
 * @author cw 
 * 
 */
@Admin(mail="wang.cun@lifecycle.cn")
public class PageConfirm extends CmdHandler{

	private WxMpService mpService;
	private String appid;
	private String secret;
	private String token;
	private String asdkey;
	/**
	 * Guest can execute this task, default to false
	 * 
	 * @return
	 */
	public boolean allowGuest() {
		return true;
	}
	@Override
	public CmdResult execute(JSONObject jo) throws Exception {
		String url = jo.getString("url");
		mpService = new WxMpServiceImpl();
		WxMpInMemoryConfigStorage config = new WxMpInMemoryConfigStorage();
		
		
		//测试中，值先写死
		/*appid = ConfigValues.get("wechat.appid");
		secret = ConfigValues.get("wechat.secret");
		token = ConfigValues.get("wechat.token");
	    asdkey = ConfigValues.get("wechat.asdkey");*/
		
		appid = "wx721253a1f71930eb";
		secret = "9281bd1a26ff420ab22b722dcc159b15";
		token = "maijia";
	    asdkey = "MahBQUHMoKcaINKHX6wite2FmpKFU6M6Nn1ju96tfHR";
		
		config.setAppId(appid); // 设置微信公众号的appid
		config.setSecret(secret); // 设置微信公众号的app corpSecret
		config.setToken(token); // 设置微信公众号的token
		config.setAesKey(asdkey); // 设置微信公众号的EncodingAESKey
		
		mpService.setWxMpConfigStorage(config);
		JSONObject json = new JSONObject();
		try {
			
			WxJsapiSignature wjs = mpService.createJsapiSignature(url);

			String noncestr = wjs.getNoncestr();
			long timestamp = wjs.getTimestamp();
			String signature = wjs.getSignature();
			json.put("appId", appid);
			json.put("nonceStr", noncestr);
			json.put("timeStamp", timestamp);
			json.put("signature", signature);
		} catch (WxErrorException e) {
			e.printStackTrace();
		}
		return new CmdResult(json);
	}

}
