package com.agilecontrol.b2bweb.cmd;

import java.util.Locale;

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
import com.agilecontrol.phone.LanguageManager;
import com.agilecontrol.phone.PhoneController;
import com.agilecontrol.phone.PhoneUtils;

/**
h1. ��Ʒ�������

h2. ����

��Ʒ����,��ȡ��Ʒ����

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

 * @author wu.qiong
 *
 */
public class NewsGet extends ObjectGet {
	
	
	public CmdResult execute(JSONObject jo) throws Exception {
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
}
