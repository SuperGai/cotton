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
 * ������ͺͺ����ѯ
 * @author cw
 *
 */
@Admin(mail="wang.cun@lifecycle.cn")
public class SendRedPackage {
	//������͵�ַ
	final static  String url = "https://api.mch.weixin.qq.com/mmpaymkttransfers/sendredpack";
	//�����ѯ��ַ
	final static  String queryUrl = "https://api.mch.weixin.qq.com/mmpaymkttransfers/gethbinfo";
	private static final Logger logger = LoggerFactory.getLogger(SendRedPackage.class);

	public static Map sendredpackage(JSONObject jso) throws JSONException{
		String money = jso.getString("money");
		String note = jso.getString("note");
		String openid = jso.getString("openid");
		String orderNNo =  MoneyUtils.getOrderNo();
		SortedMap<String, String> map = new TreeMap<String, String>();
		map.put("nonce_str", MoneyUtils.buildRandom());//����ַ���
		map.put("mch_billno", orderNNo);//�̻�����
		map.put("mch_id", "1330570501");//�̻���
		map.put("wxappid", "wx721253a1f71930eb");//�̻�appid
		map.put("nick_name", "��+");//�ṩ������
		map.put("send_name", "��+");//�û���
		map.put("re_openid", openid);//�û�openid
		map.put("total_amount", money);//������
		map.put("min_value", money);//��С���
		map.put("max_value", money);//�����
		map.put("total_num", "1");//�������������
		map.put("wishing", "������¡����Դ���");//���ף����
		map.put("client_ip", "127.0.0.1");//ip��ַ
		map.put("act_name", "��ֵ����");//�����
		map.put("remark", note);//��ע
		map.put("sign", MoneyUtils.createSign(map));//ǩ��
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
		map.put("nonce_str", MoneyUtils.buildRandom());//����ַ���
		map.put("mch_billno", orderNNo);//�̻�����
		map.put("mch_id", "1330570501");//�̻���
		map.put("appid", "wx721253a1f71930eb");//�̻�appid
		map.put("bill_type", "MCHT");
		map.put("sign", MoneyUtils.createSign(map));//ǩ��
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