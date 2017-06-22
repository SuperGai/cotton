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
h1. ��Ʒ�������Ŀ�ѡ�

h2. ����

��Ʒ����,��ȡ��Ʒ����

h2. ����

> {cmd:"b2b.pdt.acts",id }

*id* - int pdt.id

h2. ���

> [{id,pprice,name,enddate} ]

 * @author yfzhu
 *
 */
@Admin(mail="yfzhu@lifecycle.cn")
public class PdtActs extends ObjectGet {
	
	
	public CmdResult execute(JSONObject jo) throws Exception {
		String pdtIdOrName=jo.optString("id",null);
		if(pdtIdOrName==null) throw new NDSException("��Ҫid����");
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
			//�п����ǻ��ţ���: pdt.name
			objectId=engine.doQueryInt("select id from m_product where name=?", new Object[]{pdtIdOrName}, conn);
			obj = fetchObject(objectId, "pdt",false);
		}
		if(objectId==-1) throw new ObjectNotFoundException("��Ʒδ�ҵ�(Not Found)");
		
		
		//����pprice
		vc.put("pdtid", objectId);
		vc.put("marketid", usr.getMarketId());
		vc.put("uid", usr.getId());
		//select ����� name, �۸� pprice, ��ȡ��ֹʱ�� enddate, �id
		JSONArray rows= PhoneController.getInstance().getDataArrayByADSQL("pdt_act_list", vc, conn, true/*return obj*/);
		
		CmdResult res=new CmdResult(rows );
		return res;
		
	}

}













