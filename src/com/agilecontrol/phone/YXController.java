package com.agilecontrol.phone;

import java.io.StringWriter;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;

import com.agilecontrol.b2b.schema.Column;
import com.agilecontrol.b2b.schema.Table;
import com.agilecontrol.b2b.schema.TableManager;
import com.agilecontrol.nea.core.query.QueryEngine;
import com.agilecontrol.nea.core.util.ConfigValues;
import com.agilecontrol.nea.core.velocity.VelocityUtils;
import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.nea.util.Validator;

/**
 * code状态表
	code	详细描述
	200	操作成功
	201	客户端版本不对，需升级sdk
	301	被封禁
	302	用户名或密码错误
	315	IP限制
	403	非法操作或没有权限
	404	对象不存在
	405	参数长度过长
	406	对象只读
	408	客户端请求超时
	413	验证失败(短信服务)
	414	参数错误
	415	客户端网络问题
	416	频率控制
	417	自动登录失效
	418	通道不可用(短信服务)
	419	数量超过上限
	422	账号被禁用
	431	HTTP重复请求
	500	服务器内部错误
	503	服务器繁忙
	514	服务不可用
	509	无效协议
	998	解包错误
	999	打包错误
	
	群相关错误码	
	801	群人数达到上限
	802	没有权限
	803	群不存在
	804	用户不在群
	805	群类型不匹配
	806	创建群数量达到限制
	807	群成员状态错误
	808	申请成功
	809	已经在群内
	810	邀请成功
	
	音视频、白板通话相关错误码	
	9102	通道失效
	9103	已经在他端对这个呼叫响应过了
	11001	通话不可达，对方离线状态
	
	聊天室相关错误码	
	13001	IM主连接状态异常
	13002	聊天室状态异常
	13003	账号在黑名单中,不允许进入聊天室
	13004	在禁言列表中,不允许发言
	
	特定业务相关错误码	
	10431	输入email不是邮箱
	10432	输入mobile不是手机号码
	10433	注册输入的两次密码不相同
	10434	企业不存在
	10435	登陆密码或帐号不对
	10436	app不存在
	10437	email已注册
	10438	手机号已注册
	10441	app名字已经存在

 * 云信接口
 * @author chenmengqi
 *
 */
@Admin(mail="chen.mengqi@lifecycle.cn")
public class YXController {
	private static final Logger logger = LoggerFactory.getLogger(PhoneController.class);
	
	private static  YXController instance;
	
	private YXController(){}
	
	public static YXController getInstance(){
		if(instance == null){
			instance = new YXController();
		}
		return instance;
	}
	
	 /**
     * 注册云信用户
     * @param yxid  云信ID
     * @param yxpwd 云信token
     * @param name  名称
     * @return
     * @throws Exception
     * 
     * 用户已注册  
     * {"desc":"already register","code":414}
     */
	public static JSONObject YXUsrAdd(String yxid,String yxpwd,String name,String icon) throws Exception{
		
	   String url = "https://api.netease.im/nimserver/user/create.action";//创建云信ID
       // String url = "https://api.netease.im/nimserver/user/update.action";//云信ID更新
       // String url = "https://api.netease.im/nimserver/user/refreshToken.action";//更新并获取新token
       // String url = "https://api.netease.im/nimserver/user/updateUinfo.action";//更新用户名片
       // String url = "https://api.netease.im/nimserver/user/getUinfos.action";//获取用户名片
	  
	   // 设置请求的参数
       List<NameValuePair> nvps = new ArrayList<NameValuePair>();
     
       nvps.add(new BasicNameValuePair("accid", yxid)); 
       nvps.add(new BasicNameValuePair("token", yxpwd));
       nvps.add(new BasicNameValuePair("name", name));
     //  nvps.add(new BasicNameValuePair("icon", icon));
       /*nvps.add(new BasicNameValuePair("sign", "棒呆了。"));
        nvps.add(new BasicNameValuePair("email", "mu_daidai@126.com"));
        nvps.add(new BasicNameValuePair("gender", "0"));*/
        JSONObject result=yunxin(url, nvps);
        if(result.optInt("code")==200){
        	JSONObject upinfo=YXUpdateUinfo(yxid, icon,"");
        	if(upinfo.optInt("code")!=200){
        		result.put("Mext", "更新用户头像信息失败，请重新更新信息");
        		result.put("Mcode", -1);
        	}
        }
        return result;
	}
	
