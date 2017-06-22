/*
 * Agile Control Technologies Ltd,. CO.
 * http://www.agileControl.com
 */
package com.agilecontrol.b2bweb.binhandler;

import javax.servlet.*;
import javax.servlet.http.*;

import org.apache.commons.lang.SystemUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.agilecontrol.nea.core.query.ColumnLink;
import com.agilecontrol.nea.core.query.Expression;
import com.agilecontrol.nea.core.query.QueryEngine;
import com.agilecontrol.nea.core.query.QueryException;
import com.agilecontrol.nea.core.query.QueryRequestImpl;
import com.agilecontrol.nea.core.query.QueryResult;
import com.agilecontrol.nea.core.query.QuerySession;
import com.agilecontrol.nea.core.schema.*;
import com.agilecontrol.nea.core.schema.Filter;
import com.agilecontrol.nea.core.security.Directory;
import com.agilecontrol.nea.core.security.SecurityUtils;
import com.agilecontrol.nea.core.util.ConfigValues;
import com.agilecontrol.nea.core.util.ParamUtils;
import com.agilecontrol.nea.core.util.WebKeys;
import com.agilecontrol.nea.util.*;

import java.net.URLEncoder;
import java.sql.Connection;
import java.util.*;
import java.io.*;

import com.agilecontrol.nea.core.control.util.FileDownload;
import com.agilecontrol.nea.core.control.web.*;
import com.agilecontrol.nea.core.control.web.binhandler.BinaryHandler;

/**
 * 参照com.agilecontrol.nea.core.control.web.binhandler.Attach，
 * 创建com.agilecontrol.b2bweb.binhandler.Img，
 * 使之可以通过/servlets/binserv/D/Img/pc:loginbg返回图片，其中pc:loginbg是b_imgset表的name字段。
 * 
 * @author sun.yifan
 */

public class Img implements BinaryHandler{
	private static Logger logger= LoggerFactory.getLogger(Img.class);
	public void init(ServletContext context){}
		
	public void process(HttpServletRequest request,HttpServletResponse  response)  throws Exception{
		try {
			String path = request.getRequestURI();
			String imgName = path.substring(path.lastIndexOf("/")+1);
			JSONObject imgObj = QueryEngine.getInstance().doQueryObject("select id,imgurl from b_imgset where name=?",new Object[]{imgName});
			if(imgObj==null){
				logger.error(imgName+" not found of b_imgset");
			}
			String imgurl = imgObj.getString("imgurl");
			request.getRequestDispatcher(imgurl).forward(request, response);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
 
}
