package com.agilecontrol.b2bweb.cmd;

import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONObject;

import com.agilecontrol.b2b.cmd.Search;
import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.phone.Admin;
import com.agilecontrol.phone.CmdHandler;
import com.agilecontrol.phone.CmdResult;
import com.agilecontrol.phone.PhoneController;

/**
 ����Search, ����"mark", ��: b_pdt_fav

> {cmd:"b2b.mark.search", xx}


 * @author yfzhu
 *
 */
@Admin(mail="yfzhu@lifecycle.cn")
public class MarkSearch extends Search {
	
	
	/**
	 * ������Ĳ�ѯ���������ع�����֧������Ĳ�ѯ����
	 * @param jo ԭʼ�Ĳ�ѯ�������п����ڴ˵ر��ع�
	 * @return ����Ĳ�ѯ�����������ӵ���ѯwhere�����
	 * key: ��Ҫ���䵽sql where ���ֵ�clause������ݣ����� "emp_id=?"
	 * value ���ʺŶ�Ӧ��ʵ��ֵ��Ŀǰkey��֧��һ���ʺű����Ӧ�����value= ��ǰemp��id
	 * 
	 */
	protected HashMap<String, Object> reviseSearchCondition(JSONObject jo) throws Exception{
		int pdtId=this.getInt(jo, "pdtid");
		int actId=jo.optInt( "actid",-1);
		int customerId= getCustomerIdByActOrMarket(actId,usr.getMarketId()) ;
		
		HashMap<String,Object> data=new HashMap();
		data.put("c_customer_id=?", customerId);
		data.put("m_product_id=?", pdtId);
		
		return data;
	}

}
