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
   针对后台的运行
   输入: 
   {
   	name: ad_sql.name 这是运行命令的配置名称，运行命令是lua脚本对应的ad_sql.name
   	args: {} 脚本所需要的参数，lua脚本接受的参数将强制被填充上 comid,stid,uid,empid
   }
   
   返回: 
   lua 脚本生成的对象
   
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
