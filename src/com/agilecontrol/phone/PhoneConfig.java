package com.agilecontrol.phone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.agilecontrol.nea.core.util.ConfigValues;

public class PhoneConfig {
	
	private static final Logger logger = LoggerFactory.getLogger(PhoneConfig.class);

	
	/**
	 * 是否调试模式
	 */
	public static boolean DEBUG=ConfigValues.get("phone.debug",true);
	
	public static boolean PUSH_XINGE=ConfigValues.get("phone.push_xinge", false);
	
	/**
	* android最小的可用客户端版本号，形式: 2.9.13 （含）
	*/
	public static String ANDROID_VERSION_MIN=ConfigValues.get("phone.android_version_min");
	/**
	* android最大的可用客户端版本号，形式: 2.9.13 （含）
	*/
	public static String ANDROID_VERSION_MAX=ConfigValues.get("phone.android_version_max");
	/**
	* ios最小的可用客户端版本号，形式: 2.9.13 （含）
	*/
	public static String IOS_VERSION_MIN=ConfigValues.get("phone.ios_version_min");
	/**
	* ios最大的可用客户端版本号，形式: 2.9.13 （含）
	*/
	public static String IOS_VERSION_MAX=ConfigValues.get("phone.ios_version_max");
	
	/**
	 * 默认的一页查询记录数
	 */
	public static int QUERY_RANGE_DEFAULT=ConfigValues.get("phone.query_range_default",20);
	
	/**
	 * 地素webservice接口地址
	 */
	public static String DAZZLE_WSDL=ConfigValues.get("phone.dazzle_wsdl");
	
	/**
	 * User name case sensitive
	 */
	public static boolean USERNAME_CASE_SENSITIVE = ConfigValues.get("phone.username_case_sensitive", false);
	/**
	 * Cookie 的timeout 时间，以秒计算，默认7天
	 */
	public static int COOKIE_TIMEOUT=ConfigValues.get("phone.cookie_timeout", 7*24*3600);
	/**
	 * 七牛图片服务器的access key
	 */
	public static String QINIU_ACCESS_KEY=ConfigValues.get("phone.qiniu_access_key", "_H9szGv4IddCaVBTOk7qrRcXzNenKRf2ys5jgvrz");
	/**
	 * 七牛图片服务器的secret key
	 */
	public static String QINIU_SECRET_KEY=ConfigValues.get("phone.qiniu_secret_key", "oXwKJCXg66SSft6IjVXCiCBqAC0OjDIHSEPRhtG2");
	/**
	 * 七牛图片服务器的bucket, 就是存储空间名
	 */
	public static String QINIU_BUCKET=ConfigValues.get("phone.qiniu_bucket", "devspace");

	/**
	 * 七牛图片服务器的最大上传时间，默认60分钟, 按秒计算
	 */
	public static int QINIU_UPLOAD_EXPIRE=ConfigValues.get("phone.qiniu_upload_expire", 60*60);
	
	/**
	 * 七牛图片服务器的最大上传时间，默认60分钟, 按秒计算
	 */
	public static String QINIU_DOMAIN=ConfigValues.get("phone.qiniu_domain", "http://img.1688mj.com/");
	
	/**
	 * 邀请码链接格式, 形式: http://172.16.0.253/bin/Join? 后面的querystring会被填充
	 */
	public static String JOIN_URL=ConfigValues.get("phone.join_url", "");
	
	
	/**
	 * 最大的查询结果数
	 */
	public static int MAX_SEARCH_RESULT=ConfigValues.get("phone.max_search_result",2000);
	
	/**
	 * 网易云信 APPKEY
	 */
	public static String YUNXIN_APPKEY =ConfigValues.get("yunxin_appkey","ea8bc67bd431f533088812b696dbd567");
	/**
	 * 网易云信 APPSECRET
	 */
	public static String YUNXIN_APPSECRET =ConfigValues.get("yunxin_appsecret","32aabf959e93");
	
	
	/**
	 * 系统用户：麦+钱包
	 * {"code":200,"info":{"name":"麦+钱包","accid":"maijiatbrfvyujmikmju","token":"13074185296"}}
	 */
	public static String YUNXIN_LUCKY_MONEY=ConfigValues.get("yunxin_lucky_money","maijiatbrfvyujmikmju");
	
