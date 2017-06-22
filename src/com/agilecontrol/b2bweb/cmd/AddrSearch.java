package com.agilecontrol.b2bweb.cmd;

import java.util.HashMap;

import org.json.JSONObject;

import com.agilecontrol.b2b.cmd.Search;
import com.agilecontrol.phone.Admin;
import com.agilecontrol.phone.CmdHandler;
import com.agilecontrol.phone.CmdResult;

/**
 h1. user_address �û���ַ����

> {cmd:"b2b.addr.search"}

ǿ���������: isactive='Y' and user_id=$uid

 * @author yfzhu
 *
 */
@Admin(mail="yfzhu@lifecycle.cn")
public class AddrSearch extends Search {
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
		data.put("isactive=?", "Y");
		data.put("user_id=?", usr.getId());
		
		return data;
	}

}
