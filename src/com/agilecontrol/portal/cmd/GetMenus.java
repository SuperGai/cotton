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
����

request ����  id:�û�id ��ѡ����  (δ��¼����²���Ҫ����id)

���

{
    "menus": [
              {
                "text": "������Ӫ",
                "roles": ["guest","admin","supplier","purchaser"],   //(����ж�Ȩ������)
                "is_accessble":true/false,                           //(ǰ���Ƿ���õ��ж�����,��Ҫ�ں�˽��д����ٷ��ظ�ǰ��)
                "items": [
                           {
                             "text": "�ŵ궩��",
                             "roles": ["admin","purchaser"],
                             "type": "url",
                             "value": "/b2b/",
                             "is_accessble":true/false,             //(ǰ���Ƿ���õ��ж�����,��Ҫ�ں�˽��д����ٷ��ظ�ǰ��)
                           }]
              },
              {
                 "text": "���¼��˵���",
                 "roles": "guest",
                 "type": "url",
                 "value": "http://www.google.com/",
                 "is_accessble":true/false,                        //(ǰ���Ƿ���õ��ж�����,��Ҫ�ں�˽��д����ٷ��ظ�ǰ��)

               }],

    "inaccessible_items": "hidden|disabled"
}

��1����ȡ�˵�������(ad_sql��ȡĬ�ϲ˵����� json�������� )�� ad_sql#portal:menubar
��2���˵��� roles ����Ϊ��ʱ����ʾĬ��Ȩ�ޣ��������˿ɼ�
 ��3������roles��� Ȩ�ޣ���ǰ�û���Ȩ�ޱ�������� roles �У����򲻿ɼ�
��4�����ظ��ͻ���
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
					//�Ե���menu��������  ��is_accessble��
					JSONObject menu = menus.getJSONObject( i);
					JSONArray menuRoles = menu.optJSONArray( "roles");
					//��δ���û������õ�  ��roles�� ���� Ϊ������ʱ��Ĭ��Ϊ �����˶��ɼ�
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
				
				//����idֵ�Ƿ���������Χ���ж��Ƿ�����û������֤
				if( 0 > id) {
					for( int i = 0; i < menus.length(); i++){
						//�Ե���menu��������  ��is_accessble��
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
							
							//��һ���˵���������
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
							
							//�Զ����˵���������
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
