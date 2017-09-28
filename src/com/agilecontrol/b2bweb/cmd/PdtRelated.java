package com.agilecontrol.b2bweb.cmd;

import org.json.JSONArray;
import org.json.JSONObject;

import com.agilecontrol.nea.core.control.event.NDSEventException;
import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.phone.CmdHandler;
import com.agilecontrol.phone.CmdResult;
import com.agilecontrol.phone.PhoneController;

/**
 * {cmd:"b2b.pdt.related",pdtid,actid} 
 * 
 * 
 * 描述：通过商品id返回该商品的搭配款商品信息
 * 
 * 输出： [
 * {"pdtid":"2029","note":"经典M系单肩斜挎包-浅2029","no":"19020032B2029","mainpic":
 * "/images/6970283825932_1.jpg","price":"59"},
 * {"pdtid":"2030","note":"韩国简约单肩包-黑2030","no":"19020034B2030","mainpic":
 * "/images/6970283827738_1.jpg","price":"29"},
 * {"pdtid":"2031","note":"帆布印花方形零钱包-蓝2031","no":"19010066B2031","mainpic":
 * "/images/6970283827158_1.jpg","price":"10"},
 * {"pdtid":"2032","note":"动物纹斜跨手拿包-粉2032","no":"19020035B2032","mainpic":
 * "/images/6970283827899_1.jpg","price":"49"},
 * {"pdtid":"2033","note":"韩国镭射零钱包-蓝2033","no":"19010067B2033","mainpic":
 * "/images/6970283827998_1.jpg","price":"19"},
 * {"pdtid":"2034","note":"韩国镭射流苏小方包-银2034","no":"19020037B2034","mainpic":
 * "/images/6970283827943_1.jpg","price":"59"},
 * {"pdtid":"2035","note":"帆布印花方形零钱包-紫2035","no":"19010066B2035","mainpic":
 * "/images/6970283827165_1.jpg","price":"10"},
 * {"pdtid":"2036","note":"经典M系单肩斜挎包-2036","no":"19020032B2036","mainpic":
 * "/images/6970283825949_1.jpg","price":"59"} ]
 * 
 * pdtid:商品id
 * note:商品描述
 * no：货号
 * mainpic:主图
 * price:商品价格
 * @author sun.yifan
 *
 */
public class PdtRelated extends CmdHandler {

	@Override
	public CmdResult execute(JSONObject jo) throws Exception {
		long pdtid = jo.optLong("pdtid",-1);
		if(pdtid==-1){
			throw new NDSException("pdtid not found");
		}
		long actid = jo.optLong("actid",-1);
		String pdtsql = PhoneController.getInstance().getValueFromADSQL("b2b:pdt:related",conn);	
		JSONArray pdtList = engine.doQueryObjectArray(pdtsql, new Object[]{pdtid},conn);
		if(pdtList==null){
			pdtList = new JSONArray();
		}
		return new CmdResult(pdtList);
	}

}
