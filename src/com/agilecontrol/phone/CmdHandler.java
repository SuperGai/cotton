package com.agilecontrol.phone;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.sql.Connection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.apache.velocity.VelocityContext;
import org.json.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Transaction;

import com.agilecontrol.b2b.query.SearchRequest;
import com.agilecontrol.b2b.query.SearchResult;
import com.agilecontrol.b2b.schema.Column;
import com.agilecontrol.b2b.schema.RefByTable;
import com.agilecontrol.b2b.schema.Table;
import com.agilecontrol.b2b.schema.TableManager;
import com.agilecontrol.b2b.schema.TagColumn;
import com.agilecontrol.nea.core.control.ejb.DefaultWebEventHelper;
import com.agilecontrol.nea.core.control.event.DefaultWebEvent;
import com.agilecontrol.nea.core.control.web.UserWebImpl;
import com.agilecontrol.nea.core.control.web.WebUtils;
import com.agilecontrol.nea.core.query.ColumnLink;
import com.agilecontrol.nea.core.query.Expression;
import com.agilecontrol.nea.core.query.QueryEngine;
import com.agilecontrol.nea.core.query.QueryException;
import com.agilecontrol.nea.core.query.QueryUtils;
import com.agilecontrol.phone.UserObj;
import com.agilecontrol.nea.core.security.DESUtil;
import com.agilecontrol.nea.core.security.User;
import com.agilecontrol.nea.core.util.ConfigValues;
import com.agilecontrol.nea.core.velocity.VelocityUtils;
import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.nea.util.ObjectNotFoundException;
import com.agilecontrol.nea.util.StringUtils;
import com.agilecontrol.nea.util.Tools;
import com.agilecontrol.nea.util.Validator;

/**
 * 
 * MiscCmd ��ʵ������ÿ���������һ������������request����ģ�����Command��application�����
 * 
 * @author yfzhu
 *
 */
@Admin(mail="sun.yifan@lifecycle.cn")
public abstract class CmdHandler {

	protected Logger logger = LoggerFactory.getLogger(getClass());

	protected UserObj usr;
	/**
	 * �����jedis���ӣ���connһ������Ҫ�����ر�
	 */
	protected Jedis jedis;

	protected DefaultWebEvent event;
	protected Connection conn;
	protected QueryEngine engine;
	protected TableManager manager;
	protected String phone;
	protected VelocityContext vc;
	protected DefaultWebEventHelper helper;
	protected String rootwar;/*
								 * E:\portal5\jboss\server\default\deploy\ROOT.war
								 */
	protected String serverURL;/* http://192.168.1.158:8080 */
	protected Locale locale;
	
	/**
	 * ���Խ���jo�е�keyΪlong�����key��jsonobjҲ���ԣ���Ĭ�϶�ȡobj��id key��Ӧ��ֵ
	 * @param jo ���洫�˵Ķ���
	 * @param key �����keyֵ��Ӧlong
	 * @return -1 ���û���ҵ�
	 * @throws Exception
	 */
	protected long optLong(JSONObject jo, String key) throws Exception{
		Object value=jo.opt(key);
		if(value==null || value==JSONObject.NULL) return -1;
		if(value instanceof JSONObject){
			return ((JSONObject)value).optLong(key, -1);
		}else if (value instanceof Number){
			return ((Number)value).longValue();
		}else if (value instanceof String){
			return Tools.getLong(value, -1);
		}else{
			logger.debug("��֧�ֵ�����:"+value.getClass().getName());
			return -1;
		}
	}
	/**
	 * ������Ϣ������ID���������������۵�ת�ɹ�����
	 * @param obj spo
	 * @throws Exception
	 */
	protected String spoPaid(JSONObject obj) throws Exception{
		if("WP".equals(obj.optString("pstate"))) throw new NDSException("�踶����ܷ���Ϣ");
		String otype = obj.optString("otype","");
		long o_com_id = obj.getLong("o_com_id");
		long i_com_id = obj.optLong("i_com_id",-1);
		long custId= obj.optLong("cust_id", -1);
		long uid = obj.optLong("cusr");
		long o_emp_id = obj.getLong("emp_id");
		boolean flag = false;
		if ("INL".equals(otype) && i_com_id!=-1) {
			if(!checkPackage(i_com_id, true)){
				return "";
			}else{
				flag=true;
			}
		}else{
			uid = usr.getId();
		}
		JSONObject cust=PhoneUtils.getRedisObj("cust", custId, conn, jedis);
		long custEmpId= cust.optLong("my_emp_id", -1);
		long custComId= cust.optLong("my_com_id", -1);
		if(custComId<=0) return "�ͻ������̼�δ����";
		
		JSONObject custCom=PhoneUtils.getRedisObj("com", custComId, conn, jedis);
		if(custEmpId<=0){
			//looking for boss
			long bossUsrId=custCom.getLong("u_id");
			custEmpId=PhoneUtils.findEmpId(custComId, bossUsrId, vc, conn, jedis);			
		}
		if(custEmpId<=0) return "δ�ҵ��Է�������Ա";
		
		
		JSONObject com=PhoneUtils.getRedisObj("com", o_com_id, conn, jedis);
		vc.put("com",  com);
		
		JSONObject spo=PhoneUtils.getRedisObj("spo", obj.getLong("id"), conn, jedis);
		vc.put("docno", spo.getString("docno"));
		
		JSONObject ext=new JSONObject().put("spo_id", obj.getLong("id")).put("com_id", o_com_id).put("u_id", uid).put("type", "po").put("cmd", "PoAdd");
		YXController.getInstance().YXSendMsg(PhoneConfig.YUNXIN_SECRETARY, custEmpId, "yunxin:spo_paid", ext, vc, conn, jedis);
		
		//֪ͨ�������˸���
		if (flag) {
			JSONObject i_com=PhoneUtils.getRedisObj("com", i_com_id, conn, jedis);
			vc.put("i_com",  i_com);
			YXController.getInstance().YXSendMsg(PhoneConfig.YUNXIN_SECRETARY, o_emp_id, "yunxin:spo_paid_online", null, vc, conn, jedis);
		}
				
		return "�ѷ�����Ϣ���Է�";
	}
	
	


	/**
	 * ��ȡJOIN_URL, {0} �滻Ϊquerystr, querystr �ĸ�ʽ:
	 * DESUtil.encrypt($uid+","+$comid) ע�����encrypt�������urlsafe��
	 * 
	 * @return ȫ��ַ, ����û���ǰ������Ч����ʹ���ߣ�������Ч
	 * @throws Exception
	 */
	protected String getJoinURL() throws Exception {
		String param = String.valueOf(usr.getId()) + "," + String.valueOf(usr.getComId());

		String query = DESUtil.encrypt(param, ConfigValues.SECURITY_SIMPLE_ENCRYPT_KEY);
		return PhoneConfig.JOIN_URL + query;
	}
	
	/**
	 * ����usrId+comId����JOIN_URL��ַ
	 * @param usrId
	 * @param comId
	 * @return
	 * @throws Exception
	 */
	protected String getJoinURL(Long usrId,Long comId) throws Exception{
		String param=usrId+","+ comId;
		String query= DESUtil.encrypt(param, ConfigValues.SECURITY_SIMPLE_ENCRYPT_KEY);
		return PhoneConfig.JOIN_URL+query;
	}

	/**
	 * ��������redis����Ķ�����Ҫ����TagColumnת�䣬��ΪTagColumn������Ҫ����JSONArray,
	 * ������jedis/db�д洢��string
	 * 
	 * @param editObj
	 * @param table
	 *            ������һ��
	 * @return editObj
	 * @throws Exception
	 */
	protected JSONObject reviseColumnsOfJSONTypeValue(JSONObject editObj, Table table) throws Exception {
		ArrayList<Column> cols = table.getColumnsOfJSONType();
		for (Column tcol : cols) {
			Object cv = editObj.opt(tcol.getName());
			boolean isJa = (boolean) tcol.getJSONProp("ja", false);
			boolean isJo = (boolean) tcol.getJSONProp("jo", false);
			if (cv instanceof String) {
				Object colValue = null;
				if (Validator.isNull((String) cv)) {
					if (isJo) {
						colValue = new JSONObject();
					} else if (isJa) {
						colValue = new JSONArray();
					} else {
						throw new NDSException("�ֶ���չ���Զ���������ֻ֧��  ja ���� jo");
					}
				} else {
					if (isJo) {
						colValue = new JSONObject((String) cv);
					} else if (isJa) {
						colValue = new JSONArray((String) cv);
					} else {
						throw new NDSException("�ֶ���չ���Զ���������ֻ֧��  ja ���� jo");
					}

				}
				editObj.put(tcol.getName(), colValue);

			}

		}
		return editObj;
	}

	/**
	 * ���е��ֶ�����������չ����: {tagtable: { tb:"ptag", tag_column:"tag_id",
	 * main_column:"pdt_id" }} ����ֶε�������ʽ: [{id,name}]������ id ��Ϊ�գ���ʾΪ�½�
	 * ��Ҫһ���潫�½���tag���½���id,����һ���棬��Ҫ��tag���ݲ�ɢ��tbָ����table��
	 * 
	 * @param objectId
	 *            ��ǰ�����ع��ļ�¼��id��ע���������������������insert���ǰ
	 * @param table
	 *            take as pdt
	 * @param editObj
	 *            �ں� tagtable column
	 */
	protected void reviseTagColumnValue(long objectId, Table table, JSONObject editObj) throws Exception {
		ArrayList<TagColumn> cols = table.getTagColumns();
		for (TagColumn tcol : cols) {
			Object cv = editObj.opt(tcol.getName());
			JSONArray colValue = null;
			if (cv instanceof JSONArray) {
				colValue = (JSONArray) cv;
			} else if (cv instanceof String) {
				if(((String) cv).trim().startsWith("["))colValue = new JSONArray((String) cv);
				else if(Validator.isNull((String)cv)) colValue=new JSONArray();
				else throw new NDSException("˵�õĴ�JSONArray, ������:"+ cv);
			} else {
				logger.debug("unsupported colum value for tagcolumn:" + cv);
			}
			//Ϊ�ձ�ʾ�ͻ���û�д������������[] ��ʾҪ��գ�����[] ����Ҫ��delete ������
			if (colValue == null )
				continue;
			// ά����ptag��
			engine.executeUpdate("delete from " + tcol.getTagTable() + " where " + tcol.getTagMainColumn() + " =?",
					new Object[] { objectId }, conn);
			if(colValue.length()==0){
				//����Ҫɾ��col�����ݣ������������ݿ��������[], ������Ҫָ��is null�Ĳ�ѯ��������Ҫ����editObj������StringBuilder.class��ȥ
				editObj.put(tcol.getName(), StringBuilder.class);
			}else 
				for (int i = 0; i < colValue.length(); i++) {
				JSONObject tagObj = colValue.getJSONObject(i);
				long tagId = tagObj.optLong("id", -1);
				if (tagId <= 0) {
					// û�ҵ���2�ֿ��ܣ��ͻ��˴�����ʱ�������ͻ����Ѿ����������Ϊ�¿ͻ���, �޸� pdt_tag��
					tagId = getTagIdByName(tcol.getTagStoreTable(), getString(tagObj, "name"), true);
					tagObj.put("id", tagId);
				}
				// insert ptag
				engine.executeUpdate("insert into " + tcol.getTagTable() + "(id," + tcol.getTagColumn() + ","
						+ tcol.getTagMainColumn() + ", orderno) values (seq_" + tcol.getTagTable() + ".nextval"
						+ ",?,?,?)", new Object[] { tagId, objectId, i + 1 }, conn);

			}
		}
	}

