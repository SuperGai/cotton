package com.agilecontrol.phone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.agilecontrol.nea.core.util.ConfigValues;

public class PhoneConfig {
	
	private static final Logger logger = LoggerFactory.getLogger(PhoneConfig.class);

	
	/**
	 * �Ƿ����ģʽ
	 */
	public static boolean DEBUG=ConfigValues.get("phone.debug",true);
	
	public static boolean PUSH_XINGE=ConfigValues.get("phone.push_xinge", false);
	
	/**
	* android��С�Ŀ��ÿͻ��˰汾�ţ���ʽ: 2.9.13 ������
	*/
	public static String ANDROID_VERSION_MIN=ConfigValues.get("phone.android_version_min");
	/**
	* android���Ŀ��ÿͻ��˰汾�ţ���ʽ: 2.9.13 ������
	*/
	public static String ANDROID_VERSION_MAX=ConfigValues.get("phone.android_version_max");
	/**
	* ios��С�Ŀ��ÿͻ��˰汾�ţ���ʽ: 2.9.13 ������
	*/
	public static String IOS_VERSION_MIN=ConfigValues.get("phone.ios_version_min");
	/**
	* ios���Ŀ��ÿͻ��˰汾�ţ���ʽ: 2.9.13 ������
	*/
	public static String IOS_VERSION_MAX=ConfigValues.get("phone.ios_version_max");
	
	/**
	 * Ĭ�ϵ�һҳ��ѯ��¼��
	 */
	public static int QUERY_RANGE_DEFAULT=ConfigValues.get("phone.query_range_default",20);
	
	/**
	 * ����webservice�ӿڵ�ַ
	 */
	public static String DAZZLE_WSDL=ConfigValues.get("phone.dazzle_wsdl");
	
	/**
	 * User name case sensitive
	 */
	public static boolean USERNAME_CASE_SENSITIVE = ConfigValues.get("phone.username_case_sensitive", false);
	/**
	 * Cookie ��timeout ʱ�䣬������㣬Ĭ��7��
	 */
	public static int COOKIE_TIMEOUT=ConfigValues.get("phone.cookie_timeout", 7*24*3600);
	/**
	 * ��ţͼƬ��������access key
	 */
	public static String QINIU_ACCESS_KEY=ConfigValues.get("phone.qiniu_access_key", "_H9szGv4IddCaVBTOk7qrRcXzNenKRf2ys5jgvrz");
	/**
	 * ��ţͼƬ��������secret key
	 */
	public static String QINIU_SECRET_KEY=ConfigValues.get("phone.qiniu_secret_key", "oXwKJCXg66SSft6IjVXCiCBqAC0OjDIHSEPRhtG2");
	/**
	 * ��ţͼƬ��������bucket, ���Ǵ洢�ռ���
	 */
	public static String QINIU_BUCKET=ConfigValues.get("phone.qiniu_bucket", "devspace");

	/**
	 * ��ţͼƬ������������ϴ�ʱ�䣬Ĭ��60����, �������
	 */
	public static int QINIU_UPLOAD_EXPIRE=ConfigValues.get("phone.qiniu_upload_expire", 60*60);
	
	/**
	 * ��ţͼƬ������������ϴ�ʱ�䣬Ĭ��60����, �������
	 */
	public static String QINIU_DOMAIN=ConfigValues.get("phone.qiniu_domain", "http://img.1688mj.com/");
	
	/**
	 * ���������Ӹ�ʽ, ��ʽ: http://172.16.0.253/bin/Join? �����querystring�ᱻ���
	 */
	public static String JOIN_URL=ConfigValues.get("phone.join_url", "");
	
	
	/**
	 * ���Ĳ�ѯ�����
	 */
	public static int MAX_SEARCH_RESULT=ConfigValues.get("phone.max_search_result",2000);
	
	/**
	 * �������� APPKEY
	 */
	public static String YUNXIN_APPKEY =ConfigValues.get("yunxin_appkey","ea8bc67bd431f533088812b696dbd567");
	/**
	 * �������� APPSECRET
	 */
	public static String YUNXIN_APPSECRET =ConfigValues.get("yunxin_appsecret","32aabf959e93");
	
	
	/**
	 * ϵͳ�û�����+Ǯ��
	 * {"code":200,"info":{"name":"��+Ǯ��","accid":"maijiatbrfvyujmikmju","token":"13074185296"}}
	 */
	public static String YUNXIN_LUCKY_MONEY=ConfigValues.get("yunxin_lucky_money","maijiatbrfvyujmikmju");
	
