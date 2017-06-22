package com.agilecontrol.b2bweb.cmd;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;

import com.agilecontrol.b2b.schema.Table;
import com.agilecontrol.b2bweb.WebController;
import com.agilecontrol.b2bweb.sheet.ProductMatrix;
import com.agilecontrol.nea.core.schema.ClientManager;
import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.nea.util.Tools;
import com.agilecontrol.nea.util.Validator;
import com.agilecontrol.phone.Admin;
import com.agilecontrol.phone.CmdHandler;
import com.agilecontrol.phone.CmdResult;
import com.agilecontrol.phone.PhoneController;
import com.agilecontrol.phone.PhoneUtils;

/**
h1. 保存矩阵

h2. 输入

> {cmd:"b2b.cart.savepdt", pdtid, actid, qtys}

*pdtid* - 商品id
*actid* - 活动id，可不提供，表示获取不参加活动的商品矩阵，注意参加活动和不参加活动的商品sku不会合并
*qtys* -  jsonobject {key: value}, key 是"p"+$pdtid+"_"+$asi， value是界面输入的值，
	注意商品表的下单系数若不为1，需要界面输入x下单系数(m_product.packqty)
	若商品表的sizefactor规格系数不为1，需要界面输入x规格系数(m_attributevalue.factor)

h2. 返回

> {code,message}

h2. 处理说明

h3. sql

以下语句用于获取用户针对当前商品可以下单的asi，对应不参加促销活动的，语句为 ad_sql#pdt_asi_list， 参加活动的商品，语句为 pdt_act_asi_list （需要使用$actid 变量），语句的输出格式：

> select xxx asi from xxx where xxx

可以使用的变量: 
*$pdtid* - 商品ID
*$actid* - 活动ID，传人值可能是-1
*$uid* - 当前用户id
*$marketid* - 当前市场id

h3. 更新购物车的语句

> update b_cart i set qty=?, modifieddate=sysdate where i.user_id=? and i.m_product_id=? and i.m_attributesetinstance_id=? and B_PRMT_ID is null
> update b_cart i set qty=?, modifieddate=sysdate where i.user_id=? and i.m_product_id=? and i.m_attributesetinstance_id=? and B_PRMT_ID=?
> insert into b_cart i(id, m_product_id, m_attributesetinstance_id, user_id,ad_client_id, ownerid, modifierid, creationdate,modifieddate,isactive,B_PRMT_ID,qty) values(get_sequences('b_cart'),?, ?,?,?,?,?,sysdate,sysdate,'Y',?,?)


h3. 存储过程

在保存了所有的明细到b_cart表后，对更新的商品（由于是全色码矩阵，可能更新主商品外的其他商品id，就是其他色的商品），将分别调用存储过程

<pre><code class="sql">
//在购物车更新后调用
create or replace procedure b_cart_upd(p_user_id in number, p_pdt_id in number, p_act_id in number)
//p_user_id - 当前用户id
//p_pdt_id - 当前商品id
//p_act_id - 活动id， -1 表示不在活动中的商品

end ;

</code></pe>


 * @author yfzhu
 *
 */
@Admin(mail="yfzhu@lifecycle.cn")
public class SaveSheet extends CmdHandler {
	/**
	 * key: pdtid, value:  HashSet from getAvaiableASIs
	 */
	private HashMap<Integer, HashSet> pdtAsis=new HashMap();
	/**
	 * key:  asi, value: sizeFactor, 来自于尺码系数，默认相同商品，尺码
	 */
	private HashMap<Integer, Integer> asiSizeFactors=new HashMap();
	/**
	 * 验证商品可以下单
	 * @param pdtId
	 * @param actId
	 * @throws Exception
	 */
	protected void checkProductModifiable(int pdtId, int actId ) throws Exception{
		
		
	}
	
