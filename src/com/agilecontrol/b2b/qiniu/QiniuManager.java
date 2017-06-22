package com.agilecontrol.b2b.qiniu;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.agilecontrol.nea.util.NDSRuntimeException;
import com.agilecontrol.nea.util.Validator;
import com.agilecontrol.phone.Admin;
import com.agilecontrol.phone.PhoneConfig;
import com.qiniu.util.Auth;
import com.qiniu.util.StringMap;

/**
 * 实现七牛的服务访问
 * 
 * 七牛是图片CDN服务器，客户端上传图片到七牛前，需要获取 uploadToken，得到授权后才能上传，从而保证安全性
 * 
 * 关于七牛接口说明，可以访问：
 * http://developer.qiniu.com/code/v7/sdk/java.html#install
 * 
 * @author yfzhu
 *
 */
@Admin(mail="yfzhu@lifecycle.cn")
public class QiniuManager {
	private static final Logger logger = LoggerFactory.getLogger(QiniuManager.class);
	private static QiniuManager instance=null;
	/**
	 * 七牛的认证计算器
	 */
	private Auth auth;
	
	
	private QiniuManager(){}
	/**
	 * 图片token需要读取ACCESS_KEY, SECRET_KEY，如果修改了这两个参数，需要重新加载
	 */
	public void reload(){
		if(Validator.isNull(PhoneConfig.QINIU_ACCESS_KEY) || Validator.isNull(PhoneConfig.QINIU_SECRET_KEY)
					|| Validator.isNull(PhoneConfig.QINIU_BUCKET) )
			throw new NDSRuntimeException("请配置七牛相关参数");
		auth=Auth.create(PhoneConfig.QINIU_ACCESS_KEY,PhoneConfig.QINIU_SECRET_KEY);
	}
	/**
	 * 获取指定文件的上传token
	 * @param key 文件名，可以包含/path
	 * @param expires 按秒计算的token的过期时间，是从当前系统时间开始算的，一般为3600
	 * @return 有效的token
	 */
	public String uploadToken(String key, long expires){
		return auth.uploadToken(PhoneConfig.QINIU_BUCKET, key, expires, null);
	}
	
	/**
	 * 获取指定文件的上传token
	 * @param key 文件名，可以包含/path
	 * @param expires 按秒计算的token的过期时间，是从当前系统时间开始算的，一般为3600
	 * @return 有效的token
	 */
	public String uploadToken(String key, long expires, StringMap map){
		return auth.uploadToken(PhoneConfig.QINIU_BUCKET, key, expires, map);
	}
	
	public static QiniuManager getInstance(){
		if(instance==null){
			instance=new QiniuManager();
			instance.reload();
		}
		return instance;
	}
	
	
	
}
