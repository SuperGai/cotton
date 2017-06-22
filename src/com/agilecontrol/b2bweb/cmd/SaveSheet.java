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
h1. �������

h2. ����

> {cmd:"b2b.cart.savepdt", pdtid, actid, qtys}

*pdtid* - ��Ʒid
*actid* - �id���ɲ��ṩ����ʾ��ȡ���μӻ����Ʒ����ע��μӻ�Ͳ��μӻ����Ʒsku����ϲ�
*qtys* -  jsonobject {key: value}, key ��"p"+$pdtid+"_"+$asi�� value�ǽ��������ֵ��
	ע����Ʒ����µ�ϵ������Ϊ1����Ҫ��������x�µ�ϵ��(m_product.packqty)
	����Ʒ���sizefactor���ϵ����Ϊ1����Ҫ��������x���ϵ��(m_attributevalue.factor)

h2. ����

> {code,message}

h2. ����˵��

h3. sql

����������ڻ�ȡ�û���Ե�ǰ��Ʒ�����µ���asi����Ӧ���μӴ�����ģ����Ϊ ad_sql#pdt_asi_list�� �μӻ����Ʒ�����Ϊ pdt_act_asi_list ����Ҫʹ��$actid �����������������ʽ��

> select xxx asi from xxx where xxx

����ʹ�õı���: 
*$pdtid* - ��ƷID
*$actid* - �ID������ֵ������-1
*$uid* - ��ǰ�û�id
*$marketid* - ��ǰ�г�id

h3. ���¹��ﳵ�����

> update b_cart i set qty=?, modifieddate=sysdate where i.user_id=? and i.m_product_id=? and i.m_attributesetinstance_id=? and B_PRMT_ID is null
> update b_cart i set qty=?, modifieddate=sysdate where i.user_id=? and i.m_product_id=? and i.m_attributesetinstance_id=? and B_PRMT_ID=?
> insert into b_cart i(id, m_product_id, m_attributesetinstance_id, user_id,ad_client_id, ownerid, modifierid, creationdate,modifieddate,isactive,B_PRMT_ID,qty) values(get_sequences('b_cart'),?, ?,?,?,?,?,sysdate,sysdate,'Y',?,?)


h3. �洢����

�ڱ��������е���ϸ��b_cart��󣬶Ը��µ���Ʒ��������ȫɫ����󣬿��ܸ�������Ʒ���������Ʒid����������ɫ����Ʒ�������ֱ���ô洢����

<pre><code class="sql">
//�ڹ��ﳵ���º����
create or replace procedure b_cart_upd(p_user_id in number, p_pdt_id in number, p_act_id in number)
//p_user_id - ��ǰ�û�id
//p_pdt_id - ��ǰ��Ʒid
//p_act_id - �id�� -1 ��ʾ���ڻ�е���Ʒ

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
	 * key:  asi, value: sizeFactor, �����ڳ���ϵ����Ĭ����ͬ��Ʒ������
	 */
	private HashMap<Integer, Integer> asiSizeFactors=new HashMap();
	/**
	 * ��֤��Ʒ�����µ�
	 * @param pdtId
	 * @param actId
	 * @throws Exception
	 */
	protected void checkProductModifiable(int pdtId, int actId ) throws Exception{
		
		
	}
	
	private HashSet<Integer> getAvaiableASIs(int pdtId, int actId) throws Exception{
		
		
		//key: asi �����µ���asi
		HashSet<Integer> alaiableAsis= pdtAsis.get(pdtId);
		if(alaiableAsis!=null) return alaiableAsis;		
		alaiableAsis=new HashSet();
		//��ȡ���µ���asi, ��Ҫ���ڵ�ǰ�, row: [asi], ���׼����ͬ���ǣ���ͬ�Ļ����ͬ���г�����Ʒ��Ȼ�в�ͬ
		JSONArray alaiableAsiArray;
		vc.put("pdtid", pdtId);
		
		if(actId==-1) alaiableAsiArray=PhoneController.getInstance().getDataArrayByADSQL("pdt_asi_list", vc, conn, false);
		else alaiableAsiArray=PhoneController.getInstance().getDataArrayByADSQL("pdt_act_asi_list", vc, conn, false);
		for(int i=0;i<alaiableAsiArray.length();i++) alaiableAsis.add(alaiableAsiArray.getInt(i));
		
		if(alaiableAsis.size()==0) throw new NDSException("��Ʒ�����µ�:"+ pdtId);
		
		pdtAsis.put(pdtId,  alaiableAsis);
		
		return alaiableAsis;
	}
	/**
	 * ���ݵ�ǰ����Ʒ��������redis�����Ӧ����Ʒ
	 * @param mainPdtId
	 * @return key: pdtid, value redis pdt(all columns)
	 * @throws Exception ��Ҫ����getsheet
	 */
	private HashMap<Integer,JSONObject> fetchAllPdts(int mainPdtId) throws Exception{
		HashMap<Integer,JSONObject> redisPdts=new HashMap();
		Table pdtTable=manager.getTable("pdt");
		
		//����ķ�������SheetBuilder,����Ҫ�����µ�����
		String val= jedis.get("pdt:"+mainPdtId+":sheet");
		ProductMatrix mat=null;
		if(Validator.isNull(val)){
			logger.warn("must getsheet then savesheet since jedis cache for sheet required");
			throw new NDSException("��ͨ����������µ�");
		}else{
			mat=new ProductMatrix(new JSONObject(val));
		}
		//����sizeFactors
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
			JSONObject po=PhoneUtils.fetchObjectAllColumns(pdtTable, pdtId, conn, jedis);//��Ҫ�õ�packqty��mask=00
			//ʡ������ģ�����Ҫ
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
		//JSONObject pdt=PhoneUtils.fetchObjectAllColumns(pdtTable, mainPdtId, vc, conn, jedis);// ��Ҫpackqty, mask=00
		
		//key: pdtid, value: redisPdt(all columns)
		HashMap<Integer,JSONObject> redisPdts=fetchAllPdts(mainPdtId);
		int actId=jo.optInt( "actid",-1);
		//key ��"p"+$pdtid+"_"+$asi�� value�ǽ��������ֵ
		JSONObject qtys=this.getObject(jo, "qtys");

		vc.put("actid", actId);
		vc.put("uid", usr.getId());
		vc.put("marketid", usr.getMarketId());
		
		//��֤��Ʒ���µ�
		checkProductModifiable(mainPdtId, actId);
		
		int clientId=ClientManager.getInstance().getDefaultClientId();
		//���¹��ﳵ
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
					throw new NDSException("��ѱ������ˢ�½��������");
				}
				
				JSONObject redisPdt=redisPdts.get(pdtId);//all columns
				/*
				 * ����ľ����Ŀ��ʼ���µ����������絥���ǰ�֧����ģ��������а������14�����µ�1��Ϊ14��2����28�����ݿ�洢��Ȼ��֧��
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
					if(cnt!=1) throw new NDSException("���⣬���¹��ﳵ����ֵ����1:"+ cnt);
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













