package com.agilecontrol.phone.wechat.auth;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.text.MessageFormat;
import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONException;
import org.json.JSONObject;

import com.agilecontrol.nea.core.util.ConfigValues;
import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.nea.util.Validator;
import com.agilecontrol.phone.Admin;
import com.agilecontrol.phone.CmdHandler;
import com.agilecontrol.phone.CmdResult;

/**
 * <h1>Authentication with WeChat</h1>
 * <pre>
 * <b>Inputs:</b>
 *   * code: a authentication code from wechat server
 *   * state: used to prevent csrf attack
 * 
 * <b>Authentication:</b>
 *   Code will be sent to wechat server to do verification.
 *   
 * <b>Get user details:</b>
 *   Get nickname, gender, avatar, etc after authenticated.  
 * 
 * <b>Then:</b>
 *   Implements {@code #accept(JSONObject)} to do anything
 *   after code is authenticated from wechat server.
 * 
 * <b>Returns:</b>
 *   Anything the implementation class returns
 * </pre>
 * 
 * @author ZhangBH
 */
@Admin(mail="wang.cun@lifecycle.cn")
public abstract class WeChatAuth extends CmdHandler {
	
	public static final String KEY_APP_ID = "wechat.appid";
	public static final String KEY_SECRET = "wechat.secret";
	public static final String KEY_LANG = "wechat.lang";
	
	public static final String SCOPE_BASE = "snsapi_base";
	public static final String SCOPE_USERINFO = "snsapi_userinfo";
	
	public static final String LANG_ZH_CN = "zh_CN";
	public static final String LANG_ZH_TW = "zh_TW";
	public static final String LANG_EN = "en";
	
	public static final String URL_GET_TOKEN = "https://api.weixin.qq.com/sns/oauth2/access_token?appid={0}&secret={1}&code={2}&grant_type=authorization_code";
	public static final String URL_REFRESH_TOKEN = "https://api.weixin.qq.com/sns/oauth2/refresh_token?appid={0}&grant_type=refresh_token&refresh_token={1}";
	public static final String URL_GET_USERINFO = "https://api.weixin.qq.com/sns/userinfo?access_token={0}&openid={1}&lang={2}";

	private String appid;
	private String secret;
	private String lang;
	
	public WeChatAuth() {
		
		appid = ConfigValues.get(KEY_APP_ID, "wx721253a1f71930eb");
		secret = ConfigValues.get(KEY_SECRET, "9281bd1a26ff420ab22b722dcc159b15");
		lang = ConfigValues.get(KEY_LANG, LANG_ZH_CN);
		
		logger.debug("AppID: " + appid + ", Secret: " + secret);
	}
	
	/**
	 * @param jo {
	 *     "code": "CODE",
	 *     "state": "STATE"
	 * }
	 * @return anything the implementation class returns
	 */
	@Override
	public CmdResult execute(JSONObject jo) throws Exception {
		logger.debug("jo= " + jo);
		String code = jo.getString("code");
		String state = jo.getString("state");
		String joincode = jo.optString("joincode");
		String ip = getHttpRequest().getRemoteAddr();
		
		JSONObject json = new JSONObject();
		json.put("ip", ip);
		json.put("state", state);
		json.put("lang", lang);
		if(Validator.isNotNull(joincode)){
			json.put("joincode",joincode);
		}
		JSONObject token = getToken(code);
		logger.debug("Token: " + token);
		
		if (token.optInt("errcode") == 0) {
			
			String accessToken = token.getString("access_token");
			//String refreshToken = token.getString("refresh_token"); // Not used yet
			String openId = token.getString("openid");
			String scope = token.getString("scope");
			String unionId = token.getString("unionid");
			
			json.put("openid", openId);
			json.put("scope", scope);
			json.put("unionid", unionId);
			
			if (scope.contains(SCOPE_USERINFO)) {
				
				JSONObject info = getUserInfo(accessToken, openId);
				logger.debug("UserInfo: " + info);
				
				if (info.optInt("errcode") == 0) {
					Iterator<?> ite = info.keys();
					while (ite.hasNext()) {
						String key = (String) ite.next();
						json.put(key, info.get(key));
					}
					
				} else {
					logger.info(info.put("ip", ip).toString());
					throw new NDSException(info.getString("errmsg"));
				}
			}
		} else {
			logger.info(token.put("ip", ip).toString());
			throw new NDSException(token.getString("errmsg"));
		}
		
		logger.debug("json: " + json);
		
		Object ret = accept(json);
		logger.debug("ret: " + ret);
		//logger.info(MessageFormat.format("Client (IP:{0}, OpenID:{1}) accepted.", ip, json.get("openid")));		
		return new CmdResult(ret);
	}
	
	/**
	 * After wechat authentication finished, implements this method to do some
	 * things like login to portal or register new user, etc.
	 * @param json {
	 *     "ip": "REMOTE ADDR"
	 *     "state": "USED TO PREVENT CSRF ATTACK",
	 *     "openid": "OPENID",
	 *     "scope": "snsapi_base | snsapi_userinfo",
	 *     "unionid": "UNIONID",
	 *     // If scope eq snsapi_userinfo, the following will be included
	 *     "lang": "In which language city, province and country will be returned, configured in ad_param",
	 *     "language": "User's preferred language",
	 *     "nickname": "NICKNAME",
	 *     "sex": "SEX",
	 *     "province": "PROVINCE",
	 *     "city": "CITY",
	 *     "country": "COUNTRY",
	 *     "headimgurl": "HEADIMGURL"
	 *     "privilege": "PRIVILEGE"
	 * }
	 * @return JSONObject which will return to the client
	 * @throws Exception
	 */
	protected abstract Object accept(JSONObject json) throws Exception;
	
	protected JSONObject getToken(String code) throws IOException {
		URL url = new URL(MessageFormat.format(URL_GET_TOKEN, appid, secret, code));
		return httpGet(url);
	}
	
	protected JSONObject refreshToken(String refreshToken) throws IOException {
		URL url = new URL(MessageFormat.format(URL_REFRESH_TOKEN, appid, refreshToken));
		return httpGet(url);
	}
	
	protected JSONObject getUserInfo(String accessToken, String openId) throws IOException {
		URL url = new URL(MessageFormat.format(URL_GET_USERINFO, accessToken, openId, lang));
		return httpGet(url);
	}
	
	protected JSONObject httpGet(URL url) throws IOException {
		URLConnection c = url.openConnection();
		JSONObject json;
		BufferedReader r = null;
		try {
			r = new BufferedReader(new InputStreamReader(c.getInputStream(), "UTF-8"));
			StringBuilder sb = new StringBuilder();
			String ln;
			while ((ln = r.readLine()) != null) {
				sb.append(ln);
			}
			String s = sb.toString();
			//logger.debug("Get JSON: " + s);
			try {
				json = new JSONObject(s);
			} catch (JSONException e) {
				throw new IOException(e);
			}
		} catch (IOException e) {
			throw e;
		} finally {
			if (r != null) r.close();
		}
		return json;
	}
	
	protected HttpServletRequest getHttpRequest() {
		return event.getContext().getHttpServletRequest();
	}

	@Override
	public boolean allowGuest() {
		return true;
	}

}