	/**
	 * ���ݱ�ǩ�����ҵ���Ӧid�����û�У�����-1�򴴽��µı�ǩ���ڵ�ǰ�̼ң� ��redis�д洢�ṹ��
	 * 
	 * mj:pdt_tag - hash key: name, value: id mj:pdt_tag:list - list, pdt_tag.id
	 * com:$comid:pdt_tag - hash key: name, value:id com:$comid:pdt_tag:list -
	 * list, pdt_tag.id
	 * 
	 * ά��: ��� mj:pdt_tag:list �����ڣ������ݿ��һ�飬��� com:$comid:pdt_tag:lis
	 * �����ڣ����ȶ�ȡһ�飬Ȼ��ʼ����, ����������
	 * 
	 * @param table
	 *            ��ǩ�洢������pdt_tag
	 * @param tagName
	 *            ��ǩ��
	 * @param create
	 *            �Ƿ񴴽��¼�¼�����û���ҵ��Ļ�����ƽ̨�����tagName������������⣬��ֻҪ��com�����ҾͿ�����
	 * @return -1 ��ָ����ǩ��id��-1����create=false�������
	 * @throws Exception
	 */
	protected long getTagIdByName(String table, String tagName, boolean create) throws Exception {
		getTagList(table, true);
		long id = Tools.getLong(jedis.hget("com:" + usr.getComId() + ":" + table, tagName), -1);
		if (id < 0 && create) {
			id = PhoneController.getInstance().getNextId(table, conn);
			engine.executeUpdate("insert into " + table + "(id,name,com_id,is_hq,cusr,musr) values(?,?,?,'N',?,?)",
					new Object[] { id, tagName, usr.getComId(), usr.getId(), usr.getId() }, conn);
			// update redis, hash & list
			String listKey = "com:" + usr.getComId() + ":" + table + ":list";
			boolean isString = "string".equalsIgnoreCase(jedis.type(listKey));
			Transaction ts = jedis.multi();
			jedis.hset("com:" + usr.getComId() + ":" + table, tagName, String.valueOf(id));
			if (isString)
				jedis.del(listKey);
			jedis.lpush(listKey, String.valueOf(id));
			ts.exec();
		}
		return id;
	}

	/**
	 * �����Լ�������õ�tag id�б�����ƽ̨�����̼Ҽ� ��redis�д洢�ṹ��
	 * 
	 * mj:pdt_tag - hash key: name, value: id mj:pdt_tag_s - list, pdt_tag.id
	 * com:$comid:pdt_tag - hash key: name, value:id com:$comid:pdt_tag_s -
	 * list, pdt_tag.id
	 * 
	 * ά��: ��� mj:pdt_tag_s �����ڣ������ݿ��һ�飬��� com:$comid:pdt_tag_s
	 * �����ڣ����ȶ�ȡһ�飬Ȼ��ʼ����, ����������
	 *
	 * @param table
	 *            ָ���ı�ǩ���� pdt_tag, sup_tag
	 * @param checkOnly
	 *            ����Ҫ�������ݣ�ֻҪȷ��redis�ڴ�������
	 * @return id list of tag
	 * @throws Exception
	 */
	protected ArrayList<Long> getTagList(String table, boolean checkOnly) throws Exception {
		// mj:<table>
		ArrayList<Long> retIds = new ArrayList();
		// Transaction trans=jedis.multi();
		String listKey = "mj:" + table + "_s";
		String type = jedis.type(listKey);
		if ("none".equals(type)) {
			// δ���ع�
			JSONArray list = engine.doQueryObjectArray(
					"select id,name from " + table + " where com_id is null order by id asc", new Object[] {}, conn);
			if (list.length() > 0) {
				String[] ids = new String[list.length()];
				HashMap<String, String> tags = new HashMap();// key: name,
																// value: id
				for (int i = 0; i < list.length(); i++) {
					JSONObject one = list.getJSONObject(i);
					long idLong = one.getLong("id");
					String id = String.valueOf(idLong);
					ids[i] = id;
					tags.put(one.getString("name"), id);

					if (!checkOnly) {
						retIds.add(idLong);
					}
				}

				jedis.hmset("mj:" + table, tags);
				jedis.lpush(listKey, ids);

			} else {
				jedis.set(listKey, "");// ���ˣ���û��ֵ
			}
		} else if ("string".equals(type)) {
			// ���ˣ���û��ֵ

		} else if ("list".equals(type)) {
			List<String> list = jedis.lrange(listKey, 0, jedis.llen(listKey));
			if (!checkOnly)
				for (String one : list)
					retIds.add(Tools.getLong(one, -1));
		} else
			throw new NDSException("�������������:" + type + ",key=" + "mj:" + table + "_s");

		// com:$comid:pdt_tag - hash key: name, value:id
		// com:$comid:pdt_tag:list - list, pdt_tag.id

		listKey = "com:" + usr.getComId() + ":" + table + "_s";
		type = jedis.type(listKey);
		if ("none".equals(type)) {
			// δ���ع�
			JSONArray list = engine.doQueryObjectArray(
					"select id,name from " + table + " where com_id=? order by id asc", new Object[] { usr.getComId() },
					conn);
			if (list.length() > 0) {
				String[] ids = new String[list.length()];
				HashMap<String, String> tags = new HashMap();// key: name,
																// value: id
				for (int i = 0; i < list.length(); i++) {
					JSONObject one = list.getJSONObject(i);
					long idLong = one.getLong("id");
					String id = String.valueOf(idLong);
					ids[i] = id;
					tags.put(one.getString("name"), id);

					if (!checkOnly) {
						retIds.add(idLong);
					}
				}

				jedis.hmset("com:" + usr.getComId() + ":" + table, tags);
				jedis.lpush(listKey, ids);

			} else {
				jedis.set(listKey, "");// ���ˣ���û��ֵ
			}
		} else if ("string".equals(type)) {
			// ���ˣ���û��ֵ

		} else if ("list".equals(type)) {
			List<String> list = jedis.lrange(listKey, 0, jedis.llen(listKey));
			if (!checkOnly)
				for (String one : list)
					retIds.add(Tools.getLong(one, -1));
		} else
			throw new NDSException("�������������:" + type + ",key=" + "mj:" + table + "_s");

		// trans.exec();
		return retIds;
	}

	/**
	 * ����comid��usrid �ҵ�empId,����yunxid
	 * 
	 * @param usrId
	 * @param comId
	 * @throws Exception
	 */
	protected void setupYXId(long usrId, long comId) throws Exception {
		long empId = Tools.getLong(
				engine.doQueryOne("select id from emp where u_id=? and com_id=?", new Object[] { usrId, comId }, conn),
				-1);
		if (empId == -1)
			throw new NDSException("Ա��δ����");
		setupYXId(empId);
	}

	/**
	 * ����yunxid������empId ��������Ϣ�����ţ����봴���û�
	 * 
	 * @param empId
	 * @throws Exception
	 */
	protected void setupYXId(long empId) throws Exception {
		// engine.doQueryObject(preparedSQL, params, upperCase)
		JSONObject emp = engine.doQueryObject("select * from emp where id=?", new Object[] { empId }, conn);
		String yxId = emp.optString("yxid");
		if (Validator.isNotNull(yxId))
			throw new NDSException("ָ��Ա���Ѿ�����������ID");
		// �������ŵ�
		int times = 0;
		boolean isSuccess = false;
		// �ҵ����ظ���yunxinUser�����20��
		while (times++ < 20) {
			try {
				String yunxinUser = UUIDUtils.compressedUuid().toLowerCase();
				String yunxinPassword = UUIDUtils.compressedUuid().substring(0, 12);
				engine.executeUpdate("update emp set yxid=?,yxpwd=? where id=?",
						new Object[] { yunxinUser, yunxinPassword, empId }, conn);
				// ���г�����
				YXController.getInstance().YXUsrAdd(yunxinUser, yunxinPassword, emp.optString("nkname"),
						emp.optString("img"));
				isSuccess = true;
				break;
			} catch (Throwable tx) {
				logger.error("Fail to update yunxin user and pwd for empid=" + empId + ":" + tx.getLocalizedMessage());
			}
		}
		if (!isSuccess)
			throw new NDSException("����Ա��ʧ�ܣ����Ժ�����");
		jedis.del("emp:" + empId);

	}

	/**
	 * �Ե�ַ���н����� ������
	 * 
	 * <pre>
	 * http://www.1688mj.com/bin/Join?usr=
	 * <encoded uid>&com=<encoded uid> </pre> �̼��� <pre>
	 * http://www.1688mj.com/bin/Join?com=<encoded uid> </pre>
	 * 
	 * @param url
	 * @return �����ʽ���ԣ����ᱨ������Ϊnull, ���� {com: long, usr:long} -1 or null when
	 *         com/usr no exists
	 */
	protected JSONObject parseJoinURL(String url) throws Exception {
		logger.debug("parsing joinurl " + url);
		if (Validator.isNull(url))
			return null;
		// http://www.1688mj.com/bin/Join
		if (!url.startsWith(PhoneConfig.JOIN_URL)) {
			return new JSONObject();
		}
		URL uRL = new URL(url);
		String querystr = uRL.getQuery();
		// usr.getId())+","+ String.valueOf(usr.getComId()
		String query = DESUtil.decrypt(querystr, ConfigValues.SECURITY_SIMPLE_ENCRYPT_KEY);
		String[] eles = query.split(",");
		JSONObject csMap = new JSONObject();
		if (eles.length == 2) {
			long usrId = Tools.getLong(eles[0], -1);
			long comId = Tools.getLong(eles[1], -1);
			if (usrId > 0 && comId > 0)
				csMap.put("usr", usrId).put("com", comId);
		}
		logger.debug("parsing joinurl " + querystr + ", to query " + query + " to json:" + csMap);
		return csMap;
	}

	/**
	 * ��Ҫ�ǹ��ŵĹ���Ա���, com_id ͨ��PhoneConfig.LIFECYCLE_COM_IDָ��
	 * 
	 * @throws Exception
	 */
	public void checkIsLifecycleManager() throws Exception {
		if (!usr.getName().equals("root"))
			throw new NDSException("�˹�����Ҫ����Ա���");
	}


	public void setDefaultWebEventHelper(DefaultWebEventHelper helper) {
		this.helper = helper;
	}

