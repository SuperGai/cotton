package com.agilecontrol.b2bweb.cmd;

import java.io.File;

import org.apache.velocity.VelocityContext;
import org.json.JSONArray;
import org.json.JSONObject;

import com.agilecontrol.nea.core.control.web.SecurityManagerWebImpl;
import com.agilecontrol.nea.core.control.web.ServletContextManager;
import com.agilecontrol.nea.core.control.web.SessionInfo;
import com.agilecontrol.nea.core.control.web.UserWebImpl;
import com.agilecontrol.nea.core.control.web.WebUtils;
import com.agilecontrol.nea.core.query.QueryEngine;
import com.agilecontrol.nea.core.util.MessagesHolder;
import com.agilecontrol.nea.core.util.WebKeys;
import com.agilecontrol.nea.core.velocity.VelocityUtils;
import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.nea.util.Validator;
import com.agilecontrol.phone.CmdHandler;
import com.agilecontrol.phone.CmdResult;
import com.agilecontrol.phone.PhoneController;

/**
 * 

h1. 处理导入的商品和订量

h2. 场景

点击导入，开始购物车导入商品和订量. 

h2. 输入

> {cmd:"b2b.cart.import", file}

h2. 输出


h2. 处理说明
 
参数配置在 ad_sql#cart_impxls, 格式:
<pre>
{
columns:{ pdt:{col:1},skuqty:{col:5},err:{col:14} },
startrow: 1,
end_proc: proc1,
start_proc: proc2
}
</pre>
*columns* - 指定主要字段在Excel模板文件中的位置，key: pdt|skuqty|err, value: {col,  isnull}
  col: int, 表示位置从0开始
  default: 缺省值，不设置表示没有
  isnull:  是否允许为空， 默认 false
*startrow* - 起始行，从0开始计数，默认1
*start_proc* - 如果设置，表示在开始导入前将执行的存储过程，参数：proc(usr_id in number)
*end_proc* - 如果设置，表示在导入全部完成且正确后将执行的存储过程，参数：proc(usr_id in number)

逐行处理，任何一行出错都会导致整个事务失败。但仍然会继续处理完全部数据


 * @author yfzhu
 *
 */
public class ImportExcelCmd extends CmdHandler {

	@Override
	public CmdResult execute(JSONObject jo) throws Exception {
		
		javax.servlet.http.HttpServletRequest request=(javax.servlet.http.HttpServletRequest) event.getContext().getHttpServletRequest();
		if(request==null) throw new NDSException("@specified-page-alert@");
		
//		String jsessionId=jo.optString("JSESSIONID");
//    	if(jsessionId!=null ){
//    	    ServletContextManager manager= WebUtils.getServletContextManager();
//    	    SecurityManagerWebImpl se=(SecurityManagerWebImpl)manager.getActor(com.agilecontrol.nea.core.util.WebKeys.SECURITY_MANAGER);
//    		SessionInfo si= se.getSession(jsessionId);
//    		if(si!=null){
//    			userWeb= si.getUserWebImpl();
//    			
//    		}else{
//    			userWeb= ((UserWebImpl)WebUtils.getSessionContextManager(request.getSession(true)).getActor(WebKeys.USER));
//    		}
//    		
//    	}else{
//    		userWeb= ((UserWebImpl)WebUtils.getSessionContextManager(request.getSession(true)).getActor(WebKeys.USER));        	
//    	}		
		
		String file= jo.optString("file");
		File f=null;
		if(Validator.isNotNull(file)){
			f= new File(file);
		}
		if(f==null || !f.exists()) throw new NDSException("@file-not-found@");
		
		String message=importFile(f);
		message=MessagesHolder.getInstance().translateMessage(message,locale);
		JSONObject data=new JSONObject();
		data.put("message", message);//将显示到客户端
		CmdResult ret= new CmdResult(0,"@complete@" ,data/*针对/nea/core/msgjson.jsp */);
		return ret;

	}
	/**
	 * 361 模式，支持散码
	 * @return message
	 * @throws Exception
	 */
	private String importFile(File xlsFile) throws Exception{
		ImportExcelWorker ie= new ImportExcelWorker();
		JSONObject config=(JSONObject)PhoneController.getInstance().getValueFromADSQLAsJSON("cart_impxls",conn);
		ie.init(config, conn, this.usr, this.jedis);

		conn.setAutoCommit(false);
		String message=null;
		boolean error=false;
		try{
			error=ie.handle(xlsFile.getAbsolutePath());
			if(error) {
				message=  ie.getErrorsHTML();
			}else 
				message= "@import-successful@(@in-all@"+ ie.getTotalRows()+"@success-datas@)";
		}catch(Throwable tx){
			logger.error("Fail in processing file:"+ xlsFile.getAbsolutePath(), tx);
			error=true;
			message= "@error-found-when-import@:"+ tx.getMessage() ; //"遇到错误："+ tx.getMessage()+", 请检查文件，重新上传";
		}finally{
			if(error){
				conn.rollback();
			}else{
				conn.commit();
			}
		}
		return message;
	}
	
	
//	/**
//	 * Guest can execute this task, default to false
//	 * @return
//	 */
//	public boolean allowGuest(){
//		return true;
//	}
}
