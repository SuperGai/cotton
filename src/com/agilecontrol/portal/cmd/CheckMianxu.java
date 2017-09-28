package com.agilecontrol.portal.cmd;

import java.sql.PreparedStatement;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;

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
	JSONObject mianxuObject ;
	@Override
	public CmdResult execute(JSONObject jo) throws Exception {
		String bao_number= jo.getString("xubaonumber");
		if(Validator.isNotNull(bao_number)){
			bao_number= bao_number.trim();
		}else throw new NDSException("需要棉蓄宝账号");
		/**
		 * 逻辑为通过bao_number获得他的店仓获得销补的所有商品
		 */
		
		PreparedStatement	pstmt = conn.prepareStatement("update M_COTTON_SAVINGS  set  VERIFYED='Y',VERIFYDATE=sysdate   where  CODE=? and VERIFYED='N' ");
		pstmt.setString(1, bao_number);
		int BAO_ID = pstmt.executeUpdate();
        if(BAO_ID!=0){     
    		String sql = "select * from M_COTTON_SAVINGS where CODE=?";
    		 mianxuObject = engine.doQueryObject(sql, new Object[]{bao_number},conn);
        }
        if(BAO_ID==0){
        	throw new NDSException("账号错误或者以使用");
        }
		return new CmdResult(mianxuObject);
	}
	


}