	/**
	 * ����Ƿ��ײ͵���, redis: com:$id key:'pkg',value:'YNW' Y - �ײͿ��ã�N �ײ͵��� W ΢���˺�
	 * ע����ô˷�����ʱ�򣬼�����W���ͣ�Ҳ�ᱨ����ʾ�ײ͹���
	 * 
	 * @throws Exception
	 *             �̼��ײ��ѹ��ڣ���֪ͨ�ϰ�����
	 */
	protected void checkPackage(long comId) throws Exception {

		String key = "com:" + comId;
		String v = jedis.hget(key, "pkg");
		if (v == null) {
			JSONObject jo = PhoneUtils.getRedisObj("com", comId, conn, jedis);
			v = jo.getString("pkg");
		}
		// ��˵����com���ײ�״̬����ÿ���Զ�����ģ������ײ�Ҳ��ģ���com�ϱض���ֵ
		// if(v==null){
		// //pkg_status(p_com_id in number) return varchar2 ,
		// ����com��id�����com��Ӧ�����˵��ײ��Ƿ��ڣ� Y ���ã�N
		// ArrayList params=new ArrayList();
		// params.add(comId);
		// ArrayList results=new ArrayList();
		// results.add(String.class);
		// Collection col=engine.executeFunction("pkg_status", params, results,
		// conn);
		// v= (String)col.toArray()[0];
		// jedis.hset(key, "pkg", v);
		// }
		if (!"Y".equals(v))
			throw new NDSException("�̼��ײ��ѹ��ڣ���֪ͨ�ϰ�����");
	}
	protected boolean checkPackage(long comId,boolean isReturn) throws Exception {

		String key = "com:" + comId;
		String v = jedis.hget(key, "pkg");
		if (v == null) {
			JSONObject jo = PhoneUtils.getRedisObj("com", comId, conn, jedis);
			v = jo.getString("pkg");
		}
		if (!"Y".equals(v)){
			return false;
		}
		return true;	
	}

	/**
	 * ���ַ����ҵ�������jo�е�table������������class������ȥ����׺
	 * 
	 * @param jo
	 * @param postFix
	 *            ��׺ "Add"|"Modify"|"Delete"|"Void"|"Get" |"Search"
	 * @return
	 * @throws Exception
	 */
	protected Table findTable(JSONObject jo, String postFix) throws Exception {
		String tableName = jo.optString("table");
		if (Validator.isNull(tableName)) {
			String sc = getClass().getSimpleName();
			if (sc.startsWith("Object") || sc.equals("Search")) {
				sc = jo.getString("cmd");
			}
			int idx = sc.lastIndexOf(postFix);
			if (idx < 0)
				throw new NDSException("������������:" + sc);
			tableName = sc.substring(0, idx);
		}
		Table table = manager.getTable(tableName);
		if (table == null)
			throw new NDSException("δ�����:" + tableName);
		return table;
	}

	/**
	 * �Ƿ���Ҫ��ʾ���ԵĽ��������userschema��������ʾ
	 * 
	 * @return
	 */
	public boolean isDebugResult() {
		return true;
	}

	/**
	 * Guest can execute this task, default to false
	 * 
	 * @return
	 */
	public boolean allowGuest() {
		return false;
	}

	/**
	 * Լ���ø�lua���ݵĽű���������
	 * 
	 * @param objectId
	 *            ������ǵ������������0����
	 * @return {objectid, comid, uid, stid, empid}
	 * @throws Exception
	 */
	protected JSONObject createLuaArgObj(long objectId) throws Exception {
		JSONObject jo = new JSONObject();
		jo.put("objectid", objectId);
		if (usr != null) {
			jo.put("comid", usr.getComId());
			jo.put("uid", usr.getId());
		} else {
			jo.put("comid", 0);
			jo.put("stid", 0);
			jo.put("empid", 0);
			jo.put("uid", 0);
		}
		return jo;
	}

	/**
	 * ����Lua�ű����ű���ad_sql.scriptNameָ���Ľű��ж��壬�������: JSONObject, ����JSONObject
	 * 
	 * @param scriptName
	 *            ad_sql.name
	 * @param jedis
	 * @return JSONObject | JSONArray Լ��lua����jsonobject�ǿͻ���Ҫ��ĸ�ʽ��lua�ڲ����ص���String
	 *         cjson.encode(result), ��Ҫ�ٴν�����
	 * @throws Exception
	 */
	protected Object execRedisLua(String scriptName, Connection conn, Jedis jedis) throws Exception {
		// read from db
		JSONObject argsObj = createLuaArgObj(-1);
		return PhoneUtils.execRedisLua(scriptName, argsObj, conn, jedis);
	}

	/**
	 * ִ�����ݿ�Ĵ洢���̣�Ҫ��洢���̲���: procName(objectId) �޷���ֵ
	 * 
	 * @param procName
	 * @param objectId
	 * @throws Exception
	 */
	protected void execProc(String procName, long objectId) throws Exception {
		if (Validator.isNotNull(procName)) {
			ArrayList params = new ArrayList();
			params.add(objectId);
			engine.executeStoredProcedure(procName, params, false);
		}
	}

	/**
	 * ��cachekey��ȡ֮ǰ�Ѿ���������id�б�ƴ�ɸ��Ӷ����б���
	 * 
	 * @param cacheKey
	 *            "list:"+table.getName()+":"+usr.getId()+":"+ja.length()+":"+
	 *            mask+":"+ UUIDUtils.compressedUuid()
	 * @param start
	 * @param pageSize
	 *            ҳ������
	 * @param table - ������ڱ�           
	 * @return
	 * @throws Exception, ObjectNotFoundException ������ʧЧ
	 */
	public SearchResult searchByCache(String cacheKey, int start, int pageSize, Table table, boolean idOnly) 
			throws ObjectNotFoundException, Exception {
		if (!jedis.exists(cacheKey))
			throw new ObjectNotFoundException("������ʧЧ�������²�ѯ");

		List<String> ids = jedis.lrange(cacheKey, start, start + pageSize - 1);
		// ���ӳ�30����
		jedis.expire(cacheKey, 30 * 60);

		JSONArray page = new JSONArray();
		for (int i = 0; i < pageSize && i < ids.size(); i++)
			page.put(Tools.getLong(ids.get(i), -1));
		
		JSONArray pageData;
		String[] keyParts = cacheKey.split(":");
		
		//Table table = manager.getTable(keyParts[1]);
		int jaLength = Tools.getInt(keyParts[3], -1);
		if (jaLength <= 0)
			throw new NDSException("cachekey��Ч(len)");

		if(!idOnly){
	
			String mask = keyParts[4];
			ArrayList<Column> cols;
			if ("obj".equals(mask)) {
				cols = table.getColumnsInObjectView();
			} else
				cols = table.getColumnsInListView();
			
			pageData= PhoneUtils.getRedisObjectArray(table.getName(), page, cols, true, conn, jedis);
		}else{
			pageData=page;
		}
		SearchResult sr = new SearchResult();
		sr.setCacheKey(cacheKey);
		sr.setCount(pageData.length());
		sr.setData(pageData);
		sr.setStart(start);
		sr.setTotal(jaLength);

		return sr;
	}

	/**
	 * ִ�����������ؽ��
	 * 
	 * @param req
	 * @return
	 * @throws Exception
	 */
	public SearchResult search(SearchRequest req) throws Exception {
		return search(req.toJSONObject());
	}

	/**
	 * �� {@link #search(JSONObject, HashMap)}
	 * 
	 * @param jo
	 * @return
	 * @throws Exception
	 */
	public SearchResult search(JSONObject jo) throws Exception {
		return search(jo, null);
	}