	/**
	 * ϵͳ�û���Ա����ְ
	 * {"code":200,"info":{"name":"Ա����ְ","accid":"maijiahusscfhuedwerf","token":"14096328521"}}
	 */
	public static String YUNXIN_EMPLOYEE_ENTRY=ConfigValues.get("yunxin_employee_entry","maijiahusscfhuedwerf");
	
	/**
	 * ϵͳ�û����ɹ����
	 * {"code":200,"info":{"name":"�ɹ����","accid":"maijiaqazxswerfvbgto","token":"15085239651"}}
	 */
	public static String YUNXIN_PURCHASING_STORAGE=ConfigValues.get("yunxin_purchasing_storage","maijiaqazxswerfvbgto");
	
	/**
	 * ϵͳ�û�����+С��
	 * {"code":200,"info":{"name":"��+С��","accid":"maijiarfgv96325ertyn","token":"16085239652"}}
	 */
	public static String YUNXIN_SECRETARY=ConfigValues.get("yunxin_secretary","maijiarfgv96325ertyn");
	/**
	 * ϵͳ�û����ͻ����
	 * {"code":200,"info":{"name":"�ͻ����","accid":"maijia5236oiuytgbhnm","token":"17063952856"}}
	 */
	public static String YUNXIN_CUST_CHECK=ConfigValues.get("yunxin_cust_check","maijia5236oiuytgbhnm");
	/**
	 * ϵͳ�û������������
	 * {"code":200,"info":{"name":"���������","accid":"maijiartyuoiuytgbhnm","token":"18032569852"}}
	 */
	public static String YUNXIN_SUP_CHECK=ConfigValues.get("yunxin_sup_check","maijiartyuoiuytgbhnm");
	/**
	 * ϵͳ�û������Ϣ
	 * {"code":200,"info":{"name":"���Ϣ","accid":"maijiat89klo85bhytoh","token":"19085632695"}}
	 */
	public static String YUNXIN_ACT_MSG=ConfigValues.get("yunxin_act_msg","maijiat89klo85bhytoh");
	
	/**
	 * ΢����ҳ��ַ
	 */
	public static String WEIXIN_PAGE_ADDR=ConfigValues.get("weixin_page_addr","/phone/index.html#/buding");
	/**
	 * �ӳ��ջ�: �ӳ�ʱ�䳤��
	 * 
	 * author:suntao 
	 * addtime:20160326
	 */
	public static int LO_DELAYTIME=ConfigValues.get("lo_delaytime", 8);
	
	/**
	 * ���Чʱ��
	 */
	public static String ACT_EFF_DAY=ConfigValues.get("act.eff.day", "[1,3,7,14]");
	
	/**
	 * �¶��ײ͵���
	 */
	public static int PKG_MONTH_PRICE=ConfigValues.get("pkg.month.price", 50);
	
	/**
	 * �Ƿ�ģ�����ţ���ʵ�ʲ�������Ϣȥ���ţ��ʺ������ܲ���
	 */
	public static boolean YUNXIN_MOCK=ConfigValues.get("phone.yunxin_mock",false );
	/**
	 * ����֧��������ʵ����
	 */
	public static String PAY_GATEWAY_IMPL=ConfigValues.get("phone.pay_gateway_impl","com.agilecontrol.b2b.pay.cfp.CFPGateway");
	/**
	 * �ȴ����ػ�����Ϣ��ʱ�䣬������㣬��ʱ��ֱ�Ӹ�֪�ͻ�����Ҫ����Ϣ
	 */
	public static int PAY_GATEWAY_WAIT=ConfigValues.get("phone.pay_gateway_wait", 10);
	
