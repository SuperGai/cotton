package com.agilecontrol.imp.mumuso;

/**
 * 
 * ľ�ȵķ��Ͷ����ǲ����첽ģʽ�����赱ǰ�̵߳ȴ�
 * 
 * @author yfzhu
 *
 */
public class OrderSender extends com.agilecontrol.b2bweb.OrderSender{

	@Override
	public void send(int orderId) {
		SyncOrder sync=new SyncOrder(orderId);
		if(this.sync){
			sync.run();
		}else{
			Thread thread=new Thread(sync);
			thread.run();
		}
	}

}
