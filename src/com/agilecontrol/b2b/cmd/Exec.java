package com.agilecontrol.b2b.cmd;

import org.apache.velocity.VelocityContext;
import org.json.*;

import java.util.*;

import com.agilecontrol.b2b.schema.*;
import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.phone.CmdHandler;
import com.agilecontrol.phone.CmdResult;
import com.agilecontrol.phone.PhoneUtils;

/**
 * 
   ��Ժ�̨������
   ����: 
   {
   	name: ad_sql.name ��������������������ƣ�����������lua�ű���Ӧ��ad_sql.name
   	args: {} �ű�����Ҫ�Ĳ�����lua�ű����ܵĲ�����ǿ�Ʊ������ comid,stid,uid,empid
   }
   
   ����: 
   lua �ű����ɵĶ���
   
 * @author yfzhu
 *
 */
public class Exec extends CmdHandler {
	
	@Override
	public CmdResult execute(JSONObject jo) throws Exception {
		String name=getString(jo,"name");
		JSONObject args=jo.optJSONObject( "args");
		if(args==null) args=new JSONObject();
		if(usr!=null){
			args.put("comid", usr.getComId());
			args.put("uid", usr.getId());
		}else{
			args.put("comid", 0);
			args.put("uid",0);
		}
		
		JSONObject obj=(JSONObject)PhoneUtils.execRedisLua(name, args, conn, jedis);
		
		CmdResult res=new CmdResult(obj );
		return res;
	}
	
}
