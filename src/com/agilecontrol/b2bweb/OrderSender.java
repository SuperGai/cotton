package com.agilecontrol.b2bweb;

import java.sql.Connection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.agilecontrol.nea.core.query.SPResult;

/**
 * 
 * ���������࣬��b_bfo_addorder�洢�������к󴴽������������ϵͳ���� phone.order_sender_class
 * 
 * @author yfzhu
 *
 */
public abstract class OrderSender {
	
	private static Logger logger = LoggerFactory.getLogger(OrderSender.class); 
	
	protected Connection conn;
	/**
	 * �Ƿ�ͬ��ģʽ
	 */
	protected boolean sync=true;
	/**
	 * ��ʼ�����ӣ�ע��������Ϊ�첽��������ģʽ��������ʹ�ô����ݿ�����
	 * @param sync �Ƿ�ͬ��ģʽ
	 * @param conn
	 */
	public void init(boolean sync, Connection conn){
		this.sync=sync;
		this.conn=conn;
	}
	
	
	/**
	 * �ڶ�����������ã����Ͷ�����������ϵͳ���ھ���ʵ�ֵ�ʱ�򣬿��Բ��۳ɹ�ʧ�ܶ�����Ϣд��b_bfo��״̬�ֶ��ϣ�Ҳ����ֱ���׳�����
	 * ֱ���׳����󽫵����û������յ�������ʾ�����ع����� b2b.order.create ������������������أ�Ĭ��ʵ�������н�״̬д�뵽b_bfo��
	 * 
	 * @param orderId
	 * @throws Exception
	 */
	public abstract void send(int orderId) throws Exception;
}