	public static void main(String[] args) throws Exception {
		/**
		 * 系统用户：麦+钱包
		 * {"code":200,"info":{"name":"麦+钱包","accid":"maijiatbrfvyujmikmju","token":"13074185296"}}
		 * 
		 * 系统用户：员工入职
		 * {"code":200,"info":{"name":"员工入职","accid":"maijiahusscfhuedwerf","token":"14096328521"}}
		 * 
		 * 系统用户：采购入库
		 * {"code":200,"info":{"name":"采购入库","accid":"maijiaqazxswerfvbgto","token":"15085239651"}}
		 * 
		 *  系统用户：麦+小秘
		 * {"code":200,"info":{"name":"麦+小秘","accid":"maijiarfgv96325ertyn","token":"16085239652"}}
		 * 
		 * 系统用户：客户审核
		 * {"code":200,"info":{"name":"客户审核","accid":"maijia5236oiuytgbhnm","token":"17063952856"}}
		 * 
		 * 系统用户：供货商审核
		 * {"code":200,"info":{"name":"供货商审核","accid":"maijiartyuoiuytgbhnm","token":"18032569852"}}
		 * 
		 * 系统用户：活动消息
		 * {"code":200,"info":{"name":"活动消息","accid":"maijiat89klo85bhytoh","token":"19085632695"}}
		 */
		
		//JSONObject j=YXUsrAdd("maijiatbrfvyujmikmju","13074185296","麦+钱包","http://img.1688mj.com/mjlogo.png");
		//JSONObject j=YXUsrAdd("maijiahusscfhuedwerf","14096328521","员工入职","http://img.1688mj.com/mjlogo.png");
		//JSONObject j=YXUsrAdd("maijiaqazxswerfvbgto","15085239651","采购入库","http://img.1688mj.com/mjlogo.png");
		//JSONObject j=YXUsrAdd("maijiarfgv96325ertyn","16085239652","麦+小秘","http://img.1688mj.com/mjlogo.png");
		//JSONObject j=YXUsrAdd("maijia5236oiuytgbhnm","17063952856","客户审核","http://img.1688mj.com/mjlogo.png");
		//JSONObject j=YXUsrAdd("maijiartyuoiuytgbhnm","18032569852","供货商审核","http://img.1688mj.com/mjlogo.png");
		//JSONObject j=YXUsrAdd("maijiat89klo85bhytoh","19085632695","活动消息","http://img.1688mj.com/mjlogo.png");
		//System.out.println(j.toString());
		
		//JSONObject jo=YXGetUinfo("maijia5236oiuytgbhnm");
		
		
	//	JSONObject jo=YXUpdateUinfo("maijiat89klo85bhytoh", "http://www.lifecycle.cn/lifecycle.gif", "");
		
	
		JSONObject jo=testSedMsg();
		System.out.println(jo.toString());
	}
	
	private static JSONObject testSedMsg() throws Exception{
		 //发送云信消息
		 String url = "https://api.netease.im/nimserver/msg/sendMsg.action";
		 List<NameValuePair> nvps = new ArrayList<NameValuePair>();
		 nvps.add(new BasicNameValuePair("from", "maijiartyuoiuytgbhnm"));
		 nvps.add(new BasicNameValuePair("ope", "0")); //0：点对点个人消息，1：群消息，其他返回414
		 nvps.add(new BasicNameValuePair("to", "ahtpsgnyt2m-dxdjeh_qag"));
		 nvps.add(new BasicNameValuePair("type", "0"));
		 
	
		 JSONObject jsoncontent=new JSONObject();
		 jsoncontent.put("msg", "亲, Jocelyn, 手机号 13661510937, 代表商家 Jocelyn 申请成为您的供货商，请审核.");
		 nvps.add(new BasicNameValuePair("body",jsoncontent.toString()));
		 JSONObject ext = new JSONObject("{\"u_id\":5,\"com_id\":158,\"cmd\":\"SupAudit\",\"type\":\"confirm\",\"emp_id\":176}");
		 nvps.add(new BasicNameValuePair("ext", ext.toString()));
		 JSONObject jo=yunxin(url, nvps);
		 
		 return jo;
	}
	
