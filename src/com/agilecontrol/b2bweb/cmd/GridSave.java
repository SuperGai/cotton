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
	 * 根据前台在界面做修改，我们在后台做即时保存，并返回前台修改过的计算值	
	 *   
	 * @throws Exception
	 * @param pdtRow {key："123_456",,new_value:"45",cashKey:,start:,cnt:}
	 *  key:$pdtId_$asiId
	 * cashKey:前台传给后台，拿到所有商品来调整grid下面的合计量的缓存key
	 * new_value:客户修改后的值
	 * cnt:当前页有多少条
	 * start:得到当前第多少页
	 * @return JSONObject
	 * {comm:[  
	 * 				{key: ,value:}..
	 *  ],sum[123,345,1111]}
	 *  comm:普通行数据
	 *  sum:grid下面的计算总量
	 *  key:前台所对应的field
	 *  value:对应单元格的值
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
		//将所有传人的参数都放置在vc中供调用
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