	/**
	 * ִ�����������ؽ��
	 * 
	 * @param jo
	 *            ���� { table, exprs, querystr, maxcnt,pagesize, mask, usepref,
	 *            orderby } table - String �����ı�������: emp exprs - Expression
	 *            ��ȷ���ֶεĹ���������Ԫ��key-valueΪ �����ֶ����Ͷ�Ӧֵ, ���������ʾ����and querystr -
	 *            String ͨ�ò�ѯ�ַ�������exprsΪand��ϵ pagesize - int ҳ������� maxcnt - int
	 *            ����ѯ���������ʱ��ͻ��˿���1����̨���Ϊ2000��ǰ̨���õ�ֵ���ܳ���2000��Ĭ��2000 mask -
	 *            string �ֶ�ȡֵ�� ��ѡ"list"|"obj", Ĭ��Ϊ"list", ��ʱ�ͻ����Կ�Ƭ��ʽ��ʾ���������Ҫ����Ϊ
	 *            obj���Ա��ȡ������ʾ�ֶ����ݣ���TableManager#Column#mask) usepref -
	 *            boolean���Ƿ�ʹ�ó��ò�ѯ������Ĭ��Ϊtrue���ڲ��ֱ��������ó��ò�ѯ����������ϴ��������й��� orderby -
	 *            string, ���ֶ���ҳ������orderbyѡ��Թؼ��ֽ���ƥ�� ����:
	 * 
	 *            { table:"spo", expres: {st_id: 33, emp_id: 20, state:
	 *            ["V","O", "S"], price: "12~20"}, querystr: "13918891588",
	 *            usepref: false, orderby: "stocktime_first"} }
	 * 
	 *            Expression ��ʽ
	 * 
	 *            {key: value} key - �ֶ����ƣ���Ҫ�ڵ�ǰ������ value -
	 *            �ֶε�ֵ��֧�������ֵ�������ʾ����һ��ƥ�� ����: {"st_id": 13} ��ʾҪ��st_id=13
	 * 
	 * @param additionalParam
	 *            ����Ĳ�����������Ʒ��Ҫ�Կ��������й��ˣ���Ҫ���������Ķ������� {key:
	 *            "exists(select 1 from stg where stg.st_id=? and stg.pdt_id=pdt.id and qty>0)"
	 *            , value: "13"}, ���value��java.util.List���������ֵ���棿��
	 * @return
	 * @throws Exception
	 */
	public SearchResult search(JSONObject jo, HashMap<String, Object> additionalParam) throws Exception {
		
		String tablename = this.getString(jo, "table");
		Table table = manager.getTable(tablename);
		JSONObject exprs = jo.optJSONObject("exprs");
		// ���ʹ��expr, Ĭ�ϲ�Ӧ����usepref=true
		// if(exprs!=null && exprs.length()>0){
		// }else{
		// //û�����ò�ѯ�����������Դ�pref���ȡ
		// if(jo.has("usepref") && jo.getBoolean("usepref")==true){
		// exprs=this.getEmpFilter(tablename);
		// }
		// }

		String querystr = jo.optString("querystr");
		int maxcnt = jo.optInt("maxcnt", 2000);
		if (maxcnt < 1)
			maxcnt = 1;
		if (maxcnt > PhoneConfig.MAX_SEARCH_RESULT)
			maxcnt = PhoneConfig.MAX_SEARCH_RESULT;

		String mask = jo.optString("mask");
		ArrayList<Column> cols;
		if ("obj".equals(mask)) {
			cols = table.getColumnsInObjectView();
		} else
			cols = table.getColumnsInListView();

		/**
		 * �����壬ad_sql#orderby:$name
		 * 
		 * �ṹ: {table: string, column: string, asc: boolean, join: string,
		 * param:[] } ָ���ǻ���ʲô����ֶ�������,
		 * joinָ��������֮�����ӵĹ�ϵ��param����join�г��ֵģ������������֧��$stid,$uid, $empid,$comid
		 * Ŀǰֻ֧�ֲ�ѯģʽ ����: ����������Ʒ��ǰ {table:"stg", column:"stg.samt", asc: false,
		 * join: "stg.pdtid=pdt.id and stg.st_id=?", param:["$stid"]}
		 */
		JSONObject orderbyDef = null;
		Object orderby = jo.opt("orderby");
		if (orderby != null && (orderby instanceof String)) {
			String orderbyDefName = "orderby:" + orderby;
			if (Validator.isNotNull(orderby.toString())) {
				// load from ad_sql
				orderbyDef = (JSONObject) PhoneController.getInstance().getValueFromADSQLAsJSON(orderbyDefName, conn,
						false);
			}
		} else if (orderby instanceof JSONObject) {
			orderbyDef = (JSONObject) orderby;
		}
		if (orderbyDef != null && orderbyDef.length() == 0)
			orderbyDef = null;// Ĭ������(id desc)��Ȼ�Ƕ��壬������Ϊ��

		ArrayList params = new ArrayList();
		StringBuilder sb = new StringBuilder();

		/**
		 * ���exprs ����tag���͵��ֶΣ��ҵ�tag����Ҫ���ж�ѡƥ��, �ͻ��˴�������id array ʲô��tag�����ֶ�:
		 * pdt���ptag��pdt.tag �ֶε���չ����:
		 * 
		 * {tagtable: { tb:"ptag", tag_column:"tag_id", main_column:"pdt_id" }}
		 * 
		 * ����: tb: tag��tag_column: tag��������ƥ��ͻ��˴�����id
		 * array���ֶΣ�main_column��tag������������������ֶ�
		 * 
		 * �����sql ���:
		 * 
		 * SELECT id FROM (SELECT pdt.id FROM pdt WHERE pdt.en = 'Y' AND EXISTS
		 * (SELECT 1 FROM ptag WHERE pdt.id = ptag.pdt_id AND ptag.tag_id IN
		 * (SELECT * FROM TABLE(split_str('16', ','))))) WHERE rownum <= 2000
		 * ORDER BY id DESC
		 *
		 */
		String pkColumn=jo.optString("pkcolumn");//some table not want id return, such as b_mk_pdt
		if(Validator.isNull(pkColumn)) pkColumn="id";
		sb.append(" select ").append(table.getName()).append(".").append(pkColumn);
		if (orderbyDef != null) {
			sb.append(",").append(orderbyDef.getString("column"));
		}
		sb.append(" from ").append(table.getRealName()).append(" ").append(table.getName());
		if (orderbyDef != null) {
			String odt = orderbyDef.getString("table");
			// �п�������
			if (!table.getName().equalsIgnoreCase(odt))
				sb.append(",").append(odt);
		}
		sb.append(" where ");
		if (orderbyDef != null) {
			sb.append(orderbyDef.getString("join")).append(" and ");
			JSONArray op = orderbyDef.optJSONArray("param");
			if (op != null)
				for (int i = 0; i < op.length(); i++) {
					String opi = op.getString(i);
					if ("$comid".equals(opi)) {
						params.add(usr.getComId());
					} else if ("$uid".equals(opi)) {
						params.add(usr.getId());
					} else if ("$marketid".equals(opi)) {
						params.add(usr.getMarketId());
					}else if ("$actid".equals(opi)) {
						params.add(jo.optInt("actid", -1));
					}else
						throw new NDSException("��֧�ֵĲ�������:" + opi);
				}
		}
		sb.append(table.getName() + ".isactive='Y'");

		// com level
		if (table.getPerms().contains(Table.Perm.COM)) {
			if (exprs == null) {
				exprs = new JSONObject();
			}
			exprs.put("com_id", usr.getComId());
		}
		if (table.getPerms().contains(Table.Perm.USER)) {
			if (exprs == null) {
				exprs = new JSONObject();
			}
			exprs.put("user_id", usr.getId());
		}

		if (exprs != null && exprs.length() > 0) {
			for (Iterator it = exprs.keys(); it.hasNext();) {
				String key = (String) it.next();
				Object val = exprs.get(key);
				String valstr = val.toString();
				if (Validator.isNull(valstr))
					continue;

				// logger.debug("find expr key="+ key+", val="+val+", valtype="+
				// val.getClass().getName());

				Column col = table.getColumn(key);
				if (col == null)
					continue;

				String columnName = table.getName() + "." + col.getName();
				if (val instanceof JSONArray) {
					// ����, ֮ǰ��tag table�Ѵ���
					JSONObject tagTable = (JSONObject) col.getJSONProp("tagtable");
					/**֧����ͨ�ֶ� 2016.4.22
					if (null == tagTable)
						throw new NDSException("Ŀǰ��֧��tagtable���������ѯ");
						*/
					if(tagTable==null){
						//����ͨ�ֶν���ʶ��ʹ���ÿ��Ԫ��֮����or ��ϵ
						JSONArray va=(JSONArray)val;
						if(va.length()>0) {
							sb.append(" and (");
						
							for(int vi=0;vi<va.length();vi++){
								sb.append(columnName).append("=?");
								if(vi<va.length()-1) sb.append(" or ");
								params.add(va.get(vi));
							}
							sb.append(")");
						}
					}

				} else if (val instanceof JSONObject) {
					// ����number���ͣ�2������{min, max}
					if (col.getType() != Column.Type.NUMBER)
						throw new NDSException(columnName + "�ֶβ����������ͣ���֧��jsonobject����");

					JSONObject vrange = (JSONObject) val;
					if (vrange.has("min")) {
						sb.append(" and ").append(columnName + " >= ? ");
						params.add(vrange.getDouble("min"));

					}
					if (vrange.has("max")) {
						sb.append(" and ").append(columnName + " <= ? ");
						params.add(vrange.getDouble("max"));
					}

				} else {
					if(valstr.equalsIgnoreCase("is null") || valstr.equalsIgnoreCase("is not null")){
						//ֱ�ӷ������ݿ�
						sb.append(" and ").append(columnName).append(" ").append(valstr);
					}else{
						// ��ֵ
						if (col.getType() == Column.Type.STRING) {
							// ȫ��ƥ��
							sb.append(" and ").append(columnName).append("=?");
							params.add(valstr);
						} else if (col.getType() == Column.Type.NUMBER || col.getType() == Column.Type.DATENUMBER) {
							// ֧�ַ�Χ�����û�еĻ����õȺ�
							if (!valstr.contains("~")) {
								sb.append(" and ").append(columnName).append("=?");
								String opi = valstr;
								if ("$comid".equals(opi)) {
									params.add(usr.getComId());
								} else if ("$uid".equals(opi)) {
									params.add(usr.getId());
								} else {
									params.add(valstr); // number?
								}
	
							} else {
	
								int hyphen = valstr.indexOf('~');
	
								if (Validator.isNull(valstr.substring(0, hyphen))) {
									sb.append(" and ").append(columnName + " <= ? ");
									params.add(valstr.substring(hyphen + 1));
								} else if (Validator.isNull(valstr.substring(hyphen + 1))) {
									sb.append(" and ").append(columnName + " >= ? ");
									params.add(valstr.substring(0, hyphen));
	
								} else {
									sb.append(" and (").append(columnName + " BETWEEN ? and ?)");
	
									params.add(valstr.substring(0, hyphen));
									params.add(valstr.substring(hyphen + 1));
	
								}
							}
						} else if (col.getType() == Column.Type.DATE) {
							valstr = StringUtils.replace(valstr, "now", "sysdate");
							String op = parseOperator(valstr);
							String var = this.parseStringExcludeOperator(valstr);
							if ("".equals(op))
								op = "=";
							sb.append(" and ").append(columnName).append(op).append("?");
							params.add(parse(var));
	
						} else
							throw new NDSException(
									"��֧�ֵ��ֶ�����:" + table.getName() + "." + columnName + "(" + col.getType() + ")");
					}//end if is null
				}
			}
		}

		// fuzzy search
		if (Validator.isNotNull(querystr)) {
			/**
			 * ��ѯ�ֶΣ����壺String, Ϊ�ֶ����ö��ŷָ�����: "docno,cust_id";
			 * ����fk���͵��ֶΣ���Ĭ��ȥ����fk�ĵ�һ������id��dks �ֶ�
			 * 
			 */
			//�������ݺ��Դ�Сд
			querystr = querystr.toLowerCase();
			String scs = (String) table.getJSONProp("search_on");
			if (Validator.isNotNull(scs)) {
				String[] scss = scs.split(",");
				sb.append(" and (");
				boolean isFirst = true;
				for (String cname : scss) {
					Column cl = table.getColumn(cname);
					if (cl == null)
						throw new NDSException(table + ".search_on��չ�����е��ֶ�δ����:" + cname);

					if (!isFirst)   
						sb.append(" or ");
					if (cl.getFkTable() == null) {
						sb.append(" lower(").append(table.getName()).append(".").append(cl.getRealName()).append(") like ?");
						params.add("%" + querystr + "%");
					} else {
						Table fkt = cl.getFkTable();
						String sdk = null;
						for (Column dk : fkt.getDisplayKeys()) {
							if (!"id".equals(dk.getName())) {
								sdk = dk.getName();
								break;
							}
						}
						if (sdk == null)
							throw new NDSException(table + ".search_on��չ�����е��ֶ�:" + cname + "��FK���dks��id������ֶ�");
						sb.append(" exists(select 1 from " + fkt + " where " + fkt + ".id=" + table + "." + cl.getName()
								+ " and " + fkt + "." + sdk + " like ?)");
						params.add("%" + querystr + "%");

					}
					isFirst = false;
				}
				sb.append(")");

			} else
				throw new NDSException("��Ҫ����" + table + "��search_on��չ����");
		}

		// ����tagtable������exists��䲹��
		if (exprs != null && exprs.length() > 0) {

			for (Iterator it = exprs.keys(); it.hasNext();) {
				String key = (String) it.next();
				Object val = exprs.get(key);
				Column col = table.getColumn(key);
				if (val == null || col == null)
					continue;

				JSONObject tagTable = (JSONObject) col.getJSONProp("tagtable");
				if (null == tagTable)
					continue;
				if (!(val instanceof JSONArray)) continue; /*ǰ���д���is null�����������Ϊstring��������array*/
					//throw new NDSException(key + "��Ӧ���ֶ�Ϊtag���ͣ��봫��tag����");
				if (((JSONArray) val).length() == 0)
					continue;

				// ����ֵ�γ�param����
				JSONArray valArray = (JSONArray) val;
				StringBuilder valstr = new StringBuilder();
				for (int i = 0; i < valArray.length(); i++) {
					JSONObject tagObj = valArray.getJSONObject(i);
					valstr.append(tagObj.getLong("id")).append(",");
				}
				valstr.deleteCharAt(valstr.length() - 1);// ��ʽ: 1,2,3

				/**
				 * {tagtable: { tb:"ptag", tag_column:"tag_id",
				 * main_column:"pdt_id" }} and exists (select 1 from ptag where
				 * pdt.id=ptag.pdt_id and ptag.tag_id in ( SELECT * FROM
				 * TABLE(split_str('16', ',')) ))
				 * 
				 * SELECT id FROM (SELECT pdt.id FROM pdt WHERE pdt.en = 'Y' AND
				 * EXISTS (SELECT 1 FROM ptag WHERE pdt.id = ptag.pdt_id AND
				 * ptag.tag_id IN ())) WHERE rownum <= 2000 ORDER BY id DESC
				 * 
				 */
				String tb = tagTable.getString("tb");
				sb.append(" and exists(select 1 from ").append(tb).append(" where ").append(table.getName())
						.append(".id=").append(tb).append(".").append(tagTable.getString("main_column")).append(" and ")
						.append(tb).append(".").append(tagTable.getString("tag_column")).append(" in (")
						.append("select * from table(split_str(?,','))").append("))");

				params.add(valstr.toString());

			}
		}

		if (additionalParam != null) {
			for (String key : additionalParam.keySet()) {
				Object value = additionalParam.get(key);
				sb.append(" and ").append(key);
				if(value instanceof List){
					for(Object v: (List)value){
						params.add(v);
					}
				}else
					params.add(value);
			}
		}
		// group by �����ر�Ŀ�浽ɫ��Ĵ���
		if (orderbyDef != null) {
			String postclause= orderbyDef.optString("postclause");
			if(Validator.isNotNull(postclause)){
				sb.append(postclause);
			}
		}
		// order by
		/**
		 * �����壬ad_sql#orderby:$name
		 * 
		 * �ṹ: {table: string, column: string, asc: boolean, join: string,
		 * param:[] } ָ���ǻ���ʲô����ֶ�������,
		 * joinָ��������֮�����ӵĹ�ϵ��param����join�г��ֵģ������������֧��$stid,$uid, $empid,$comid
		 * Ŀǰֻ֧�ֲ�ѯģʽ ����: ����������Ʒ��ǰ {table:"stg", column:"stg.samt", asc: false,
		 * join: "stg.pdtid=pdt.id and stg.st_id=?", param:["$stid"]}
		 */
		sb.append(" order by ");
		if (orderbyDef != null) {
			sb.append(orderbyDef.getString("column")).append(" ").append(orderbyDef.getBoolean("asc") ? "asc" : "desc")
					.append(" nulls last, ");
		}
		sb.append(table.getName()).append(".").append(pkColumn).append(" desc");

		StringBuilder sql = new StringBuilder();
		sql.append("select ").append(pkColumn).append(" from (").append(sb.toString()).append(") where rownum<=?");
		params.add(maxcnt);
		// ja: elements are id (long)
		JSONArray ja = engine.doQueryJSONArray(sql.toString(), params.toArray(), conn);
		
		ja=reviseSearchResultIdArray(ja);
	

		String cacheKey = "list:" + table.getName() + ":" + usr.getId() + ":" + ja.length() + ":"
				+ (Validator.isNull(mask) ? "list" : mask) + ":" + UUIDUtils.compressedUuid();
		JSONArray page = new JSONArray();

		if (ja.length() == 0) {
			SearchResult sr = new SearchResult();
			sr.setCacheKey(cacheKey);
			JSONArray pageData = new JSONArray();
			sr.setCount(pageData.length());
			sr.setData(pageData);
			sr.setStart(0);
			sr.setTotal(ja.length());

			return sr;

		}
		// cache to redis
		String[] ids = new String[ja.length()];
		for (int i = 0; i < ja.length(); i++)
			ids[i] = (String.valueOf(ja.getLong(i)));
		jedis.rpush(cacheKey, ids);
		jedis.expire(cacheKey, 30 * 60);

		// ��ҳ���أ�ǰpagesize��
		int pageSize = jo.optInt("pagesize", 20);
		int start= jo.optInt("start",0);
		if(start<0) start=0;
		for (int i = start; i < start+pageSize && i < start+ja.length() && i< ja.length(); i++)
			page.put(ja.getLong(i));

		boolean idOnly = jo.optBoolean("idonly", false);
		JSONArray pageData = idOnly ? page
				: PhoneUtils.getRedisObjectArray(table.getName(), page, cols, true, conn, jedis);
		SearchResult sr = new SearchResult();
		sr.setCacheKey(cacheKey);
		sr.setCount(pageData.length());
		sr.setData(pageData);
		sr.setStart(start);
		sr.setTotal(ja.length());

		return sr;
	}

