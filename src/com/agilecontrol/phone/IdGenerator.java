package com.agilecontrol.phone;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 
 * 最大时间 Thu Sep 06 23:47:35 CST 2085，系统超过long值，那年偶111岁，公司不会让我修bug了 :-)
 * 
 * new Date(2199023255551L+1451606400000L)
 * 1451606400000L 是2016.1.1日，41位的miliseconds
 * 
 * https://gist.github.com/andrewminerd/2681aedd1e8843de042a
 * 
 * Generates unique 64-bit IDs based on a host ID, timestamp, and
 * incrementing counter. When each instance is given a unique host ID,
 * this is guaranteed to produce globally unique IDs for 69 years
 * from the epoch.
 * 
 * This is based on the Snowflake project from Twitter
 * ({@link https://github.com/twitter/snowflake/}), and uses the same
 * ID structure: an unsigned 64-bit integer, comprised of three sections:
 * <ul>
 * <li>Time - 41 bits
 * <li>Host ID - 10 bits
 * <li>Sequence - 12 bits
 * </ul>
 * 
 * This supports 4,096 (2^12) IDs per time unit (which is typically
 * milliseconds) per host ID. If the sequence rolls over during a
 * time unit, ID generation blocks until the next time unit. If the
 * clock should move backwards, IDs will continue to be generated with
 * the last timestamp until the sequence rolls over or the clock
 * advances.
 * 
 * When using multiple instances, the IDs generated will be time
 * ordered (i.e., if ID1 < ID2, ID1 happened before ID2) to the precision
 * of the time unit in use (two events that occur in the same
 * millisecond will not have a deterministic order).
 * 
 * Unlike the Twitter implementation, this is non-blocking and can
 * be used from multiple threads concurrently.
 * 
 * @author Andrew Minerd
 */
public class IdGenerator {
	/**
	 * Default epoch -- 2016-01-01 00:00:00.00
	 */
	private static final Logger log = LoggerFactory.getLogger(IdGenerator.class);
	 /**
     * UTC 2016／01/01 00:00:00，与UNIX TIMESTAMP的差值为1451606400000毫秒
     */                             
	public static final long EPOCH =1451606400000L;// 1230796800L;
	public static final long TIME_MASK = Long.MAX_VALUE << 22;
	private static final long SEQ_MASK = 0x0fffL;
	
	private final long epoch;
	private final long hostId;
	private final AtomicLong last = new AtomicLong(0L);
	
	/**
	 * Constructs an IdGenerator with the given host ID and the default
	 * epoch (2016-01-01 00:00:00.00).
	 * @param hostId Host ID
	 */
	public IdGenerator(long hostId) {
		this(hostId, EPOCH);
	}
	
	/**
	 * Constructs an IdGenerator with the given host ID and epoch.
	 * @param hostId Host ID
	 * @param epoch Epoch is milliseconds
	 */
	public IdGenerator(long hostId, long epoch) {
		this.hostId = hostId;
		this.epoch = epoch;
		if (hostId < 0 || hostId > 1023) {
            throw new IllegalArgumentException("hostId is illegal(0~1023): " + hostId);
        }
	}
	
	public long nextId() {
		final long id;
		while (true) {
			final long time = System.currentTimeMillis(),
				t = (time - epoch) << 22,
				l = last.get();
			long next;
			if (t <= (l & TIME_MASK)) {
				final long seq = (l + 1) & SEQ_MASK;
				if (seq == 0) {
					// sequence rolled over; block until time advances
					while (System.currentTimeMillis() <= time);
					continue;
				}
				next = (l & ~SEQ_MASK) | seq;
			} else {
				next = t | (hostId << 12);
			}
			if (last.compareAndSet(l, next)) {
				id = next;
				break;
			}
		}
		log.debug("Generating id {}.", id);
		return id;
	}
	public static void main(String[] args) throws Exception{
    	IdGenerator worker=new IdGenerator(99);
    	long start=System.currentTimeMillis();
    	long nextId=worker.nextId();
    	System.out.println("start="+ start+",idnow="+ nextId);
    	
    	long e=0;
    	long cnt=0;
    	HashSet<Long> values=new HashSet();
    	//在我的机器上1秒内产生了4063000个
    	while(e<1000){
    		for(int i=0;i<1000;i++) {
    			worker.nextId();
    			//if(!values.add( worker.nextId())) System.err.println("Find duplicate one");
    			cnt++;
    		}
    		e=System.currentTimeMillis() - start;
    	}
    	long idnow= worker.nextId();
    	System.out.println("1seconds: "+ (cnt)+", idnow="+ idnow);
    	
    }
}