package com.agilecontrol.b2bweb.cmd;

import java.util.Iterator;

import org.json.JSONObject;

import com.agilecontrol.b2bweb.grid.GridBuilder;

import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.phone.Admin;
import com.agilecontrol.phone.CmdHandler;
import com.agilecontrol.phone.CmdResult;

/**
 * 
 * 
 * 
 * @author Administrator
 *
 */
@Admin(mail = "li.shuhao@lifecycle.cn")
public class GridDelete extends CmdHandler{
	@Override
	public CmdResult execute(JSONObject jo) throws Exception {
		
		for(Iterator it=jo.keys();it.hasNext();){
			String key=(String)it.next();
			Object v=jo.get(key);
			vc.put(key, v);
		}
		
		GridBuilder builder=new GridBuilder();
		builder.init(usr, jo,  event, vc, jedis, conn);
		return new CmdResult(builder.gridDelete(jo));
	}

}