	protected JSONArray reviseSearchResultIdArray(JSONArray ja) {
		return ja;
	}

	/**
	 * get operator string from <code>input<code> operator includes " ",>,<,=,
	 * IN, IS
	 */
	private String parseOperator(String str) {
		char[] cs = str.toCharArray();
		int i;
		for (i = 0; i < cs.length; i++) {
			char c = cs[i];
			if (c == ' ' || c == '>' || c == '<' || c == '=')
				continue;
			// add "in" operator
			if ((c == 'i' || c == 'I') && i < (cs.length - 1) && (cs[i + 1] == 'N' || cs[i + 1] == 'n')) {
				i++;
			}
			// add "not in" operator
			if ((c == 'n' || c == 'N') && i < (cs.length - 1) && (cs[i + 1] == 'O' || cs[i + 1] == 'o')
					&& i < (cs.length - 2) && (cs[i + 2] == 't' || cs[i + 2] == 'T') && i < (cs.length - 3)
					&& (cs[i + 3] == ' ') && i < (cs.length - 4) && (cs[i + 4] == 'I' || cs[i + 4] == 'i')
					&& i < (cs.length - 5) && (cs[i + 5] == 'n' || cs[i + 5] == 'N')) {
				i++;
			}
			// add "is" operator
			if ((c == 'i' || c == 'I') && i < (cs.length - 1) && (cs[i + 1] == 'S' || cs[i + 1] == 's')) {
				i++;
			}
			break;
		}
		return str.substring(0, i);
	}

	/**
	 * get other string except operator string from <code>input<code>
	 */
	private String parseStringExcludeOperator(String str) {
		char[] cs = str.toCharArray();
		int i;
		for (i = 0; i < cs.length; i++) {
			char c = cs[i];
			if (c == ' ' || c == '>' || c == '<' || c == '=')
				continue;
			// add "in" operator
			if ((c == 'i' || c == 'I') && i < (cs.length - 1) && (cs[i + 1] == 'N' || cs[i + 1] == 'n')) {
				i++;
			}
			// add "not in" operator
			if ((c == 'n' || c == 'N') && i < (cs.length - 1) && (cs[i + 1] == 'O' || cs[i + 1] == 'o')
					&& i < (cs.length - 2) && (cs[i + 2] == 't' || cs[i + 2] == 'T') && i < (cs.length - 3)
					&& (cs[i + 3] == ' ') && i < (cs.length - 4) && (cs[i + 4] == 'I' || cs[i + 4] == 'i')
					&& i < (cs.length - 5) && (cs[i + 5] == 'n' || cs[i + 5] == 'N')) {
				i++;
			}
			// add "is" operator
			if ((c == 'i' || c == 'I') && i < (cs.length - 1) && (cs[i + 1] == 'S' || cs[i + 1] == 's')) {
				i++;
			}
			break;
		}
		return str.substring(i);
	}

	/**
	 * ����ָ��id����ϸ, ���Ȱ������ҵ� $table:$objectid:$rbf.name ��Ӧ��List������У�ȫȫ����¼���� ����
	 * ��ȡrbf��lua���ã����������lua������ֱ�����з��ؽ���� ��ȡrbf��adsql���ã�������䣬���Խ���䣬��ʽ: select id
	 * from $rbf.table where $rbf.column=$objectid and $rbf.filter order by id
	 * desc ������ϸ�� $table:$objectid:$rbf.name
	 * 
	 * @param objectId
	 *            ����ID
	 * @param table
	 *            ����
	 * @param rbf
	 *            �ӱ�
	 * @return elements are {}
	 * @throws Exception
	 */
	protected JSONArray getItemIds(long objectId, Table table, RefByTable rbf) throws Exception {
		JSONArray rows = null;

		// $table:$objectid:$rbf.name
		String key = table.getName() + ":" + objectId + ":" + rbf.getName();
		String type = jedis.type(key);

		if ("list".equals(type)) {
			List<String> itemIdsInDB = jedis.lrange(key, 0, jedis.llen(key));
			rows = new JSONArray();
			for (String id : itemIdsInDB) {
				long oid = Tools.getLong(id, -1);
				if (oid > 0)
					rows.put(oid);
				else
					logger.error("Find bad id in " + key + " (" + id + ")");
			}
		} else if ("none".equals(type)) {

			if (Validator.isNotNull(rbf.getLuaName())) {
				// ��ȫͨ��lua�����췵�ص�json array
				rows = (JSONArray) PhoneUtils.execRedisLua(rbf.getLuaName(), this.createLuaArgObj(objectId), conn,
						jedis);
			} else {

				if (Validator.isNotNull(rbf.getAdSQLName())) {
					// ��sql��������еĽ��������: select id from xxx
					vc.put("objectid", objectId);
					rows = PhoneController.getInstance().getDataArrayByADSQL(rbf.getAdSQLName(), vc, conn, false);
				} else {

					StringBuilder sb = new StringBuilder(
							"select id from " + rbf.getTable() + " where en='Y' and " + rbf.getColumn() + "=? ");
					if (Validator.isNotNull(rbf.getFilter())) {
						sb.append(" and ").append(rbf.getFilter());
					}
					sb.append(" order by id desc");
					rows = engine.doQueryJSONArray(sb.toString(), new Object[] { objectId }, 0, 2000, conn);
				}
			}
			// set into key
			if (rows.length() > 0) {
				String[] itemIdsInDB = new String[rows.length()];
				for (int i = 0; i < rows.length(); i++) {
					itemIdsInDB[i] = String.valueOf(rows.getLong(i));
				}
				jedis.lpush(key, itemIdsInDB);
			} else
				jedis.set(key, "");
		} else if ("string".equals(type)) {
			// �����ʾû��Ԫ�أ�ֱ�ӷ���
			return new JSONArray();
		} else
			throw new NDSException("�ڲ�����δ֪������");
		//
		Table rbfTable = manager.getTable(rbf.getTable());
		return PhoneUtils.getRedisObjectArray(rbfTable.getName(), rows, rbfTable.getColumnsInListView(), true, conn,
				jedis);

	}

