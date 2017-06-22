package com.agilecontrol.b2bweb.cmd;

import java.util.Iterator;

import org.json.JSONObject;
import org.stringtree.regex.Matcher;
import org.stringtree.regex.Pattern;

import com.agilecontrol.b2bweb.grid.GridBuilder;
import com.agilecontrol.b2bweb.grid.GridViewDefine;
import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.phone.CmdHandler;
import com.agilecontrol.phone.CmdResult;

public class GridSave extends CmdHandler {
	/**
	 * 
	 * ����ǰ̨�ڽ������޸ģ������ں�̨����ʱ���棬������ǰ̨�޸Ĺ��ļ���ֵ	
	 *   
	 * @throws Exception
	 * @param pdtRow {key��"123_456",,new_value:"45",cashKey:,start:,cnt:}
	 *  key:$pdtId_$asiId
	 * cashKey:ǰ̨������̨���õ�������Ʒ������grid����ĺϼ����Ļ���key
	 * new_value:�ͻ��޸ĺ��ֵ
	 * cnt:��ǰҳ�ж�����
	 * start:�õ���ǰ�ڶ���ҳ
	 * @return JSONObject
	 * {comm:[  
	 * 				{key: ,value:}..
	 *  ],sum[123,345,1111]}
	 *  comm:��ͨ������
	 *  sum:grid����ļ�������
	 *  key:ǰ̨����Ӧ��field
	 *  value:��Ӧ��Ԫ���ֵ
	 * @throws Exception
	 */
	@Override
	public CmdResult execute(JSONObject jo) throws Exception {
		String testQty = jo.optString("qty").trim();
		try{
			long qty = Long.parseLong(testQty);
			if(qty > Integer.MAX_VALUE||qty < 0){
				throw new NDSException("@b2bedit-count@");
			}
		}catch(Exception e){
			throw new NDSException("@b2bedit-count@");
		}
		//�����д��˵Ĳ�����������vc�й�����
		for(Iterator it=jo.keys();it.hasNext();){
			String key=(String)it.next();
			Object v=jo.get(key);
			vc.put(key, v);
		}
		
		GridBuilder builder=new GridBuilder();
		builder.init(usr, jo,  event, vc, jedis, conn);
		return new CmdResult(builder.gridSave(jo));
	}

}
