package com.agilecontrol.b2bweb.cmd;

import java.sql.Connection;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.agilecontrol.b2b.cmd.ObjectGet;
import com.agilecontrol.b2b.cmd.Search;
import com.agilecontrol.b2b.schema.Table;
import com.agilecontrol.nea.core.control.ejb.command.PasswordBack;
import com.agilecontrol.nea.core.control.event.DefaultEventContext;
import com.agilecontrol.nea.core.control.event.DefaultWebEvent;
import com.agilecontrol.nea.core.control.event.EventContext;
import com.agilecontrol.nea.core.control.util.ValueHolder;
import com.agilecontrol.nea.core.control.web.WebUtils;
import com.agilecontrol.nea.core.query.QueryEngine;
import com.agilecontrol.nea.core.schema.TableManager;
import com.agilecontrol.nea.core.security.SecurityUtils;
import com.agilecontrol.nea.core.security.User;
import com.agilecontrol.nea.core.security.UserManager;
import com.agilecontrol.nea.core.security.pwd.PwdEncryptor;
import com.agilecontrol.nea.core.security.pwd.PwdStrength;
import com.agilecontrol.nea.core.util.ConfigValues;
import com.agilecontrol.nea.core.util.PwdGenerator;
import com.agilecontrol.nea.util.MD5Sum;
import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.nea.util.Tools;
import com.agilecontrol.nea.util.Validator;
import com.agilecontrol.phone.Admin;
import com.agilecontrol.phone.CmdHandler;
import com.agilecontrol.phone.CmdResult;
import com.agilecontrol.phone.PhoneController;
import com.agilecontrol.phone.PhoneUtils;

/**

����
{cmd:"b2b.password.back", email}

email - string ע��ʱ�Ǽǵ�����

ͨ��email�ҵ��û���Ϣ

Edit
���
{
    code,message
}

 * @author wu.qiong
 *
 */
public class PwdBack extends CmdHandler {
	private static Logger logger=LoggerFactory.getLogger(PwdBack.class);
	private EventContext rootUserContext;
	
	/**
	 * Guest can execute this task, default to false
	 * 
	 * @return
	 */
	public boolean allowGuest() {
		return true;
	}
	
	/**
	 * ϵͳ����Ա�Ļ���
	 * @return
	 */
	private static EventContext createRootUserContext(){
		EventContext rootUserContext=null;
		try{
			int rootUserId=Tools.getInt(QueryEngine.getInstance().doQueryOne("select id from users where name='root'"),893);
			User user=SecurityUtils.getUser(rootUserId);
			rootUserContext=new DefaultEventContext(user);
		}catch(Throwable t){
			logger.error("Fail to load root id", t);
			
		}
		return rootUserContext;
	}
	public PwdBack(){
		rootUserContext =createRootUserContext();
	}
	
	 /**
	 
	 * 
     *  0 ���������ʼ���Ψһ��ʶ�����ѷ��͵����� (mailsent) , �������Ϊ email
     *  1.�ж������Ƿ���� 
     *  1 ��ʾ��������ҳ��  fail ���κ��������
     *  2 ��ʾ����δ�ҵ� fail �������Ϊ email 
     *  3 �����쳣���Ժ����� fail
     *  4 ���������Ѿ����ڣ����������� fail �������Ϊ ck + uid
     *  0 ȷ������������Ч����ʾ��������ҳ��(Ψһ��ʶ��) (enterpwd) �������Ϊ ck+uid
     *  0 �����뱣��ɹ� (success) �������Ϊck �� pwd/pwdnew
     *  5 ���벻һ�»�̫��  fail �������Ϊck �� pwd/pwdnew enterpwd
     *  6 �ʼ��Ѿ���15�����ڷ�����������������ң����Ժ�����
     *
	 * 
	 */
	public CmdResult execute(JSONObject jo) throws Exception {
		
		String email = jo.optString("email", "");
		String ck = jo.optString("ck","");
		int uid = jo.optInt("uid", -1);
		
		JSONObject res = new JSONObject();
		if(email != ""){
			res = checkEmail(jo);
		}else if(ck != "" && uid != -1){
			res = checkCode(jo);
		}else{
			throw new NDSException("��֧�ָ�ģʽ!");
		}
		
		return new CmdResult(res);
		
	}
	
	/**
	 * 
	 * @Title:PwdBack ��֤����
	 * @Description:TODO
	 * @param jo - {email}
	 * @return {code,message}
	 * code -    int      0 - success    	|   1 -fail			|	2 fail						|	3 fail
	 * message - string   "������֤�ɹ�"  	|   "������δע��!"	|	"�ʼ��Ѿ�����,�뵽�����в鿴!"	|	"ҵ��ع��Ĵ���"
	 * @throws Exception JSONObject
	 * @throws
	 */
	public JSONObject checkEmail(JSONObject jo) throws Exception{
		JSONObject res = new JSONObject();
		try{
			String email = jo.optString("email");
			List al=QueryEngine.getInstance().doQueryList("select id, name, truename from users where email=?",new Object[]{email},conn);
			
			if(al.size()==0){
				res.put("code", 1);
				res.put("message", "������δע��!");
				return res;
			}
			// just one:
			al=(List) al.get(0);
			int userId=Tools.getInt( al.get(0),-1);
			String userName= (String)al.get(1);
			String trueName= (String)al.get(2);
			// ����ʼ���15�������Ѿ�����������ֹ�ٴη���
			int cnt=Tools.getInt(QueryEngine.getInstance().doQueryOne("select count(*) from u_pwdback where user_id=? and creationdate>sysdate - 1/96",new Object[]{userId},conn),0);
			if(cnt>0){
				res.put("code", 2);//too frequent
				res.put("message", "�ʼ��Ѿ�����,�뵽�����в鿴!");
				return res;
			}
			sendMail(userId, userName,conn);
			res.put("code", 0);
			res.put("message", "���������ʼ��ѷ��͵�����");
		}catch(Throwable t){
    		logger.debug(event.toDetailString());
    		logger.error("Fail",t);
    		//�ᵼ�������ҵ��ȫ���ع�
    		res.put("code", 3);
    		res.put("message", t.getMessage());
    	}finally{
    		helper.closeConnection(conn, event);
    	}
		return res;
	}
	