	/**
	 * ���洫������Ϣ�������Σ���Ҫ�������п��Ա༭(column.edit=true)�������ع�����
	 * ���⣬���������������������ʱ�򣬻���Ҫȡ��edit=false, defaultvalue!=null��ֵ��ֱ�Ӳ���,
	 * defaultValue��ֵ���Ժ�̨�Ļ�����������:$uid, $empid, $comid, $stid, $sysdate, $docno
	 * 
	 * @param table
	 * @param obj
	 *            ���ܰ���������༭������
	 * @param useDefaultValue
	 *            �Ƿ����edit=false, defaultvalue!=null���ֶ�
	 * @param checkNull
	 *            �Ƿ��鲻����Ϊ�յ��ֶ���Ҫ���룬���add������޸ĵ�ʱ����Ҫ����ΪΪ�պ󲻻�����䵽update����
	 * @return ÿ�����Զ��ǵ�ǰ�û����Ա༭��
	 */
	protected JSONObject createEditObject(Table table, JSONObject obj, boolean useDefaultValue, boolean checkNull)
			throws Exception {
		// ����table�ɱ༭�ֶ�
		JSONObject editObj = new JSONObject();
		ArrayList<Column> cols = table.getEditColumns(0);
		for (Column col : cols) {
			Object value = obj.opt(col.getName());
			if (value != null) {
				if (value instanceof JSONObject) {
					if (col.getFkTable() == null)
						throw new NDSException("�ֶ�" + col.getNote() + "����FK���ͣ�ȴ����JSONObject");
					// ȡid
					long fkId = ((JSONObject) value).optLong("id", -1);
					if (fkId <= 0) {
						editObj.put(col.getName(), JSONObject.NULL);
						// throw new
						// NDSException("�ֶ�"+col.getNote()+"��ӦJSONObject������idֵ");
					} else
						editObj.put(col.getName(), fkId);
				} /*
					 * else if(value instanceof JSONArray ){
					 * editObj.put(col.getName(), value.toString()); }
					 */else
					editObj.put(col.getName(), value);
			}
		}
		// ���ز��ɱ༭����Ĭ��ֵ���ֶ�
		if (useDefaultValue) {
			for (Column col : table.getColumns()) {
				if (!col.isEdit()) {
					String defaultValue = col.getDefaultValue();
					if (Validator.isNotNull(defaultValue)) {
						Object value = null;
						// Ŀǰ֧�ֵľ���$uid, $empid, $comid, $stid
						if ("$uid".equals(defaultValue)) {
							value = usr.getId();
						}  else if ("$comid".equals(defaultValue)) {
							value = usr.getComId();
						}else if ("$docno".equals(defaultValue)) {
							
							value = PhoneUtils.createDocNo(table.getName(), conn);
						} else if ("$sysdate".equals(defaultValue)) {
							value = new java.util.Date();
						} else {
							throw new NDSException(
									"��֧�ֵı���" + table.getName() + "." + col.getName() + ".dfv:" + defaultValue);
						}
						editObj.put(col.getName(), value);
						logger.debug("add " + col.getName() + ", value=" + value);
					}
				}
			}
		}
		// �������id,����Ϊupdate,��������ڣ�����Ϊcreate, ���cusr,musr,ctime,mtime���д���
		long objectId = obj.optLong("id", -1);
		java.util.Date now = new java.util.Date();
		if (objectId == -1) {
			editObj.put("ownerid", usr.getId());
			editObj.put("creationdate", now);
		}
		editObj.put("modifierid", usr.getId());
		editObj.put("modifieddate", now);

		if (checkNull)
			for (Column col : cols) {
				Object value = editObj.opt(col.getName());
				// ����Ƿ���Ϊ��
				if (!col.isNull() && (value == null || (Validator.isNull(value.toString()))))
					throw new NDSException("��Ҫ����" + col.getNote());
			}
		return editObj; 
	}

	/**
	 * �ж�obj��������ָ����table�ļ�¼���Ƿ�ɱ���ǰ�����û�����
	 * ���������comȨ�ޣ���ʾ�������ڲ�Ա�����ʣ������ǰ�û���comid��ҵ�����ݲ�һ�£������� ���������admin, ��ʾ���������Ա����
	 * 
	 * @param table
	 * @param obj
	 * @throws Exception
	 *             ���û��Ȩ�޾ͱ���
	 */
	protected void checkTableWritePermission(Table table, JSONObject obj) throws Exception {
		
	}
	
	/**
	 * �ж�obj��������ָ����table�ļ�¼���Ƿ�ɱ���ǰ�����û�����
	 * ���������comȨ�ޣ���ʾ�������ڲ�Ա�����ʣ������ǰ�û���comid��ҵ�����ݲ�һ�£������� ���������admin, ��ʾ���������Ա����
	 * 
	 * @param table
	 * @param obj
	 * @throws Exception
	 *             ���û��Ȩ�޾ͱ���
	 */
	protected void checkTableReadPermission(Table table, JSONObject obj) throws Exception {
//		ArrayList<Table.Perm> perms = table.getPerms();
//		if (perms.contains(Table.Perm.ADMIN)) {
//			// ��ǰ��Ա�ڵ�ǰ�̼���Ҫ�ǹ���Ա
//			if (getCurrentUserRole() != UserObj.EmployeeRole.MANAGER)
//				throw new NDSException("��Ҫ����Ա��ݲ��ܷ���");
//		}
//		if (perms.contains(Table.Perm.COM)) {
//			// ��ǰ������ݽ����̼��ڲ���Ա����
//			long comId = obj.optLong("com_id", 0);
//			// if(comId<=0){
//			// logger.error("table "+ table.getName()+" obj "+ obj+" : not found
//			// com_id in it");
//			// throw new NDSException("���������̼���Ϣδ����");
//			//
//			// }
//			if (comId > 0 && comId != this.usr.getComId())
//				throw new NDSException("�̼��ڲ�����, �������ⲿ��Ա����");
//
//		}
	}

	protected void loggerDefaultWebEvent() throws Exception {
		logger.debug(event.toDetailString());
	}

	/**
	 * ����pad�˴����Ĺؼ��֣����ؼ����еĵ������滻�����������š�
	 * 
	 * @param keyword
	 * @return
	 */
	public String dealWithTheKeyword(String keyword) {
		keyword.replaceAll("'", "''");
		return keyword;
	}

	/**
	 * �������Ļ��������, conn �� jedis�����ⲿ���죬���軺��
	 */
	public void distroy() {
	}

	/**
	 * @param event
	 * @param conn
	 * @throws Exception
	 */
	public void setContext(DefaultWebEvent event, Connection conn, Jedis jedis, UserObj usr) throws Exception {
		this.usr = usr;
		this.event = event;
		this.conn = conn;
		this.jedis = jedis;
		engine = QueryEngine.getInstance();
		this.manager = TableManager.getInstance();
		this.rootwar = event.getContext().getSession().getServletContext().getRealPath("");
		HttpServletRequest request = event.getContext().getHttpServletRequest();
		this.serverURL = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();

		vc = VelocityUtils.createContext();
		if (usr != null) {
			vc.put("uid", usr.getId());
			vc.put("comid", usr.getComId());

			//update session lang
			Locale newLocale=LanguageManager.getInstance().getLocale(usr.getLangId());
			//web session
			Locale currentLocale=(Locale)request.getSession().getAttribute(org.apache.struts.Globals.LOCALE_KEY);
			if(!newLocale.equals(currentLocale))request.getSession().setAttribute(org.apache.struts.Globals.LOCALE_KEY,newLocale);
			this.locale=newLocale;
		}else{
			this.locale=request.getLocale();
		}
		
	}

	/**
	 * Called before execution
	 * 
	 * @param jo
	 * @throws Exception
	 */
	public void prepare(JSONObject jo) throws Exception {
		loggerDefaultWebEvent();
	}

	/**
	 * ִ������
	 * 
	 * @param jo
	 *            �������
	 * @return ���ص����ݽ�ȫ����Ӧ��ValueHolder�����
	 */
	public abstract CmdResult execute(JSONObject jo) throws Exception;

	/**
	 * Create task by name
	 * 
	 * @param taskName
	 *            without dot, will loop over task class packages for first
	 *            matched path
	 * @return
	 * @throws Exception
	 */
	protected CmdHandler createCmdHandler(String cmdName) throws Exception {
		String className;
		Class clazz;
		CmdHandler task = null;
		for (String packageName : PhoneController.getInstance().getCmdHandlerPackages()) {
			className = packageName + "." + cmdName;
			try {
				clazz = Class.forName(className);
				task = (CmdHandler) clazz.newInstance();
				break;
			} catch (ClassNotFoundException cnfe) {

			}
		}
		if (task == null) {
			logger.error("Fail to find cmdhandler " + cmdName + " in packages: "
					+ PhoneController.getInstance().getCmdHandlerPackages());
			throw new NDSException("cmdhandler " + cmdName + " not found");
		}
		return task;
	}

	protected int getInt(JSONObject jo, String name) throws Exception {
		int value = jo.optInt(name, -1);
		if (value == -1)
			throw new NDSException(name + "δ����");
		return value;
	}

	protected long getLong(JSONObject jo, String name) throws Exception {
		long value = jo.optLong(name, -1);
		if (value == -1)
			throw new NDSException(name + "δ����");
		return value;
	}

	protected String getString(JSONObject jo, String name) throws Exception {
		String value = jo.optString(name);
		if (Validator.isNull(value))
			throw new NDSException(name + "δ����");
		return value;
	}

	protected JSONObject getObject(JSONObject jo, String name) throws Exception {
		JSONObject value = jo.optJSONObject(name);
		if (value == null)
			throw new NDSException(name + "δ����");
		return value;
	}

	/**
	 * @param dateOrTime
	 *            3 formats: 20101231, 2010-12-31 12:59:59 or sysdate+1/96
	 * @return null if error found
	 */
	protected java.util.Date parse(String dateOrTime) {
		if (dateOrTime == null)
			return null;
		if (dateOrTime.contains("sysdate")) {
			try {
				Object obj = engine.doQueryOne("select " + dateOrTime + " from dual", conn);
				if (obj instanceof java.util.Date) {
					return (java.util.Date) obj;
				}
			} catch (Throwable t) {

			}
			return null;
		} else {
			SimpleDateFormat sdf = QueryUtils.dateNumberFormatter.get();
			try {
				java.util.Date d = sdf.parse(dateOrTime);
				return d;
			} catch (Exception e2) {
				try {
					sdf = PhoneUtils.dateTimeSecondsFormatter.get();
					java.util.Date d = sdf.parse(dateOrTime);
					return d;
				} catch (Exception ex) {
					return null;
				}
			}
		}
	}

	/**
	 * Get product ids according to filter list
	 * 
	 * @param filterList
	 *            in format like: "13,343,4343"
	 * @return
	 * @throws Exception
	 */
	protected ArrayList<Integer> getPdtIds(String filterList) throws Exception {
		ArrayList ja = new ArrayList();
		for (String s : filterList.split(",")) {
			int pdtId = Tools.getInt(s, -1);
			if (pdtId != -1)
				ja.add(pdtId);
		}
		return ja;
	}

//	public String optString(String origin) {
//		if (origin == null || origin == "null")
//			origin = "";
//		/* ȥ���س����� */
//		Pattern p = Pattern.compile("\\t|\r|\n");
//		Matcher m = p.matcher(origin);
//		origin = m.replaceAll(" ");
//		return origin;
//	}

	public JSONArray insertOneItem(JSONArray origin, int index, Object item) {
		JSONArray result = new JSONArray();
		for (int i = 0; i < origin.length(); i++) {
			if (i == index)
				result.put(item);
			result.put(origin.opt(i));
		}
		return result;
	}

	/**
	 * Convert hex string to byte[]
	 * 
	 * @param hexString
	 *            the hex string
	 * @return byte[]
	 */
	public byte[] hexStringToBytes(String hexString) {
		if (hexString == null || hexString.equals("")) {
			return null;
		}
		hexString = hexString.toUpperCase();
		int length = hexString.length() / 2;
		char[] hexChars = hexString.toCharArray();
		byte[] d = new byte[length];
		for (int i = 0; i < length; i++) {
			int pos = i * 2;
			d[i] = (byte) (charToByte(hexChars[pos]) << 4 | charToByte(hexChars[pos + 1]));
		}
		return d;
	}

	/**
	 * Convert char to byte
	 * 
	 * @param c
	 *            char
	 * @return byte
	 */
	private byte charToByte(char c) {
		return (byte) "0123456789ABCDEF".indexOf(c);
	}

	public String dealWithIds(String ids) {
		if (ids == null || "".equals(ids))
			ids = "-1";
		return ids;
	}

