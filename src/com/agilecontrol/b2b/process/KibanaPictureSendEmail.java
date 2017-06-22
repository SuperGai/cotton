package com.agilecontrol.b2b.process;

import java.io.File;
import java.io.StringWriter;
import java.sql.Connection;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.agilecontrol.b2b.qiniu.QiniuManager;
import com.agilecontrol.nea.core.process.SvrProcess;
import com.agilecontrol.nea.core.query.QueryEngine;
import com.agilecontrol.nea.core.util.ConfigValues;
import com.agilecontrol.nea.core.velocity.VelocityUtils;
import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.phone.Admin;
import com.agilecontrol.phone.CommandExecuter;
import com.agilecontrol.phone.MailUtil;
import com.agilecontrol.phone.PhoneConfig;
import com.agilecontrol.phone.PhoneController;
import com.qiniu.common.QiniuException;
import com.qiniu.http.Response;
import com.qiniu.storage.UploadManager;

/**
 * Kibana截图并发送邮件
 * @author chenmengqi
 *ad_sql#maijia_kibana_email
 {
 	"emails":["jocelyn@lifecycle.cn","chen.mengqi@lifecycle.cn"],
 	"title":"Kibana监控数据发送",
 	"body"："hello Kibana",
 	"url":"http://120.27.210.166:5601/goto/b424d746092b349be698579d2352748f"
 }
 */
@Admin(mail="chen.mengqi@lifecycle.cn")
public class KibanaPictureSendEmail extends SvrProcess {
	private static final Logger logger = LoggerFactory.getLogger(KibanaPictureSendEmail.class);
	
	@Override
	protected void prepare() {

	}

	@Override
	protected String doIt() throws Exception {
		Connection conn = null;
		QueryEngine engine=QueryEngine.getInstance();
		try {
			conn = engine.getConnection();
			
			JSONObject configs=(JSONObject) PhoneController.getInstance().getValueFromADSQLAsJSON("maijia_kibana_email", conn);
			if(configs == null) throw new NDSException("配置信息不存在ad_sql#maijia_kibana_email!");
			
			JSONArray emails = configs.optJSONArray("emails");
			String title = configs.optString("title","Kibana监控数据发送");
			String body = configs.optString("body","hello Kibana");
			String url = configs.optString("url","http://172.16.1.3:5601/goto/ff79a837b716dfdc3827b53b16e10b6b");
			String jsFile = configs.optString("jsFile","/opt/kibana/kibana.js");
			
		/*	String url="http://120.27.210.166:5601/goto/b424d746092b349be698579d2352748f";
			String title = "Kibana监控数据发送";
			String body = "hello Kibana";*/
			
			String picUrl=getKibanaPicture(url,jsFile);	
			MailUtil mailUtil = new MailUtil();
			String img = "<img src='"+picUrl+"'>";
			mailUtil.sendEmail(emails, title, body+img); //收件人
		} finally {
			engine.closeConnection(conn);
		}
		return "邮件发送成功";
	}
	
	 public String getKibanaPicture(String url,String jsFile) throws Exception{
	   
	    	String imageFileName="KibanaPicture"+System.currentTimeMillis()+".jpg";
	    	String imageURL="http://img.1688mj.com/"+ imageFileName;
	    	String imageFilePath=ConfigValues.DIR_NEA_ROOT+"/tmp/KibanaPicture.jpg";//+imageFileName;
	    	
	    	//"phantomjs ..\examples\dai_sec.js http://120.27.210.166:5601/goto/b424d746092b349be698579d2352748f  KibanaPicture.jpg"
	    	//"convert -size 512x512 canvas:$bgcolor -fill white -font /opt/portal6/$font -pointsize $pointsize -gravity Center -draw 'text 0,0 \"$txt\"' $output"
	    	//这里有个processcall的问题：draw后面的参数无法传递给runtime，通过生成sh 命令来解决
	    	VelocityContext vc = VelocityUtils.createContext();
			StringWriter output = new StringWriter();
			vc.put("jsFile", jsFile);
			vc.put("url", url);
			vc.put("output", imageFilePath);
			
			Velocity.evaluate(vc, output, VelocityUtils.class.getName(), PhoneConfig.KIBANA_PICTURE);
			String cmd=output.toString();

			CommandExecuter cmdrunner = new CommandExecuter(null);
			cmdrunner.run(cmd);
			
			File nf=new File(imageFilePath);
			if(nf.exists() && nf.length()>0){
				//ok
				String token=QiniuManager.getInstance().uploadToken(imageFileName, PhoneConfig.QINIU_UPLOAD_EXPIRE );
				Response res=null;
				try{
					long startTime=System.currentTimeMillis();
					res= new UploadManager().put(imageFilePath, imageFileName, token);
					logger.debug("send "+ imageFilePath+" with response: "+ res.bodyString()+", key="+imageFileName+", time="+ (System.currentTimeMillis()-startTime)/1000.0+" seconds");
					
				}catch(QiniuException ex){
					Response r = ex.response;
			        try {
				         
				        logger.error("Fail to upload to qiniu using "+ imageFilePath+": "+ r.bodyString());
			        } catch (QiniuException e1) {
			        	logger.error("Fail to read error message from qiniu", e1);
			        }
				}
				if(res==null || !res.isOK()){
					throw new NDSException("Kibana图片生成失败，请稍后再试");
				}
				
				nf.delete();
		 	      
			}else{
				logger.error("Fail to create "+ imageFilePath+", file not found");
				throw new NDSException("Kibana图片生成失败，请稍后再试");
			}
			
			return imageURL;
			
	    }

}
