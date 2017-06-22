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

request 参数  id:用户id 可选参数  (未登录情况下不需要不给id)

输出

{
    "menus": [
              {
                "text": "店铺运营",
                "roles": ["guest","admin","supplier","purchaser"],   //(后端判断权限依据)
                "is_accessble":true/false,                           //(前端是否可用的判断依据,需要在后端进行处理，再返回给前端)
                "items": [
                           {
                             "text": "门店订货",
                             "roles": ["admin","purchaser"],
                             "type": "url",
                             "value": "/b2b/",
                             "is_accessble":true/false,             //(前端是否可用的判断依据,需要在后端进行处理，再返回给前端)
                           }]
              },
              {
                 "text": "无下级菜单项",
                 "roles": "guest",
                 "type": "url",
                 "value": "http://www.google.com/",
                 "is_accessble":true/false,                        //(前端是否可用的判断依据,需要在后端进行处理，再返回给前端)

               }],

    "inaccessible_items": "hidden|disabled"
}

（1）获取菜单配置项(ad_sql获取默认菜单配置 json数据类型 )； ad_sql#portal:menubar
（2）菜单的 roles 数组为空时，表示默认权限，即所有人可见
 （3）若对roles添加 权限，则当前用户的权限必须包含在 roles 中，否则不可见
（4）返回给客户端
 * 
 * @author stao
 *
 */
@Admin(mail="sun.tao@lifecycle.cn")
public class GetMenus extends CmdHandler {
	
	@Override
	public CmdResult execute(JSONObject jo) throws Exception{
		long id = jo.optLong( "id", -100);
		String adSql = "portal:menubar";
		JSONArray roles = null;
		
		if( -100 != id) {
			roles = engine.doQueryObjectArray(
			        "select g.name from groupuser gu, groups g where gu.groupid = g.id and userid=?",
			        new Object[]{ id }, conn);
		}
		
		JSONObject menusObj = (JSONObject) PhoneController.getInstance().getValueFromADSQLAsJSON( adSql, conn);
		if( null != menusObj) {
			JSONArray menus = menusObj.optJSONArray( "menus");
			if( null != menus && 0 != menus.length()) {
				for( int i = 0; i < menus.length(); i++){
					//对单个menu设置属性  “is_accessble”
					JSONObject menu = menus.getJSONObject( i);
					JSONArray menuRoles = menu.optJSONArray( "roles");
					//当未配置或者配置的  ‘roles’ 属性 为空数组时，默认为 所有人都可见
					if( null == menuRoles || 0 == menuRoles.length()) {
						menu.put( "is_accessble", true);
					}
					else{
						menu.put( "is_accessble", false);
					}
					
					JSONArray items = menu.optJSONArray( "items");
					if( null != items && 0 != items.length()) {
						for( int j = 0; j < items.length(); j++){
							JSONObject item = items.optJSONObject( j);
							JSONArray itemRoles = item.optJSONArray( "roles");
							if( null == itemRoles || 0 == itemRoles.length()) {
								item.put( "is_accessble", true);
							}
							else{
								item.put( "is_accessble", false);
							}
						}
					}
				}
				
				//根据id值是否在正常范围来判断是否进行用户身份验证
				if( 0 > id) {
					for( int i = 0; i < menus.length(); i++){
						//对单个menu设置属性  “is_accessble”
						JSONObject menu = menus.getJSONObject( i);
						JSONArray menuRoles = menu.optJSONArray( "roles");
						
						if( null != menuRoles && 0 != menuRoles.length()) {
							for( int j = 0; j < menuRoles.length(); j++){
								String menuroleStr = menuRoles.optString( j);
								if( "guest".equalsIgnoreCase( menuroleStr)) {
									menu.put( "is_accessble", true);
									break;
								}
							}
						}
						
						JSONArray items = menu.optJSONArray( "items");
						if( null != items && 0 != items.length()) {
							for( int j = 0; j < items.length(); j++){
								JSONObject item = items.optJSONObject( j);
								JSONArray itemRoles = item.optJSONArray( "roles");
								if( null != itemRoles && 0 != itemRoles.length()) {
									for( int k = 0; k < itemRoles.length(); k++){
										String itemroleStr = itemRoles.optString( k);
										if( "guest".equalsIgnoreCase( itemroleStr)) {
											item.put( "is_accessble", true);
											break;
										}
									}
									
								}
							}
						}
					}
				}
				if( 0 <= id) {
					if( null != roles && 0 != roles.length()) {
						for( int i = 0; i < menus.length(); i++){
							JSONObject menu = menus.getJSONObject( i);
							
							//对一级菜单进行设置
							JSONArray menuRoles = menu.optJSONArray( "roles");
							if( null != menuRoles && 0 != menuRoles.length()) {
								for( int j = 0; j < menuRoles.length(); j++){
									for( int k = 0; k < roles.length(); k++){
										if( menuRoles.getString( j)
										        .equals( roles.getJSONObject( k).optString( "name"))) {
											menu.put( "is_accessble", true);
											j = menuRoles.length();
											break;
										}
									}
								}
							}
							
							//对二级菜单进行设置
							JSONArray items = menu.optJSONArray( "items");
							if( null != items && 0 != items.length()) {
								for( int j = 0; j < items.length(); j++){
									JSONObject item = items.optJSONObject( j);
									
									JSONArray itemRoles = item.optJSONArray( "roles");
									if( null != itemRoles && 0 != itemRoles.length()) {
										for( int m = 0; m < itemRoles.length(); m++){
											for( int k = 0; k < roles.length(); k++){
												if( itemRoles.getString( m)
												        .equals( roles.getJSONObject( k).optString( "name"))) {
													item.put( "is_accessble", true);
													m = itemRoles.length();
													break;
												}
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}
		
		return new CmdResult( menusObj);
	}
	
	/**
	 * Guest can execute this task, default to false
	 * @return
	 */
	public boolean allowGuest(){
		return true;
	}
}