	private HashSet<Integer> getAvaiableASIs(int pdtId, int actId) throws Exception{
		
		
		//key: asi 可以下单的asi
		HashSet<Integer> alaiableAsis= pdtAsis.get(pdtId);
		if(alaiableAsis!=null) return alaiableAsis;		
		alaiableAsis=new HashSet();
		//获取可下单的asi, 需要基于当前活动, row: [asi], 与标准矩阵不同的是：不同的活动，不同的市场，商品仍然有不同
		JSONArray alaiableAsiArray;
		vc.put("pdtid", pdtId);
		
		if(actId==-1) alaiableAsiArray=PhoneController.getInstance().getDataArrayByADSQL("pdt_asi_list", vc, conn, false);
		else alaiableAsiArray=PhoneController.getInstance().getDataArrayByADSQL("pdt_act_asi_list", vc, conn, false);
		for(int i=0;i<alaiableAsiArray.length();i++) alaiableAsis.add(alaiableAsiArray.getInt(i));
		
		if(alaiableAsis.size()==0) throw new NDSException("商品不可下单:"+ pdtId);
		
		pdtAsis.put(pdtId,  alaiableAsis);
		
		return alaiableAsis;
	}
	/**
	 * 根据当前主商品返回所有redis对象对应的商品
	 * @param mainPdtId
	 * @return key: pdtid, value redis pdt(all columns)
	 * @throws Exception 需要先用getsheet
	 */
	private HashMap<Integer,JSONObject> fetchAllPdts(int mainPdtId) throws Exception{
		HashMap<Integer,JSONObject> redisPdts=new HashMap();
		Table pdtTable=manager.getTable("pdt");
		
		//下面的方法来自SheetBuilder,故需要先走下单矩阵
		String val= jedis.get("pdt:"+mainPdtId+":sheet");
		ProductMatrix mat=null;
		if(Validator.isNull(val)){
			logger.warn("must getsheet then savesheet since jedis cache for sheet required");
			throw new NDSException("请通过界面矩阵下单");
		}else{
			mat=new ProductMatrix(new JSONObject(val));
		}
		//生成sizeFactors
		ArrayList<Integer> sizeFactors=mat.getSizeFactors();
		JSONArray asis=mat.getASIArrays();
		for(int i=0;i<asis.length();i++){
			//i is for color
			JSONArray row=asis.getJSONArray(i);
			for(int k=0;k<row.length();k++){
				//k is for size
				int asi=row.optInt(k,-1);
				if(asi>0){
					int sizeFactor=sizeFactors.get(k);
					this.asiSizeFactors.put(asi, sizeFactor);
				}
			}
		}
		
		ArrayList<Integer> pdtIds=mat.getProductIds();
		for(int i=0;i< pdtIds.size();i++){
			int pdtId= pdtIds.get(i);
			JSONObject po=PhoneUtils.fetchObjectAllColumns(pdtTable, pdtId, conn, jedis);//需要拿到packqty，mask=00
			//省了下面的，不需要
//			WebController.getInstance().replacePdtValues(po, usr.getLangId(), usr.getMarketId(), vc, jedis, conn);
			redisPdts.put(pdtId, po);
		}
		return redisPdts;
	}
	/**
	 * @param jo {pdtid, actid, qtys}
	 */
	public CmdResult execute(JSONObject jo) throws Exception {
		
		int mainPdtId=this.getInt(jo, "pdtid");
		Table pdtTable=manager.getTable("pdt");
		//JSONObject pdt=PhoneUtils.fetchObjectAllColumns(pdtTable, mainPdtId, vc, conn, jedis);// 需要packqty, mask=00
		
		//key: pdtid, value: redisPdt(all columns)
		HashMap<Integer,JSONObject> redisPdts=fetchAllPdts(mainPdtId);
		int actId=jo.optInt( "actid",-1);
		//key 是"p"+$pdtid+"_"+$asi， value是界面输入的值
		JSONObject qtys=this.getObject(jo, "qtys");

		vc.put("actid", actId);
		vc.put("uid", usr.getId());
		vc.put("marketid", usr.getMarketId());
		
		//验证商品可下单
		checkProductModifiable(mainPdtId, actId);
		
		int clientId=ClientManager.getInstance().getDefaultClientId();
		//更新购物车
		PreparedStatement pstmt = null;
		PreparedStatement pstmt2 = null;
		
		HashSet<Integer> pdtIds=new HashSet();
		try{		
			if(actId==-1)
				pstmt = conn.prepareStatement("update b_cart i set qty=?, price_list=null, price=null, modifieddate=sysdate where i.user_id=? and i.m_product_id=? and i.m_attributesetinstance_id=? and B_PRMT_ID is null");
			else
				pstmt = conn.prepareStatement("update b_cart i set qty=?, price_list=null, price=null,modifieddate=sysdate where i.user_id=? and i.m_product_id=? and i.m_attributesetinstance_id=? and B_PRMT_ID=?");
			
			
			pstmt2 = conn.prepareStatement("insert into b_cart i(id, m_product_id, m_attributesetinstance_id, " +
							"user_id,ad_client_id, ownerid, modifierid, creationdate,modifieddate,isactive,B_PRMT_ID,qty,b_market_id)"
							+ " values(get_sequences('b_cart'),?, ?,?,?,?,?,sysdate,sysdate,'Y',?,?,?)");
			
			Pattern pattern=Pattern.compile("p(\\d+)_(\\d+)");
			
			for(Iterator it=qtys.keys();it.hasNext();){
				String key=(String)it.next();
				if(!key.startsWith("p")) continue;
				Matcher matcher=pattern.matcher(key);
				if(!matcher.find()) {
					logger.debug("not a valid key:"+ key);
					continue;
				}
				int pdtId=Tools.getInt( matcher.group(1),-1);
				int asi=Tools.getInt( matcher.group(2),-1);
				if(pdtId==-1 || asi==-1) {
					logger.debug("not a valid key(pdtid or asi not found):"+ key);
					continue;
				}
				HashSet<Integer> asis= getAvaiableASIs(pdtId, actId);
				if(!asis.contains(asi)){
					throw new NDSException("活动已变更，请刷新界面后再试");
				}
				
				JSONObject redisPdt=redisPdts.get(pdtId);//all columns
				/*
				 * 这是木槿项目开始的下单倍数，比如单价是按支定义的，界面上中包规格是14，则下单1即为14，2就是28，数据库存储仍然按支数
				 */
				int packQty=redisPdt.optInt("packqty", 1);
				if(packQty<=0)packQty=1;
				
				int sizeFactor= asiSizeFactors.get(asi);
				
				pdtIds.add(pdtId);
				int qty= qtys.getInt(key)*packQty*sizeFactor;
				
				pstmt.setInt(1, qty);
				pstmt.setLong(2, usr.getId());
				pstmt.setInt(3, pdtId);
				pstmt.setInt(4, asi);
				if(actId>0) pstmt.setInt(5, actId);
				
				int cnt=pstmt.executeUpdate();
				if(cnt==0){
					//insert
					pstmt2.setInt(1, pdtId);
					pstmt2.setInt(2, asi);
					pstmt2.setLong(3, usr.getId());
					pstmt2.setInt(4, clientId);
					pstmt2.setLong(5, usr.getId());
					pstmt2.setLong(6, usr.getId());
					if(actId>0) pstmt2.setInt(7, actId);
					else pstmt2.setNull(7, java.sql.Types.INTEGER);
					pstmt2.setInt(8, qty);
					pstmt2.setInt(9, usr.getMarketId());
					cnt=pstmt2.executeUpdate();
					if(cnt!=1) throw new NDSException("意外，更新购物车返回值不是1:"+ cnt);
				}
				
			}
			for(Integer pid: pdtIds){
				ArrayList params=new ArrayList();
				params.add(usr.getId());
				params.add(pid);
				params.add(actId);
				engine.executeStoredProcedure("b_cart_upd", params, false, conn);
				JSONObject state=this.getCartPdtState(pid);//{c,m} c-code, m-message
				if(state!=null) throw new NDSException(state.getString("m"));
			}
		}finally {
			try {if (pstmt != null)pstmt.close();} catch (Throwable e) {}
			try {if (pstmt2 != null)pstmt2.close();} catch (Throwable e) {}
		}
		
		return CmdResult.SUCCESS;
	}

}