	/**
	 * ������ͨѶ������֤������
	 */
	public static int SMS_TEMPLATEID=ConfigValues.get("sms.templateid", 86199);//��+��79570��΢�Ŷ����᣺66870
	public static int SMS_READTIMEOUT=ConfigValues.get("sms.readtimeout", 60);
	public static int SMS_CONNECTTIMEOUT=ConfigValues.get("sms.connecttimeout", 60);
	//�����ַ
	public static String SMS_SERVERURL = ConfigValues.get("sms.url", "https://app.cloopen.com:8883/2013-12-26/Accounts/{accountSid}/SMS/TemplateSMS?sig={SigParameter}");
	//�˺�id
	public static String SMS_ACCOUNTSID = ConfigValues.get("sms.accountsid", "8a22b8b852a4bc9f0152a4e4642b001f"); 
	//�˺�token
	public static String SMS_AUTHTOKEN = ConfigValues.get("sms.authtoken", "95f82e108d65487ea41e542de310459a"); 
	//Ӧ��id
	public static String SMS_APPID = ConfigValues.get("sms.appid", "8a48b55153eae51101540e5e69c93818"); 
	
// ���Խӿ�	
//	// �̻�˽Կ
//	public static String PAY_PRIVATE_KEY = ConfigValues.get("phone.pay_private_key", "MIICdwIBADANBgkqhkiG9w0BAQEFAASCAmEwggJdAgEAAoGBAL4ymxhVgPq9hkxLn/SleIcujJR9D+ryzw6PEpkwRFwq6Iq+UFKM52m04qF2u+ejv0K7FsJY9kStbvMaphRimOmejv9E8v2acQMHaUpiO5eaGoR/Nz66aHmXc6KlJ8nfnrdOR/hDIk9rT9pppfiNpGiIorObIL1+jo4POkaY4SnrAgMBAAECgYAqj1ZnTpLLCOfpYK7NZs2eqkro20dZkrXEkz1dLBDP8wYQLd/5aPBLlh90dAY+IkUlIIpKOO/6lDiUi60IOLvwgDlRArmQ7RIWLbltVE/8Ru0S9A4AvCwzM1h5FuWLvlPY7OOfMuAPR8jWsYy8LQPKrTIJ9iXdj0pov35y6gSFGQJBAN7US9JXZmOgSvBItNE40cjrSBu/JSauWZ5M9kDlslfmTi6GN8ThP28YsCGRlX5Y9Ivn4j1x04FsWdJBNqkPzz8CQQDagsQiqgz/6g/HHQolTG3R/7QwwjlYSehmS4Q8M6lhtQmAdNq4vxiKqJ/PQWmBbRRpt0EDwA+6ItB5zjMIoiZVAkEAqj56u3bpFF7IQnLaKyuFJEOWcRSF9tqoP8i/L/AOZRfhTaxf+Xy6sU+kadFH7SNbm3SLprRLiwtUSM5oS5x3kwJBAMircqhK9ulG8Ppw5tJeIDTM2ZQ1qig0p6LaEzSeVR2P/ovjxMIJbOZZ+XmCnvvnSunTC3gAN/E+66oQ/bkeAIkCQHAjmoyoPmQNs/JCwrtEbduPyUPSa3XUZNCmsZLWT6/BVxoI8oyPnY8ul0m/j5wlHoJocVa/6FiFhaJdkBdDgWw=");
//		
//		//��Կ
//	public static String PAY_PUB_KEY =ConfigValues.get("phone.pay_pub_key","MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQC+MpsYVYD6vYZMS5/0pXiHLoyUfQ/q8s8OjxKZMERcKuiKvlBSjOdptOKhdrvno79CuxbCWPZErW7zGqYUYpjpno7/RPL9mnEDB2lKYjuXmhqEfzc+umh5l3OipSfJ3563Tkf4QyJPa0/aaaX4jaRoiKKzmyC9fo6ODzpGmOEp6wIDAQAB");
//		
//		// �̻�id ��Ϊ�����˺�
//	public static String PAY_CUST_ID =ConfigValues.get("phone.pay_cust_id", "CB0000001956"); 
//		
//		// ��ƽ̨��ѯUrl(����)
//	public static String PAY_QUERY_URL = ConfigValues.get("phone.pay_query_url","http://58.42.236.252:8500/gzubcp-svc-facade/sxf/queryForPay.do");
//		
//		// ��ƽ̨����Url(����)
//	public static String PAY_PAY_URL =  ConfigValues.get("phone.pay_pay_url", "http://58.42.236.252:8500/gzubcp-svc-facade/sxf/forPayXml.do"); 

// ��ʽ�ӿ�	
	// �̻�˽Կ
	public static String PAY_PRIVATE_KEY = ConfigValues.get("phone.pay_private_key", "MIICdQIBADANBgkqhkiG9w0BAQEFAASCAl8wggJbAgEAAoGBAJ8M3hySXncnoC1TDP8Qt66dHR/tFaOvGp6juTWqhUN5jGn9BissB1i3A87b7hLq0Jgf6iryu/S2scnc1wInferGiJoTlwwVle/NTw6775DLWRXCtU+AUuwa8AVl9FBfz3Bv/oEpM8JLUjvHPupOujuuLY2uQGWTrgTbA9Gc/7LXAgMBAAECgYBYwXmBQDzvCXHdWSc7fzzBeHO0ST12JlUYigzk4c+UI9QzoTs8BEnlO9woJ5rne5oECmtGpEY2/WyhVVe2oAsngrmMw3Xfgm/3FFVqScK2GhveL73EJp3+aBk/K60HbYBn9P2Wt+qLXcneirLh2PA4E87tvHOGDNAayrDIBkPMAQJBANt6iGjx/W1b/m9PeszL+EAQC8FncXWMSyk1UraDUq+zxe03RmlPsdWt9q0vyjFQye7M+GaWGHAFuBDDZnNy9lcCQQC5hCy8LpkrNkOn4r96ro8Eu86mFPkWRMmc68lNcQZ2SkZuPmXNZ+Qpcjt+dJ667Ed30T4X+SFt+y5ya4TYNleBAkAFouDr6QL8Eve2vhDGP5qxcngK0HA+d4ralQ75tuehsXksvVWmkLBdb2k9S1Pi7lMxObxLTiF0hwESFSKFZndjAkBzdYOyCv5hGoC4+DJb1FBGjexrCRqNdXpVI5pBjFqNPGThMAyD7mjeMq48YbB4fZ1tQNj4aqEXpgCeTbR8LDYBAkAIi/Z5uxIAGh0da2uZQAWJC8QvP0HQF+5Ofp9gQoaN6ycd6KsNmAdSDODi2ReZkZtc5MJTCiOgICxkRdEzoAPC");
		
