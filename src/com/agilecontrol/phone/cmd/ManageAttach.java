package com.agilecontrol.phone.cmd;

import java.io.File;
import java.net.URLEncoder;
import java.util.*;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONArray;
import org.json.JSONObject;

import com.agilecontrol.nea.core.control.web.AttachmentManager;
import com.agilecontrol.nea.core.control.web.UserWebImpl;
import com.agilecontrol.nea.core.control.web.WebUtils;
import com.agilecontrol.nea.core.schema.Column;
import com.agilecontrol.nea.core.schema.Table;
import com.agilecontrol.nea.core.schema.TableManager;
import com.agilecontrol.nea.core.security.Directory;
import com.agilecontrol.nea.core.util.WebKeys;
import com.agilecontrol.nea.core.web.config.QueryListConfig;
import com.agilecontrol.nea.core.web.config.QueryListConfigManager;
import com.agilecontrol.nea.util.Attachment;
import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.nea.util.Tools;
import com.agilecontrol.nea.util.Validator;
import com.agilecontrol.phone.CmdHandler;
import com.agilecontrol.phone.CmdResult;
import com.agilecontrol.phone.PhoneController;

/**

�б�

    {cmd:"ManageAttach", action:"list", table:"$tablename", column:"$columnname", 
    objectid:"$recordid", file:"$url"}

ManageAttach֧�ֵ�������action������ delete��list���ṩ����Ϣ�У�file�ǵ�ǰ���µĴ洢���ݵ�url��ַ��ϵͳ��ʶ��url���ݣ�
���Ի�ȡ���е�tֵ���Զ�λ��̨��Ŀ¼����tֵ�����ڣ����Զ�ȡobjectidֵ�������ϰ汾��Ϊ�ձ�ʾ��δ�ϴ���ϵͳ��Ҫ�����µ�Ŀ¼��
֮������Ҫfile������Ϊ�����������棬���������޴�֪���ļ��洢��Ϣ��tֵ����Ҫ�ͻ��˸�����url�д洢��tֵ��Ϣ���������ڽ�����

ϵͳ����:

    {files:[url1,url2,...]}

ÿ��url��ַ����һ��ͼƬ�ļ�������version��Ϣ����ʽ�磺

    /servlets/binserv/Attach?table=16524&column=505822&t=1323&f=7D6AEB07&version=3.jpg

˳���ǰ��ϴ�˳�����У��汾�������۽��������µ��ļ���Ҳ�ǽ�����ʾ���ļ�����version=-1��������󡣿ͻ����ڷ��ص���������ʱ��
���Կ�����files[last]�����ݽ��и��¡�

ɾ��

    {cmd:"ManageAttach", action:"delete", table:"$tablename", column:"$columnname",
     objectid:"$recordid",file:"$url", files:[url1,url2]}

file �ǵ�ǰ�ֶα����urlֵ��ע�������Ȼ��������δ����״̬����̨������������tֵ���жϴ洢·����
files Ϊ���url��ַ��Ϣ���������˽�����ÿ��url�е�version��Ϣ���ҵ���̨���ļ�����ɾ����

����: {files:[url1,url2,...]} ��Ч��list���ͻ�����Ҫ����һ��url���쵽�ֶ�ֵ�ϡ�


2016.11.2 ����bug, ���url�Ǵ���������ֶθ��ƹ����ģ�list��ʱ����ʾԭ�ֶε�����ͼƬ

 * @author yfzhu
 *
 */
public class ManageAttach extends CmdHandler {
	
	private Table table;
	private Column col;
	private int objectId;
	private AttachmentManager attm;
	private TableManager manager;
	private UserWebImpl userWeb;
	
