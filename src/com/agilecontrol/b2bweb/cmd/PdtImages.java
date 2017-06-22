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
 * {cmd:"b2b.pdt.images",pdtid,actid} 
 * 
 * pdtid:��Ʒid
 * actid:�id
 * 
 * ������������Ʒ�������ͼƬ���ƣ����ڵ�Ʒ�����չʾ
 * 
 * �����
 {
    [
        "/pdt/x/images/6970283827165_1.jpg",
        "/pdt/x/images/6970283825949_1.jpg"
    ]
}               
 * 
 * pdtid:��Ʒid
 * note:��Ʒ����
 * no������
 * mainpic:��ͼ
 * price:��Ʒ�۸�
 * @author sun.yifan
 *
 */
public class PdtImages extends CmdHandler {

	@Override
	public CmdResult execute(JSONObject jo) throws Exception {
		long pdtid = jo.optLong("pdtid",-1);
		if(pdtid==-1){
			throw new NDSException("pdtid not found");
		}
		long actid = jo.optLong("actid",-1);
		String images=jedis.hget("pdt:"+ pdtid, "images");
		if(Validator.isNull(images)){
			vc.put("pdtid", pdtid);
			JSONArray imgs=PhoneController.getInstance().getDataArrayByADSQL("pdt_img_list", vc, conn, false);
			images= imgs.toString();
			jedis.hset("pdt:"+ pdtid, "images",images);
		}
		JSONArray photos = new JSONArray(images);
		return new CmdResult(photos);
	}

}
