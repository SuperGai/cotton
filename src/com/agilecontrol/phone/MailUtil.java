package com.agilecontrol.phone;

import java.util.Date;
import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.agilecontrol.nea.util.NDSException;

/**
 * 发送邮件
 * @author chenmengqi
 *
 */
@Admin(mail="chen.mengqi@lifecycle.cn")
public class MailUtil {
	private static final Logger logger = LoggerFactory.getLogger(MailUtil.class);
	
	static int port = 25;
	 
    static String server = "smtp.exmail.qq.com";//邮件服务器mail.cpip.net.cn
 
    static String from = "麦+";//发送者,显示的发件人名字
 
    static String user = "noreply@lifecycle.cn";//发送者邮箱地址
 
    static String password = "lifecycle753951";//密码
    
    /**
     * 发送邮件，利用固定参数
     * @param email 发送的邮箱地址
     * @param subject 
     * @param body
     * @throws Exception
     */
    public static void sendEmail(JSONArray email,  String subject, String body) throws Exception {
        try {
            Properties props = new Properties();
            props.put("mail.smtp.host", server);
            props.put("mail.smtp.port", String.valueOf(port));
            props.put("mail.smtp.auth", "true");
            Transport transport = null;
            Session session = Session.getInstance(props, new Authenticator() {
                protected javax.mail.PasswordAuthentication getPasswordAuthentication() {
                    return new javax.mail.PasswordAuthentication(user,
                            password);
                }
            });
            transport = session.getTransport("smtp");
            transport.connect(server, user, password);
            MimeMessage msg = new MimeMessage(session);
            msg.setSentDate(new Date());
            InternetAddress fromAddress = new InternetAddress(user,from,"UTF-8");
            msg.setFrom(fromAddress);
            int len = email.length();
            InternetAddress[] toAddress = new InternetAddress[len];
            if(len>0){
            	for (int i = 0; i < len; i++) {
            		toAddress[i] = new InternetAddress(email.getString(i));
				}
            }else{
            	throw new  NDSException("发送人邮箱不能为空！");
            }
           
            //toAddress = new InternetAddress(email);
            msg.setRecipients(Message.RecipientType.TO, toAddress);
            msg.setSubject(subject, "UTF-8");  
            //msg.setText(body, "UTF-8");
            //MiniMultipart类是一个容器类，包含MimeBodyPart类型的对象   
            Multipart mainPart = new MimeMultipart();   
            //创建一个包含HTML内容的MimeBodyPart   
            BodyPart html = new MimeBodyPart();   
            //设置HTML内容   
            html.setContent(body, "text/html; charset=utf-8");   
            mainPart.addBodyPart(html);   
            //将MiniMultipart对象设置为邮件内容   
            msg.setContent(mainPart);   
            msg.saveChanges();
            //transport.sendMessage(msg, msg.getAllRecipients());   本地测试正常，服务器采用该方式报错
            Transport.send(msg);   
        } catch (NoSuchProviderException e) {
            e.printStackTrace();
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }
    public static void main(String args[]) throws Exception
    {
    	JSONArray emails = new JSONArray();
    	/*emails.put("jocelyn@lifecycle.cn");*/
    	emails.put("chen.mengqi@lifecycle.cn");
    	String img = "<img src='http://img.1688mj.com//storage/emulated/0/DCIM/Screenshots/Screenshot_2016-06-14-00-05-37-54.png'>";
    	
    	String meta ="<meta name=\"viewport\" content=\"initial-scale=1, maximum-scale=1, user-scalable=no, width=device-width\">";
    	String content = "kibana截图"+img;
        sendEmail(emails,"邮件测试",content);//收件人
        System.out.println("ok");
    }
}