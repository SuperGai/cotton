package com.agilecontrol.b2bweb.cmd;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;

import com.agilecontrol.b2b.cmd.Search;
import com.agilecontrol.b2bweb.WebController;
import com.agilecontrol.b2bweb.sheet.Color;
import com.agilecontrol.b2bweb.sheet.ProductMatrix;
import com.agilecontrol.b2bweb.sheet.ProductMatrixLoader;
import com.agilecontrol.b2bweb.sheet.AllocationObject;
import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.nea.util.Tools;
import com.agilecontrol.nea.util.Validator;
import com.agilecontrol.phone.Admin;
import com.agilecontrol.phone.CmdHandler;
import com.agilecontrol.phone.CmdResult;
import com.agilecontrol.phone.PhoneController;

/**
 
h1. ������������������


��װ����Ʒ�ڳ����ϵķֲ�����������̬�����в��ĳ�����࣬�������μ��١�
����ȡ�������������루Ҳһ����������һ�£���������ӣ��ķ�ʽ���ϼ��������Ԥ��������һ�¡�
��ʱ��Ҫ����������������򣬴����еĳ��뿪ʼ�����߷ţ�ע����һ�����ķš�

�����㷨��Ŀ���ǣ�

*�����ܱ�֤������ĳ��������������������ľ�������С*

�ͻ���ͨ�������������ȡ������̯���������������
>{cmd:"b2b.pdt.allocqty", pdtid, actid,qty, colorcode}
*pdtid* - ��ǰ��Ʒ
*colorcode* - �����ж�ɫ�������Ʒ���ͻ��˻����ṩ��ɫ��
*actid* - ��ǰ���-1��ʾδ�μӻ
*qty* - ����

����
>{qtys: [null, 1,0,3,null]}
*qtys* - jsonarray of int, ע��null��ʾ��֧����Ϊ������
 

 * @author yfzhu
 *
 */
@Admin(mail="yfzhu@lifecycle.cn")
public class PdtAllocateQty extends CmdHandler {
	
	/**
	 * ִ������
	 * 
	 * @param jo
	 *            �������
	 * @return ���ص����ݽ�ȫ����Ӧ��ValueHolder�����
	 */
	public  CmdResult execute(JSONObject jo) throws Exception{
		int pdtId= this.getInt(jo, "pdtid");
		int actId= jo.optInt("actid",-1);
		int qty= this.getInt(jo, "qty");
		String colorCode= jo.optString("colorcode");
		
		ProductMatrixLoader loader=new ProductMatrixLoader(jedis,conn);
		
		ProductMatrix matrix=loader.getProductMatrix(pdtId);
		float[] ratios=null;
		// ��Ӧ�Ŀ�ɫm_product.id��һ���ǵ�ǰpdtId
		int colorPdtId=-1;
		ArrayList<Color> colors=matrix.getColors();
		JSONArray asis=null; //��Ч��asi��Integer[],  null ��ʾû��asi
		logger.debug("colors:"+ Tools.toString(colors, ",") );
		if(Validator.isNotNull(colorCode)){
			boolean found=false;
			for(int i=0;i<colors.size();i++){
				Color color=colors.get(i);
				if(color.getCode().equals(colorCode)){
					found=true;
					colorPdtId=matrix.getProductIds().get(i);
					asis=matrix.getASIArrays().getJSONArray(i);
					ratios=loader.getProductSizeRatios(colorPdtId, usr.getId());
					break;
				}
			}
			if(!found) throw new NDSException("��������colorcode="+ colorCode+",���ڵ�ǰpdtid="+ pdtId+", colors="+ Tools.toString(colors, ","));
		}else{
			if(matrix.getColors().size()!=1) throw new NDSException("�������󣬱����ṩcolorcode");
			colorPdtId=matrix.getProductIds().get(0);
			ratios=loader.getProductSizeRatios(colorPdtId, usr.getId());
			asis=matrix.getASIArrays().getJSONArray(0);
			
		}
		ratios=loader.clearNoneExistsSizeRatios(ratios, asis);

		AllocationObject aobj;
		//���벻����������ȫ����ȥ
		aobj= new AllocationObject(false, AllocationObject.CONVERTION_ROUND );
		JSONArray qtys=aobj.allocateSKU( qty,ratios ,asis);
		
		JSONObject ret=new JSONObject();
		ret.put("qtys", qtys);
		
		return new CmdResult(ret);
	}


	
}
