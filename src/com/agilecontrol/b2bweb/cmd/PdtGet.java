package com.agilecontrol.b2bweb.cmd;

import java.util.Locale;

import org.apache.commons.lang.StringUtils;
import org.apache.velocity.VelocityContext;
import org.json.JSONArray;
import org.json.JSONObject;

import com.agilecontrol.b2b.cmd.ObjectGet;
import com.agilecontrol.b2b.cmd.Search;
import com.agilecontrol.b2b.schema.Table;
import com.agilecontrol.b2bweb.WebController;
import com.agilecontrol.nea.core.util.MessagesHolder;
import com.agilecontrol.nea.core.velocity.VelocityUtils;
import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.nea.util.ObjectNotFoundException;
import com.agilecontrol.nea.util.Tools;
import com.agilecontrol.nea.util.Validator;
import com.agilecontrol.phone.Admin;
import com.agilecontrol.phone.CmdHandler;
import com.agilecontrol.phone.CmdResult;
import com.agilecontrol.phone.LanguageManager;
import com.agilecontrol.phone.PhoneController;
import com.agilecontrol.phone.PhoneUtils;

/**
 * h1. ��Ʒ�������
 * 
 * h2. ����
 * 
 * ��Ʒ����,��ȡ��Ʒ����
 * 
 * h2. ����
 * 
 * > {cmd:"b2b.pdt.get",id }
 * 
 * id* - int pdt.id
 * 
 * h2. ���
 * 
 * > {pdtid, note,no,mainpic,price,tags,allpic,isfav,dtls}
 * 
 * pdtid* - int pdtid note* - string ��Ʒ��ע no* - string ��Ʒ��� mainpic* -string
 * ��ƷͼƬ price* - double ��ʾ�ļ۸� tags* - jsonarray of string ��Ʒ��ǩ����: ["����","������ͬ��"]
 * allpic* - jsonarray of string ��ƷͼƬ, ������b_img/b_pdt_img isfav* -1/0 ״̬
 * �Ƿ��ղأ������� B_FAVOURITE dtls* - string ��Ʒ����, ���� M_PRODUCT_TRANS
 * 
 * @author yfzhu
 *
 */
@Admin(mail = "yfzhu@lifecycle.cn")
public class PdtGet extends ObjectGet {

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.agilecontrol.b2b.cmd.ObjectGet#execute(org.json.JSONObject)
	 */
	/*
	 * (non-Javadoc)
	 * 
	 * @see com.agilecontrol.b2b.cmd.ObjectGet#execute(org.json.JSONObject)
	 */
	public CmdResult execute(JSONObject jo) throws Exception {
		String pdtIdOrName=jo.optString("id",null);
		if(pdtIdOrName==null) throw new NDSException("��Ҫid����");
		long objectId=-1;
		JSONObject obj =null;
		if(StringUtils.isNumeric(pdtIdOrName)){
			objectId=Tools.getInt(pdtIdOrName, -1);
			try{
				obj = fetchObject(objectId, "pdt",false);
			}catch(Throwable tx){
				logger.debug("Fail to find pdt.id="+ objectId+":"+ tx.getMessage());
				objectId=-1;
			}
		}
		if(objectId==-1){
			//�п����ǻ��ţ���: pdt.name
		/**
		 * ���ô洢����
		 */
			//engine.executeStoredProcedure('', params, hasReturnValue)
			objectId=engine.doQueryInt("select id from m_product where name=?", new Object[]{pdtIdOrName}, conn);
			obj = fetchObject(objectId, "pdt",false);
		}
		if(objectId==-1) throw new ObjectNotFoundException("��Ʒδ�ҵ�(Not Found)");
		//������Ʒ�����Ϣ�����������ڣ�mainpic, price��
		JSONObject objTrans=WebController.getInstance().replacePdtValues(obj, usr.getLangId(), usr.getMarketId(), vc, jedis, conn);
		
	
		//����pprice
		vc.put("pdtid", objectId);
		vc.put("marketid", usr.getMarketId());
		vc.put("uid", usr.getId());
//Ǩ�Ƶ�PdtActs������
		//select ����� name, �۸� pprice, ��ȡ��ֹʱ�� enddate, �id
//		JSONArray rows= PhoneController.getInstance().getDataArrayByADSQL("pdt_act_list", vc, conn, true/*return obj*/);
//		obj.put("acts", rows);
		
		
		//װ��allpic,isFav,dtls
		//allpic ������ pdt:$id ������: allpic�����û�ж��壬��Ҫ�����ݿ��м���
		String images=jedis.hget("pdt:"+ objectId, "images");
		if(Validator.isNull(images)){
			JSONArray imgs=PhoneController.getInstance().getDataArrayByADSQL("pdt_img_list", vc, conn, false);
			images= imgs.toString();
			jedis.hset("pdt:"+ objectId, "images",images);
		}
		JSONArray photos=new JSONArray(images);
		obj.put("allpic", photos);
		
		/**
		 * B_FAVOURITE �ղر�
		 */
		
		int fav=engine.doQueryInt("select count(*) from B_FAVOURITE where user_id=? and m_product_id=? and isactive='Y' and rownum<2", new Object[]{usr.getId(), objectId}, conn);
		obj.put("isfav", fav>0?1:0);
		
		//dtls, ��objTrans����
		if(objTrans!=null){
			String dtls=objTrans.optString("dtls");
			if(Validator.isNotNull(dtls)){
				obj.put("dtls", dtls);
			}
			
		}
		
		postAction(obj);
		CmdResult res=new CmdResult(obj );
		return res;
		
	}

