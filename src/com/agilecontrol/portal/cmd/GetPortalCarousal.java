package com.agilecontrol.portal.cmd;

import org.json.JSONArray;
import org.json.JSONObject;

import com.agilecontrol.nea.core.query.QueryEngine;
import com.agilecontrol.nea.util.Validator;
import com.agilecontrol.phone.Admin;
import com.agilecontrol.phone.CmdHandler;
import com.agilecontrol.phone.CmdResult;
import com.agilecontrol.phone.PhoneController;

/**
输入

输出

[
    {
    	title:"",
    	img:"",
    },
    {
    	title:"",
    	img:"",
    },
]

（1）获取 carousal 配置项(ad_sql获取默认菜单配置 jsonArray 数据类型 )； ad_sql#portal:carousal
（2）菜单的 roles 数组为空时，表示默认权限，即所有人可见
 （3）若对roles添加 权限，则当前用户的权限必须包含在 roles 中，否则不可见
（4）返回给客户端
 * 
 * @author stao
 *
 */
@Admin(mail="sun.tao@lifecycle.cn")
public class GetPortalCarousal extends CmdHandler {
	
	@Override
	public CmdResult execute(JSONObject jo) throws Exception{
		String adSql = "portal:carousal";
		JSONArray carouArray = (JSONArray) PhoneController.getInstance().getValueFromADSQLAsJSON( adSql, conn);
		return new CmdResult( carouArray);
	}
	
	
	/**
	 * Guest can execute this task, default to false
	 * @return
	 */
	public boolean allowGuest(){
		return true;
	}
}