	//�̻��Ĺ�Կ, ���������Լ��ģ��ò���
	//public static String PAY_PUB_KEY =ConfigValues.get("phone.pay_pub_key","MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCfDN4ckl53J6AtUwz/ELeunR0f7RWjrxqeo7k1qoVDeYxp/QYrLAdYtwPO2+4S6tCYH+oq8rv0trHJ3NcCJ33qxoiaE5cMFZXvzU8Ou++Qy1kVwrVPgFLsGvAFZfRQX89wb/6BKTPCS1I7xz7qTro7ri2NrkBlk64E2wPRnP+y1wIDAQAB");
	//ƽ̨�Ĺ�Կ
	public static String PAY_PUB_KEY =ConfigValues.get("phone.pay_pub_key","MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQC+IMBbuSZVmiilWuGhGm4cgTmw7YBXykebkQkIDJEifj+SZxjMJBsjZ5JqjAFSlPNW+gv9T3UXe5gBQPM8YqB+kwAWtHjzRDlU/kaAq2A+MVCqR44KDNaVK+raiBme1wJ3w0bxDPwxjMPkg2psc0jGuP+lovS3fJwNbkEHRne68wIDAQAB");
	
		// �̻�id ��Ϊ�����˺�
	public static String PAY_CUST_ID =ConfigValues.get("phone.pay_cust_id", "CB0000002932"); 
		
		// ��ƽ̨��ѯUrl(����)
	public static String PAY_QUERY_URL = ConfigValues.get("phone.pay_query_url","http://58.42.236.252:8501/gzubcp-svc-facade/sxf/queryForPay.do");
		
		// ��ƽ̨����Url(����)
	public static String PAY_PAY_URL =  ConfigValues.get("phone.pay_pay_url", "http://58.42.236.252:8501/gzubcp-svc-facade/sxf/forPayXml.do"); 
	
	/**
	 * ��ϵͳ����Ҫ�и�com�Ĺ���Ա���Զ�ϵͳ���й���������������ad_sql������queue��	
	 */
	public static long LIFECYCLE_COM_ID=ConfigValues.get("phone.lifecycle_com_id", 1);
	
	/**
	 * ת��ͷ���convert����
	 */
	public static String IMAGE_TXT_HEAD_CONVERT=ConfigValues.get("phone.image_txt_head_convert", "/home/nea/bin/convimg.sh -size 512x512 canvas:$bgcolor -fill white -font /opt/portal6a/$font -pointsize $pointsize -gravity Center -draw 'text 0,0 \"$txt\"' $output");
	public static String IMAGE_TINT=ConfigValues.get("phone.image_tint", "/home/nea/bin/tint.sh");
	/**
	 * id����������Ҫ��1~1023֮�䣬���������ݿ���
	 */
	public static int ID_WORKER=ConfigValues.get("phone.id_worker", 0);
	
	public static String KIBANA_PICTURE = ConfigValues.get("phone.kibana_picture", "phantomjs $jsFile $url $output");
	
