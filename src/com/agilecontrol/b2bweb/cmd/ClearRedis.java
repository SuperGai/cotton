package com.agilecontrol.b2bweb.cmd;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONArray;
import org.json.JSONObject;

import com.agilecontrol.b2b.cmd.ObjectGet;
import com.agilecontrol.b2b.cmd.Search;
import com.agilecontrol.b2bweb.DimDefTranslator;
import com.agilecontrol.b2bweb.DimTranslator;
import com.agilecontrol.b2bweb.WebController;
import com.agilecontrol.nea.core.control.web.SessionContextManager;
import com.agilecontrol.nea.core.control.web.UserWebImpl;
import com.agilecontrol.nea.core.control.web.WebUtils;
import com.agilecontrol.nea.core.db.oracle.QDate;
import com.agilecontrol.nea.core.schema.TableManager;
import com.agilecontrol.nea.core.security.Directory;
import com.agilecontrol.nea.core.security.SecurityUtils;
import com.agilecontrol.nea.core.security.User;
import com.agilecontrol.nea.core.security.UserManager;
import com.agilecontrol.nea.core.util.ConfigValues;
import com.agilecontrol.nea.core.util.WebKeys;
import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.nea.util.Tools;
import com.agilecontrol.nea.util.Validator;
import com.agilecontrol.phone.Admin;
import com.agilecontrol.phone.CmdHandler;
import com.agilecontrol.phone.CmdResult;
import com.agilecontrol.phone.LanguageManager;
import com.agilecontrol.phone.MarketManager;
import com.agilecontrol.phone.PhoneController;
import com.agilecontrol.phone.PhoneUtils;

/**

h1. Clear ���Redis����

h2. web����

�ڵ�½������½�web�����У�֧�����¹ؼ�����:

> bb -c clear [key]

key���Ժ���ͨ������磺act:* ��ʾ���еĻ��b2b:* ��ʾ���еķ�����������:

> bb -c clear pdt:*

������ṩkey����������ȡad_sql#clear_redis_keys �����ã����з��õ���jsonarray of string��
 ÿ��string ���Ǻ�ͨ�����key, Ĭ������Ϊ:
>["b2b:*", "a*", "b*", "c*", "p*", "sys*"]
��ʾ������������е�redis key���ų�user:*)

h2. �г����ϵ��б�ť

����������ҵ����Ϊ �г���(b_market)���б�ť ������������ϻ��桷��Ҫ������û���root���b_market�����޸�Ȩ�޵���

h2. �Զ��Ļ������

���⣬��framework5�У�ͨ��ע��SchemaActionListener�����������еı���޸ĺ�ɾ���ύ������
����λredis���Ա�ı���+":*" ��key��ȫ�������ע�⣬�������еı�����ȫ��Ӧ��redis key��
���� b_market��û�С�����Ʒ������Ҳ���Ǳ�׼��key��������b2b:��ͷ

 * @author yfzhu
 *
 */
@Admin(mail="yfzhu@lifecycle.cn")
public class ClearRedis extends CmdHandler {
	/**
	 * 
	 */
	public CmdResult execute(JSONObject jo) throws Exception {
		
		HttpServletRequest req = event.getContext().getHttpServletRequest();
		UserWebImpl userWeb =event.getContext().getUserWeb();

		if(!userWeb.getUserName().equals("root")  && !usr.getName().equals("root")){
			//user should has permission to change b_market
			userWeb.checkPermission(TableManager.getInstance().getTable("b_market").getSecurityDirectory()	,Directory.WRITE);
		}

		String key=jo.optString("args");
		long cnt=0;
		if(Validator.isNotNull(key)){
			if(key.equals("*")) throw new NDSException("������ȫ��");
			LanguageManager.getInstance().clear(conn);
			cnt+=delKeyPattern(key);
		}else{
			JSONArray keys=(JSONArray) PhoneController.getInstance().getValueFromADSQLAsJSON("clear_redis_keys", conn, false);
			for(int i=0;i<keys.length();i++){
				key= keys.getString(i);
				cnt+=delKeyPattern(key);
			}
			//������dim,lang
			LanguageManager.getInstance().clear(conn);
			DimTranslator.getInstance().clear(conn);
			DimDefTranslator.getInstance().clear(conn);
			
			PhoneController.getInstance().clear();
			com.agilecontrol.b2b.schema.TableManager.getInstance().reload();
            
		}
		MarketManager.getInstance().clear(conn);
		JSONObject ret=new JSONObject();
		String message="���redis key�ϼ�"+ cnt+"��";
		ret.put("message", message);//����Ϊshell�ű��ܿ�����ʾ
		CmdResult cr=new CmdResult(0, message/*����Ϊ��ad_action��ť�ܿ�����ʾ*/,ret);
		
		return cr;
	}
	
	private long delKeyPattern(String keyPatter) throws Exception{
		Set<String> set=jedis.keys(keyPatter);
		if(set.size()>0){
			String[] keyall=new String[set.size()];
			set.toArray(keyall);
			long cnt=jedis.del(keyall);
			logger.debug("delete jedis keys:"+ Tools.toString(keyall)+", cnt="+ cnt);
			return cnt;
		}
		return 0;
	}
}













