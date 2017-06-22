package com.agilecontrol.b2bweb.cmd;

import org.json.JSONArray;
import org.json.JSONObject;

import com.agilecontrol.b2b.cmd.ObjectGet;
import com.agilecontrol.b2b.cmd.Search;
import com.agilecontrol.b2b.schema.Table;
import com.agilecontrol.b2bweb.WebController;
import com.agilecontrol.nea.util.Validator;
import com.agilecontrol.phone.Admin;
import com.agilecontrol.phone.CmdHandler;
import com.agilecontrol.phone.CmdResult;
import com.agilecontrol.phone.PhoneController;
import com.agilecontrol.phone.PhoneUtils;

/**

 输入
{cmd:"b2b.act.get", id}

id - 活动id, int b_prmt.id

输出
{id, name, begindate, enddate, pic, ptype, pdts:[{pdt}]}

pdt 对象定义需要独立的基于商品的搜索语句

{pdtid,note,no,mainpic,price,tags}

pdtid - int pdtid
note - string 商品备注 
no - string 商品编号
mainPic -string 商品图片
price - double 显示的价格
tags - jsonarray of string 商品标签，如: ["爆款","范冰冰同款"]

 * @author yfzhu
 *
 */
@Admin(mail="yfzhu@lifecycle.cn")
public class ActGet extends ObjectGet {
	
//	public CmdResult execute(JSONObject jo) throws Exception {
//		//需要实现为基于redis cache，将act_pdts的内容缓存下来
//		int actId= this.getInt(jo, "id");
//		vc.put("actid", actId);
//		vc.put("marketid", usr.getMarketId());
//		JSONArray pdts=null;
//		//pdt ids 来自所有的明细表
//		JSONArray rows= PhoneController.getInstance().getDataArrayByADSQL("act_pdts", vc, conn, false);
//		
//		Table table=manager.getTable("pdt");
//		
//		pdts=PhoneUtils.getRedisObjectArray("pdt", rows, table.getColumnsInListView(), true, vc, conn,jedis);
//		for(int i=0;i<pdts.length();i++){
//			JSONObject pdt=pdts.getJSONObject(i);
//			//更新语言， 以及相关市场的价格，图片等相关信息
//			WebController.getInstance().replacePdtValues(pdt, usr.getLangId(), usr.getMarketId(), vc, jedis, conn);
//			
//		}				
//		CmdResult res=new CmdResult(pdts );
//		return res;
//		
//	}

}
