package com.agilecontrol.b2bweb;

import java.sql.Connection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.agilecontrol.nea.core.query.SPResult;

/**
 * 
 * 订单发送类，在b_bfo_addorder存储过程运行后创建，如果配置了系统参数 phone.order_sender_class
 * 
 * @author yfzhu
 *
 */
public abstract class OrderSender {
	
	private static Logger logger = LoggerFactory.getLogger(OrderSender.class); 
	
	protected Connection conn;
	/**
	 * 是否同步模式
	 */
	protected boolean sync=true;
	/**
	 * 初始化连接，注意如果设计为异步订单发送模式，将不能使用此数据库连接
	 * @param sync 是否同步模式
	 * @param conn
	 */
	public void init(boolean sync, Connection conn){
		this.sync=sync;
		this.conn=conn;
	}
	
	
	/**
	 * 在订单创建后调用，发送订单到第三方系统。在具体实现的时候，可以不论成功失败都将信息写到b_bfo的状态字段上，也可以直接抛出错误。
	 * 直接抛出错误将导致用户界面收到错误提示，并回滚所有 b2b.order.create 的事务。如果是正常返回，默认实现类自行将状态写入到b_bfo表
	 * 
	 * @param orderId
	 * @throws Exception
	 */
	public abstract void send(int orderId) throws Exception;
}
