package com.agilecontrol.imp.mumuso;

/**
 * 
 * 木槿的发送订单是采用异步模式，无需当前线程等待
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
