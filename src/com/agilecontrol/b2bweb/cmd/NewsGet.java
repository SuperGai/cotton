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
h1. 商品详情界面

h2. 场景

单品界面,获取商品详情

h2. 输入

> {cmd:"b2b.news.get",id }

*id* - int pdt.id

h2. 输出

> {id，title,content,doctype,date,author}

id - Number 新闻id
title - String 新闻标题
desc - String 新闻描述
doctype - String 新闻类型
date -- string 新闻创建时间
author -- string 新闻供稿人

 * @author wu.qiong
 *
 */
public class NewsGet extends ObjectGet {
	
	
	public CmdResult execute(JSONObject jo) throws Exception {
		String newsIdOrNo=jo.optString("id",null);
		if(newsIdOrNo==null) throw new NDSException("需要id参数");
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
		if(objectId==-1) throw new ObjectNotFoundException("新闻未找到(Not Found)");
		CmdResult res=new CmdResult(obj );
		return res;
		
	}
}