	@Override
	public CmdResult execute(JSONObject jo) throws Exception {
		
		Object tb= jo.get("table");
		manager=TableManager.getInstance();
		table= manager.findTable(tb);
		
		/*
		 * Accept whether column id or column number
		 * Modified by zhangbh on 20161127
		 */
		//col= manager.getColumn(jo.getInt("column") );
		Object oCol = jo.get("column");
		col = oCol instanceof Number ? 
				table.getColumn(((Number) oCol).intValue()) : 
				table.getColumn(oCol.toString());
				
		objectId= jo.optInt("objectid",-1);
		
		String fileURL= jo.optString("file");
		/*
		 * If no fileURL present, get it from database
		 * Added by zhangbh on 20161125
		 */
		if (fileURL == null || fileURL.isEmpty()) {
			String sql = "select " + col.getName() + " from " + table.getName() + " where id=?";
			logger.debug(sql);
			Object o = engine.doQueryOne(sql, new Object[] {objectId}, conn);
			if (o != null && o instanceof String) {
				fileURL = (String) o;
			}
		}
		
		String action= jo.getString("action");
		
		attm=(AttachmentManager)WebUtils.getServletContextManager().getActor(WebKeys.ATTACHMENT_MANAGER);
		
		/*
		 * No userWeb in the new CmdHandler, create it manually
		 * Added by zhangbh on 20161125
		 */
		userWeb = WebUtils.getUser(event.getContext().getHttpServletRequest());
		
		CmdResult cr=CmdResult.SUCCESS;
		if("list".equals(action)){
			// ��Ȩ��
        	if(objectId!=-1){
            	//
                if(!userWeb.hasObjectPermission(table.getName(),objectId,  
                		com.agilecontrol.nea.core.security.Directory.READ)){
                	throw new NDSException("@no-permission@");
                }
        	}else{
        		if(!userWeb.isPermissionEnabled(table.getSecurityDirectory(), Directory.READ))
            		throw new NDSException("@no-permission@");
        	}
			
			JSONArray files=new JSONArray();
			JSONObject obj= new JSONObject();
			obj.put("files", files);
			cr.setObject(obj);
			if(Validator.isNotNull(fileURL)) {
				if(fileURL.startsWith("/servlets/binserv/Attach?")|| fileURL.startsWith("/Attach")){
					String t=attm.findParamValue(fileURL,"t");
					Table tbl=TableManager.getInstance().getTable(Tools.getInt( attm.findParamValue(fileURL, "table"), -1));
					Column ccl=TableManager.getInstance().getColumn(Tools.getInt( attm.findParamValue(fileURL, "column"),-1));
					if(Validator.isNotNull(t)){
						long time= Tools.getLong(t, -1);
						if(time>0){
							File folder= attm.getTimeFolder(this.userWeb,tbl,ccl, time);
							String attachDir=String.valueOf(time);
							String attachParent=attm.getTimeFolderParent(userWeb, tbl, ccl, time);
							Attachment att=attm.getAttachmentInfo( attachParent, attachDir, -1 );
							
							if(att==null){
								//Ĭ�ϵĵ�һ���Ż�ȥ
								files.put(fileURL);
							}else{
								List atts=attm.getVersionHistory(att);
								for(int i=0;i<atts.size();i++){
									Attachment one= (Attachment)atts.get(i);
									String fname=one.getOrigFileName();
									if(Validator.isNull(fname)) fname="unknown";
									String url= "/servlets/binserv/Attach?table="+tbl.getId()+
											"&column="+ccl.getId()+"&objectid="+ objectId +"&version="+one.getVersion()
											+"&t="+ t +"&f="+ URLEncoder.encode(fname,"UTF-8");
									files.put(url);
								}
							}
						}else{
							logger.debug("Find t="+t+",not valid" );
							files.put(fileURL);
						}
					}else{
						//�����ϰ汾������t������Ŀ¼
						if(objectId>0){
							Attachment att=new Attachment( userWeb.getClientDomain()+"/" + table.getRealTableName()+"/"+col.getName(),  ""+objectId );
							List atts= attm.getVersionHistory(att);
							for(int i=0;i<atts.size();i++){
								Attachment one= (Attachment)atts.get(i);
								String fname=one.getOrigFileName();
								if(Validator.isNull(fname)) fname="unknown";
								String url= "/servlets/binserv/Attach?table="+table.getId()+
										"&column="+col.getId()+"&objectid="+ objectId +"&version="+one.getVersion()
										 +"&f="+ URLEncoder.encode(fname,"UTF-8");
								files.put(url);
							}
						}
					}
				}else{
					//Ĭ�ϵĵ�һ���Ż�ȥ
					logger.debug("Find url"+fileURL+", not starts with /servlets/binserv/Attach?");
					files.put(fileURL);
				}
			}else{
				// empty list
				logger.debug("fileURL is empty");
			}
		}else if("delete".equals(action)){
			//����Ȩ��
			// ��Ȩ��
        	if(objectId!=-1){
            	//
                if(!userWeb.hasObjectPermission(table.getName(),objectId,  
                		com.agilecontrol.nea.core.security.Directory.WRITE)){
                	throw new NDSException("@no-permission@");
                }
        	}else{
        		if(!userWeb.isPermissionEnabled(table.getSecurityDirectory(), Directory.WRITE))
            		throw new NDSException("@no-permission@");
        	}			
			
			JSONArray files=new JSONArray();
			JSONObject obj= new JSONObject();
			obj.put("files", files);
			cr.setObject(obj);

			
			JSONArray delFiles= jo.optJSONArray("files");
			
			if(Validator.isNotNull(fileURL)) {
				if(fileURL.startsWith("/servlets/binserv/Attach?") || fileURL.startsWith("/Attach")){
					String t=attm.findParamValue(fileURL,"t");
					if(Validator.isNotNull(t)){
						//�ҵ��ļ�Ŀ¼
						long time= Tools.getLong(t, -1);
						if(time>0){
							File folder= attm.getTimeFolder(this.userWeb,table,col, time);
							String attachParent=attm.getTimeFolderParent(userWeb, table, col, time);
							String attachDir=String.valueOf(time);
							Attachment att=attm.getAttachmentInfo(attachParent, attachDir, -1 );
							if(att==null){
								logger.debug("Not found attach folder");
								
							}else{
								List atts=attm.getVersionHistory(att);
								removeAttaches(atts, delFiles, files, att,t);
							}
						}else{
							logger.debug("Find t="+t+",not valid, empty all" );
							
						}
					}else{
						//�����ϰ汾������tΪĿ¼
						Attachment att=attm.getAttachmentInfo(userWeb.getClientDomain()+"/" + table.getRealTableName()+"/"+col.getName(),""+objectId, -1 );
						//new Attachment( userWeb.getClientDomain()+"/" + table.getRealTableName()+"/"+col.getName(),  ""+objectId );
						List atts=attm.getVersionHistory(att);
						removeAttaches(atts, delFiles, files, att,t);
					}
				}else{
					//��Ϊ��һ����ַ����attach��ֻ�Ǹ���ͨ��ַ��
					logger.debug("Find url"+fileURL+", not starts with /servlets/binserv/Attach, empty return");
					if(delFiles==null && delFiles.length()==0){
						//ֻ�в�ɾ���ļ���ʱ�򣬲ŷ���ԭʼ��Ϣ
						files.put(fileURL);
					}
				}
				
			}else{
				// empty list
				throw new NDSException("δ����file����");
			}
			//ǿ�Ƹ������ݿ���ֶ�����
			if(objectId!=-1){
				Object value;
				if(files.length()>0){
					value= files.getString(0);
				}else{
					value=String.class;
				}
				engine.executeUpdate("update "+ table.getRealTableName()+" set "+ col.getName()+"=? where id=?",
							new Object[]{value, objectId}, conn);
			}
			
		}else throw new NDSException("Unsupported action="+action);
		
		return cr;
	}
	
