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
 * code״̬��
	code	��ϸ����
	200	�����ɹ�
	201	�ͻ��˰汾���ԣ�������sdk
	301	�����
	302	�û������������
	315	IP����
	403	�Ƿ�������û��Ȩ��
	404	���󲻴���
	405	�������ȹ���
	406	����ֻ��
	408	�ͻ�������ʱ
	413	��֤ʧ��(���ŷ���)
	414	��������
	415	�ͻ�����������
	416	Ƶ�ʿ���
	417	�Զ���¼ʧЧ
	418	ͨ��������(���ŷ���)
	419	������������
	422	�˺ű�����
	431	HTTP�ظ�����
	500	�������ڲ�����
	503	��������æ
	514	���񲻿���
	509	��ЧЭ��
	998	�������
	999	�������
	
	Ⱥ��ش�����	
	801	Ⱥ�����ﵽ����
	802	û��Ȩ��
	803	Ⱥ������
	804	�û�����Ⱥ
	805	Ⱥ���Ͳ�ƥ��
	806	����Ⱥ�����ﵽ����
	807	Ⱥ��Ա״̬����
	808	����ɹ�
	809	�Ѿ���Ⱥ��
	810	����ɹ�
	
	����Ƶ���װ�ͨ����ش�����	
	9102	ͨ��ʧЧ
	9103	�Ѿ������˶����������Ӧ����
	11001	ͨ�����ɴ�Է�����״̬
	
	��������ش�����	
	13001	IM������״̬�쳣
	13002	������״̬�쳣
	13003	�˺��ں�������,���������������
	13004	�ڽ����б���,��������
	
	�ض�ҵ����ش�����	
	10431	����email��������
	10432	����mobile�����ֻ�����
	10433	ע��������������벻��ͬ
	10434	��ҵ������
	10435	��½������ʺŲ���
	10436	app������
	10437	email��ע��
	10438	�ֻ�����ע��
	10441	app�����Ѿ�����

 * ���Žӿ�
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
     * ע�������û�
     * @param yxid  ����ID
     * @param yxpwd ����token
     * @param name  ����
     * @return
     * @throws Exception
     * 
     * �û���ע��  
     * {"desc":"already register","code":414}
     */
	public static JSONObject YXUsrAdd(String yxid,String yxpwd,String name,String icon) throws Exception{
		
	   String url = "https://api.netease.im/nimserver/user/create.action";//��������ID
       // String url = "https://api.netease.im/nimserver/user/update.action";//����ID����
       // String url = "https://api.netease.im/nimserver/user/refreshToken.action";//���²���ȡ��token
       // String url = "https://api.netease.im/nimserver/user/updateUinfo.action";//�����û���Ƭ
       // String url = "https://api.netease.im/nimserver/user/getUinfos.action";//��ȡ�û���Ƭ
	  
	   // ��������Ĳ���
       List<NameValuePair> nvps = new ArrayList<NameValuePair>();
     
       nvps.add(new BasicNameValuePair("accid", yxid)); 
       nvps.add(new BasicNameValuePair("token", yxpwd));
       nvps.add(new BasicNameValuePair("name", name));
     //  nvps.add(new BasicNameValuePair("icon", icon));
       /*nvps.add(new BasicNameValuePair("sign", "�����ˡ�"));
        nvps.add(new BasicNameValuePair("email", "mu_daidai@126.com"));
        nvps.add(new BasicNameValuePair("gender", "0"));*/
        JSONObject result=yunxin(url, nvps);
        if(result.optInt("code")==200){
        	JSONObject upinfo=YXUpdateUinfo(yxid, icon,"");
        	if(upinfo.optInt("code")!=200){
        		result.put("Mext", "�����û�ͷ����Ϣʧ�ܣ������¸�����Ϣ");
        		result.put("Mcode", -1);
        	}
        }
        return result;
	}
	
	public static void main(String[] args) throws Exception {
		/**
		 * ϵͳ�û�����+Ǯ��
		 * {"code":200,"info":{"name":"��+Ǯ��","accid":"maijiatbrfvyujmikmju","token":"13074185296"}}
		 * 
		 * ϵͳ�û���Ա����ְ
		 * {"code":200,"info":{"name":"Ա����ְ","accid":"maijiahusscfhuedwerf","token":"14096328521"}}
		 * 
		 * ϵͳ�û����ɹ����
		 * {"code":200,"info":{"name":"�ɹ����","accid":"maijiaqazxswerfvbgto","token":"15085239651"}}
		 * 
		 *  ϵͳ�û�����+С��
		 * {"code":200,"info":{"name":"��+С��","accid":"maijiarfgv96325ertyn","token":"16085239652"}}
		 * 
		 * ϵͳ�û����ͻ����
		 * {"code":200,"info":{"name":"�ͻ����","accid":"maijia5236oiuytgbhnm","token":"17063952856"}}
		 * 
		 * ϵͳ�û������������
		 * {"code":200,"info":{"name":"���������","accid":"maijiartyuoiuytgbhnm","token":"18032569852"}}
		 * 
		 * ϵͳ�û������Ϣ
		 * {"code":200,"info":{"name":"���Ϣ","accid":"maijiat89klo85bhytoh","token":"19085632695"}}
		 */
		
		//JSONObject j=YXUsrAdd("maijiatbrfvyujmikmju","13074185296","��+Ǯ��","http://img.1688mj.com/mjlogo.png");
		//JSONObject j=YXUsrAdd("maijiahusscfhuedwerf","14096328521","Ա����ְ","http://img.1688mj.com/mjlogo.png");
		//JSONObject j=YXUsrAdd("maijiaqazxswerfvbgto","15085239651","�ɹ����","http://img.1688mj.com/mjlogo.png");
		//JSONObject j=YXUsrAdd("maijiarfgv96325ertyn","16085239652","��+С��","http://img.1688mj.com/mjlogo.png");
		//JSONObject j=YXUsrAdd("maijia5236oiuytgbhnm","17063952856","�ͻ����","http://img.1688mj.com/mjlogo.png");
		//JSONObject j=YXUsrAdd("maijiartyuoiuytgbhnm","18032569852","���������","http://img.1688mj.com/mjlogo.png");
		//JSONObject j=YXUsrAdd("maijiat89klo85bhytoh","19085632695","���Ϣ","http://img.1688mj.com/mjlogo.png");
		//System.out.println(j.toString());
		
		//JSONObject jo=YXGetUinfo("maijia5236oiuytgbhnm");
		
		
	//	JSONObject jo=YXUpdateUinfo("maijiat89klo85bhytoh", "http://www.lifecycle.cn/lifecycle.gif", "");
		
	
		JSONObject jo=testSedMsg();
		System.out.println(jo.toString());
	}
	
	private static JSONObject testSedMsg() throws Exception{
		 //����������Ϣ
		 String url = "https://api.netease.im/nimserver/msg/sendMsg.action";
		 List<NameValuePair> nvps = new ArrayList<NameValuePair>();
		 nvps.add(new BasicNameValuePair("from", "maijiartyuoiuytgbhnm"));
		 nvps.add(new BasicNameValuePair("ope", "0")); //0����Ե������Ϣ��1��Ⱥ��Ϣ����������414
		 nvps.add(new BasicNameValuePair("to", "ahtpsgnyt2m-dxdjeh_qag"));
		 nvps.add(new BasicNameValuePair("type", "0"));
		 
	
		 JSONObject jsoncontent=new JSONObject();
		 jsoncontent.put("msg", "��, Jocelyn, �ֻ��� 13661510937, �����̼� Jocelyn �����Ϊ���Ĺ����̣������.");
		 nvps.add(new BasicNameValuePair("body",jsoncontent.toString()));
		 JSONObject ext = new JSONObject("{\"u_id\":5,\"com_id\":158,\"cmd\":\"SupAudit\",\"type\":\"confirm\",\"emp_id\":176}");
		 nvps.add(new BasicNameValuePair("ext", ext.toString()));
		 JSONObject jo=yunxin(url, nvps);
		 
		 return jo;
	}
	
	/**
	 * ����ID����--����token����
	 * @param yxid
	 * @param yxpwd
	 * @return
	 * @throws Exception
	 */
