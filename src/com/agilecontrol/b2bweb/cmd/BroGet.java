package com.agilecontrol.b2bweb.cmd;

import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONObject;

import com.agilecontrol.b2b.cmd.ObjectGet;
import com.agilecontrol.b2b.cmd.Search;
import com.agilecontrol.b2b.schema.Table;
import com.agilecontrol.b2bweb.WebController;
import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.nea.util.Tools;
import com.agilecontrol.nea.util.Validator;
import com.agilecontrol.phone.Admin;
import com.agilecontrol.phone.CmdHandler;
import com.agilecontrol.phone.CmdResult;
import com.agilecontrol.phone.PhoneConfig;
import com.agilecontrol.phone.PhoneController;
import com.agilecontrol.phone.PhoneUtils;

/**
 h1. b_bro退货单

> {cmd:"b2b.bro.get",id}
> id -- number 退货单id

	添加额外的items[{imageurl,value,name,color,sz,qty,amount,remarks,enclosure}]
	imageurl -- string 商品图片
	value -- string 商品描述
	name -- string 商品尺码
	color -- 商品颜色
	sz -- 商品尺码
	qty -- 退货数量
	amount -- 退货金额
	remarks -- 备注
	enclosure -- 附件
 * @author wu.qiong
 *
 */
public class BroGet extends ObjectGet {
	
	/**
	 * redis完成缓存更新后的处理
	 * @param table
	 * @param retObj 正常处理完的，将返回客户端的对象，可以重构, 这里已经是装配完的对象
	 * @throws Exception
	 */
	protected void postAction(Table bfoTable, JSONObject retObj) throws Exception{
		
		vc.put("uid",usr.getId());
		vc.put("marketid", usr.getMarketId());
		vc.put("id", retObj.getInt("id"));

		//select m_product_id pdtid, B_PRMT_ID actid, sum(qty) qty, sum(price*qty) amt, max(modifieddate) mdate
		//from b_cart where xxx and user_id=$uid and is_order='Y' group by  m_product_id, B_PRMT_ID
		JSONArray items= PhoneController.getInstance().getDataArrayByADSQL("b2b_bro_ptds", vc, conn, true);
		
		retObj.put("items", items);
	}

}