	/**
	 * ��ad_sql#b2b:pdt:summary�����õ���ʾ�ֶμ������ݶ�����
	 * 
	 * @param retObj
	 * @throws Exception
	 */
	protected void postAction(JSONObject retObj) throws Exception {
		long pdtid = retObj.getLong("id");
		String pdtSummary = PhoneController.getInstance().getValueFromADSQL("b2b:pdt:summary", conn);
		if (Validator.isNull(pdtSummary)) {
			return;
		}
		JSONObject confObj = new JSONObject(pdtSummary);
		JSONArray sumconf = confObj.getJSONArray("props");
		JSONArray extlist = new JSONArray();
		String extSqlName = confObj.optString("ext", "");

		VelocityContext vc = VelocityUtils.createContext();
		vc.put("conn", conn);
		vc.put("c", this);
		vc.put("userid", usr.getId());
		vc.put("username", usr.getName());
		vc.put("longid", usr.getLangId());
		vc.put("pdtid", pdtid);
		if (Validator.isNotNull(extSqlName)) {
			String extsql = PhoneController.getInstance().getValueFromADSQL(extSqlName, conn);
			if (Validator.isNotNull(extsql)) {
				extlist = PhoneController.getInstance().getDataArrayByADSQL(extSqlName, vc, conn, true);
				if (extlist == null) {
					extlist = new JSONArray();
				}
			}
		}
		Locale newLocale = LanguageManager.getInstance().getLocale(usr.getLangId());
		JSONArray pdtSlist = new JSONArray();
		for (int i = 0; i < sumconf.length(); i++) {
			JSONObject sobj = new JSONObject();
			JSONObject editObj = sumconf.getJSONObject(i);
			String colname = editObj.getString("name");

			String coltext = editObj.optString("text");
			boolean colflag = true;
			if (Validator.isNotNull(coltext)) {
				colflag = false;
				coltext = MessagesHolder.getInstance().translateMessage(coltext, newLocale);
			} else {
				if (manager.getTable("pdt").getColumn(colname) != null) {
					String realName = manager.getTable("pdt").getColumn(colname).getRealName();
					coltext = com.agilecontrol.nea.core.schema.TableManager.getInstance()
							.getColumn("m_product", realName).getDescription(newLocale);
				}
			}
			Object colvalue = retObj.opt(colname);
			for (int j = 0; j < extlist.length(); j++) {
				JSONObject extObj = extlist.getJSONObject(j);
				String extitle = extObj.getString("title");
				String extvalue = extObj.getString("value");
				if (extitle.equals(colname)) {
					if (colflag) {
						coltext = MessagesHolder.getInstance().translateMessage("@" + extitle + "@", this.locale);
					}
					colvalue = extvalue;
					break;
				}
			}

			sobj.put("text", coltext);
			sobj.put("value", colvalue);
			pdtSlist.put(sobj);
		}

		retObj.put("summary", pdtSlist);
	}

}
