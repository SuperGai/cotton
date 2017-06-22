package com.agilecontrol.b2bweb.cmd;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;

import com.agilecontrol.b2bweb.OrderSender;
import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.nea.util.Tools;
import com.agilecontrol.nea.util.Validator;
import com.agilecontrol.phone.CmdHandler;
import com.agilecontrol.phone.CmdResult;
import com.agilecontrol.phone.PhoneConfig;

/**
h1. ���Ͷ���

h2. ����

> {cmd:"b2b.order.send",  id}

���Ͷ������Է�erpϵͳ

*id* - int  b_bfo.id


 * 
 * @author yfzhu
 *
 */
public class SendOrder extends CmdHandler {
	
	
	public CmdResult execute(JSONObject jo) throws Exception {
		int orderId= this.getInt(jo, "id");

		//��չ����������ҵ��Ҫ������Ƿ���Ҫ���Ͷ���
		String senderClass=PhoneConfig.ORDER_SENDER_CLASS;
				
		if(Validator.isNotNull(senderClass)){
			
			OrderSender sender=null;
			try{
				sender=(OrderSender)Class.forName(senderClass).newInstance();
				sender.init(true,conn);//ͬ��ģʽ��ֱ�ӿ��Բ�ѯ�������� echocode �ֶ���
			}catch(Throwable tx){
				logger.error("check param#phone.order_sender_class", tx);
			}
			sender.send(orderId);
		}else{
			throw new NDSException("need param#phone.order_sender_class" );
		}
		JSONObject one=engine.doQueryObject("select echocode, echomessage from b_bfo where id=?", new Object[]{orderId}, conn);
		JSONObject ret=new JSONObject();
		String message;
		if( one.optInt("echocode")==0){
			message="����ɹ�";
		}else{
			message="����ʧ�ܣ�"+ one.optString("echomessage");
		}

		ret.put("message", message);//����Ϊshell�ű��ܿ�����ʾ
		ret.put("code", 1);//�ý�����ʾmessage��ˢ�£��μ��� objcontrol.js#_handleSPResult, ���Ǹ�webaction����ķ�ʽ
		CmdResult cr=new CmdResult(0, message/*����Ϊ��ad_action��ť�ܿ�����ʾ*/,ret);
		
		return cr;
	}

}
