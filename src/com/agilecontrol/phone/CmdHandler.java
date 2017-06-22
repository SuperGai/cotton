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
 * MiscCmd 的实现器。每次命令都创建一个单独对象，是request级别的，不像Command是application级别的
 * 
 * @author yfzhu
 *
 */
@Admin(mail="sun.yifan@lifecycle.cn")
public abstract class CmdHandler {

	protected Logger logger = LoggerFactory.getLogger(getClass());

	protected UserObj usr;
	/**
	 * 标配的jedis连接，和conn一样，需要主动关闭
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
	 * 尝试解析jo中的key为long，如果key是jsonobj也可以，将默认读取obj的id key对应的值
	 * @param jo 界面传人的对象
	 * @param key 对象的key值对应long
	 * @return -1 如果没有找到
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
			logger.debug("不支持的类型:"+value.getClass().getName());
			return -1;
		}
	}
	/**
	 * 发送消息给云信ID，这样可以做销售单转采购单了
	 * @param obj spo
	 * @throws Exception
	 */
	protected String spoPaid(JSONObject obj) throws Exception{
		if("WP".equals(obj.optString("pstate"))) throw new NDSException("需付款才能发消息");
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
		if(custComId<=0) return "客户对于商家未连接";
		
		JSONObject custCom=PhoneUtils.getRedisObj("com", custComId, conn, jedis);
		if(custEmpId<=0){
			//looking for boss
			long bossUsrId=custCom.getLong("u_id");
			custEmpId=PhoneUtils.findEmpId(custComId, bossUsrId, vc, conn, jedis);			
		}
		if(custEmpId<=0) return "未找到对方服务人员";
		
		
		JSONObject com=PhoneUtils.getRedisObj("com", o_com_id, conn, jedis);
		vc.put("com",  com);
		
		JSONObject spo=PhoneUtils.getRedisObj("spo", obj.getLong("id"), conn, jedis);
		vc.put("docno", spo.getString("docno"));
		
		JSONObject ext=new JSONObject().put("spo_id", obj.getLong("id")).put("com_id", o_com_id).put("u_id", uid).put("type", "po").put("cmd", "PoAdd");
		YXController.getInstance().YXSendMsg(PhoneConfig.YUNXIN_SECRETARY, custEmpId, "yunxin:spo_paid", ext, vc, conn, jedis);
		
		//通知卖家有人付款
		if (flag) {
			JSONObject i_com=PhoneUtils.getRedisObj("com", i_com_id, conn, jedis);
			vc.put("i_com",  i_com);
			YXController.getInstance().YXSendMsg(PhoneConfig.YUNXIN_SECRETARY, o_emp_id, "yunxin:spo_paid_online", null, vc, conn, jedis);
		}
				
		return "已发送消息给对方";
	}
	
	


	/**
	 * 读取JOIN_URL, {0} 替换为querystr, querystr 的格式:
	 * DESUtil.encrypt($uid+","+$comid) 注意这个encrypt的输出是urlsafe的
	 * 
	 * @return 全网址, 如果用户当前不是有效付费使用者，分享无效
	 * @throws Exception
	 */
	protected String getJoinURL() throws Exception {
		String param = String.valueOf(usr.getId()) + "," + String.valueOf(usr.getComId());

		String query = DESUtil.encrypt(param, ConfigValues.SECURITY_SIMPLE_ENCRYPT_KEY);
		return PhoneConfig.JOIN_URL + query;
	}
	
