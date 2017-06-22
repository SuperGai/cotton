package com.agilecontrol.portal.cmd;

import org.json.JSONArray;
import org.json.JSONObject;

import com.agilecontrol.b2b.cmd.ObjectGet;
import com.agilecontrol.b2b.cmd.Search;
import com.agilecontrol.b2b.schema.Table;
import com.agilecontrol.b2bweb.WebController;
import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.nea.util.Validator;
import com.agilecontrol.phone.Admin;
import com.agilecontrol.phone.CmdHandler;
import com.agilecontrol.phone.CmdResult;
import com.agilecontrol.phone.PhoneController;
import com.agilecontrol.phone.PhoneUtils;

/**

 输入

bao_number- 棉蓄宝账号

输出
{1、如何账号存在则删除信息，添加一条校验记录，提示校验成功
 2、如何账号不存在则提示，账号错误或者以使用
}



 * @author Supergai
 *
 */
@Admin(mail="xuwj@cottonshop.com")
public class CheckMianxu extends ObjectGet {
	
	@Override
	public CmdResult execute(JSONObject jo) throws Exception {
		String bao_number= jo.getString("bao_number");
		if(Validator.isNotNull(bao_number)){
			bao_number= bao_number.trim();
		}else throw new NDSException("需要bao_number");
		/**
		 * 逻辑为通过bao_number获得他的店仓获得销补的所有商品
		 */
	    String sql = "SELECT BAO_ID FROM M_COTTON_SAVINGS WHERE BAO_NUMER=?";
		int BAO_ID = engine.doQueryInt(sql, new Object[]{bao_number}, conn);
        if(BAO_ID!=0){ 
        	String sqlinsert="insert * into BAO_INFO values(?,?,?)";
        	engine.executeUpdate(sqlinsert, new Object[]{bao_number}, conn);
        	throw new NDSException("校验成功");
        }
        if(BAO_ID==0){
        	throw new NDSException("账号错误或者以使用");
        }
		return new CmdResult();
	}

}