	/**
	 * 系统用户：员工入职
	 * {"code":200,"info":{"name":"员工入职","accid":"maijiahusscfhuedwerf","token":"14096328521"}}
	 */
	public static String YUNXIN_EMPLOYEE_ENTRY=ConfigValues.get("yunxin_employee_entry","maijiahusscfhuedwerf");
	
	/**
	 * 系统用户：采购入库
	 * {"code":200,"info":{"name":"采购入库","accid":"maijiaqazxswerfvbgto","token":"15085239651"}}
	 */
	public static String YUNXIN_PURCHASING_STORAGE=ConfigValues.get("yunxin_purchasing_storage","maijiaqazxswerfvbgto");
	
	/**
	 * 系统用户：麦+小秘
	 * {"code":200,"info":{"name":"麦+小秘","accid":"maijiarfgv96325ertyn","token":"16085239652"}}
	 */
	public static String YUNXIN_SECRETARY=ConfigValues.get("yunxin_secretary","maijiarfgv96325ertyn");
	/**
	 * 系统用户：客户审核
	 * {"code":200,"info":{"name":"客户审核","accid":"maijia5236oiuytgbhnm","token":"17063952856"}}
	 */
	public static String YUNXIN_CUST_CHECK=ConfigValues.get("yunxin_cust_check","maijia5236oiuytgbhnm");
	/**
	 * 系统用户：供货商审核
	 * {"code":200,"info":{"name":"供货商审核","accid":"maijiartyuoiuytgbhnm","token":"18032569852"}}
	 */
	public static String YUNXIN_SUP_CHECK=ConfigValues.get("yunxin_sup_check","maijiartyuoiuytgbhnm");
	/**
	 * 系统用户：活动消息
	 * {"code":200,"info":{"name":"活动消息","accid":"maijiat89klo85bhytoh","token":"19085632695"}}
	 */
	public static String YUNXIN_ACT_MSG=ConfigValues.get("yunxin_act_msg","maijiat89klo85bhytoh");
	
	/**
	 * 微信网页地址
	 */
	public static String WEIXIN_PAGE_ADDR=ConfigValues.get("weixin_page_addr","/phone/index.html#/buding");
	/**
	 * 延迟收货: 延迟时间长度
	 * 
	 * author:suntao 
	 * addtime:20160326
	 */
	public static int LO_DELAYTIME=ConfigValues.get("lo_delaytime", 8);
	
	/**
	 * 活动有效时间
	 */
	public static String ACT_EFF_DAY=ConfigValues.get("act.eff.day", "[1,3,7,14]");
	
	/**
	 * 月度套餐单价
	 */
	public static int PKG_MONTH_PRICE=ConfigValues.get("pkg.month.price", 50);
	
	/**
	 * 是否模拟云信，但实际并不发消息去云信，适合于性能测试
	 */
	public static boolean YUNXIN_MOCK=ConfigValues.get("phone.yunxin_mock",false );
	/**
	 * 用于支付的网关实现类
	 */
	public static String PAY_GATEWAY_IMPL=ConfigValues.get("phone.pay_gateway_impl","com.agilecontrol.b2b.pay.cfp.CFPGateway");
	/**
	 * 等待网关回馈消息的时间，以秒计算，超时后直接告知客户端需要等消息
	 */
	public static int PAY_GATEWAY_WAIT=ConfigValues.get("phone.pay_gateway_wait", 10);
	