	/**
	 * 根据usrId+comId生成JOIN_URL地址
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
	 * 服务器从redis构造的对象，需要进行TagColumn转变，因为TagColumn的内容要求是JSONArray,
	 * 而不是jedis/db中存储的string
	 * 
	 * @param editObj
	 * @param table
	 *            仅处理一层
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
						throw new NDSException("字段扩展属性定义有误，暂只支持  ja 或者 jo");
					}
				} else {
					if (isJo) {
						colValue = new JSONObject((String) cv);
					} else if (isJa) {
						colValue = new JSONArray((String) cv);
					} else {
						throw new NDSException("字段扩展属性定义有误，暂只支持  ja 或者 jo");
					}

				}
				editObj.put(tcol.getName(), colValue);

			}

		}
		return editObj;
	}

	/**
	 * 表中的字段若定义了扩展属性: {tagtable: { tb:"ptag", tag_column:"tag_id",
	 * main_column:"pdt_id" }} 则此字段的内容形式: [{id,name}]，其中 id 若为空，表示为新建
	 * 需要一方面将新建的tag重新建立id,另外一方面，需要将tag内容拆散到tb指定的table里
	 * 
	 * @param objectId
	 *            当前正在重构的记录的id，注意对新增操作，此命令在insert语句前
	 * @param table
	 *            take as pdt
	 * @param editObj
	 *            内含 tagtable column
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
				else throw new NDSException("说好的传JSONArray, 给的是:"+ cv);
			} else {
				logger.debug("unsupported colum value for tagcolumn:" + cv);
			}
			//为空表示客户端没有传过来，如果是[] 表示要清空，所以[] 是需要做delete 动作的
			if (colValue == null )
				continue;
			// 维护好ptag表
			engine.executeUpdate("delete from " + tcol.getTagTable() + " where " + tcol.getTagMainColumn() + " =?",
					new Object[] { objectId }, conn);
			if(colValue.length()==0){
				//这是要删除col的内容，正常传到数据库的内容是[], 由于需要指出is null的查询，所以需要改造editObj，传递StringBuilder.class进去
				editObj.put(tcol.getName(), StringBuilder.class);
			}else 
				for (int i = 0; i < colValue.length(); i++) {
				JSONObject tagObj = colValue.getJSONObject(i);
				long tagId = tagObj.optLong("id", -1);
				if (tagId <= 0) {
					// 没找到，2种可能，客户端创建的时候，其他客户端已经创建，或此为新客户端, 修改 pdt_tag表
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
	 * 根据标签名称找到相应id，如果没有，返回-1或创建新的标签（在当前商家） 在redis中存储结构：
	 * 
	 * mj:pdt_tag - hash key: name, value: id mj:pdt_tag:list - list, pdt_tag.id
	 * com:$comid:pdt_tag - hash key: name, value:id com:$comid:pdt_tag:list -
	 * list, pdt_tag.id
	 * 
	 * 维护: 如果 mj:pdt_tag:list 不存在，从数据库读一遍，如果 com:$comid:pdt_tag:lis
	 * 不存在，将先读取一遍，然后开始创建, 并更新上来
	 * 
	 * @param table
	 *            标签存储表，比如pdt_tag
	 * @param tagName
	 *            标签名
	 * @param create
	 *            是否创建新记录，如果没有找到的话。在平台级别的tagName不会有这个问题，故只要在com级别找就可以了
	 * @return -1 或指定标签的id，-1仅在create=false的情况下
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
	 * 将尝试加载排序好的tag id列表，包含平台级和商家级 在redis中存储结构：
	 * 
	 * mj:pdt_tag - hash key: name, value: id mj:pdt_tag_s - list, pdt_tag.id
	 * com:$comid:pdt_tag - hash key: name, value:id com:$comid:pdt_tag_s -
	 * list, pdt_tag.id
	 * 
	 * 维护: 如果 mj:pdt_tag_s 不存在，从数据库读一遍，如果 com:$comid:pdt_tag_s
	 * 不存在，将先读取一遍，然后开始创建, 并更新上来
	 *
	 * @param table
	 *            指定的标签表，如 pdt_tag, sup_tag
	 * @param checkOnly
	 *            不需要返回内容，只要确认redis内存加载完成
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
			// 未加载过
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
				jedis.set(listKey, "");// 拿了，但没有值
			}
		} else if ("string".equals(type)) {
			// 拿了，但没有值

		} else if ("list".equals(type)) {
			List<String> list = jedis.lrange(listKey, 0, jedis.llen(listKey));
			if (!checkOnly)
				for (String one : list)
					retIds.add(Tools.getLong(one, -1));
		} else
			throw new NDSException("错误的数据类型:" + type + ",key=" + "mj:" + table + "_s");

		// com:$comid:pdt_tag - hash key: name, value:id
		// com:$comid:pdt_tag:list - list, pdt_tag.id

		listKey = "com:" + usr.getComId() + ":" + table + "_s";
		type = jedis.type(listKey);
		if ("none".equals(type)) {
			// 未加载过
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
				jedis.set(listKey, "");// 拿了，但没有值
			}
		} else if ("string".equals(type)) {
			// 拿了，但没有值

		} else if ("list".equals(type)) {
			List<String> list = jedis.lrange(listKey, 0, jedis.llen(listKey));
			if (!checkOnly)
				for (String one : list)
					retIds.add(Tools.getLong(one, -1));
		} else
			throw new NDSException("错误的数据类型:" + type + ",key=" + "mj:" + table + "_s");

		// trans.exec();
		return retIds;
	}

	/**
	 * 根据comid和usrid 找到empId,创建yunxid
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
			throw new NDSException("员工未创建");
		setupYXId(empId);
	}

	/**
	 * 创建yunxid，根据empId 将发送信息给云信，申请创建用户
	 * 
	 * @param empId
	 * @throws Exception
	 */
	protected void setupYXId(long empId) throws Exception {
		// engine.doQueryObject(preparedSQL, params, upperCase)
		JSONObject emp = engine.doQueryObject("select * from emp where id=?", new Object[] { empId }, conn);
		String yxId = emp.optString("yxid");
		if (Validator.isNotNull(yxId))
			throw new NDSException("指定员工已经创建了云信ID");
		// 创建云信的
		int times = 0;
		boolean isSuccess = false;
		// 找到不重复的yunxinUser，最多20次
		while (times++ < 20) {
			try {
				String yunxinUser = UUIDUtils.compressedUuid().toLowerCase();
				String yunxinPassword = UUIDUtils.compressedUuid().substring(0, 12);
				engine.executeUpdate("update emp set yxid=?,yxpwd=? where id=?",
						new Object[] { yunxinUser, yunxinPassword, empId }, conn);
				// 呼叫陈梦琪
				YXController.getInstance().YXUsrAdd(yunxinUser, yunxinPassword, emp.optString("nkname"),
						emp.optString("img"));
				isSuccess = true;
				break;
			} catch (Throwable tx) {
				logger.error("Fail to update yunxin user and pwd for empid=" + empId + ":" + tx.getLocalizedMessage());
			}
		}
		if (!isSuccess)
			throw new NDSException("创建员工失败，请稍后再试");
		jedis.del("emp:" + empId);

	}

	/**
	 * 对地址进行解析， 邀请码
	 * 
	 * <pre>
	 * http://www.1688mj.com/bin/Join?usr=
	 * <encoded uid>&com=<encoded uid> </pre> 商家码 <pre>
	 * http://www.1688mj.com/bin/Join?com=<encoded uid> </pre>
	 * 
	 * @param url
	 * @return 如果格式不对，不会报错，返回为null, 或者 {com: long, usr:long} -1 or null when
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
	 * 需要是贯信的管理员身份, com_id 通过PhoneConfig.LIFECYCLE_COM_ID指定
	 * 
	 * @throws Exception
	 */
	public void checkIsLifecycleManager() throws Exception {
		if (!usr.getName().equals("root"))
			throw new NDSException("此功能需要管理员身份");
	}