	/**
	 * ���󱨸淢������
	 * stao 2016/7/21
	 */
	public static String ERROR_MAILBOX = ConfigValues.get("phone.error_mailbox", "mj_error@lifecycle.cn");
	
	/**
	 * ���ý��ִ��󱨸����Ϣ��������
	 * stao 2016/7/22 
	 */
	public static String ERROR_MJ_QUEUE = ConfigValues.get("phone.error_mj_queue", "sys:exception");
	
	/**
	 * ���� "IS_DEVELOP_ENV" Ϊ ��is develop environment����д
	 * �����Ƿ����ñ���ʱ��mail���͹��ܡ�false��ʾ����ʱ�������ʼ�(��ʽ������ʹ�ø�ֵ);true��ʾ�������ʼ�����ʱ�ͻ��˽��������Եײ���쳣��Ϣ �� nullexception�ȣ�����������ʹ�ø�ֵ��
	 * stao 2016/8/2 
	 */
	public static boolean IS_DEVELOP_ENV  = ConfigValues.get("phone.is_develop_env", true);
	/**
	 * �Ƿ�ֱ�Ӵ�core.schema����Ԫ���ݶ��壬������ͨ�� ad_sql#table: ������
	 */
	public static boolean LOAD_AD_TABLE_META=ConfigValues.get("phone.load_ad_table_meta", false);
	
	/**
	 * Ĭ�ϵ����ԣ�ȱʡ��zh
	 */
	public static String LANG_DEFAULT=ConfigValues.get("phone.lang_default", "zh");
	/**
	 * ���ﳵ�����Ƿ�sku��������ʾ
	 */
	public static boolean CART_SKU_LEVEL=ConfigValues.get("phone.cart_sku_level",true);
	
	/**
	 * ������ɫ�е������ֶ�
	 */
	public static  String SIMPLE_MATRIX_ROW_DESC= ConfigValues.get("fair.simple_matrix_row_desc", "$color.description");
	
	/**
	 * ���ڲ���ŮװƷ�ƣ������ǿ�ɫģʽ�������µ����Ǿ���ģʽ��������ɫ����ʾ���������������������������
	 */
	public static boolean FULL_COLOR_MATRIX_BY_STYLE= ConfigValues.get("fair.full_color_matrix_by_style", false);
	
	/**
	 * �ڶ��������󣬶Զ��������⴦����࣬��Ҫʵ�ֽӿ� send(int orderId)
	 */
	public static String ORDER_SENDER_CLASS= ConfigValues.get("phone.order_sender_class");
	
	/**
	 * ���������󲢽��������͵�ERP����Ҫ�Է��ṩ��service�ӿ�
	 */
	public static String ORDER_SENDER_URL= ConfigValues.get("phone.order_sender_ur", "http://wdbl.app.hd123.cn:8880/h4rest-server/rest/h5rest-server/core/withOutOrderService/save/withOutOrder");
	
	/**
	 * ���������͵�ERP����Ҫ�Է���service�ӿ��ṩ���û���
	 */
	public static String ORDER_SENDER_LOGIN= ConfigValues.get("phone.order_sender_login", "guest");
	
	/**
	 * ���������͵�ERP����Ҫ�Է���service�ӿ��ṩ������
	 */
	public static String ORDER_SENDER_PASSWORD= ConfigValues.get("phone.order_sender_password", "guest");
	/**
	 * �Ƿ��ڴ��������������ύ���������������ݿ��b_bfo_submit���������޸�ϵͳ�ڵĿ�棩
	 */
	public static boolean SUBMIT_ORDER_AFTER_CREATE= ConfigValues.get("phone.submit_order_after_create",true);
	
	/**
	 * �۸���ֶΣ���b_mk_pdt���ϵĶ��彫���ǵ�m_product��Ķ��壬��Ҫ���⻯����
	 */
	public final static String PRICE_RANGE_DIM="dim13";
	
	/**
	 * �Ƿ�ʹ���ϼ��Ķ���ģ�壬�ϼ����壺users.manager_id
	 */
	public static boolean USING_PARENT_TEMPLATES=ConfigValues.get("phone.using_parent_templates",true);
	/**
	 * �Ƿ�ʹ�ó������ģ�幦�ܣ������ֻ�����ʾ1�֣������ȹ���
	 */
	public static boolean USING_SIZE_RATIOS=ConfigValues.get("phone.using_size_ratios",false);
	
	/**
	 * B2B���û��飬��������ˣ��û����������������ܶ���
	 */
	public static String B2B_GROUP=ConfigValues.get("phone.b2b_group");
}
