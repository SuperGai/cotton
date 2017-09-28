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
 * ������ͨ����Ʒid���ظ���Ʒ�Ĵ������Ʒ��Ϣ
 * 
 * ����� [
 * {"pdtid":"2029","note":"����Mϵ����б���-ǳ2029","no":"19020032B2029","mainpic":
 * "/images/6970283825932_1.jpg","price":"59"},
 * {"pdtid":"2030","note":"������Լ�����-��2030","no":"19020034B2030","mainpic":
 * "/images/6970283827738_1.jpg","price":"29"},
 * {"pdtid":"2031","note":"����ӡ��������Ǯ��-��2031","no":"19010066B2031","mainpic":
 * "/images/6970283827158_1.jpg","price":"10"},
 * {"pdtid":"2032","note":"������б�����ð�-��2032","no":"19020035B2032","mainpic":
 * "/images/6970283827899_1.jpg","price":"49"},
 * {"pdtid":"2033","note":"����������Ǯ��-��2033","no":"19010067B2033","mainpic":
 * "/images/6970283827998_1.jpg","price":"19"},
 * {"pdtid":"2034","note":"������������С����-��2034","no":"19020037B2034","mainpic":
 * "/images/6970283827943_1.jpg","price":"59"},
 * {"pdtid":"2035","note":"����ӡ��������Ǯ��-��2035","no":"19010066B2035","mainpic":
 * "/images/6970283827165_1.jpg","price":"10"},
 * {"pdtid":"2036","note":"����Mϵ����б���-2036","no":"19020032B2036","mainpic":
 * "/images/6970283825949_1.jpg","price":"59"} ]
 * 
 * pdtid:��Ʒid
 * note:��Ʒ����
 * no������
 * mainpic:��ͼ
 * price:��Ʒ�۸�
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
