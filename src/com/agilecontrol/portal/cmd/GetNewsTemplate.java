package com.agilecontrol.portal.cmd;

import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;

import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.nea.util.ObjectNotFoundException;
import com.agilecontrol.nea.util.Tools;
import com.agilecontrol.phone.Admin;
import com.agilecontrol.phone.CmdHandler;
import com.agilecontrol.phone.CmdResult;
/**
h2. ����

> {cmd:"b2b.news.get",id }

*id* - int pdt.id

h2. ���

> {id��title,content,doctype,date,author}

id - Number ����id
title - String ���ű���
desc - String ��������
doctype - String ��������
date -- string ���Ŵ���ʱ��
author -- string ���Ź�����

 * 
 * @author sun.tao
 *
 */
@Admin(mail="sun.tao@lifecycle.cn")
public class GetNewsTemplate extends CmdHandler {

	public CmdResult execute(JSONObject jo) throws Exception{
		String newsIdOrNo=jo.optString("id",null);
		if(newsIdOrNo==null) throw new NDSException("��Ҫid����");
		long objectId=-1;
		JSONObject obj =null;
		if(StringUtils.isNumeric(newsIdOrNo)){
			objectId=Tools.getInt(newsIdOrNo, -1);
			try{
				obj = fetchObject(objectId, "news",false);
			}catch(Throwable tx){
				logger.debug("Fail to find news.id="+ objectId+":"+ tx.getMessage());
				objectId=-1;
			}
		}
		if(objectId==-1){
			// news.no (ak)
			objectId=engine.doQueryInt("select id from u_news where no=?", new Object[]{newsIdOrNo}, conn);
			try{
				obj = fetchObject(objectId, "news",false);
			}catch(Throwable tx){
				logger.debug("Fail to find news.no="+ objectId+":"+ tx.getMessage());
				objectId=-1;
			}
		}
		if(objectId==-1) throw new ObjectNotFoundException("����δ�ҵ�(Not Found)");
		CmdResult res=new CmdResult(obj );
		return res;
	}
	
	/**
	 * Guest can execute this task, default to false
	 * @return
	 */
	public boolean allowGuest(){
		return true;
	}
	
}