	/**
	 * 容联云通讯短信验证码配置
	 */
	public static int SMS_TEMPLATEID=ConfigValues.get("sms.templateid", 86199);//麦+：79570；微信订货会：66870
	public static int SMS_READTIMEOUT=ConfigValues.get("sms.readtimeout", 60);
	public static int SMS_CONNECTTIMEOUT=ConfigValues.get("sms.connecttimeout", 60);
	//请求地址
	public static String SMS_SERVERURL = ConfigValues.get("sms.url", "https://app.cloopen.com:8883/2013-12-26/Accounts/{accountSid}/SMS/TemplateSMS?sig={SigParameter}");
	//账号id
	public static String SMS_ACCOUNTSID = ConfigValues.get("sms.accountsid", "8a22b8b852a4bc9f0152a4e4642b001f"); 
	//账号token
	public static String SMS_AUTHTOKEN = ConfigValues.get("sms.authtoken", "95f82e108d65487ea41e542de310459a"); 
	//应用id
	public static String SMS_APPID = ConfigValues.get("sms.appid", "8a48b55153eae51101540e5e69c93818"); 
	
// 测试接口	
//	// 商户私钥
//	public static String PAY_PRIVATE_KEY = ConfigValues.get("phone.pay_private_key", "MIICdwIBADANBgkqhkiG9w0BAQEFAASCAmEwggJdAgEAAoGBAL4ymxhVgPq9hkxLn/SleIcujJR9D+ryzw6PEpkwRFwq6Iq+UFKM52m04qF2u+ejv0K7FsJY9kStbvMaphRimOmejv9E8v2acQMHaUpiO5eaGoR/Nz66aHmXc6KlJ8nfnrdOR/hDIk9rT9pppfiNpGiIorObIL1+jo4POkaY4SnrAgMBAAECgYAqj1ZnTpLLCOfpYK7NZs2eqkro20dZkrXEkz1dLBDP8wYQLd/5aPBLlh90dAY+IkUlIIpKOO/6lDiUi60IOLvwgDlRArmQ7RIWLbltVE/8Ru0S9A4AvCwzM1h5FuWLvlPY7OOfMuAPR8jWsYy8LQPKrTIJ9iXdj0pov35y6gSFGQJBAN7US9JXZmOgSvBItNE40cjrSBu/JSauWZ5M9kDlslfmTi6GN8ThP28YsCGRlX5Y9Ivn4j1x04FsWdJBNqkPzz8CQQDagsQiqgz/6g/HHQolTG3R/7QwwjlYSehmS4Q8M6lhtQmAdNq4vxiKqJ/PQWmBbRRpt0EDwA+6ItB5zjMIoiZVAkEAqj56u3bpFF7IQnLaKyuFJEOWcRSF9tqoP8i/L/AOZRfhTaxf+Xy6sU+kadFH7SNbm3SLprRLiwtUSM5oS5x3kwJBAMircqhK9ulG8Ppw5tJeIDTM2ZQ1qig0p6LaEzSeVR2P/ovjxMIJbOZZ+XmCnvvnSunTC3gAN/E+66oQ/bkeAIkCQHAjmoyoPmQNs/JCwrtEbduPyUPSa3XUZNCmsZLWT6/BVxoI8oyPnY8ul0m/j5wlHoJocVa/6FiFhaJdkBdDgWw=");
//		
//		//公钥
//	public static String PAY_PUB_KEY =ConfigValues.get("phone.pay_pub_key","MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQC+MpsYVYD6vYZMS5/0pXiHLoyUfQ/q8s8OjxKZMERcKuiKvlBSjOdptOKhdrvno79CuxbCWPZErW7zGqYUYpjpno7/RPL9mnEDB2lKYjuXmhqEfzc+umh5l3OipSfJ3563Tkf4QyJPa0/aaaX4jaRoiKKzmyC9fo6ODzpGmOEp6wIDAQAB");
//		
//		// 商户id 此为测试账号
//	public static String PAY_CUST_ID =ConfigValues.get("phone.pay_cust_id", "CB0000001956"); 
//		
//		// 云平台查询Url(测试)
//	public static String PAY_QUERY_URL = ConfigValues.get("phone.pay_query_url","http://58.42.236.252:8500/gzubcp-svc-facade/sxf/queryForPay.do");
//		
//		// 云平台代付Url(测试)
//	public static String PAY_PAY_URL =  ConfigValues.get("phone.pay_pay_url", "http://58.42.236.252:8500/gzubcp-svc-facade/sxf/forPayXml.do"); 

// 正式接口	
	// 商户私钥
	public static String PAY_PRIVATE_KEY = ConfigValues.get("phone.pay_private_key", "MIICdQIBADANBgkqhkiG9w0BAQEFAASCAl8wggJbAgEAAoGBAJ8M3hySXncnoC1TDP8Qt66dHR/tFaOvGp6juTWqhUN5jGn9BissB1i3A87b7hLq0Jgf6iryu/S2scnc1wInferGiJoTlwwVle/NTw6775DLWRXCtU+AUuwa8AVl9FBfz3Bv/oEpM8JLUjvHPupOujuuLY2uQGWTrgTbA9Gc/7LXAgMBAAECgYBYwXmBQDzvCXHdWSc7fzzBeHO0ST12JlUYigzk4c+UI9QzoTs8BEnlO9woJ5rne5oECmtGpEY2/WyhVVe2oAsngrmMw3Xfgm/3FFVqScK2GhveL73EJp3+aBk/K60HbYBn9P2Wt+qLXcneirLh2PA4E87tvHOGDNAayrDIBkPMAQJBANt6iGjx/W1b/m9PeszL+EAQC8FncXWMSyk1UraDUq+zxe03RmlPsdWt9q0vyjFQye7M+GaWGHAFuBDDZnNy9lcCQQC5hCy8LpkrNkOn4r96ro8Eu86mFPkWRMmc68lNcQZ2SkZuPmXNZ+Qpcjt+dJ667Ed30T4X+SFt+y5ya4TYNleBAkAFouDr6QL8Eve2vhDGP5qxcngK0HA+d4ralQ75tuehsXksvVWmkLBdb2k9S1Pi7lMxObxLTiF0hwESFSKFZndjAkBzdYOyCv5hGoC4+DJb1FBGjexrCRqNdXpVI5pBjFqNPGThMAyD7mjeMq48YbB4fZ1tQNj4aqEXpgCeTbR8LDYBAkAIi/Z5uxIAGh0da2uZQAWJC8QvP0HQF+5Ofp9gQoaN6ycd6KsNmAdSDODi2ReZkZtc5MJTCiOgICxkRdEzoAPC");
		
