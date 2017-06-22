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
 * ʵ����ţ�ķ������
 * 
 * ��ţ��ͼƬCDN���������ͻ����ϴ�ͼƬ����ţǰ����Ҫ��ȡ uploadToken���õ���Ȩ������ϴ����Ӷ���֤��ȫ��
 * 
 * ������ţ�ӿ�˵�������Է��ʣ�
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
	 * ��ţ����֤������
	 */
	private Auth auth;
	
	
	private QiniuManager(){}
	/**
	 * ͼƬtoken��Ҫ��ȡACCESS_KEY, SECRET_KEY������޸�����������������Ҫ���¼���
	 */
	public void reload(){
		if(Validator.isNull(PhoneConfig.QINIU_ACCESS_KEY) || Validator.isNull(PhoneConfig.QINIU_SECRET_KEY)
					|| Validator.isNull(PhoneConfig.QINIU_BUCKET) )
			throw new NDSRuntimeException("��������ţ��ز���");
		auth=Auth.create(PhoneConfig.QINIU_ACCESS_KEY,PhoneConfig.QINIU_SECRET_KEY);
	}
	/**
	 * ��ȡָ���ļ����ϴ�token
	 * @param key �ļ��������԰���/path
	 * @param expires ��������token�Ĺ���ʱ�䣬�Ǵӵ�ǰϵͳʱ�俪ʼ��ģ�һ��Ϊ3600
	 * @return ��Ч��token
	 */
	public String uploadToken(String key, long expires){
		return auth.uploadToken(PhoneConfig.QINIU_BUCKET, key, expires, null);
	}
	
	/**
	 * ��ȡָ���ļ����ϴ�token
	 * @param key �ļ��������԰���/path
	 * @param expires ��������token�Ĺ���ʱ�䣬�Ǵӵ�ǰϵͳʱ�俪ʼ��ģ�һ��Ϊ3600
	 * @return ��Ч��token
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
