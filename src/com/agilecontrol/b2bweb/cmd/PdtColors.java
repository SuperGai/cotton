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
 * 描述：通过商品id返回该商品同款异色的其他商品
 * 
 * 输出：
[
    {
        "pdtid": 1,
        "image": "/images/6970283825932_1.jpg"
    },
    {
        "pdtid": 1,
        "image": "/images/6970283825932_1.jpg"
    },
    {
        "pdtid": 1,
        "image": "/images/6970283825932_1.jpg"
    },]
    {
        "pdtid": 1,
        "image": "/images/6970283825932_1.jpg"
    }
]         
 * 
 * pdtid:商品id
 * note:商品描述
 * no：货号
 * mainpic:主图
 * price:商品价格
 * @author sun.yifan
 *
 */
public class PdtColors extends CmdHandler {

	@Override
	public CmdResult execute(JSONObject jo) throws Exception {
		long pdtid = jo.optLong("pdtid",-1);
		if(pdtid==-1){
			throw new NDSException("pdtid not found！");
		}
		String sql = "SELECT mp.id pdtid, mp.imageurl as mainpic,mp.value note,mp.name as no,mp.pricelist price FROM B_PDT_ALIKE bpp, m_product mp WHERE bpp.M_PAIR_ID = mp.id AND bpp.m_product_id = ? ";
		JSONArray pdtList = engine.doQueryObjectArray(sql, new Object[]{pdtid},conn);
		if(pdtList==null){
			pdtList = new JSONArray();
		}
		return new CmdResult(pdtList);
	}

}
