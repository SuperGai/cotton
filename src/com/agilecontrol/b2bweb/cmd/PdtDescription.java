package com.agilecontrol.b2bweb.cmd;

import org.apache.commons.lang.Validate;
import org.json.JSONArray;
import org.json.JSONObject;

import com.agilecontrol.nea.core.control.event.NDSEventException;
import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.nea.util.Validator;
import com.agilecontrol.phone.CmdHandler;
import com.agilecontrol.phone.CmdResult;
import com.agilecontrol.phone.PhoneController;

/**
 * 场景：单品页面商品详细属性展示
 * 
 * 输入：
 * pdtid: 商品id
 * 
 * 输出：
 * 
 [
 	{
 		"title":"@m_dim1_id@","value":"棉购"
 	},
 	{
 		"title":"@m_dim1_id@","value":"棉购"
 	},
 	{
 		"title":"@m_dim1_id@","value":"棉购"
 	}
 ]
 * 
 * @author sun.yifan
 *
 */
public class PdtDescription extends CmdHandler {

	@Override
	public CmdResult execute(JSONObject jo) throws Exception {
		long pdtid = jo.optLong("pdtid",-1);
		if(pdtid==-1){
			throw new NDSException("pdtid not found");
		}
		String pdtsql = PhoneController.getInstance().getValueFromADSQL("b2b:pdt:desc",conn);
		if(Validator.isNull(pdtsql)){
			throw new NDSException("ad_sql#b2b:pdt:desc not found");
		} 
		JSONArray pdtdesc = engine.doQueryObjectArray(pdtsql, new Object[]{pdtid,usr.getLangId(),usr.getLangId()},conn);
		if(pdtdesc==null){
			pdtdesc = new JSONArray();
		}
		return new CmdResult(pdtdesc);
	}

}
