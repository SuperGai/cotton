package com.agilecontrol.b2bweb.cmd;

import org.json.JSONArray;
import org.json.JSONObject;

import com.agilecontrol.nea.core.control.event.NDSEventException;
import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.nea.util.Validator;
import com.agilecontrol.phone.CmdHandler;
import com.agilecontrol.phone.CmdResult;
import com.agilecontrol.phone.PhoneController;

/**
 * {cmd:"b2b.pdt.colors",id:1} 
 * 
 * id:商品id
 * 
 * 描述：通过商品id以及富文本名称，返回相应的富文本内容
 * 
 * 输出：
{
	"<div><img src="/images/act/carousel2.jpg"></div>"
}       
 * 
 * pdtid:商品id
 * note:商品描述
 * no：货号
 * mainpic:主图
 * price:商品价格
 * @author sun.yifan
 *
 */
public class PdtRichText extends CmdHandler {

	@Override
	public CmdResult execute(JSONObject jo) throws Exception {
		long pdtid = jo.optLong("pdtid",-1);
		if(pdtid==-1){
			throw new NDSException("pdtid not found！");
		}
		long actid = jo.optLong("actid",-1);
		String colname = jo.optString("colname","pdtdtls");
		String sql = "SELECT "+colname+" value FROM m_product WHERE id = ?";
		String colstr = engine.doQueryString(sql, new Object[]{pdtid},conn);
		if(Validator.isNull(colstr)){
			colstr = "<div></div>";
		}
		return new CmdResult(colstr);
	}

}