	/**
	 * 云信ID更新--更新token密码
	 * @param yxid
	 * @param yxpwd
	 * @return
	 * @throws Exception
	 */
/*	public JSONObject YXUsrUpdate(String yxid,String yxpwd) throws Exception{
		 String url = "https://api.netease.im/nimserver/user/update.action";
		  // 设置请求的参数
	       List<NameValuePair> nvps = new ArrayList<NameValuePair>();
	       nvps.add(new BasicNameValuePair("accid", yxid)); 
	       nvps.add(new BasicNameValuePair("token", yxpwd));
	       JSONObject result=yunxin(url, nvps);
	       return result;
	}*/
	
	/**
	 * 封禁云信ID
	 * @param yxid
	 * @return
	 * @throws Exception
	 */
	public JSONObject YXUsrBlock(String yxid) throws Exception{
		   String url = "https://api.netease.im/nimserver/user/block.action";
		   // 设置请求的参数
	       List<NameValuePair> nvps = new ArrayList<NameValuePair>();
	       nvps.add(new BasicNameValuePair("accid", yxid)); 
	       JSONObject result=yunxin(url, nvps);
	       return result;
	}
	
	/**
	 * 解禁云信ID
	 * @param yxid
	 * @return
	 * @throws Exception
	 */
	public JSONObject YXUsrUnblock(String yxid) throws Exception{
		   String url = "https://api.netease.im/nimserver/user/unblock.action";
		   // 设置请求的参数
	       List<NameValuePair> nvps = new ArrayList<NameValuePair>();
	       nvps.add(new BasicNameValuePair("accid", yxid)); 
	        
	       JSONObject result=yunxin(url, nvps);
	       
	       return result;
	}
	
	/**
	 * 更新用户信息 -- 头像,名称
	 * @param accid
	 * @param icon
	 * @return
	 * @throws Exception
	 */
	public static JSONObject YXUpdateUinfo(String accid,String icon,String name) throws Exception{
		   String url = "https://api.netease.im/nimserver/user/updateUinfo.action"; //更新用户名片
		   // 设置请求的参数
	       List<NameValuePair> nvps = new ArrayList<NameValuePair>();
	       nvps.add(new BasicNameValuePair("accid", accid)); 
	       if(!"".equals(icon)) nvps.add(new BasicNameValuePair("icon", icon)); 
	       if(!"".equals(name)) nvps.add(new BasicNameValuePair("name", name));
	        
	       JSONObject result=yunxin(url, nvps);
	       return result;
	}
	
	public static JSONObject YXGetUinfo(String accid) throws Exception{
		   String url = "https://api.netease.im/nimserver/user/getUinfos.action"; //更新用户名片
		   // 设置请求的参数
	       List<NameValuePair> nvps = new ArrayList<NameValuePair>();
	       JSONArray accids = new JSONArray();
	       accids.put(accid);
	       nvps.add(new BasicNameValuePair("accids",accids.toString())); 
	        
	       JSONObject result=yunxin(url, nvps);
	       return result;
	}
	
