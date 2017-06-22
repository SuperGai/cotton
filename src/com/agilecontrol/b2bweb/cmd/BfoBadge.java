package com.agilecontrol.b2bweb.cmd;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;

import com.agilecontrol.b2b.cmd.ObjectGet;
import com.agilecontrol.b2b.cmd.Search;
import com.agilecontrol.b2b.schema.Table;
import com.agilecontrol.b2bweb.WebController;
import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.nea.util.Tools;
import com.agilecontrol.nea.util.Validator;
import com.agilecontrol.phone.Admin;
import com.agilecontrol.phone.CmdHandler;
import com.agilecontrol.phone.CmdResult;
import com.agilecontrol.phone.PhoneController;
import com.agilecontrol.phone.PhoneUtils;

/**
h1. ������ͬ����״̬��ͳ����

h2. ����

> {cmd:"b2b.order.badge"}

������״̬send_status��ͳ��ֵ���ϣ����磺{"1": 5, "3": 5} ��ʾ ��ȷ����5�ŵ�����������3�ŵ�

h2. ���

<pre>
{ k:v }
</pre>

*k* - ����״ֵ̬
*v* - ״̬��Ӧ������

����
> {"1": 5, "3": 5}

 * @author yfzhu
 *
 */
@Admin(mail="yfzhu@lifecycle.cn")
public class BfoBadge extends CmdHandler {
	
	/**
	 * 
	 */
	public CmdResult execute(JSONObject jo) throws Exception {
		
		JSONObject obj=toKVObject(engine.doQueryJSONArray(
				"select send_status, count(*) from b_bfo where DEST_USER_ID=? and send_status in(2,3) group by send_status",
				new Object[]{usr.getId()}, conn));
		
		return new CmdResult(obj);
	}

}