	/**
	 * @Title:PwdBack ��֤check code
	 * @Description:TODO
	 * @param jo {ck,uid,loginpass,verifypass}
	 * @return {code,message}
	 * code -    int      0 - success    	|   4 -fail				|	5 fail						
	 * message - string   "��֤�ɹ�"  		|   "���������Ѿ�����"	|	"����̫��"	
	 * @throws Exception JSONObject
	 * @throws
	 */
	public JSONObject checkCode(JSONObject jo) throws Exception{
		JSONObject res = new JSONObject();
		String ck = jo.optString("ck");
		int uid = jo.optInt("uid");
		try{
			// У��ck ����Ч��
			String name=findUser(ck,uid,conn);
			if(name!=null){
				// ������������
				String password= jo.optString("loginpass");
				String password2= jo.optString("verifypass");
				if(Validator.isNull(password)){
					res.put("code", 0);
    	    		res.put("message", "������������");
    	    		res.put("username",name);
				}else{
					if(PwdStrength.check(password)>1){
						saveNewPassword(uid,name, password,ck, conn,event);
						res.put("code", 0);
        	    		res.put("message", "���뱣��ɹ�");
        	    		res.put("username",name);
					}else{
						res.put("code", 5);
        	    		res.put("message", "����̫��");
        	    		res.put("username",name);
					}
				}
			}else{
				res.put("code", 4);
	    		res.put("message", "���������Ѿ�����");
			}
		}catch(Throwable t){
    		logger.debug(event.toDetailString());
    		logger.error("Fail",t);
    		//�ᵼ�������ҵ��ȫ���ع�
    		res.put("code", 3);
    		res.put("message", t.getMessage());
    	}finally{
    		helper.closeConnection(conn, event);
    	}
		return res;
	}
	/**
     * ���������ʼ���ָ���û�
     * @param userId
     * @param name users.name
     * @throws Exception
     */
	private void sendMail(int userId, String name, Connection conn) throws Exception{
		DefaultWebEvent evt=new DefaultWebEvent("CommandEvent",rootUserContext);
		evt.put(helper.PARAM_SQL_CONNECTION, conn);
		evt.setEventName("CommandEvent");
		
		// ������¼����ͨ������������ʼ�
		evt.setParameter("command", "ObjectCreate");
		evt.setParameter("table", "u_pwdback");
		evt.setParameter("nds.control.ejb.UserTransaction", "N");
		//evt.setParameter("directory", TableManager.getInstance().getTable("USERS").getSecurityDirectory());
		evt.setParameter("user_id__name", name);
		evt.setParameter("chkcode", createCheckCode());
		evt.setParameter("isused", "N");
		
		ValueHolder holder=helper.handleEvent(evt);
		logger.debug("send mail:"+holder.get("message"));
		/*if( Tools.getInt(holder.get("code"),-1)!=0){
			throw new NDSException( (String)holder.get("message"));
		}*/
	}
	 /**
     * ����ַ�����MD5��
     * @return
     */
    private String createCheckCode(){
    	String r=PwdGenerator.getPassword(32);
    	try{
    		r= MD5Sum.toCheckSumStr(r );
    	}catch(Throwable t){
    		logger.error("Fail to do md5", r);
    	}
		return r;
    }
    /**
	 * ����Ƿ�Ϊ��Ч�����ñ�ʶ
	 * @param ck
	 * @param uid users.id
	 * @return null if not found valid one
	 * @throws Exception
	 */
	private String findUser(String ck, int uid, Connection conn) throws Exception{
		return (String)QueryEngine.getInstance().doQueryOne(
				"select u.name from u_pwdback p, users u where p.user_id=? and p.chkcode=? and p.isused='N' and p.creationdate>sysdate- 1/48 and u.id=p.user_id",
				new Object[]{uid, ck}, conn);
	}
	/**
	 * ����ָ���û�������
	 * @param ck ��Ѱ�ҵ��û�
	 * @param passwd
	 * @throws Exception
	 */
	private void saveNewPassword(int userId,String userName, String passwd, String chkcode, Connection conn,DefaultWebEvent event) throws Exception{
		
		DefaultWebEvent evt=new DefaultWebEvent("CommandEvent",rootUserContext);
		evt.put(helper.PARAM_SQL_CONNECTION, conn);
		evt.setEventName("CommandEvent");
		
		evt.setParameter("command", "ChangePassword");
		evt.setParameter("userid", String.valueOf(userId));
		evt.setParameter("password1", passwd);
		evt.setParameter("password2", passwd);
		
		ValueHolder holder=helper.handleEvent(evt);
		logger.debug("change password:"+holder.get("message"));

		QueryEngine.getInstance().executeUpdate("update u_pwdback set isused='Y' where user_id=? and chkcode=?", 
				new Object[]{userId, chkcode}, conn);
		
		User user= SecurityUtils.getUser(userId);
		// ���浽COOKIE��
		SecurityUtils.addCookies(event.getContext().getHttpServletRequest(), 
				event.getContext().getHttpServletResponse(), userName, passwd,
				ConfigValues.SECURITY_REMEMBER_ME);
	}

}
