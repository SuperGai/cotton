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

h1. ���������Ʒ�Ͷ���

h2. ����

������룬��ʼ���ﳵ������Ʒ�Ͷ���. 

h2. ����

> {cmd:"b2b.cart.import", file}

h2. ���


h2. ����˵��
 
���������� ad_sql#cart_impxls, ��ʽ:
<pre>
{
columns:{ pdt:{col:1},skuqty:{col:5},err:{col:14} },
startrow: 1,
end_proc: proc1,
start_proc: proc2
}
</pre>
*columns* - ָ����Ҫ�ֶ���Excelģ���ļ��е�λ�ã�key: pdt|skuqty|err, value: {col,  isnull}
  col: int, ��ʾλ�ô�0��ʼ
  default: ȱʡֵ�������ñ�ʾû��
  isnull:  �Ƿ�����Ϊ�գ� Ĭ�� false
*startrow* - ��ʼ�У���0��ʼ������Ĭ��1
*start_proc* - ������ã���ʾ�ڿ�ʼ����ǰ��ִ�еĴ洢���̣�������proc(usr_id in number)
*end_proc* - ������ã���ʾ�ڵ���ȫ���������ȷ��ִ�еĴ洢���̣�������proc(usr_id in number)

���д����κ�һ�г����ᵼ����������ʧ�ܡ�����Ȼ�����������ȫ������


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
		data.put("message", message);//����ʾ���ͻ���
		CmdResult ret= new CmdResult(0,"@complete@" ,data/*���/nea/core/msgjson.jsp */);
		return ret;

	}
	/**
	 * 361 ģʽ��֧��ɢ��
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
			message= "@error-found-when-import@:"+ tx.getMessage() ; //"��������"+ tx.getMessage()+", �����ļ��������ϴ�";
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