	//商户的公钥, 这是我们自己的，用不上
	//public static String PAY_PUB_KEY =ConfigValues.get("phone.pay_pub_key","MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCfDN4ckl53J6AtUwz/ELeunR0f7RWjrxqeo7k1qoVDeYxp/QYrLAdYtwPO2+4S6tCYH+oq8rv0trHJ3NcCJ33qxoiaE5cMFZXvzU8Ou++Qy1kVwrVPgFLsGvAFZfRQX89wb/6BKTPCS1I7xz7qTro7ri2NrkBlk64E2wPRnP+y1wIDAQAB");
	//平台的公钥
	public static String PAY_PUB_KEY =ConfigValues.get("phone.pay_pub_key","MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQC+IMBbuSZVmiilWuGhGm4cgTmw7YBXykebkQkIDJEifj+SZxjMJBsjZ5JqjAFSlPNW+gv9T3UXe5gBQPM8YqB+kwAWtHjzRDlU/kaAq2A+MVCqR44KDNaVK+raiBme1wJ3w0bxDPwxjMPkg2psc0jGuP+lovS3fJwNbkEHRne68wIDAQAB");
	
		// 商户id 此为测试账号
	public static String PAY_CUST_ID =ConfigValues.get("phone.pay_cust_id", "CB0000002932"); 
		
		// 云平台查询Url(测试)
	public static String PAY_QUERY_URL = ConfigValues.get("phone.pay_query_url","http://58.42.236.252:8501/gzubcp-svc-facade/sxf/queryForPay.do");
		
		// 云平台代付Url(测试)
	public static String PAY_PAY_URL =  ConfigValues.get("phone.pay_pay_url", "http://58.42.236.252:8501/gzubcp-svc-facade/sxf/forPayXml.do"); 
	
	/**
	 * 在系统中需要有个com的管理员可以对系统进行管理操作，比如加载ad_sql，启动queue等	
	 */
	public static long LIFECYCLE_COM_ID=ConfigValues.get("phone.lifecycle_com_id", 1);
	
	/**
	 * 转换头像的convert命令
	 */
	public static String IMAGE_TXT_HEAD_CONVERT=ConfigValues.get("phone.image_txt_head_convert", "/home/nea/bin/convimg.sh -size 512x512 canvas:$bgcolor -fill white -font /opt/portal6a/$font -pointsize $pointsize -gravity Center -draw 'text 0,0 \"$txt\"' $output");
	public static String IMAGE_TINT=ConfigValues.get("phone.image_tint", "/home/nea/bin/tint.sh");
	/**
	 * id生成器，需要在1~1023之间，不能在数据库中
	 */
	public static int ID_WORKER=ConfigValues.get("phone.id_worker", 0);
	