/*	public JSONObject YXUsrUpdate(String yxid,String yxpwd) throws Exception{
		 String url = "https://api.netease.im/nimserver/user/update.action";
		  // ��������Ĳ���
	       List<NameValuePair> nvps = new ArrayList<NameValuePair>();
	       nvps.add(new BasicNameValuePair("accid", yxid)); 
	       nvps.add(new BasicNameValuePair("token", yxpwd));
	       JSONObject result=yunxin(url, nvps);
	       return result;
	}*/
	
	/**
	 * �������ID
	 * @param yxid
	 * @return
	 * @throws Exception
	 */
	public JSONObject YXUsrBlock(String yxid) throws Exception{
		   String url = "https://api.netease.im/nimserver/user/block.action";
		   // ��������Ĳ���
	       List<NameValuePair> nvps = new ArrayList<NameValuePair>();
	       nvps.add(new BasicNameValuePair("accid", yxid)); 
	       JSONObject result=yunxin(url, nvps);
	       return result;
	}
	
	/**
	 * �������ID
	 * @param yxid
	 * @return
	 * @throws Exception
	 */
	public JSONObject YXUsrUnblock(String yxid) throws Exception{
		   String url = "https://api.netease.im/nimserver/user/unblock.action";
		   // ��������Ĳ���
	       List<NameValuePair> nvps = new ArrayList<NameValuePair>();
	       nvps.add(new BasicNameValuePair("accid", yxid)); 
	        
	       JSONObject result=yunxin(url, nvps);
	       
	       return result;
	}
	
	/**
	 * �����û���Ϣ -- ͷ��,����
	 * @param accid
	 * @param icon
	 * @return
	 * @throws Exception
	 */
	public static JSONObject YXUpdateUinfo(String accid,String icon,String name) throws Exception{
		   String url = "https://api.netease.im/nimserver/user/updateUinfo.action"; //�����û���Ƭ
		   // ��������Ĳ���
	       List<NameValuePair> nvps = new ArrayList<NameValuePair>();
	       nvps.add(new BasicNameValuePair("accid", accid)); 
	       if(!"".equals(icon)) nvps.add(new BasicNameValuePair("icon", icon)); 
	       if(!"".equals(name)) nvps.add(new BasicNameValuePair("name", name));
	        
	       JSONObject result=yunxin(url, nvps);
	       return result;
	}
	
	public static JSONObject YXGetUinfo(String accid) throws Exception{
		   String url = "https://api.netease.im/nimserver/user/getUinfos.action"; //�����û���Ƭ
		   // ��������Ĳ���
	       List<NameValuePair> nvps = new ArrayList<NameValuePair>();
	       JSONArray accids = new JSONArray();
	       accids.put(accid);
	       nvps.add(new BasicNameValuePair("accids",accids.toString())); 
	        
	       JSONObject result=yunxin(url, nvps);
	       return result;
	}
	
	/**
	 * ������Ϣ����
	 * @param fromAccid ������accid���û��ʺţ����32�ֽ�
	 * @param toAccid ������accid
	 * @param vc
	 * @param modelContent ��Ϣ����ģ��, ����ad_sql.name ����������ǽ�Ҫ��������Ϣ����ģ�壬��������vc�������
	 * @param ext	��չģ�� ����������չ�ֶΣ���������1024�ֽڡ� ����Ϊ��
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
			//������ͨ��Ϣ
			 String url = "https://api.netease.im/nimserver/msg/sendMsg.action";
			 List<NameValuePair> nvps = new ArrayList<NameValuePair>();
			 nvps.add(new BasicNameValuePair("from", fromYXid));
			 nvps.add(new BasicNameValuePair("ope", "0")); //0����Ե������Ϣ��1��Ⱥ��Ϣ����������414
			 nvps.add(new BasicNameValuePair("to", obj.optString("yxid")));
			 /**
			  * 0 ��ʾ�ı���Ϣ,
				1 ��ʾͼƬ��
				2 ��ʾ������
				3 ��ʾ��Ƶ��
				4 ��ʾ����λ����Ϣ��
				6 ��ʾ�ļ���
				100 �Զ�����Ϣ����
			  */
			 nvps.add(new BasicNameValuePair("type", "0"));
			 
			 /*JSONObject content=new JSONObject();
	      	 content.put("msg", "hello,buding,���Ĳ�����");*/
			 if(Validator.isNull(modelContent)) throw new NDSException("YunXin Error: msg content is null");
			 String content=PhoneController.getInstance().getValueFromADSQL(modelContent, conn);
			 if(Validator.isNull(content)) throw new NDSException("δ����ad_sql#"+modelContent );
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
		 nvps.add(new BasicNameValuePair("ope", "0")); //0����Ե������Ϣ��1��Ⱥ��Ϣ����������414
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
	 * �������͵�Ե��Զ���ϵͳ֪ͨ
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
			//������ͨ��Ϣ
			 String url = "https://api.netease.im/nimserver/msg/sendBatchAttachMsg.action";
			 List<NameValuePair> nvps = new ArrayList<NameValuePair>();
			 nvps.add(new BasicNameValuePair("fromAccid", fromYXid));
			 
		   if(Validator.isNull(modelContent)) throw new NDSException("YunXin Error: msg content is null");
			 String content=PhoneController.getInstance().getValueFromADSQL(modelContent, conn);
			 if(Validator.isNull(content)) throw new NDSException("δ����ad_sql#"+modelContent );
			 StringWriter output = new StringWriter();
			 Velocity.evaluate(vc, output, VelocityUtils.class.getName(), content);
			 content=output.toString();
			 content = "��+����";
			 JSONObject attach = new JSONObject();
			 attach.put("myattach",content);
			// attach.put("myattach", "��+����");
			 nvps.add(new BasicNameValuePair("attach", attach.toString())); 
			 
			 //["aaa","bbb"]��JSONArray��Ӧ��accid��������������ᱨ414���󣩣������500��
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
				 * ����Ϣʱ����ָ������Ϊѡ��,Json��ʽ��������ָ����Ϣ������������Ϊ;option���ֶβ���ʱ��ʾĬ��ֵ�� 
				 * badge:����Ϣ�Ƿ���Ҫ���뵽δ�������У�Ĭ��true;
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
	 * �������Žӿ�
	 * @param url ���Žӿڵ�ַ
	 * @param nvps
	 * @return
	 * @throws Exception
	 */
	public static JSONObject yunxin(String url,List<NameValuePair> nvps) throws Exception{
		/**
		 * ����򿪲�������ģ�⣬��Ϣ����������
		 */
		/*if(PhoneConfig.YUNXIN_MOCK){
			return new JSONObject().put("code", 200);
		}*/
		DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpPost httpPost = new HttpPost(url);
         
        String nonce =  "qwertyui";   
        String curTime = String.valueOf((new Date()).getTime() / 1000L);
      //  String checkSum = CheckSumBuilder.getCheckSum(PhoneConfig.YUNXIN_APPSECRET, nonce ,curTime);//�ο� ����CheckSum��java����
        String checkSum = CheckSumBuilder.getCheckSum("32aabf959e93", nonce ,curTime);//�ο� ����CheckSum��java����

        // ���������header
      //  httpPost.addHeader("AppKey", PhoneConfig.YUNXIN_APPKEY);
        httpPost.addHeader("AppKey", "ea8bc67bd431f533088812b696dbd567");
        httpPost.addHeader("Nonce", nonce);
        httpPost.addHeader("CurTime", curTime);
        httpPost.addHeader("CheckSum", checkSum);
        httpPost.addHeader("Content-Type", "application/x-www-form-urlencoded;charset=utf-8");
        
        httpPost.setEntity(new UrlEncodedFormEntity(nvps, "utf-8"));

        // ִ������
        HttpResponse response = httpClient.execute(httpPost);

        // ��ӡִ�н��
        // System.out.println(EntityUtils.toString(response.getEntity(), "utf-8"));
        JSONObject result= new JSONObject(EntityUtils.toString(response.getEntity(), "utf-8"));
        if(result.optInt("code") != 200){
			throw new NDSException("YunXin Error:"+result.toString());
		 }
        return result;
	}
	
	/**
	 * д�����ݿ� ymsg
	 * @param fromYXid ����Ϣ����id
	 * @param toYXid   ����Ϣ����id
	 * @param content  ��Ϣ����
	 * @param ext	        �Զ�����չ
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
