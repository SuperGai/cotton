package com.agilecontrol.b2bweb.cmd;

import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import com.agilecontrol.b2b.cmd.ObjectGet;
import com.agilecontrol.b2b.cmd.Search;
import com.agilecontrol.b2b.schema.Table;
import com.agilecontrol.b2bweb.WebController;
import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.nea.util.ObjectNotFoundException;
import com.agilecontrol.nea.util.Tools;
import com.agilecontrol.nea.util.Validator;
import com.agilecontrol.phone.Admin;
import com.agilecontrol.phone.CmdHandler;
import com.agilecontrol.phone.CmdResult;
import com.agilecontrol.phone.PhoneController;
import com.agilecontrol.phone.PhoneUtils;

/**
h1. 商品详情界面的可选活动

h2. 场景

单品界面,获取商品详情

h2. 输入

> {cmd:"b2b.pdt.acts",id }

*id* - int pdt.id

h2. 输出

> [{id,pprice,name,enddate} ]

 * @author yfzhu
 *
 */
@Admin(mail="yfzhu@lifecycle.cn")
public class PdtActs extends ObjectGet {
	
	
	public CmdResult execute(JSONObject jo) throws Exception {
		String pdtIdOrName=jo.optString("id",null);
		if(pdtIdOrName==null) throw new NDSException("需要id参数");
		long objectId=-1;
		JSONObject obj =null;
		if(StringUtils.isNumeric(pdtIdOrName)){
			objectId=Tools.getInt(pdtIdOrName, -1);
			try{
				obj = fetchObject(objectId, "pdt",false);
			}catch(Throwable tx){
				logger.debug("Fail to find pdt.id="+ objectId+":"+ tx.getMessage());
				objectId=-1;
			}
		}
		if(objectId==-1){
			//有可能是货号，即: pdt.name
			objectId=engine.doQueryInt("select id from m_product where name=?", new Object[]{pdtIdOrName}, conn);
			obj = fetchObject(objectId, "pdt",false);
		}
		if(objectId==-1) throw new ObjectNotFoundException("商品未找到(Not Found)");
		
		
		//计算pprice
		vc.put("pdtid", objectId);
		vc.put("marketid", usr.getMarketId());
		vc.put("uid", usr.getId());
		//select 活动名称 name, 价格 pprice, 获取截止时间 enddate, 活动id
		JSONArray rows= PhoneController.getInstance().getDataArrayByADSQL("pdt_act_list", vc, conn, true/*return obj*/);
		
		CmdResult res=new CmdResult(rows );
		return res;
		
	}

}













