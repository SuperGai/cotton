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

列表

    {cmd:"ManageAttach", action:"list", table:"$tablename", column:"$columnname", 
    objectid:"$recordid", file:"$url"}

ManageAttach支持的子命令action，包括 delete和list，提供的信息中，file是当前最新的存储内容的url地址，系统将识别url内容，
尝试获取其中的t值，以定位后台的目录。当t值不存在，尝试读取objectid值来兼容老版本。为空表示尚未上传，系统需要创建新的目录。
之所以需要file，是因为对于新增界面，服务器端无从知道文件存储信息即t值，需要客户端给出。url中存储了t值信息，可以用于解析。

系统返回:

    {files:[url1,url2,...]}

每个url地址都是一个图片文件，具有version信息，形式如：

    /servlets/binserv/Attach?table=16524&column=505822&t=1323&f=7D6AEB07&version=3.jpg

顺序是按上传顺序排列，版本号依次累进，即最新的文件（也是界面显示的文件，即version=-1）放在最后。客户端在返回单对象界面的时候，
可以考虑用files[last]的内容进行更新。

删除

    {cmd:"ManageAttach", action:"delete", table:"$tablename", column:"$columnname",
     objectid:"$recordid",file:"$url", files:[url1,url2]}

file 是当前字段保存的url值，注意可能仍然处于新增未保存状态，后台在其他检索到t值来判断存储路径。
files 为多个url地址信息，服务器端将解析每个url中的version信息，找到后台的文件进行删除。

返回: {files:[url1,url2,...]} 有效的list，客户端需要将第一个url构造到字段值上。


2016.11.2 发现bug, 如果url是从其他表的字段复制过来的，list的时候，显示原字段的所有图片

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
			// 读权限
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
								//默认的第一个放回去
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
						//兼容老版本，不以t来管理目录
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
					//默认的第一个放回去
					logger.debug("Find url"+fileURL+", not starts with /servlets/binserv/Attach?");
					files.put(fileURL);
				}
			}else{
				// empty list
				logger.debug("fileURL is empty");
			}
		}else if("delete".equals(action)){
			//更新权限
			// 读权限
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
						//找到文件目录
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
						//兼容老版本，不以t为目录
						Attachment att=attm.getAttachmentInfo(userWeb.getClientDomain()+"/" + table.getRealTableName()+"/"+col.getName(),""+objectId, -1 );
						//new Attachment( userWeb.getClientDomain()+"/" + table.getRealTableName()+"/"+col.getName(),  ""+objectId );
						List atts=attm.getVersionHistory(att);
						removeAttaches(atts, delFiles, files, att,t);
					}
				}else{
					//因为第一个地址不是attach，只是个普通网址，
					logger.debug("Find url"+fileURL+", not starts with /servlets/binserv/Attach, empty return");
					if(delFiles==null && delFiles.length()==0){
						//只有不删除文件的时候，才返回原始信息
						files.put(fileURL);
					}
				}
				
			}else{
				// empty list
				throw new NDSException("未定义file参数");
			}
			//强制更新数据库的字段内容
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
