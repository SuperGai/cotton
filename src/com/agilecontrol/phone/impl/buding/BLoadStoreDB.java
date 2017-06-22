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

��������ȡ��ǰ����store����Ľ�ɫ������Ϊnone�����ɫʱ���������ݿ⣨��Ҫ�ϳ�ʱ�䣩���·�url
Resposne: {
code: int, message: String, 
obj: {
url: String
}
}
�ͻ���ȡ���ݿ⣬Ȼ����������ҳ
 * @author yfzhu
 *
 */
public class BLoadStoreDB extends BudingHandler {
	/**
	 * ����sqlite
	 * ������Ҫ���������ڴ���sqlite��ʱ��Ӧ�ü�¼�õ��̵�����message����sqlite���ɵ㿪ʼ�����ȶ�ס���̱䶯��
	 * ����sqlite�󣬽����̱䶯�ſ������ҽ�����message���͸���ǰ�û�
	 * �����ȼ�ʵ�֣�����sqlite
	 * 
	 * @param userId
	 * @param storeId
	 * @return url to ����
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
		if(role==null) throw new NDSException("�õ���û�����ļ�¼���������������");
		String rl=role.getString("role");
		if("none".equals(role))throw new NDSException("��Ҫ��������Ȩ���ܽ���");
		
		String url=createSQLite(userId, storeId);
		
		JSONObject dburl=new JSONObject();
		dburl.put("url", url);
		return new CmdResult(dburl);
	}
	
	
}