	/**
	 * �жϵ�ǰ�ļ��Ƿ���ͼƬ
	 * 
	 * @param file
	 * @return
	 * @throws IOException
	 */
	public boolean judgeIfImage(File file) throws IOException {
		boolean flag = false;
		if (file.isFile()) {
			BufferedImage img = ImageIO.read(file);
			flag = null != img;
		}
		return flag;
	}

	/**
	 * �ݹ�ɾ��Ŀ¼�µ������ļ�����Ŀ¼�������ļ�
	 * 
	 * @param dir
	 *            ��Ҫɾ�����ļ�Ŀ¼
	 * @return boolean Returns "true" if all deletions were successful. If a
	 *         deletion fails, the method stops attempting to delete and returns
	 *         "false".
	 */
	private boolean _deleteDir(File dir) {
		if (dir.isDirectory()) {
			String[] children = dir.list();
			// �ݹ�ɾ��Ŀ¼�е���Ŀ¼��
			for (int i = 0; i < children.length; i++) {
				boolean success = _deleteDir(new File(dir, children[i]));
				if (!success) {
					return false;
				}
			}
		}
		// Ŀ¼��ʱΪ�գ�����ɾ��
		return dir.delete();
	}

	/***
	 * ���ĳһ��Ŀ¼
	 * 
	 * @param dir
	 */
	public void cleanDir(File dir) {
		if (!dir.isDirectory()) {
			dir.mkdirs();
		} else {
			if (_deleteDir(dir)) {
				logger.debug("cleanDir �ݹ�ɾ��Ŀ¼�ɹ�:" + dir.getAbsolutePath());
				dir.mkdir();
			} else {
				logger.debug("cleanDir �ݹ�ɾ��Ŀ¼ʧ��:" + dir.getAbsolutePath());
			}
		}
	}

	protected String capitalizeFirstLetter(String data) {
		String firstLetter = data.substring(0, 1).toUpperCase();
		String restLetters = data.substring(1);
		return firstLetter + restLetters;
	}

	/**
	 * �������ٴ���װ���� ϵͳ����Ҳ��������ֹͣ�����fk����������󱨴�
	 * 
	 * @param rows
	 *            element ��fetchObject����Ķ��󣬷���Ԫ���ݵĽṹ�������ع�, Ŀǰ��֧����ϸ��refbytable
	 * @param adsqlConfName
	 *            ad_sql��nameָ��������. ����������json array�� Ԫ����ʽ: "field.fk1.fk2.."
	 *            fk��Ҫ���������, field ��obj�����ԣ�������2��: column��refbytable
	 *            �����column��������fk����
	 * @param table
	 *            ��ǰ���ݵı�
	 */
	protected void assembleArrayByConfName(JSONArray rows, String adsqlConfName, Table table) throws Exception {
		JSONArray conf = (JSONArray) PhoneController.getInstance().getValueFromADSQLAsJSON(adsqlConfName,
				new JSONArray(), conn);
		assembleArray(rows, conf, table);
	}

	/**
	 * ��ȡָ����¼�Ķ���ṹ
	 * 
	 * @param rows
	 *            element ��fetchObject����Ķ��󣬷���Ԫ���ݵĽṹ�������ع�, Ŀǰ��֧����ϸ��refbytable
	 * @param link
	 *            string��ʽ: "field.fk1.fk2.." fk��Ҫ���������, field ��obj�����ԣ�������2��:
	 *            column��refbytable �����column��������fk���� ϵͳ����Ҳ��������ֹͣ�����fk����������󱨴�
	 * @param table
	 *            ��ǰ���ݵı�
	 */
	protected void assembleArrayByLinkString(JSONArray rows, String link, Table table) throws Exception {
		String[] ols = link.split("\\.");
		LinkedList<String> olink = new LinkedList();
		for (int i = 0; i < ols.length; i++)
			olink.addLast(ols[i]);

		for (int i = 0; i < rows.length(); i++) {
			JSONObject one = rows.getJSONObject(i);
			LinkedList<String> copyLink = (LinkedList<String>) olink.clone();
			assembleObjectByLinkedList(one, copyLink, table);
		}
	}

	/**
	 * ��ȡָ����¼�Ķ���ṹ
	 * 
	 * @param rows
	 *            element ��fetchObject����Ķ��󣬷���Ԫ���ݵĽṹ�������ع�, Ŀǰ��֧����ϸ��refbytable
	 * @param objAssembleConf
	 *            Ԫ��string��ʽ: "field.fk1.fk2.." fk��Ҫ���������, field ��obj�����ԣ�������2��:
	 *            column��refbytable �����column��������fk���� ϵͳ����Ҳ��������ֹͣ�����fk����������󱨴�
	 * @param table
	 *            ��ǰ���ݵı�
	 */
	protected void assembleArray(JSONArray rows, JSONArray objAssembleConf, Table table) throws Exception {
		if (objAssembleConf == null || objAssembleConf.length() == 0)
			return;
		for (int j = 0; j < objAssembleConf.length(); j++) {
			String link = objAssembleConf.getString(j);
			assembleArrayByLinkString(rows, link, table);
		}
	}

	/**
	 * �������ٴ���װ���� ϵͳ����Ҳ��������ֹͣ�����fk����������󱨴�
	 * 
	 * @param obj
	 *            ��fetchObject����Ķ��󣬷���Ԫ���ݵĽṹ�������ع�
	 * @param adsqlConfName
	 *            ad_sql��nameָ��������. ����������json array�� Ԫ����ʽ: "field.fk1.fk2.."
	 *            fk��Ҫ���������, field ��obj�����ԣ�������2��: column��refbytable
	 *            �����column��������fk����
	 * @param table
	 *            ��ǰ���ݵı�
	 */
	protected void assembleObjectByConfName(JSONObject obj, String adsqlConfName, Table table) throws Exception {
		JSONArray conf = (JSONArray) PhoneController.getInstance().getValueFromADSQLAsJSON(adsqlConfName,
				new JSONArray(), conn);
		assembleObject(obj, conf, table);
	}

	/**
	 * ��ȡָ����¼�Ķ���ṹ
	 * 
	 * @param obj
	 *            ��fetchObject����Ķ��󣬷���Ԫ���ݵĽṹ�������ع�
	 * @param objAssembleConf
	 *            Ԫ��string��ʽ: "field.fk1.fk2.." fk��Ҫ���������, field ��obj�����ԣ�������2��:
	 *            column��refbytable �����column��������fk���� ϵͳ����Ҳ��������ֹͣ�����fk����������󱨴�
	 * @param table
	 *            ��ǰ���ݵı�
	 */
	protected void assembleObject(JSONObject obj, JSONArray objAssembleConf, Table table) throws Exception {
		if (objAssembleConf == null || objAssembleConf.length() == 0)
			return;
		for (int i = 0; i < objAssembleConf.length(); i++) {
			String olink = objAssembleConf.getString(i);
			assembleObjectByLinkString(obj, olink, table);
		}
	}

	/**
	 * ��ȡָ����¼�Ķ���ṹ
	 * 
	 * @param obj
	 *            ��fetchObject����Ķ��󣬷���Ԫ���ݵĽṹ�������ع�
	 * @param objAssembleConf
	 *            Ԫ��string��ʽ: "field.fk1.fk2.." fk��Ҫ���������, field ��obj�����ԣ�������2��:
	 *            column��refbytable �����column��������fk���� ϵͳ����Ҳ��������ֹͣ�����fk����������󱨴�
	 * @param table
	 *            ��ǰ���ݵı�
	 */
	protected void assembleObjectByLinkString(JSONObject obj, String link, Table table) throws Exception {
		String[] ols = link.split("\\.");
		LinkedList<String> olink = new LinkedList();
		for (int i = 0; i < ols.length; i++)
			olink.addLast(ols[i]);

		String current = olink.getFirst();
		Column cl = table.getColumn(current);
		if (cl != null) {
			assembleObjectByLinkedList(obj, olink, table);
		} else {
			RefByTable rbt = table.getRefByTable(current);
			if (rbt == null)
				throw new NDSException("link����������������崮:" + current + "(" + table + ")");
			if (rbt.getAssocType() == RefByTable.ONE_TO_ONE) {
				olink.pop();
				assembleObjectByLinkedList(obj.optJSONObject(current), olink, manager.getTable(rbt.getTable()));
			} else {
				JSONArray ja = obj.optJSONArray(current);
				if (ja != null)
					for (int i = 0; i < ja.length(); i++) {
						JSONObject one = ja.getJSONObject(i);
						LinkedList<String> copyLink = (LinkedList<String>) olink.clone();
						copyLink.pop();
						Table rbtt = manager.getTable(rbt.getTable());
						assembleObjectByLinkedList(one, copyLink, rbtt);
					}
			}
		}

	}

	/**
	 * ��ȡָ����¼�Ķ���ṹ
	 * 
	 * @param obj
	 *            ��fetchObject����Ķ��󣬷���Ԫ���ݵĽṹ�������ع�
	 * @param olink
	 *            ��һ��Ԫ����table��column(fk���ͣ��������Ԫ��Ҳ��fk ϵͳ����Ҳ��������ֹͣ�����fk����������󱨴�
	 * @param table
	 *            ��ǰ���ݵı�
	 */
	private void assembleObjectByLinkedList(JSONObject obj, LinkedList<String> olink, Table table) throws Exception {
		if (olink.size() == 0 || obj == null)
			return;
		String colName = olink.pop();
//		logger.debug("before assemble:"+table+" onobj "+ obj+" for colName:"+ colName);
		Column col = table.getColumn(colName);
		if (col == null)
			throw new NDSException("δ��" + table + "�����ҵ��ֶ�" + colName);

		Table fkTable = col.getFkTable();
		if (fkTable == null)
			throw new NDSException(table + "���ϵ��ֶ�" + colName + "����FK����");

		Object fkValue = obj.opt(colName);
		JSONObject fkObj = null;
		if (fkValue instanceof JSONObject) {
			// already fetched
			fkObj = (JSONObject) fkValue;
		} else {
			long fkId = Tools.getLong(fkValue, -1);

//			 logger.debug("find fkid="+ fkId+", of " + col.getTable()+"."+col.getName()+" obj:"+ obj+", fkValue="+(fkValue==null?"null":fkValue.getClass().getName())+" data:"+ fkValue);

			if (fkId == -1) {
				// ����һ���������ȥ����
//				 logger.debug("$$$$$$$$$$$$$$$$$ found emptypefkId"+" for:"+colName+": "+fkValue );
				 obj.put(colName, new JSONObject());
				return;
			}
			// ����fk
			fkObj = fetchObject(fkId, fkTable.getName(), false);
			// ��װ����ǰ����
			obj.put(colName, fkObj);
		}
		// ����һ��
		assembleObjectByLinkedList(fkObj, olink, fkTable);

		logger.debug("after assemble:"+table+" onobj "+ obj);
	}

	/**
	 * ��ȫ��metaԪ�������������ض���ṹ��Ԫ����Ĭ�Ϸ��ص�һ��
	 * 
	 * @param objectId
	 * @param tableName
	 * @param withItemTables
	 *            �Ƿ���Ҫ������ϸ�����/���飬true����ȡ����table�϶����refbys����ϸ�������Ҫ������أ�ʹ�÷���
	 *            {@link #fetchObject(long, String, String)}
	 * @return �����Ķ���
	 */
	protected JSONObject fetchObject(long objectId, String tableName, boolean withItemTables) throws Exception {
		String[] itemTables;
		if (withItemTables == false)
			itemTables = null;
		else {
			Table table = manager.getTable(tableName);
			itemTables = new String[table.getRefByTables().size()];
			for (int i = 0; i < itemTables.length; i++) {
				itemTables[i] = table.getRefByTables().get(i).getName();
			}
		}
		return fetchObject(objectId, tableName, itemTables);
	}

