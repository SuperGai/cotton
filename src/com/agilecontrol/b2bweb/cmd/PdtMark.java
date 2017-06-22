package com.agilecontrol.b2bweb.cmd;

import org.json.JSONArray;
import org.json.JSONObject;

import com.agilecontrol.b2b.cmd.Search;
import com.agilecontrol.b2bweb.WebController;
import com.agilecontrol.phone.Admin;
import com.agilecontrol.phone.CmdHandler;
import com.agilecontrol.phone.CmdResult;
import com.agilecontrol.phone.PhoneController;

/**
 
h1. 获取商品评分

h2. 输入

> {cmd:"b2b.pdt.mark" , pdtid, actid, cnt}

*pdtid* - int 商品
*actid* - int 活动id， -1 表示不在活动中
*cnt* - int 显示最近cnt条评分，默认为0

h2. 输出

> {score, list}
*score* - double 平均分，0 表示无评分, 通过 ad_sql#pdt_mark 获取
*list* - [{userid, uname, time, score, comments, url}]， 通过pdt_mark_top获取
> *userid* - 用户id
> *uname* - string 用户名称(truename)
> *time* - string format: 2014-11-24 14:30:25
> *score* - int 0~5
> *comments* - string 评论文字
> *url* - string， 图片信息

 

 * @author yfzhu
 *
 */
@Admin(mail="yfzhu@lifecycle.cn")
public class PdtMark extends CmdHandler {
	
	/**
	 * 执行任务
	 * 
	 * @param jo
	 *            任务参数
	 * @return 返回的内容将全部对应到ValueHolder相关项
	 */
	public  CmdResult execute(JSONObject jo) throws Exception{
		int pdtId= this.getInt(jo, "pdtid");
		int actId= jo.optInt("actid",-1);
		vc.put("actid", actId);
		vc.put("pdtid", pdtId);
		vc.put("marketid", usr.getMarketId());
		vc.put("customerid",getCustomerIdByActOrMarket(actId,usr.getMarketId()) );
		vc.put("uid", usr.getId());
		
		JSONObject ret=new JSONObject();
		double score=0;
		JSONArray rows= PhoneController.getInstance().getDataArrayByADSQL("pdt_mark", vc, conn, false/*return list*/);
		if(rows.length()>0) score=rows.getDouble(0);
		ret.put("score", score);
		
		int cnt=jo.optInt("cnt");
		if(cnt>0){
			vc.put("cnt", cnt);
			rows=PhoneController.getInstance().getDataArrayByADSQL("pdt_mark_top", vc, conn, true/*return obj*/);
			ret.put("list", rows);
		}
		
		return new CmdResult(ret);
	}
}
