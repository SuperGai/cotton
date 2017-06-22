package com.agilecontrol.b2bweb.cmd;

import org.json.JSONArray;
import org.json.JSONObject;

import com.agilecontrol.b2b.cmd.Search;
import com.agilecontrol.b2bweb.WebController;
import com.agilecontrol.phone.Admin;
import com.agilecontrol.phone.CmdHandler;
import com.agilecontrol.phone.CmdResult;

/**
 
h1. 切换收藏

h2. 输入

> {cmd:"b2b.mfav.switch" , pdtid, en}

*pdtid* - int 商品
*en* - string "Y"|"N"

将读取B_FAVOURITE表，识别是add还是update，en对应isactive字段

h2. 输出

{code,message}


 * @author yfzhu
 *
 */
@Admin(mail="yfzhu@lifecycle.cn")
public class MfavSwitch extends CmdHandler {
	
	/**
	 * 执行任务
	 * 
	 * @param jo
	 *            任务参数
	 * @return 返回的内容将全部对应到ValueHolder相关项
	 */
	public  CmdResult execute(JSONObject jo) throws Exception{
		int pdtId= this.getInt(jo, "pdtid");
		String en= this.getString(jo, "en");
		int id=engine.doQueryInt("select id from b_favourite where user_id=? and m_product_id=?", new Object[]{usr.getId(),pdtId}, conn);
		if(!"Y".equals(en)) {
			//取消收藏
			if(id>0){
				engine.executeUpdate("delete from b_favourite where user_id=? and m_product_id=?",new Object[]{usr.getId(), pdtId	}, conn);
				//engine.executeUpdate("update b_favourite set isactive=?,modifieddate=sysdate where user_id=? and m_product_id=?",new Object[]{en,usr.getId(), pdtId	}, conn);
				jedis.del("mfav:"+id );
			}
		}else{
			//添加收藏
			if(id>0){
				engine.executeUpdate("update b_favourite set isactive=?,modifieddate=sysdate where user_id=? and m_product_id=?",new Object[]{en,usr.getId(), pdtId	}, conn);
				jedis.del("mfav:"+id );
			}else{
				engine.executeUpdate("insert into b_favourite (id,user_id,m_product_id,isactive) values (get_sequences('b_favourite'), ?,?,?)",
						new Object[]{usr.getId(), pdtId,en}, conn);
			}
		}
		
		return CmdResult.SUCCESS;
	}
}
