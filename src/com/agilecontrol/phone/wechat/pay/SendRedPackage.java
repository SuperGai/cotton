package com.agilecontrol.phone.wechat.pay;

import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;






import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.agilecontrol.phone.Admin;
import com.agilecontrol.phone.wechat.auth.BudingAuth;
import com.agilecontrol.phone.wechat.pay.util.MoneyUtils;
/**
 * 红包发送和红包查询
 * @author cw
 *
 */
@Admin(mail="wang.cun@lifecycle.cn")
public class SendRedPackage {
	//红包发送地址
	final static  String url = "https://api.mch.weixin.qq.com/mmpaymkttransfers/sendredpack";
	//红包查询地址
	final static  String queryUrl = "https://api.mch.weixin.qq.com/mmpaymkttransfers/gethbinfo";
	private static final Logger logger = LoggerFactory.getLogger(SendRedPackage.class);

	public static Map sendredpackage(JSONObject jso) throws JSONException{
		String money = jso.getString("money");
		String note = jso.getString("note");
		String openid = jso.getString("openid");
		String orderNNo =  MoneyUtils.getOrderNo();
		SortedMap<String, String> map = new TreeMap<String, String>();
		map.put("nonce_str", MoneyUtils.buildRandom());//随机字符串
		map.put("mch_billno", orderNNo);//商户订单
		map.put("mch_id", "1330570501");//商户号
		map.put("wxappid", "wx721253a1f71930eb");//商户appid
		map.put("nick_name", "麦+");//提供方名称
		map.put("send_name", "麦+");//用户名
		map.put("re_openid", openid);//用户openid
		map.put("total_amount", money);//付款金额
		map.put("min_value", money);//最小红包
		map.put("max_value", money);//最大红包
		map.put("total_num", "1");//红包发送总人数
		map.put("wishing", "生意兴隆，财源广进");//红包祝福语
		map.put("client_ip", "127.0.0.1");//ip地址
		map.put("act_name", "充值返现");//活动名称
		map.put("remark", note);//备注
		map.put("sign", MoneyUtils.createSign(map));//签名
		Map result = null;
		try {
			result = MoneyUtils.doSendMoney(url, MoneyUtils.createXML(map));
			logger.debug("Send="+result.toString());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}
	
	public static Map queryredpackage(JSONObject jso) throws JSONException{
		String orderNNo =  jso.getString("orderNNo"); 
		SortedMap<String, String> map = new TreeMap<String, String>();
		map.put("nonce_str", MoneyUtils.buildRandom());//随机字符串
		map.put("mch_billno", orderNNo);//商户订单
		map.put("mch_id", "1330570501");//商户号
		map.put("appid", "wx721253a1f71930eb");//商户appid
		map.put("bill_type", "MCHT");
		map.put("sign", MoneyUtils.createSign(map));//签名
		Map result = null;
		try {
			result = MoneyUtils.doQueryMoney(queryUrl, MoneyUtils.createXML(map));
			logger.debug("Query:"+result.toString());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

}