	/**
	 * ��ȫ��metaԪ�������������ض���ṹ��Ԫ����Ĭ�Ϸ��ص�һ��, cols �� getColumnsInObjectView
	 * 
	 * @param objectId
	 * @param tableName
	 *            ������
	 * @param itemTables
	 *            ָ������Ҫװ�ڵ���ϸ���refbys.name, ע�ⲻ����ϸ������ƣ�������ϸ��+"_s"
	 * @return
	 */
	protected JSONObject fetchObject(long objectId, String tableName, String[] itemTables) throws Exception {
		Table table = manager.getTable(tableName);
		ArrayList<Column> cols = table.getColumnsInObjectView();
		return fetchObject(objectId, tableName, cols, itemTables);
	}

	/**
	 * ��ȫ��metaԪ�������������ض���ṹ��Ԫ����Ĭ�Ϸ��ص�һ��
	 * 
	 * @param objectId
	 * @param tableName
	 *            ������
	 * @param itemTables
	 *            ָ������Ҫװ�ڵ���ϸ���refbys.name, ע�ⲻ����ϸ������ƣ�������ϸ��+"_s"
	 * @return
	 */
	protected JSONObject fetchObject(long objectId, String tableName, ArrayList<Column> cols, String[] itemTables)
			throws Exception {

		Table table = manager.getTable(tableName);

		// logger.debug(table.toJSONObject().toString());

		JSONObject obj = PhoneUtils.getRedisObj(tableName, objectId, cols, conn, jedis);

		// Ȩ���ж�
		// checkTableReadPermission(table, obj);

		// for(Column fkc: cols){
		// if(fkc.getFkTable()==null)continue;
		// long fkId=obj.optLong(fkc.getName(), -1);
		// JSONObject fko=null;
		// if(fkId<=0){
		// //������Ҫ����Ҫ��������ݹ�ȥ��������null
		// fko=PhoneUtils.createBlankFKObj(fkc.getFkTable());
		// }else{
		// fko=PhoneUtils.getRedisObj(fkc.getFkTable().getName(), fkId ,
		// fkc.getFkTable().getColumnsInObjectView()
		// /*fkc.getFkTable().getDisplayKeys()*/ , vc, conn, jedis);
		// }
		// obj.put(fkc.getName(),fko );
		// }

		// ��ϸ�����
		if (itemTables != null) {
			for (String name : itemTables) {
				RefByTable rbf = table.getRefByTable(name);
				if (rbf == null)
					throw new NDSException("δ�����ӱ�" + name);
				if (rbf.getAssocType() == RefByTable.ONE_TO_ONE) {
					// fint this one
					long rboId = Tools.getLong(engine.doQueryOne(
							"select id from " + rbf.getTable() + " where " + rbf.getColumn() + "=? order by id desc",
							new Object[] { objectId }, conn), 0);
					JSONObject rbo;
					if (rboId == -1) {
						rbo = new JSONObject();
					} else {
						rbo = PhoneUtils.getRedisObj(rbf.getTable(), rboId, table.getColumnsInObjectView(), conn,
								jedis);
						// װ��
						JSONArray conf = (JSONArray) PhoneController.getInstance().getValueFromADSQLAsJSON(
								"table:" + rbf.getTable() + ":assemble:obj", new JSONArray(), conn);
						this.assembleObject(rbo, conf, manager.getTable(rbf.getTable()));

					}
					obj.put(rbf.getName(), rbo);
				} else {
					// multiple lines
					JSONArray idObjs = getItemIds(objectId, table, rbf);

					// װ��
					this.assembleArrayByConfName(idObjs, "table:" + rbf.getTable() + ":assemble:list",
							manager.getTable(rbf.getTable()));
					obj.put(rbf.getName(), idObjs);
				}
			}
		}

		return obj;
	}

	/**
	 * ���µ�ǰ��¼�˴��ڲ�ͬ����״̬�Ķ���������redis�еĻ���
	 * 
	 * @throws Exception
	 */
	protected void reloadOrderInfo() throws Exception {
		JSONArray order = engine.doQueryObjectArray(
				"select status,count(1) cnt from spo where i_com_id=? and otype='INL' group by status", new Object[] { usr.getComId() },
				conn);
		HashMap<String, String> orderMap = new HashMap();
		if (order.length() == 0) {
			// ������
			jedis.hdel("com:" + usr.getComId() + ":spostatus", "WP");
			// ������
			jedis.hdel("com:" + usr.getComId() + ":spostatus", "AP");
			// ���ջ�
			jedis.hdel("com:" + usr.getComId() + ":spostatus", "AS");
		} else {
			for (int i = 0; i < order.length(); i++) {
				JSONObject obj = order.getJSONObject(i);
				orderMap.put(obj.getString("status"), obj.getString("cnt"));
			}
			// ���redis�еĿ����Ϣ
			jedis.hmset("com:" + usr.getComId() + ":spostatus", orderMap);
			if (null == orderMap.get("WP")) {
				jedis.hdel("com:" + usr.getComId() + ":spostatus", "WP");
			}

			if (null == orderMap.get("AP")) {
				jedis.hdel("com:" + usr.getComId() + ":spostatus", "AP");
			}

			if (null == orderMap.get("AS")) {
				jedis.hdel("com:" + usr.getComId() + ":spostatus", "AS");
			}
		}
	}

	/**
	 * ���¼���ָ���̼Ҷ�����Ϣ
	 * 
	 * @param comid
	 * @throws Exception
	 */
	protected void reloadOrderInfo(long comid) throws Exception {
		JSONArray order = engine.doQueryObjectArray(
				"select status,count(1) cnt from spo where i_com_id=? and otype='INL' group by status", new Object[] { comid }, conn);
		HashMap<String, String> orderMap = new HashMap();
		if (order.length() == 0) {
			// ������
			jedis.hdel("com:" + comid + ":spostatus", "WP");
			// ������
			jedis.hdel("com:" + comid + ":spostatus", "AP");
			// ���ջ�
			jedis.hdel("com:" + comid + ":spostatus", "AS");
		} else {
			for (int i = 0; i < order.length(); i++) {
				JSONObject obj = order.getJSONObject(i);
				orderMap.put(obj.getString("status"), obj.getString("cnt"));
			}
			// ���redis�еĿ����Ϣ
			jedis.hmset("com:" + comid + ":spostatus", orderMap);
			if (null == orderMap.get("WP")) {
				jedis.hdel("com:" + comid + ":spostatus", "WP");
			}

			if (null == orderMap.get("AP")) {
				jedis.hdel("com:" + comid + ":spostatus", "AP");
			}

			if (null == orderMap.get("AS")) {
				jedis.hdel("com:" + comid + ":spostatus", "AS");
			}
		}
	}

	/**
	 * ��sql����ĵ�һ����Ϊkey���ڶ�����Ϊvalue���γɷ��صĶ���
	 * @param rows array of array, ������2�У����н�tostring����Ϊreturn obj��key
	 * @return ����
	 * @throws Exception
	 */
	protected JSONObject toKVObject(JSONArray rows) throws Exception{
		JSONObject kvObj=new JSONObject();
		for(int i=0;i<rows.length();i++){
			JSONArray row=rows.getJSONArray(i);
			String key=row.getString(0);
			Object value=row.get(1);
			kvObj.put(key, value);
		}
		return kvObj;
	}	
	
	/**
	 * �������ڣ�ȡ������˵ľ�����id������ȡ�г��ľ�����id
	 * @param actId b_prmt.id
	 * @param marketId b_market.id
	 * @return 
	 * @throws Exception
	 */
	protected int getCustomerIdByActOrMarket(int actId, int marketId) throws Exception{
		int customerId;
		if(actId==-1){
			customerId=engine.doQueryInt("select c_customer_id from b_market m where id=?",new Object[]{marketId}  , conn);
		}else{
			customerId=engine.doQueryInt("select u.c_customer_id from users u, b_prmt act where act.id=? and act.ownerid=u.id",new Object[]{actId}  , conn);
		}
		return customerId;
	}
	
	/**
	 * У�鲢���ع��ﳵ��Ʒ�Ƿ��������ҵ��Ҫ�����磬both��ĿҪ���ﳵÿ����Ʒ��Ҫ��ż���䣬�Ҷ���>=6
	 * ����ȡad_sql#cart_checkout_ptds_state ��ʽ: select pdtid, code,message from b_cart where xxx�����صĶ����д������Ʒ����code >0
	 * 
	 * @return key: pdtid, value: status {c,m} c - code ���� int����0���Ǳ�ʾ����m - message ������ʾ string�� st �����ڱ�ʾ�޴���
	 * @throws Exception
	 */
	protected HashMap<Integer, JSONObject> getCartPdtStates() throws Exception{
		String sql=PhoneController.getInstance().getValueFromADSQL("cart_checkout_pdts_state", conn);
		HashMap<Integer, JSONObject> pdtObjs=new HashMap();
		if(Validator.isNotNull(sql)){
			JSONArray rows=PhoneController.getInstance().getDataArrayByADSQL("cart_checkout_pdts_state", vc, conn, true);
			for(int i=0;i<rows.length();i++){
				JSONObject row=rows.getJSONObject(i);
				int pdtId=row.getInt("pdtid");
				int code=row.getInt("code");
				String message=row.getString("message");
				JSONObject one=new JSONObject();
				one.put("c", code);
				one.put("m", message);
				pdtObjs.put(pdtId, one);
			}
		}
		
		return pdtObjs;
	}
	/**
	 * У�鲢���ع��ﳵ��Ʒ�Ƿ��������ҵ��Ҫ�����磬both��ĿҪ���ﳵÿ����Ʒ��Ҫ��ż���䣬�Ҷ���>=6
	 * ����ȡad_sql#cart_checkout_ptds_state ��ʽ: select pdtid, code,message from b_cart where xxx�����صĶ����д������Ʒ����code >0
	 * 
	 * @return key: pdtid, value: status {c,m} c - code ���� int����0���Ǳ�ʾ����m - message ������ʾ string�� st �����ڱ�ʾ�޴���
	 * @throws Exception
	 */
	protected JSONObject getCartPdtState(int pdtId) throws Exception{
		
		String sql=PhoneController.getInstance().getValueFromADSQL("cart_checkout_pdt_state", conn);
		JSONObject pdtObj=null;
		if(Validator.isNotNull(sql)){
			vc.put("pdtid", pdtId);
			JSONArray rows=PhoneController.getInstance().getDataArrayByADSQL("cart_checkout_pdt_state", vc, conn, true);
			
			for(int i=0;i<rows.length();i++){
				JSONObject row=rows.getJSONObject(i);
				int code=row.getInt("code");
				String message=row.getString("message");
				JSONObject one=new JSONObject();
				one.put("c", code);
				one.put("m", message);
				return one;
			}
		}
		
		return null;
	}
	
}