	public static String KIBANA_PICTURE = ConfigValues.get("phone.kibana_picture", "phantomjs $jsFile $url $output");
	
	/**
	 * 错误报告发送邮箱
	 * stao 2016/7/21
	 */
	public static String ERROR_MAILBOX = ConfigValues.get("phone.error_mailbox", "mj_error@lifecycle.cn");
	
	/**
	 * 配置接手错误报告的消息队列名称
	 * stao 2016/7/22 
	 */
	public static String ERROR_MJ_QUEUE = ConfigValues.get("phone.error_mj_queue", "sys:exception");
	
	/**
	 * 参数 "IS_DEVELOP_ENV" 为 “is develop environment”简写
	 * 配置是否启用报错时，mail发送功能。false表示报错时，发送邮件(正式环境下使用该值);true表示不发送邮件，此时客户端将看到来自底层的异常信息 如 nullexception等（开发环境下使用该值）
	 * stao 2016/8/2 
	 */
	public static boolean IS_DEVELOP_ENV  = ConfigValues.get("phone.is_develop_env", true);
	/**
	 * 是否直接从core.schema加载元数据定义，而不是通过 ad_sql#table: 来加载
	 */
	public static boolean LOAD_AD_TABLE_META=ConfigValues.get("phone.load_ad_table_meta", false);
	
	/**
	 * 默认的语言，缺省是zh
	 */
	public static String LANG_DEFAULT=ConfigValues.get("phone.lang_default", "zh");
	/**
	 * 购物车结算是否按sku级别来显示
	 */
	public static boolean CART_SKU_LEVEL=ConfigValues.get("phone.cart_sku_level",true);
	
	/**
	 * 矩阵颜色行的描述字段
	 */
	public static  String SIMPLE_MATRIX_ROW_DESC= ConfigValues.get("fair.simple_matrix_row_desc", "$color.description");
	
	/**
	 * 对于部分女装品牌，看货是款色模式，但是下单还是矩阵模式，所有颜色都显示出来，所有买手用这个参数控制
	 */
	public static boolean FULL_COLOR_MATRIX_BY_STYLE= ConfigValues.get("fair.full_color_matrix_by_style", false);
	
	/**
	 * 在订单创建后，对订单做额外处理的类，需要实现接口 send(int orderId)
	 */
	public static String ORDER_SENDER_CLASS= ConfigValues.get("phone.order_sender_class");
	
	/**
	 * 订单创建后并将订单发送到ERP，需要对方提供的service接口
	 */
	public static String ORDER_SENDER_URL= ConfigValues.get("phone.order_sender_ur", "http://wdbl.app.hd123.cn:8880/h4rest-server/rest/h5rest-server/core/withOutOrderService/save/withOutOrder");
	
	/**
	 * 将订单发送到ERP，需要对方的service接口提供的用户名
	 */
	public static String ORDER_SENDER_LOGIN= ConfigValues.get("phone.order_sender_login", "guest");
	
	/**
	 * 将订单发送到ERP，需要对方的service接口提供的密码
	 */
	public static String ORDER_SENDER_PASSWORD= ConfigValues.get("phone.order_sender_password", "guest");
	/**
	 * 是否在创建订单后立刻提交订单，即调用数据库的b_bfo_submit方法（会修改系统内的库存）
	 */
	public static boolean SUBMIT_ORDER_AFTER_CREATE= ConfigValues.get("phone.submit_order_after_create",true);
	
	/**
	 * 价格带字段，在b_mk_pdt表上的定义将覆盖掉m_product表的定义，需要特殊化处理
	 */
	public final static String PRICE_RANGE_DIM="dim13";
	
	/**
	 * 是否使用上级的订货模板，上级定义：users.manager_id
	 */
	public static boolean USING_PARENT_TEMPLATES=ConfigValues.get("phone.using_parent_templates",true);
	/**
	 * 是否使用尺码比例模板功能，将在手机上显示1手，总量等功能
	 */
	public static boolean USING_SIZE_RATIOS=ConfigValues.get("phone.using_size_ratios",false);
	
	/**
	 * B2B的用户组，如果设置了，用户必须属于这个组才能订货
	 */
	public static String B2B_GROUP=ConfigValues.get("phone.b2b_group");
}
