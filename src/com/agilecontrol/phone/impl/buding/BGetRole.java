package com.agilecontrol.phone.impl.buding;

import org.json.JSONObject;

import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.phone.CmdHandler;
import com.agilecontrol.phone.CmdResult;
import com.agilecontrol.phone.PhoneController;
/**
 * 
 * �������յ�Http MiscCmd�����󣬱��״̬Ϊ����ѡ����̡�����storeid Ϊ�ش�ֵ������ȴ�ҳ�棬����ˢ�°�ť����app���´�ʱ������������:
Http MiscCmd:

{
    cmd: "BGetRole", 
    userid: long,
    time: long,
    sign:String,
    obj:{storeid: long}
}

��������ȡ��ǰ����store����Ľ�ɫ������:
Resposne: {
code: int, message: String, 
obj: {
role: string none|admin|mgr| pur|emp ��Ȩ��|����Ա|�곤|�ɹ�|��Ա
}
}

 * @author yfzhu
 *
 */
public class BGetRole extends BudingHandler {

	@Override
	public CmdResult execute(JSONObject jo) throws Exception {
		long userId= jo.getLong("userid");
		
		long storeId=jo.getJSONObject("obj").getLong("storeid");
		
		JSONObject role=engine.doQueryObject("select priv role from d_store_emp where user_id=? and d_store_id=? ", new Object[]{userId,storeId} , conn);
		if(role==null) throw new NDSException("�õ���û�����ļ�¼���������������");
		return new CmdResult(role);
	}
	
	
}