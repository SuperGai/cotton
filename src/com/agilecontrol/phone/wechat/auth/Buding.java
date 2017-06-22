package com.agilecontrol.phone.wechat.auth;

import org.json.JSONObject;

import redis.clients.jedis.Jedis;

import com.agilecontrol.nea.core.control.web.WebUtils;
import com.agilecontrol.phone.Admin;
/**
 * 布丁项目在微信登陆的时候进行确认，如果不存在就注册，存在就登陆，根据openid
 * 
 * 
 * URL: /servlets/binserv/Phone
 * MiscCmd: {
 *     "cmd": "com.agilecontrol.phone.wechat.auth.Buding",
 *     "code": "", --这里有wechat的token相关信息可以
 *     "state": ""
 *     "joincode": 来自客户Join的时候的querystring，即邀请码的加密信息
 * }
 * Result: {
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

 * @author yfzhu
 *
 */
@Admin(mail="wang.cun@lifecycle.cn")
public class Buding extends WeChatAuth {

	/**
	 *@param json -  
	 *     "code": "", --这里有wechat的token相关信息可以
	 *     "state": ""
	 *     "joincode": 来自客户Join的时候的querystring，即邀请码的加密信息
 
	 */
	protected Object accept(JSONObject json) throws Exception {
		logger.debug("call accept("+ json+")");
		Jedis jedis=WebUtils.getJedis();
		try{
			BudingAuth ba=new BudingAuth(jedis, conn, this.event.getContext().getHttpServletRequest(), this.event.getContext().getHttpServletResponse());
			return ba.accept(json);
		}finally{
			jedis.close();
		}
	}

}
