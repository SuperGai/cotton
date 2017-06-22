package com.agilecontrol.phone;

import java.io.File;
import java.io.FileFilter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hsqldb.Row;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;




import argparser.ArgParser;
import argparser.BooleanHolder;
import argparser.StringHolder;

import com.agilecontrol.nea.core.control.util.*;
import com.agilecontrol.nea.core.control.event.*;
import com.agilecontrol.nea.core.control.web.*;
import com.agilecontrol.nea.core.query.QueryEngine;
import com.agilecontrol.nea.core.report.ReportUtils;
import com.agilecontrol.nea.core.schema.*;
import com.agilecontrol.nea.core.util.*;
import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.nea.util.Tools;
import com.agilecontrol.nea.util.Validator;

/**
 * b2b –c 命令  
 * 用户必须具有相应权限
 * @author yfzhu
 *
 */
@Admin(mail="yfzhu@lifecycle.cn")
public class B2BShell implements com.agilecontrol.nea.core.web.shell.ShellCmd{
	
	private static  Logger logger=LoggerFactory.getLogger(B2BShell.class);
	
	private final static String HELP=
	"b2b -c [cmd] B2B 管理命令集合, -c 为子命令项，必须给出:<br><p dir='ltr' style='margin-left: 10px; margin-right: 0px'><ul> "+
	"<li><b>version</b> 查看版本  </li>"+
	"<li><b>clear [key]</b> 清除redis数据缓存, key为空则依据ad_sql的redis_clear_keys来删除 </li>"+
	"</ul></p><br>";
	
	public String getAlias(){
		return "b2b,bb";
	}
	/**
	 * Create event context accroding to shell evn
	 * @param envObj
	 * @return
	 * @throws JSONException 
	 */
	protected EventContext createEventContext(JSONObject envObj) throws JSONException{
		EventContext context= new DefaultEventContext((HttpServletRequest)envObj.get("request"), 
				(HttpServletResponse)envObj.get("response"));
		return context;
	}
	public String getHelp(java.util.Locale locale){
		return HELP;
	}
	
	
	public JSONObject execute(String args, JSONObject envObj) throws Exception{
	    
		//user must have ad_column modify permission to do this
	    UserWebImpl userWeb=(UserWebImpl)envObj.get("userweb");
		
		String[] arg=args.split(" "); 
		
	    StringHolder cmdHolder = new StringHolder();
	    //StringHolder fileHolder = new StringHolder();
	    
	    ArgParser parser = new ArgParser("");
	    parser.addOption ("-c %s #command name", cmdHolder); 
	    // match the arguments ...
	    String[] unmatched =
	        parser.matchAllArgs (arg, 0, 0);
		
		String tailingArgs=null;
	    if(unmatched!=null){
	    	tailingArgs= unmatched[0];
	    	for(int i=1;i<unmatched.length;i++){
	    		tailingArgs+=" "+unmatched[i];
	    	}
	    }

	   if(Validator.isNotNull(cmdHolder.value)){
	    	
	    	DefaultWebEvent dwe=createEvent(cmdHolder.value,tailingArgs, createEventContext(envObj));
	    	return handleEvent(dwe);
    		
	    }else{
	    	JSONObject jo=new JSONObject();
	    	jo.put("code",-1);
	    	jo.put("message", HELP);
	    	return jo;
	    }
	    
		
	}
	private DefaultWebEvent createEvent(String cmd, String args, EventContext evtContext ) throws Exception{
		DefaultWebEvent event=new DefaultWebEvent("CommandEvent",evtContext);
		event.setParameter("command","com.agilecontrol.phone.B2BCmd");
		JSONObject params=new JSONObject();
		params.put("cmd",cmd.trim());
		if(Validator.isNotNull(args))params.put("args", args);
		event.put("jsonObject", params);
		return event;
	}
	
	
	
	/**
	 * 
	 * @param fairId
	 * @return
	 * @throws Exception
	 */
	private JSONObject handleEvent(DefaultWebEvent event) throws Exception{
		
        ClientControllerWebImpl controller=(ClientControllerWebImpl)WebUtils.
        	getServletContextManager().getActor(com.agilecontrol.nea.core.util.WebKeys.WEB_CONTROLLER);
        ValueHolder vh=controller.handleEvent(event);
        
        JSONObject ret=(JSONObject)vh.get("restResult");
        logger.debug("result :"+ ret);
        
        JSONObject rr=(JSONObject)ret.get("result");
		
		JSONObject jo=new JSONObject(); 
		jo.put("code",0);
		jo.put("message", rr.opt("message") );
	   	return jo;
	   	
	}
	
	private String calcTime(long startTime){
		int seconds=(int) ((System.currentTimeMillis()- startTime)/1000.0);
		String time;
    	if(seconds>3600) time=  (int)(seconds/3600)+"h"+  (int)((seconds%3600)/60)+"m";
    	if(seconds>60) time= (int)(seconds/60)+"m" + (seconds%60)+"s";
    	else time= seconds+"s";
    	return "("+time+") ";
	}
	
	
}