	/**
	 * 云信消息发送
	 * @param fromAccid 发送者accid，用户帐号，最大32字节
	 * @param toAccid 接收者accid
	 * @param vc
	 * @param modelContent 消息内容模板, 这是ad_sql.name 里面的内容是将要发出的消息内容模板，变量将被vc变量替代
	 * @param ext	扩展模板 ，开发者扩展字段，长度限制1024字节。 可以为空
	 * @return
	 * @throws Exception
	 */
	public JSONObject YXSendMsg(String fromYXid,long toEmpId,String modelContent,JSONObject ext,VelocityContext vc,Connection conn,Jedis jedis) throws Exception{
		 String tableName="emp";
		 Table table=TableManager.getInstance().getTable(tableName);
		 ArrayList<Column> cols=table.getColumnsInObjectView();
		 JSONObject obj=PhoneUtils.getRedisObj(tableName, toEmpId, cols, conn, jedis);  
		 JSONObject result = new JSONObject(); 
		 if(Validator.isNotNull(obj.optString("yxid"))){
			//发送普通消息
			 String url = "https://api.netease.im/nimserver/msg/sendMsg.action";
			 List<NameValuePair> nvps = new ArrayList<NameValuePair>();
			 nvps.add(new BasicNameValuePair("from", fromYXid));
			 nvps.add(new BasicNameValuePair("ope", "0")); //0：点对点个人消息，1：群消息，其他返回414
			 nvps.add(new BasicNameValuePair("to", obj.optString("yxid")));
			 /**
			  * 0 表示文本消息,
				1 表示图片，
				2 表示语音，
				3 表示视频，
				4 表示地理位置信息，
				6 表示文件，
				100 自定义消息类型
			  */
			 nvps.add(new BasicNameValuePair("type", "0"));
			 
			 /*JSONObject content=new JSONObject();
	      	 content.put("msg", "hello,buding,中文不乱码");*/
			 if(Validator.isNull(modelContent)) throw new NDSException("YunXin Error: msg content is null");
			 String content=PhoneController.getInstance().getValueFromADSQL(modelContent, conn);
			 if(Validator.isNull(content)) throw new NDSException("未配置ad_sql#"+modelContent );
			 StringWriter output = new StringWriter();
			 Velocity.evaluate(vc, output, VelocityUtils.class.getName(), content);
			 content=output.toString();
			 JSONObject jsoncontent=new JSONObject();
			 jsoncontent.put("msg", content);
			 nvps.add(new BasicNameValuePair("body",jsoncontent.toString()));
			 
			 /*JSONObject ext=new JSONObject();
      	  	 ext.put("name", "test");
      	  	 ext.put("msg", "hello world");*/
			 if(ext!=null)nvps.add(new BasicNameValuePair("ext", ext.toString()));
			 result=yunxin(url, nvps);
			 
			 insertTable(fromYXid, obj.optString("yxid"), content, ext, conn);
		 }else{
			 throw new NDSException("YunXin Error: not found send to yxid");
		 }
		 
		 return result;
	}
	
	public JSONObject YXSendMsg(String fromYXid,String toYXid,String content,Connection conn) throws Exception{
		String url = "https://api.netease.im/nimserver/msg/sendMsg.action";
		 List<NameValuePair> nvps = new ArrayList<NameValuePair>();
		 nvps.add(new BasicNameValuePair("from", fromYXid));
		 nvps.add(new BasicNameValuePair("ope", "0")); //0：点对点个人消息，1：群消息，其他返回414
		 nvps.add(new BasicNameValuePair("to", toYXid));
		 nvps.add(new BasicNameValuePair("type", "0"));
		 
	
		 JSONObject jsoncontent=new JSONObject();
		 jsoncontent.put("msg", content);
		 nvps.add(new BasicNameValuePair("body",jsoncontent.toString()));
		 JSONObject jo=yunxin(url, nvps);
		 
		 insertTable(fromYXid, toYXid, content, new JSONObject(), conn);
		 return jo;
	}
	
