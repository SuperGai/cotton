package com.agilecontrol.b2b.cmd;

import org.json.JSONObject;

import com.agilecontrol.phone.Admin;
import com.agilecontrol.phone.CmdHandler;
import com.agilecontrol.phone.CmdResult;
/**
 * ������
 * ��ȡ��ά��
 * ������
 * �������ӵ�ַ��ת�����ļ������ؿͻ���
 * ���룺{
 * 
 * }
 * �����{
 * 
 * }
 * @author sunyifan
 *
 */
@Admin(mail="sun.yifan@lifecycle.cn")
public class GetQRCode extends CmdHandler{

	@Override
	public CmdResult execute(JSONObject jo) throws Exception {
		//String url = jo.getString("url");
		String url = this.getJoinURL();
		String logo = jo.getString("logo");
		//url = java.net.URLEncoder.encode(url,"UTF-8");
		logo = java.net.URLEncoder.encode(logo,"UTF-8");
		this.getJoinURL();
		String backUrl = "http://qr.topscan.com/api.php?logo="+logo+"&el:m&w=200&m=10&text="+url;
		JSONObject ret = new JSONObject();
		ret.put("qrpic",backUrl);
		return new CmdResult(ret);
	}

}