	private void removeAttaches(List atts, JSONArray delFiles, JSONArray files, Attachment att, String t) throws Exception{
		logger.debug("Remove att:"+ att.getFileName()+ " according to delFiles:"+ delFiles+", with file="+files+", for t="+ t);
		
		for(int i=0;i<atts.size();i++){
			Attachment one= (Attachment)atts.get(i);
//			logger.debug("one="+ one.getOrigFileName()+", "+one.getFileName()+", "+ one.getParentName());
			String url= "/servlets/binserv/Attach?table="+table.getId()+
					"&column="+col.getId()+"&objectid="+ objectId +"&version="+one.getVersion()
					+ (t==null? "": "&t="+ t )+"&f="+ URLEncoder.encode(att.getOrigFileName(),"UTF-8");
			int version=one.getVersion();
			logger.debug("find one :"+ one.getFileName()+", version="+ version);
			boolean deleted=false;
			for(int j=0;j<delFiles.length();j++){
				String delFile= delFiles.getString(j);
				int v=Tools.getInt( attm.findParamValue(delFile,"version"),-2);
				if(v==-2) v=Tools.getInt( attm.findParamValue(delFile,"v"),-2);
				if(v==version){
					//delete file
					File f=attm.getAttachmentFile(one);
					
					deleted=f.delete();
					logger.debug("Delete "+ f.getAbsolutePath()+":"+ deleted);
					break;
				}
			}
			if(!deleted)
				files.put(url);
		}		
	}
}
