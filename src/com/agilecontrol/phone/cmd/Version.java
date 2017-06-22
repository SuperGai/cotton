package com.agilecontrol.phone.cmd;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONArray;
import org.json.JSONObject;

import com.agilecontrol.phone.CmdResult;
import com.agilecontrol.phone.CmdHandler;
import com.agilecontrol.phone.PhoneUtils;
import com.agilecontrol.phone.util.*;
import com.agilecontrol.nea.core.query.QueryEngine;
import com.agilecontrol.nea.core.schema.TableManager;
import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.nea.util.Tools;
/**
 * ��ǰphone�İ汾�ţ��洢��plugin-phone.jar#phone.txt�ļ��ĵ�һ��
 * @author yfzhu
 *
 */
public class Version extends CmdHandler {
	/**
	 * Guest can execute this task, default to false
	 * 
	 * @return
	 */
	public boolean allowGuest() {
		return true;
	}
	@Override
	public CmdResult execute(JSONObject jo) throws Exception {
		
		String message=getPluginVersion(true);
		
		CmdResult cr=new CmdResult();
		JSONObject cro= new JSONObject();
		cro.put("message", message);
		cr.setObject(cro);
		
		return cr;
	}
	/**
	 * ��ȡ��ǰPlugin�İ汾��Ϣ
	 * @param includePath �Ƿ��е�ǰplugin·��
	 * @return
	 * @throws Exception
	 */
	private String getPluginVersion(boolean includePath) throws Exception{
		StringBuilder sb=new StringBuilder();
		if(includePath){
			String path= Tools.which(Version.class , Version.class.getName());
			int jarPos= path.indexOf(".jar");
			if(jarPos>0){
				path= path.substring(0, jarPos+4);
			}
			sb.append("�ļ�λ��: ").append(path).append("</br> ");
		}
		
		String message=getResourceAsString("version.txt");
		if(message!=null){
			message=message.split("\\n")[0];//ȡ��һ��
		}else{
			message="�ް汾��Ϣ";
		}
		
		sb.append("�汾: ").append(message);
		return sb.toString();
		
	}
	/**
	 * Get resource as string��·������Ե�ǰclass��classLoadder,���磬��Ҫ��ȡ��ǰjar�ļ���
	 * META-INF�µ��ļ�a.txt, ��дΪ"META-INF/a.txt"
	 * @param resPath ���jar�ĸ�Ŀ¼��ʼ��·��
	 * @return �ļ����� null if not found
	 * @throws Exception
	 */
	private String getResourceAsString(String resPath) throws Exception{
		String content=null;
		
		InputStream ins =Version.class.getClassLoader().getResourceAsStream(resPath);
		if(ins==null){
			logger.warn("Not found resource "+ resPath );
			return content; 
		}
    	ByteArrayOutputStream outputstream = new ByteArrayOutputStream();
        byte[] str_b = new byte[8192];
        int i = -1;
        while ((i=ins.read(str_b)) > 0) {
           outputstream.write(str_b,0,i);
        }
        content = outputstream.toString();
        return content;
	}
}

