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
h1. 发送订单

h2. 输入

> {cmd:"b2b.order.send",  id}

发送订单到对方erp系统

*id* - int  b_bfo.id


 * 
 * @author yfzhu
 *
 */
public class SendOrder extends CmdHandler {
	
	
	public CmdResult execute(JSONObject jo) throws Exception {
		int orderId= this.getInt(jo, "id");

		//扩展订单，根据业务要求决定是否需要传送订单
		String senderClass=PhoneConfig.ORDER_SENDER_CLASS;
				
		if(Validator.isNotNull(senderClass)){
			
			OrderSender sender=null;
			try{
				sender=(OrderSender)Class.forName(senderClass).newInstance();
				sender.init(true,conn);//同步模式，直接可以查询处理结果在 echocode 字段上
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
			message="传输成功";
		}else{
			message="传输失败："+ one.optString("echomessage");
		}

		ret.put("message", message);//这是为shell脚本能看到提示
		ret.put("code", 1);//让界面显示message后刷新，参见： objcontrol.js#_handleSPResult, 这是给webaction处理的方式
		CmdResult cr=new CmdResult(0, message/*这是为了ad_action按钮能看到提示*/,ret);
		
		return cr;
	}

}
