package com.agilecontrol.portal.cmd;

import org.json.JSONArray;
import org.json.JSONObject;

import com.agilecontrol.nea.util.Validator;
import com.agilecontrol.phone.Admin;
import com.agilecontrol.phone.CmdHandler;
import com.agilecontrol.phone.CmdResult;
import com.agilecontrol.phone.PhoneController;

/**
输入


输出

{
	


}


 * 
 * @author stao
 *
 */
@Admin(mail="sun.tao@lifecycle.cn")
public class GetSelectParamsArray extends CmdHandler {
	@Override
	
	public CmdResult execute(JSONObject jo) throws Exception{
		JSONObject obj = new JSONObject();
		
		//男女
		String sexStr = PhoneController.getInstance().getValueFromADSQL("portal:getsex", conn);
		if(Validator.isNotNull(sexStr)){
			JSONArray rows = null;
			rows= PhoneController.getInstance().getDataArrayByADSQL("portal:getsex", vc, conn, true);
			if(rows != null){
				obj.put("sex", rows);
			}
		}
		
		//店铺管理方式
		String mngtype = PhoneController.getInstance().getValueFromADSQL("portal:getmngtype", conn);
		if(Validator.isNotNull(mngtype)){
			JSONArray rows = null;
			rows= PhoneController.getInstance().getDataArrayByADSQL("portal:getmngtype", vc, conn, true);
			if(rows != null){
				obj.put("mngtype", rows);
			}
		}
		
		//投资预算
		String budget = PhoneController.getInstance().getValueFromADSQL("portal:getbudget", conn);
		if(Validator.isNotNull(budget)){
			JSONArray rows = null;
			rows= PhoneController.getInstance().getDataArrayByADSQL("portal:getbudget", vc, conn, true);
			if(rows != null){
				obj.put("budget", rows);
			}
		}
		
		//创业类型
		String entretype = PhoneController.getInstance().getValueFromADSQL("portal:getentretype", conn);
		if(Validator.isNotNull(entretype)){
			JSONArray rows = null;
			rows= PhoneController.getInstance().getDataArrayByADSQL("portal:getentretype", vc, conn, true);
			if(rows != null){
				obj.put("entretype", rows);
			}
		}
				
		//所属行业
		String getindustry = PhoneController.getInstance().getValueFromADSQL("portal:getindustry", conn);
		if(Validator.isNotNull(getindustry)){
			JSONArray rows = null;
			rows= PhoneController.getInstance().getDataArrayByADSQL("portal:getindustry", vc, conn, true);
			if(rows != null){
				obj.put("industry", rows);
			}
		}
				
		//从业经验
		String experience = PhoneController.getInstance().getValueFromADSQL("portal:getexperience", conn);
		if(Validator.isNotNull(budget)){
			JSONArray rows = null;
			rows= PhoneController.getInstance().getDataArrayByADSQL("portal:getexperience", vc, conn, true);
			if(rows != null){
				obj.put("experience", rows);
			}
		}
				
		//投资预算
		String underway = PhoneController.getInstance().getValueFromADSQL("portal:getunderway", conn);
		if(Validator.isNotNull(underway)){
			JSONArray rows = null;
			rows= PhoneController.getInstance().getDataArrayByADSQL("portal:getunderway", vc, conn, true);
			if(rows != null){
				obj.put("underway", rows);
			}
		}
		return new CmdResult( obj);
	}
	
	/**
	 * Guest can execute this task, default to false
	 * @return
	 */
	public boolean allowGuest(){
		return true;
	}
	
}
