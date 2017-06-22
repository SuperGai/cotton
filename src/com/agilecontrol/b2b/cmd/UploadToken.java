package com.agilecontrol.b2b.cmd;

import org.json.JSONObject;

import com.agilecontrol.b2b.qiniu.QiniuManager;
import com.agilecontrol.phone.CmdHandler;
import com.agilecontrol.phone.CmdResult;
import com.agilecontrol.phone.PhoneConfig;
import com.agilecontrol.phone.wechat.auth.BudingAuth;
import com.qiniu.util.StringMap;

/**
 *
 * 获取上传图片服务器的token, 目前是基于七牛的上传接口
 * 
 * 目前不对用户进行限制，但统计用户的上传次数(redis stat:emp:$empid field=uploads)
 * 
 * 传人: { file*:string, size: double=0  }
 * 返回: {token: string}
 * 
 * @author yfzhu
 *
 */
public class UploadToken extends CmdHandler {

	@Override
	public CmdResult execute(JSONObject jo) throws Exception {
		
		String file=getString(jo, "file");
		double size=jo.optDouble("size", 0);
		//insert only
		String token=QiniuManager.getInstance().uploadToken(file, PhoneConfig.QINIU_UPLOAD_EXPIRE,new StringMap().put("insertOnly", 1 ));
		JSONObject ret=new JSONObject();
		ret.put("token", token);
		CmdResult res=new CmdResult(ret);
		
		return res;
	}

}
