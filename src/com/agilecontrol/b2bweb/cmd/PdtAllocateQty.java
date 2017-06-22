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
 
h1. 分配总量到各个尺码


服装类商品在尺码上的分布呈现帕松形态，即中部的尺码最多，两边依次减少。
采用取整而非四舍五入（也一样会总量不一致，计算更复杂）的方式，合计总量会和预计总量不一致。
这时需要将不足的量按优先序，从正中的尺码开始向两边放，注意是一个个的放。

帕松算法的目标是：

*尽可能保证放置完的尺码比例与理想尺码比例间的均方差最小*

客户端通过命令来请求获取总量分摊到各个尺码的数量
>{cmd:"b2b.pdt.allocqty", pdtid, actid,qty, colorcode}
*pdtid* - 当前商品
*colorcode* - 对于有多色情况的商品，客户端还将提供颜色号
*actid* - 当前活动，-1表示未参加活动
*qty* - 总量

返回
>{qtys: [null, 1,0,3,null]}
*qtys* - jsonarray of int, 注意null表示不支持作为输入项
 

 * @author yfzhu
 *
 */
@Admin(mail="yfzhu@lifecycle.cn")
public class PdtAllocateQty extends CmdHandler {
	
	/**
	 * 执行任务
	 * 
	 * @param jo
	 *            任务参数
	 * @return 返回的内容将全部对应到ValueHolder相关项
	 */
	public  CmdResult execute(JSONObject jo) throws Exception{
		int pdtId= this.getInt(jo, "pdtid");
		int actId= jo.optInt("actid",-1);
		int qty= this.getInt(jo, "qty");
		String colorCode= jo.optString("colorcode");
		
		ProductMatrixLoader loader=new ProductMatrixLoader(jedis,conn);
		
		ProductMatrix matrix=loader.getProductMatrix(pdtId);
		float[] ratios=null;
		// 对应的款色m_product.id不一定是当前pdtId
		int colorPdtId=-1;
		ArrayList<Color> colors=matrix.getColors();
		JSONArray asis=null; //有效的asi，Integer[],  null 表示没有asi
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
			if(!found) throw new NDSException("参数错误，colorcode="+ colorCode+",不在当前pdtid="+ pdtId+", colors="+ Tools.toString(colors, ","));
		}else{
			if(matrix.getColors().size()!=1) throw new NDSException("参数错误，必须提供colorcode");
			colorPdtId=matrix.getProductIds().get(0);
			ratios=loader.getProductSizeRatios(colorPdtId, usr.getId());
			asis=matrix.getASIArrays().getJSONArray(0);
			
		}
		ratios=loader.clearNoneExistsSizeRatios(ratios, asis);

		AllocationObject aobj;
		//尺码不允许余数，全部下去
		aobj= new AllocationObject(false, AllocationObject.CONVERTION_ROUND );
		JSONArray qtys=aobj.allocateSKU( qty,ratios ,asis);
		
		JSONObject ret=new JSONObject();
		ret.put("qtys", qtys);
		
		return new CmdResult(ret);
	}


	
}
