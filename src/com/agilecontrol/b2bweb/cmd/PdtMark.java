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
 
h1. ��ȡ��Ʒ����

h2. ����

> {cmd:"b2b.pdt.mark" , pdtid, actid, cnt}

*pdtid* - int ��Ʒ
*actid* - int �id�� -1 ��ʾ���ڻ��
*cnt* - int ��ʾ���cnt�����֣�Ĭ��Ϊ0

h2. ���

> {score, list}
*score* - double ƽ���֣�0 ��ʾ������, ͨ�� ad_sql#pdt_mark ��ȡ
*list* - [{userid, uname, time, score, comments, url}]�� ͨ��pdt_mark_top��ȡ
> *userid* - �û�id
> *uname* - string �û�����(truename)
> *time* - string format: 2014-11-24 14:30:25
> *score* - int 0~5
> *comments* - string ��������
> *url* - string�� ͼƬ��Ϣ

 

 * @author yfzhu
 *
 */
@Admin(mail="yfzhu@lifecycle.cn")
public class PdtMark extends CmdHandler {
	
	/**
	 * ִ������
	 * 
	 * @param jo
	 *            �������
	 * @return ���ص����ݽ�ȫ����Ӧ��ValueHolder�����
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