	/**
	 * 批量发送点对点自定义系统通知
	 * @param fromYXid
	 * @param modelContent
	 * @param ext
	 * @param vc
	 * @param conn
	 * @param jedis
	 * @return
	 * @throws Exception
	 */
	public JSONObject YXSendBatchAttachMsg(String fromYXid,String modelContent,JSONObject ext,VelocityContext vc,Connection conn,Jedis jedis) throws Exception{
	/*	 String tableName="emp";
		 Table table=TableManager.getInstance().getTable(tableName);
		 ArrayList<Column> cols=table.getColumnsInObjectView();*/
		 JSONObject result = new JSONObject(); 
			//发送普通消息
			 String url = "https://api.netease.im/nimserver/msg/sendBatchAttachMsg.action";
			 List<NameValuePair> nvps = new ArrayList<NameValuePair>();
			 nvps.add(new BasicNameValuePair("fromAccid", fromYXid));
			 
		   if(Validator.isNull(modelContent)) throw new NDSException("YunXin Error: msg content is null");
			 String content=PhoneController.getInstance().getValueFromADSQL(modelContent, conn);
			 if(Validator.isNull(content)) throw new NDSException("未配置ad_sql#"+modelContent );
			 StringWriter output = new StringWriter();
			 Velocity.evaluate(vc, output, VelocityUtils.class.getName(), content);
			 content=output.toString();
			 content = "麦+测试";
			 JSONObject attach = new JSONObject();
			 attach.put("myattach",content);
			// attach.put("myattach", "麦+测试");
			 nvps.add(new BasicNameValuePair("attach", attach.toString())); 
			 
			 //["aaa","bbb"]（JSONArray对应的accid，如果解析出错，会报414错误），最大限500人
			QueryEngine engine=QueryEngine.getInstance();
			int count= (int) engine.doQueryInt("select count(*) from emp",new Object[]{}, conn);
			if(count>0){
				if(count % 500 == 0){
					count =count / 500;
				}else{
					count =count / 500 + 1;
				}
			}
			for (int i = 0; i < count; i++) {
				JSONArray yxids=engine.doQueryJSONArray("select yxid from emp where id between ? and ?", new Object[]{500*i,500*(i+1)-1}, conn);
				nvps.add(new BasicNameValuePair("toAccids",yxids.toString()));
				
				/*
				 * 发消息时特殊指定的行为选项,Json格式，可用于指定消息计数等特殊行为;option中字段不填时表示默认值。 
				 * badge:该消息是否需要计入到未读计数中，默认true;
				 */
				JSONObject option = new JSONObject();
				option.put("badge", false);
				nvps.add(new BasicNameValuePair("option", option.toString())); 
				
				result=yunxin(url, nvps);
				
				 insertTable(fromYXid, yxids.toString(), content, ext, conn);
			}
			 
		 return result;
	}
	
	/**
	 * 调用云信接口
	 * @param url 云信接口地址
	 * @param nvps
	 * @return
	 * @throws Exception
	 */
	public static JSONObject yunxin(String url,List<NameValuePair> nvps) throws Exception{
		/**
		 * 如果打开参数进行模拟，信息不发往云信
		 */
		/*if(PhoneConfig.YUNXIN_MOCK){
			return new JSONObject().put("code", 200);
		}*/
		DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpPost httpPost = new HttpPost(url);
         
        String nonce =  "qwertyui";   
        String curTime = String.valueOf((new Date()).getTime() / 1000L);
      //  String checkSum = CheckSumBuilder.getCheckSum(PhoneConfig.YUNXIN_APPSECRET, nonce ,curTime);//参考 计算CheckSum的java代码
        String checkSum = CheckSumBuilder.getCheckSum("32aabf959e93", nonce ,curTime);//参考 计算CheckSum的java代码

        // 设置请求的header
      //  httpPost.addHeader("AppKey", PhoneConfig.YUNXIN_APPKEY);
        httpPost.addHeader("AppKey", "ea8bc67bd431f533088812b696dbd567");
        httpPost.addHeader("Nonce", nonce);
        httpPost.addHeader("CurTime", curTime);
        httpPost.addHeader("CheckSum", checkSum);
        httpPost.addHeader("Content-Type", "application/x-www-form-urlencoded;charset=utf-8");
        
        httpPost.setEntity(new UrlEncodedFormEntity(nvps, "utf-8"));

        // 执行请求
        HttpResponse response = httpClient.execute(httpPost);

        // 打印执行结果
        // System.out.println(EntityUtils.toString(response.getEntity(), "utf-8"));
        JSONObject result= new JSONObject(EntityUtils.toString(response.getEntity(), "utf-8"));
        if(result.optInt("code") != 200){
			throw new NDSException("YunXin Error:"+result.toString());
		 }
        return result;
	}
	
	/**
	 * 写入数据库 ymsg
	 * @param fromYXid 发消息云信id
	 * @param toYXid   收消息云信id
	 * @param content  消息内容
	 * @param ext	        自定义扩展
	 * @param conn
	 * @throws Exception
	 */
	private void insertTable(String fromYXid,String toYXid,String content,JSONObject ext,Connection conn) throws Exception{
	      long objectId=PhoneController.getInstance().getNextId("ymsg", conn);
	      String extent="";
	      if(ext!=null)extent=ext.toString();
	      QueryEngine.getInstance().executeUpdate("insert into ymsg(id,yfrom,yto,content,ext) values(?,?,?,?,?)", new Object[]{objectId,fromYXid,toYXid,content,extent}, conn);
	}
}
