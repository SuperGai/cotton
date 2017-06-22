package com.agilecontrol.b2bweb.cmd;

import java.util.HashMap;

import org.json.JSONObject;

import com.agilecontrol.b2b.cmd.Search;
import com.agilecontrol.phone.Admin;
import com.agilecontrol.phone.CmdHandler;
import com.agilecontrol.phone.CmdResult;

/**
 ����Search

cmd: "b2b.act.search"

 * @author yfzhu
 *
 */
@Admin(mail="yfzhu@lifecycle.cn")
public class ActSearch extends Search {
	/**
	 * ������Ĳ�ѯ���������ع�����֧������Ĳ�ѯ����
	 * @param jo ԭʼ�Ĳ�ѯ�������п����ڴ˵ر��ع�
	 * @return ����Ĳ�ѯ�����������ӵ���ѯwhere�����
	 * key: ��Ҫ���䵽sql where ���ֵ�clause������ݣ����� "emp_id=?"
	 * value ���ʺŶ�Ӧ��ʵ��ֵ��Ŀǰkey��֧��һ���ʺű����Ӧ�����value= ��ǰemp��id
	 * 
	 */
	protected HashMap<String, Object> reviseSearchCondition(JSONObject jo) throws Exception{
		HashMap<String,Object> data=new HashMap();
		data.put("b_market_id=?", usr.getMarketId());
		
		return data;
	}

}