	public void setDefaultWebEventHelper(DefaultWebEventHelper helper) {
		this.helper = helper;
	}

	/**
	 * 检查是否套餐到期, redis: com:$id key:'pkg',value:'YNW' Y - 套餐可用，N 套餐到期 W 微信账号
	 * 注意调用此方法的时候，即便是W类型，也会报错提示套餐过期
	 * 
	 * @throws Exception
	 *             商家套餐已过期，请通知老板续费
	 */
	protected void checkPackage(long comId) throws Exception {

		String key = "com:" + comId;
		String v = jedis.hget(key, "pkg");
		if (v == null) {
			JSONObject jo = PhoneUtils.getRedisObj("com", comId, conn, jedis);
			v = jo.getString("pkg");
		}
		// 按说法，com的套餐状态是在每天自动计算的，加了套餐也会改，故com上必定有值
		// if(v==null){
		// //pkg_status(p_com_id in number) return varchar2 ,
		// 传人com的id，检查com对应创建人的套餐是否到期， Y 可用，N
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
			throw new NDSException("商家套餐已过期，请通知老板续费");
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
	 * 两种方法找到表名：jo中的table参数，或者是class的名字去掉后缀
	 * 
	 * @param jo
	 * @param postFix
	 *            后缀 "Add"|"Modify"|"Delete"|"Void"|"Get" |"Search"
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
				throw new NDSException("解析不到表名:" + sc);
			tableName = sc.substring(0, idx);
		}
		Table table = manager.getTable(tableName);
		if (table == null)
			throw new NDSException("未定义表:" + tableName);
		return table;
	}

	/**
	 * 是否需要显示调试的结果，对于userschema，不用显示
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
	 * 约定好给lua传递的脚本参数对象，
	 * 
	 * @param objectId
	 *            如果不是单对象操作，传0即可
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
	 * 运行Lua脚本，脚本在ad_sql.scriptName指定的脚本中定义，输入参数: JSONObject, 返回JSONObject
	 * 
	 * @param scriptName
	 *            ad_sql.name
	 * @param jedis
	 * @return JSONObject | JSONArray 约定lua返回jsonobject是客户端要求的格式，lua内部返回的是String
	 *         cjson.encode(result), 需要再次解析。
	 * @throws Exception
	 */
	protected Object execRedisLua(String scriptName, Connection conn, Jedis jedis) throws Exception {
		// read from db
		JSONObject argsObj = createLuaArgObj(-1);
		return PhoneUtils.execRedisLua(scriptName, argsObj, conn, jedis);
	}

	/**
	 * 执行数据库的存储过程，要求存储过程参数: procName(objectId) 无返回值
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
	 * 按cachekey获取之前已经检索到的id列表，拼成复杂对象列表返回
	 * 
	 * @param cacheKey
	 *            "list:"+table.getName()+":"+usr.getId()+":"+ja.length()+":"+
	 *            mask+":"+ UUIDUtils.compressedUuid()
	 * @param start
	 * @param pageSize
	 *            页面行数
	 * @param table - 结果所在表           
	 * @return
	 * @throws Exception, ObjectNotFoundException 缓存已失效
	 */
	public SearchResult searchByCache(String cacheKey, int start, int pageSize, Table table, boolean idOnly) 
			throws ObjectNotFoundException, Exception {
		if (!jedis.exists(cacheKey))
			throw new ObjectNotFoundException("缓存已失效，请重新查询");

		List<String> ids = jedis.lrange(cacheKey, start, start + pageSize - 1);
		// 再延长30分钟
		jedis.expire(cacheKey, 30 * 60);

		JSONArray page = new JSONArray();
		for (int i = 0; i < pageSize && i < ids.size(); i++)
			page.put(Tools.getLong(ids.get(i), -1));
		
		JSONArray pageData;
		String[] keyParts = cacheKey.split(":");
		
		//Table table = manager.getTable(keyParts[1]);
		int jaLength = Tools.getInt(keyParts[3], -1);
		if (jaLength <= 0)
			throw new NDSException("cachekey无效(len)");

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
	 * 执行搜索，返回结果
	 * 
	 * @param req
	 * @return
	 * @throws Exception
	 */
	public SearchResult search(SearchRequest req) throws Exception {
		return search(req.toJSONObject());
	}

	/**
	 * 见 {@link #search(JSONObject, HashMap)}
	 * 
	 * @param jo
	 * @return
	 * @throws Exception
	 */
	public SearchResult search(JSONObject jo) throws Exception {
		return search(jo, null);
	}

	/**
	 * 执行搜索，返回结果
	 * 
	 * @param jo
	 *            输入 { table, exprs, querystr, maxcnt,pagesize, mask, usepref,
	 *            orderby } table - String 搜索的表名，如: emp exprs - Expression
	 *            精确到字段的过滤条件，元素key-value为 主表字段名和对应值, 多个条件表示并且and querystr -
	 *            String 通用查询字符串，与exprs为and关系 pagesize - int 页面的行数 maxcnt - int
	 *            最大查询结果数，有时候客户端控制1，后台最大为2000，前台设置的值不能超过2000，默认2000 mask -
	 *            string 字段取值， 可选"list"|"obj", 默认为"list", 有时客户端以卡片形式显示结果，就需要配置为
	 *            obj，以便获取更多显示字段数据（见TableManager#Column#mask) usepref -
	 *            boolean，是否使用常用查询条件，默认为true，在部分表上有设置常用查询条件，将结合此条件进行过滤 orderby -
	 *            string, 部分定制页面上有orderby选项，以关键字进行匹配 举例:
	 * 
	 *            { table:"spo", expres: {st_id: 33, emp_id: 20, state:
	 *            ["V","O", "S"], price: "12~20"}, querystr: "13918891588",
	 *            usepref: false, orderby: "stocktime_first"} }
	 * 
	 *            Expression 格式
	 * 
	 *            {key: value} key - 字段名称，需要在当前主表上 value -
	 *            字段的值，支持数组或单值，数组表示任意一个匹配 举例: {"st_id": 13} 表示要求st_id=13
	 * 
	 * @param additionalParam
	 *            额外的参数，比如商品表要对库存有误进行过滤，需要配置这样的额外条件 {key:
	 *            "exists(select 1 from stg where stg.st_id=? and stg.pdt_id=pdt.id and qty>0)"
	 *            , value: "13"}, 如果value是java.util.List，将允许多值代替？号
	 * @return
	 * @throws Exception
	 */
	public SearchResult search(JSONObject jo, HashMap<String, Object> additionalParam) throws Exception {
		
		String tablename = this.getString(jo, "table");
		Table table = manager.getTable(tablename);
		JSONObject exprs = jo.optJSONObject("exprs");
		// 如果使用expr, 默认不应设置usepref=true
		// if(exprs!=null && exprs.length()>0){
		// }else{
		// //没有设置查询条件，将尝试从pref里获取
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
		 * 排序定义，ad_sql#orderby:$name
		 * 
		 * 结构: {table: string, column: string, asc: boolean, join: string,
		 * param:[] } 指定是基于什么表的字段做排序,
		 * join指定和主表之间连接的关系，param是在join中出现的？的替代变量，支持$stid,$uid, $empid,$comid
		 * 目前只支持查询模式 举例: 本店热销商品在前 {table:"stg", column:"stg.samt", asc: false,
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
			orderbyDef = null;// 默认排序(id desc)虽然是定义，但定义为空

		ArrayList params = new ArrayList();
		StringBuilder sb = new StringBuilder();

		/**
		 * 如果exprs 里有tag类型的字段，找到tag表，需要进行多选匹配, 客户端传来的是id array 什么是tag类型字段:
		 * pdt表和ptag表，pdt.tag 字段的扩展定义:
		 * 
		 * {tagtable: { tb:"ptag", tag_column:"tag_id", main_column:"pdt_id" }}
		 * 
		 * 其中: tb: tag表，tag_column: tag表上用来匹配客户端传来的id
		 * array的字段，main_column：tag表上用来关联主表的字段
		 * 
		 * 构造的sql 语句:
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
			// 有可能重名
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
						throw new NDSException("不支持的参数类型:" + opi);
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
					// 数组, 之前的tag table已处理
					JSONObject tagTable = (JSONObject) col.getJSONProp("tagtable");
					/**支持普通字段 2016.4.22
					if (null == tagTable)
						throw new NDSException("目前仅支持tagtable传递数组查询");
						*/
					if(tagTable==null){
						//对普通字段进行识别和处理，每个元素之间是or 关系
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
					// 对于number类型，2个参数{min, max}
					if (col.getType() != Column.Type.NUMBER)
						throw new NDSException(columnName + "字段不是数字类型，不支持jsonobject搜索");

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
						//直接发到数据库
						sb.append(" and ").append(columnName).append(" ").append(valstr);
					}else{
						// 单值
						if (col.getType() == Column.Type.STRING) {
							// 全等匹配
							sb.append(" and ").append(columnName).append("=?");
							params.add(valstr);
						} else if (col.getType() == Column.Type.NUMBER || col.getType() == Column.Type.DATENUMBER) {
							// 支持范围，如果没有的话，用等号
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
									"不支持的字段类型:" + table.getName() + "." + columnName + "(" + col.getType() + ")");
					}//end if is null
				}
			}
		}

		// fuzzy search
		if (Validator.isNotNull(querystr)) {
			/**
			 * 查询字段，定义：String, 为字段名用逗号分隔，如: "docno,cust_id";
			 * 对于fk类型的字段，将默认去查找fk的第一个不是id的dks 字段
			 * 
			 */
			//搜索内容忽略大小写
			querystr = querystr.toLowerCase();
			String scs = (String) table.getJSONProp("search_on");
			if (Validator.isNotNull(scs)) {
				String[] scss = scs.split(",");
				sb.append(" and (");
				boolean isFirst = true;
				for (String cname : scss) {
					Column cl = table.getColumn(cname);
					if (cl == null)
						throw new NDSException(table + ".search_on扩展属性中的字段未定义:" + cname);

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
							throw new NDSException(table + ".search_on扩展属性中的字段:" + cname + "的FK表的dks无id以外的字段");
						sb.append(" exists(select 1 from " + fkt + " where " + fkt + ".id=" + table + "." + cl.getName()
								+ " and " + fkt + "." + sdk + " like ?)");
						params.add("%" + querystr + "%");

					}
					isFirst = false;
				}
				sb.append(")");

			} else
				throw new NDSException("需要配置" + table + "的search_on扩展属性");
		}

		// 关于tagtable的特殊exists语句补充
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
				if (!(val instanceof JSONArray)) continue; /*前面有处理is null的情况，输入为string，而不是array*/
					//throw new NDSException(key + "对应的字段为tag类型，请传递tag对象");
				if (((JSONArray) val).length() == 0)
					continue;

				// 输入值形成param参数
				JSONArray valArray = (JSONArray) val;
				StringBuilder valstr = new StringBuilder();
				for (int i = 0; i < valArray.length(); i++) {
					JSONObject tagObj = valArray.getJSONObject(i);
					valstr.append(tagObj.getLong("id")).append(",");
				}
				valstr.deleteCharAt(valstr.length() - 1);// 格式: 1,2,3

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
		// group by 适配特别的库存到色码的处理
		if (orderbyDef != null) {
			String postclause= orderbyDef.optString("postclause");
			if(Validator.isNotNull(postclause)){
				sb.append(postclause);
			}
		}
		// order by
		/**
		 * 排序定义，ad_sql#orderby:$name
		 * 
		 * 结构: {table: string, column: string, asc: boolean, join: string,
		 * param:[] } 指定是基于什么表的字段做排序,
		 * join指定和主表之间连接的关系，param是在join中出现的？的替代变量，支持$stid,$uid, $empid,$comid
		 * 目前只支持查询模式 举例: 本店热销商品在前 {table:"stg", column:"stg.samt", asc: false,
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

		// 分页返回，前pagesize条
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
	 * 返回指定id的明细, 首先按规则找到 $table:$objectid:$rbf.name 对应的List，如果有，全全部记录返回 否则，
	 * 读取rbf的lua配置，如果设置了lua函数，直接运行返回结果， 读取rbf的adsql配置，构建语句，或自建语句，格式: select id
	 * from $rbf.table where $rbf.column=$objectid and $rbf.filter order by id
	 * desc 保存明细到 $table:$objectid:$rbf.name
	 * 
	 * @param objectId
	 *            主表ID
	 * @param table
	 *            主表
	 * @param rbf
	 *            从表
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
				// 完全通过lua来构造返回的json array
				rows = (JSONArray) PhoneUtils.execRedisLua(rbf.getLuaName(), this.createLuaArgObj(objectId), conn,
						jedis);
			} else {

				if (Validator.isNotNull(rbf.getAdSQLName())) {
					// 用sql语句来运行的结果，类似: select id from xxx
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
			// 这个表示没有元素，直接返回
			return new JSONArray();
		} else
			throw new NDSException("内部错误，未知的类型");
		//
		Table rbfTable = manager.getTable(rbf.getTable());
		return PhoneUtils.getRedisObjectArray(rbfTable.getName(), rows, rbfTable.getColumnsInListView(), true, conn,
				jedis);

	}

	/**
	 * 界面传来的信息不可信任，需要挑出其中可以编辑(column.edit=true)的属性重构对象
	 * 另外，有种特殊情况，在新增的时候，还需要取出edit=false, defaultvalue!=null的值，直接插入,
	 * defaultValue的值来自后台的环境变量，即:$uid, $empid, $comid, $stid, $sysdate, $docno
	 * 
	 * @param table
	 * @param obj
	 *            可能包含不允许编辑的属性
	 * @param useDefaultValue
	 *            是否添加edit=false, defaultvalue!=null的字段
	 * @param checkNull
	 *            是否检查不允许为空的字段需要输入，针对add情况。修改的时候不需要，因为为空后不会再填充到update里面
	 * @return 每个属性都是当前用户可以编辑的
	 */
	protected JSONObject createEditObject(Table table, JSONObject obj, boolean useDefaultValue, boolean checkNull)
			throws Exception {
		// 加载table可编辑字段
		JSONObject editObj = new JSONObject();
		ArrayList<Column> cols = table.getEditColumns(0);
		for (Column col : cols) {
			Object value = obj.opt(col.getName());
			if (value != null) {
				if (value instanceof JSONObject) {
					if (col.getFkTable() == null)
						throw new NDSException("字段" + col.getNote() + "不是FK类型，却给了JSONObject");
					// 取id
					long fkId = ((JSONObject) value).optLong("id", -1);
					if (fkId <= 0) {
						editObj.put(col.getName(), JSONObject.NULL);
						// throw new
						// NDSException("字段"+col.getNote()+"对应JSONObject必须有id值");
					} else
						editObj.put(col.getName(), fkId);
				} /*
					 * else if(value instanceof JSONArray ){
					 * editObj.put(col.getName(), value.toString()); }
					 */else
					editObj.put(col.getName(), value);
			}
		}
		// 加载不可编辑但有默认值的字段
		if (useDefaultValue) {
			for (Column col : table.getColumns()) {
				if (!col.isEdit()) {
					String defaultValue = col.getDefaultValue();
					if (Validator.isNotNull(defaultValue)) {
						Object value = null;
						// 目前支持的就是$uid, $empid, $comid, $stid
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
									"不支持的变量" + table.getName() + "." + col.getName() + ".dfv:" + defaultValue);
						}
						editObj.put(col.getName(), value);
						logger.debug("add " + col.getName() + ", value=" + value);
					}
				}
			}
		}
		// 如果存在id,就作为update,如果不存在，就作为create, 针对cusr,musr,ctime,mtime进行处理
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
				// 检查是否不能为空
				if (!col.isNull() && (value == null || (Validator.isNull(value.toString()))))
					throw new NDSException("需要输入" + col.getNote());
			}
		return editObj; 
	}

	/**
	 * 判定obj对象，属于指定的table的记录，是否可被当前操作用户访问
	 * 如果表定义了com权限，表示仅允许内部员工访问，如果当前用户的comid与业务数据不一致，将报错 如果定义了admin, 表示仅允许管理员访问
	 * 
	 * @param table
	 * @param obj
	 * @throws Exception
	 *             如果没有权限就报错
	 */
	protected void checkTableWritePermission(Table table, JSONObject obj) throws Exception {
		
	}
	
	/**
	 * 判定obj对象，属于指定的table的记录，是否可被当前操作用户访问
	 * 如果表定义了com权限，表示仅允许内部员工访问，如果当前用户的comid与业务数据不一致，将报错 如果定义了admin, 表示仅允许管理员访问
	 * 
	 * @param table
	 * @param obj
	 * @throws Exception
	 *             如果没有权限就报错
	 */
	protected void checkTableReadPermission(Table table, JSONObject obj) throws Exception {
//		ArrayList<Table.Perm> perms = table.getPerms();
//		if (perms.contains(Table.Perm.ADMIN)) {
//			// 当前人员在当前商家需要是管理员
//			if (getCurrentUserRole() != UserObj.EmployeeRole.MANAGER)
//				throw new NDSException("需要管理员身份才能访问");
//		}
//		if (perms.contains(Table.Perm.COM)) {
//			// 当前表的数据仅供商家内部人员访问
//			long comId = obj.optLong("com_id", 0);
//			// if(comId<=0){
//			// logger.error("table "+ table.getName()+" obj "+ obj+" : not found
//			// com_id in it");
//			// throw new NDSException("数据有误，商家信息未设置");
//			//
//			// }
//			if (comId > 0 && comId != this.usr.getComId())
//				throw new NDSException("商家内部资料, 不允许外部人员访问");
//
//		}
	}

	protected void loggerDefaultWebEvent() throws Exception {
		logger.debug(event.toDetailString());
	}

	/**
	 * 处理pad端传来的关键字，将关键字中的单引号替换成两个单引号。
	 * 
	 * @param keyword
	 * @return
	 */
	public String dealWithTheKeyword(String keyword) {
		keyword.replaceAll("'", "''");
		return keyword;
	}

	/**
	 * 清除本身的缓存或连接, conn 和 jedis都是外部构造，无需缓存
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
	 * 执行任务
	 * 
	 * @param jo
	 *            任务参数
	 * @return 返回的内容将全部对应到ValueHolder相关项
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
			throw new NDSException(name + "未设置");
		return value;
	}

	protected long getLong(JSONObject jo, String name) throws Exception {
		long value = jo.optLong(name, -1);
		if (value == -1)
			throw new NDSException(name + "未设置");
		return value;
	}

	protected String getString(JSONObject jo, String name) throws Exception {
		String value = jo.optString(name);
		if (Validator.isNull(value))
			throw new NDSException(name + "未设置");
		return value;
	}

	protected JSONObject getObject(JSONObject jo, String name) throws Exception {
		JSONObject value = jo.optJSONObject(name);
		if (value == null)
			throw new NDSException(name + "未设置");
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
//		/* 去掉回车换行 */
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
	 * 判断当前文件是否是图片
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
	 * 递归删除目录下的所有文件及子目录下所有文件
	 * 
	 * @param dir
	 *            将要删除的文件目录
	 * @return boolean Returns "true" if all deletions were successful. If a
	 *         deletion fails, the method stops attempting to delete and returns
	 *         "false".
	 */
	private boolean _deleteDir(File dir) {
		if (dir.isDirectory()) {
			String[] children = dir.list();
			// 递归删除目录中的子目录下
			for (int i = 0; i < children.length; i++) {
				boolean success = _deleteDir(new File(dir, children[i]));
				if (!success) {
					return false;
				}
			}
		}
		// 目录此时为空，可以删除
		return dir.delete();
	}

	/***
	 * 清空某一个目录
	 * 
	 * @param dir
	 */
	public void cleanDir(File dir) {
		if (!dir.isDirectory()) {
			dir.mkdirs();
		} else {
			if (_deleteDir(dir)) {
				logger.debug("cleanDir 递归删除目录成功:" + dir.getAbsolutePath());
				dir.mkdir();
			} else {
				logger.debug("cleanDir 递归删除目录失败:" + dir.getAbsolutePath());
			}
		}
	}

	protected String capitalizeFirstLetter(String data) {
		String firstLetter = data.substring(0, 1).toUpperCase();
		String restLetters = data.substring(1);
		return firstLetter + restLetters;
	}

	/**
	 * 按配置再次组装对象 系统如果找不到对象就停止，如果fk关联定义错误报错
	 * 
	 * @param rows
	 *            element 按fetchObject构造的对象，符合元数据的结构，将被重构, 目前不支持明细表refbytable
	 * @param adsqlConfName
	 *            ad_sql的name指定的配置. 配置内容是json array： 元素样式: "field.fk1.fk2.."
	 *            fk需要是外键关联, field 是obj的属性，可能有2种: column或refbytable
	 *            如果是column，必须是fk类型
	 * @param table
	 *            当前操纵的表
	 */
	protected void assembleArrayByConfName(JSONArray rows, String adsqlConfName, Table table) throws Exception {
		JSONArray conf = (JSONArray) PhoneController.getInstance().getValueFromADSQLAsJSON(adsqlConfName,
				new JSONArray(), conn);
		assembleArray(rows, conf, table);
	}

	/**
	 * 获取指定记录的对象结构
	 * 
	 * @param rows
	 *            element 按fetchObject构造的对象，符合元数据的结构，将被重构, 目前不支持明细表refbytable
	 * @param link
	 *            string样式: "field.fk1.fk2.." fk需要是外键关联, field 是obj的属性，可能有2种:
	 *            column或refbytable 如果是column，必须是fk类型 系统如果找不到对象就停止，如果fk关联定义错误报错
	 * @param table
	 *            当前操纵的表
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
	 * 获取指定记录的对象结构
	 * 
	 * @param rows
	 *            element 按fetchObject构造的对象，符合元数据的结构，将被重构, 目前不支持明细表refbytable
	 * @param objAssembleConf
	 *            元素string样式: "field.fk1.fk2.." fk需要是外键关联, field 是obj的属性，可能有2种:
	 *            column或refbytable 如果是column，必须是fk类型 系统如果找不到对象就停止，如果fk关联定义错误报错
	 * @param table
	 *            当前操纵的表
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
	 * 按配置再次组装对象 系统如果找不到对象就停止，如果fk关联定义错误报错
	 * 
	 * @param obj
	 *            按fetchObject构造的对象，符合元数据的结构，将被重构
	 * @param adsqlConfName
	 *            ad_sql的name指定的配置. 配置内容是json array： 元素样式: "field.fk1.fk2.."
	 *            fk需要是外键关联, field 是obj的属性，可能有2种: column或refbytable
	 *            如果是column，必须是fk类型
	 * @param table
	 *            当前操纵的表
	 */
	protected void assembleObjectByConfName(JSONObject obj, String adsqlConfName, Table table) throws Exception {
		JSONArray conf = (JSONArray) PhoneController.getInstance().getValueFromADSQLAsJSON(adsqlConfName,
				new JSONArray(), conn);
		assembleObject(obj, conf, table);
	}

	/**
	 * 获取指定记录的对象结构
	 * 
	 * @param obj
	 *            按fetchObject构造的对象，符合元数据的结构，将被重构
	 * @param objAssembleConf
	 *            元素string样式: "field.fk1.fk2.." fk需要是外键关联, field 是obj的属性，可能有2种:
	 *            column或refbytable 如果是column，必须是fk类型 系统如果找不到对象就停止，如果fk关联定义错误报错
	 * @param table
	 *            当前操纵的表
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
	 * 获取指定记录的对象结构
	 * 
	 * @param obj
	 *            按fetchObject构造的对象，符合元数据的结构，将被重构
	 * @param objAssembleConf
	 *            元素string样式: "field.fk1.fk2.." fk需要是外键关联, field 是obj的属性，可能有2种:
	 *            column或refbytable 如果是column，必须是fk类型 系统如果找不到对象就停止，如果fk关联定义错误报错
	 * @param table
	 *            当前操纵的表
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
				throw new NDSException("link不是正常的外键定义串:" + current + "(" + table + ")");
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
	 * 获取指定记录的对象结构
	 * 
	 * @param obj
	 *            按fetchObject构造的对象，符合元数据的结构，将被重构
	 * @param olink
	 *            第一个元素是table的column(fk类型），后面的元素也是fk 系统如果找不到对象就停止，如果fk关联定义错误报错
	 * @param table
	 *            当前操纵的表
	 */
	private void assembleObjectByLinkedList(JSONObject obj, LinkedList<String> olink, Table table) throws Exception {
		if (olink.size() == 0 || obj == null)
			return;
		String colName = olink.pop();
//		logger.debug("before assemble:"+table+" onobj "+ obj+" for colName:"+ colName);
		Column col = table.getColumn(colName);
		if (col == null)
			throw new NDSException("未在" + table + "表上找到字段" + colName);

		Table fkTable = col.getFkTable();
		if (fkTable == null)
			throw new NDSException(table + "表上的字段" + colName + "不是FK类型");

		Object fkValue = obj.opt(colName);
		JSONObject fkObj = null;
		if (fkValue instanceof JSONObject) {
			// already fetched
			fkObj = (JSONObject) fkValue;
		} else {
			long fkId = Tools.getLong(fkValue, -1);

//			 logger.debug("find fkid="+ fkId+", of " + col.getTable()+"."+col.getName()+" obj:"+ obj+", fkValue="+(fkValue==null?"null":fkValue.getClass().getName())+" data:"+ fkValue);

			if (fkId == -1) {
				// 创造一个对象放上去结束
//				 logger.debug("$$$$$$$$$$$$$$$$$ found emptypefkId"+" for:"+colName+": "+fkValue );
				 obj.put(colName, new JSONObject());
				return;
			}
			// 对象化fk
			fkObj = fetchObject(fkId, fkTable.getName(), false);
			// 组装到当前对象
			obj.put(colName, fkObj);
		}
		// 再下一层
		assembleObjectByLinkedList(fkObj, olink, fkTable);

		logger.debug("after assemble:"+table+" onobj "+ obj);
	}

	/**
	 * 完全按meta元数据配置来返回对象结构，元数据默认返回第一层
	 * 
	 * @param objectId
	 * @param tableName
	 * @param withItemTables
	 *            是否需要挂载明细表对象/数组，true将读取所有table上定义的refbys的明细表，如果需要个别挂载，使用方法
	 *            {@link #fetchObject(long, String, String)}
	 * @return 构建的对象
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
	 * 完全按meta元数据配置来返回对象结构，元数据默认返回第一层, cols 是 getColumnsInObjectView
	 * 
	 * @param objectId
	 * @param tableName
	 *            表名称
	 * @param itemTables
	 *            指定的需要装在的明细表的refbys.name, 注意不是明细表的名称，而是明细表+"_s"
	 * @return
	 */
	protected JSONObject fetchObject(long objectId, String tableName, String[] itemTables) throws Exception {
		Table table = manager.getTable(tableName);
		ArrayList<Column> cols = table.getColumnsInObjectView();
		return fetchObject(objectId, tableName, cols, itemTables);
	}

	/**
	 * 完全按meta元数据配置来返回对象结构，元数据默认返回第一层
	 * 
	 * @param objectId
	 * @param tableName
	 *            表名称
	 * @param itemTables
	 *            指定的需要装在的明细表的refbys.name, 注意不是明细表的名称，而是明细表+"_s"
	 * @return
	 */
	protected JSONObject fetchObject(long objectId, String tableName, ArrayList<Column> cols, String[] itemTables)
			throws Exception {

		Table table = manager.getTable(tableName);

		// logger.debug(table.toJSONObject().toString());

		JSONObject obj = PhoneUtils.getRedisObj(tableName, objectId, cols, conn, jedis);

		// 权限判定
		// checkTableReadPermission(table, obj);

		// for(Column fkc: cols){
		// if(fkc.getFkTable()==null)continue;
		// long fkId=obj.optLong(fkc.getName(), -1);
		// JSONObject fko=null;
		// if(fkId<=0){
		// //按界面要求，需要构造空数据过去，而不是null
		// fko=PhoneUtils.createBlankFKObj(fkc.getFkTable());
		// }else{
		// fko=PhoneUtils.getRedisObj(fkc.getFkTable().getName(), fkId ,
		// fkc.getFkTable().getColumnsInObjectView()
		// /*fkc.getFkTable().getDisplayKeys()*/ , vc, conn, jedis);
		// }
		// obj.put(fkc.getName(),fko );
		// }

		// 明细表控制
		if (itemTables != null) {
			for (String name : itemTables) {
				RefByTable rbf = table.getRefByTable(name);
				if (rbf == null)
					throw new NDSException("未定义子表" + name);
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
						// 装配
						JSONArray conf = (JSONArray) PhoneController.getInstance().getValueFromADSQLAsJSON(
								"table:" + rbf.getTable() + ":assemble:obj", new JSONArray(), conn);
						this.assembleObject(rbo, conf, manager.getTable(rbf.getTable()));

					}
					obj.put(rbf.getName(), rbo);
				} else {
					// multiple lines
					JSONArray idObjs = getItemIds(objectId, table, rbf);

					// 装配
					this.assembleArrayByConfName(idObjs, "table:" + rbf.getTable() + ":assemble:list",
							manager.getTable(rbf.getTable()));
					obj.put(rbf.getName(), idObjs);
				}
			}
		}

		return obj;
	}

	/**
	 * 更新当前登录人处于不同处理状态的订单数量在redis中的缓存
	 * 
	 * @throws Exception
	 */
	protected void reloadOrderInfo() throws Exception {
		JSONArray order = engine.doQueryObjectArray(
				"select status,count(1) cnt from spo where i_com_id=? and otype='INL' group by status", new Object[] { usr.getComId() },
				conn);
		HashMap<String, String> orderMap = new HashMap();
		if (order.length() == 0) {
			// 待付款
			jedis.hdel("com:" + usr.getComId() + ":spostatus", "WP");
			// 待发货
			jedis.hdel("com:" + usr.getComId() + ":spostatus", "AP");
			// 待收货
			jedis.hdel("com:" + usr.getComId() + ":spostatus", "AS");
		} else {
			for (int i = 0; i < order.length(); i++) {
				JSONObject obj = order.getJSONObject(i);
				orderMap.put(obj.getString("status"), obj.getString("cnt"));
			}
			// 变更redis中的库存信息
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
	 * 重新计算指定商家订单信息
	 * 
	 * @param comid
	 * @throws Exception
	 */
	protected void reloadOrderInfo(long comid) throws Exception {
		JSONArray order = engine.doQueryObjectArray(
				"select status,count(1) cnt from spo where i_com_id=? and otype='INL' group by status", new Object[] { comid }, conn);
		HashMap<String, String> orderMap = new HashMap();
		if (order.length() == 0) {
			// 待付款
			jedis.hdel("com:" + comid + ":spostatus", "WP");
			// 待发货
			jedis.hdel("com:" + comid + ":spostatus", "AP");
			// 待收货
			jedis.hdel("com:" + comid + ":spostatus", "AS");
		} else {
			for (int i = 0; i < order.length(); i++) {
				JSONObject obj = order.getJSONObject(i);
				orderMap.put(obj.getString("status"), obj.getString("cnt"));
			}
			// 变更redis中的库存信息
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
	 * 将sql结果的第一列作为key，第二列作为value，形成返回的对象
	 * @param rows array of array, 必须有2列，首列将tostring后作为return obj的key
	 * @return 对象
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
	 * 如果活动存在，取活动创建人的经销商id，否则，取市场的经销商id
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
	 * 校验并返回购物车商品是否符合特殊业务要求，例如，both项目要求购物车每个商品需要上偶数箱，且定量>=6
	 * 将读取ad_sql#cart_checkout_ptds_state 格式: select pdtid, code,message from b_cart where xxx，返回的都是有错误的商品，即code >0
	 * 
	 * @return key: pdtid, value: status {c,m} c - code 代码 int，非0都是表示错误，m - message 错误提示 string， st 不存在表示无错误
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
	 * 校验并返回购物车商品是否符合特殊业务要求，例如，both项目要求购物车每个商品需要上偶数箱，且定量>=6
	 * 将读取ad_sql#cart_checkout_ptds_state 格式: select pdtid, code,message from b_cart where xxx，返回的都是有错误的商品，即code >0
	 * 
	 * @return key: pdtid, value: status {c,m} c - code 代码 int，非0都是表示错误，m - message 错误提示 string， st 不存在表示无错误
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
