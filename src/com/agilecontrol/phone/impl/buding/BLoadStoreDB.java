package com.agilecontrol.phone.impl.buding;

import java.io.File;

import org.json.JSONObject;

import com.agilecontrol.nea.core.util.ConfigValues;
import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.phone.CmdHandler;
import com.agilecontrol.phone.CmdResult;
import com.agilecontrol.phone.PhoneConfig;
import com.agilecontrol.phone.PhoneController;
/**
 * 
{
    cmd: "BLoadStoreDB", 
    userid: long,
    time: long,
    sign:String,
    obj:{storeid: long}
}

服务器读取当前用在store表里的角色，鉴别为none以外角色时，构造数据库（需要较长时间），下发url
Resposne: {
code: int, message: String, 
obj: {
url: String
}
}
客户端取数据库，然后进入店铺首页
 * @author yfzhu
 *
 */
public class BLoadStoreDB extends BudingHandler {
	/**
	 * 创建sqlite
	 * 这里需要谨慎处理：在创建sqlite的时候，应该记录该店铺的所有message，在sqlite生成点开始，首先堵住店铺变动；
	 * 生成sqlite后，将店铺变动放开，并且将所有message推送给当前用户
	 * 现在先简单实现，发送sqlite
	 * 
	 * @param userId
	 * @param storeId
	 * @return url to 下载
	 * @throws Exception
	 */
	private String createSQLite(long userId, long storeId) throws Exception{
		if(PhoneConfig.DEBUG){
			SQLiteFactory.getInstance().reload();
		}
		String folder=ConfigValues.DIR_NEA_ROOT+"/bfile/db/"+storeId;
		File f=new File(folder);
		if(!f.exists()) f.mkdirs();
		String fileName=userId+"_"+ System.currentTimeMillis()+".db";
		String dBFilefullPath=folder+"/"+fileName;
		
		SQLiteFactory.getInstance().createSqliteFile(dBFilefullPath, userId, storeId, conn);
		
		String url="/bfile/db/"+ storeId+"/"+fileName;
		return url;
	}
	@Override
	public CmdResult execute(JSONObject jo) throws Exception {
		long userId= jo.getLong("userid");
		
		long storeId=jo.getJSONObject("obj").getLong("storeid");
		
		JSONObject role=engine.doQueryObject("select priv role from d_store_emp where user_id=? and d_store_id=? ", new Object[]{userId,storeId} , conn);
		if(role==null) throw new NDSException("该店铺没有您的记录，请重新申请加入");
		String rl=role.getString("role");
		if("none".equals(role))throw new NDSException("需要店主的授权才能进入");
		
		String url=createSQLite(userId, storeId);
		
		JSONObject dburl=new JSONObject();
		dburl.put("url", url);
		return new CmdResult(dburl);
	}
	
	
}