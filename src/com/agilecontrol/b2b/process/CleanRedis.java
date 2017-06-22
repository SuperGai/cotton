package com.agilecontrol.b2b.process;

import java.sql.Connection;

import org.json.JSONArray;
import org.json.JSONObject;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;

import com.agilecontrol.nea.core.control.web.WebUtils;
import com.agilecontrol.nea.core.process.SvrProcess;
import com.agilecontrol.nea.core.query.QueryEngine;
import com.agilecontrol.phone.Admin;
import com.agilecontrol.phone.PhoneUtils;

/**
 * 

h3. 每天凌晨清除redis中的过期时间

定期清除在Redis中过期的数据, 从ad_sql中读取process:cleanredis:*的sql定义，当有多条sql语句的时候，
按name asc排序，检索依据:

<pre>
select value from ad_sql where name like 'process:cleanredis:%' order by name asc
</pre>

返回的value每个都是一条sql语句，每条sql语句的格式类似于：
> wmsys.wm_concat(distinct 'com:'||com_id||':act')  from act where etime<sysdate
注意需要是单列，可以多行。每列的数据中支持按英文逗号分隔，分隔后的每个元素都对应redis中的key值

在删除的时候使用了redis的pipeline技术，并且不做有效性校验

注意清除的数据应该在oracle新数据准备完成后

最后调用lua程序：lua:cleanredis, 无补充参数，返回：string/jsonobj/jsonarray 描述处理结果, 视实现而定

 * 
 * 
 * @author yfzhu
 *
 */
@Admin(mail="yfzhu@lifecycle.cn")
public class CleanRedis  extends SvrProcess {

	/**
     * 内部独立维护transaction, 就是写一个生效一个
     * 
     * @return false if use transaction from caller
     */
    public boolean internalTransaction(){
    	return true;
    }
	
	@Override
	protected String doIt() throws Exception {
		Connection conn=null;
		Jedis jedis=WebUtils.getJedis();
		Pipeline pip=jedis.pipelined();
		QueryEngine engine=QueryEngine.getInstance();
		try{
			conn=engine.getConnection();
			JSONObject luaObj = null;
			JSONArray rows=engine.doQueryJSONArray("select value from ad_sql where name like 'process:cleanredis:%' order by name asc", new Object[]{}, conn);
			for(int i=0;i<rows.length();i++){
				String sql=rows.getString(i);
				//sql 的结构：类似：wmsys.wm_concat(distinct 'com:'||com_id||':act')  from act where etime<sysdate;
				//结果集的每行都是一列，列按英文逗号分隔，每个元素都是要删除的key
				JSONArray data= engine.doQueryJSONArray(sql, new Object[]{}, conn);
				JSONArray dataR=null;
				for(int j=0;j<data.length();j++){
					String[] value=data.getString(j).split(",");
					pip.del(value);
					dataR = new JSONArray(value);
				}
				
				//call lua script  --fix by cmq 2016-6-27 15:19:37
				luaObj = new JSONObject();
				if(dataR.length()>0){
					
					luaObj.put("keylist", dataR);
					Jedis jedis1=WebUtils.getJedis();
					Object ret=PhoneUtils.execRedisLua("lua:cleanredis", luaObj, conn, jedis1);
					log.debug("lua:cleanredis return "+ ret);
					try{jedis1.close();}catch(Throwable tx){}
				}
			
			}
			
	/*		//call lua script 
			JSONObject luaObj = new JSONObject();
			Object ret=PhoneUtils.execRedisLua("lua:cleanredis", luaObj, conn, jedis);
			log.debug("lua:cleanredis return "+ ret);*/
			return "完成清理";
		}finally{
			try{conn.close();}catch(Throwable tx){}
			try{jedis.close();}catch(Throwable tx){}
		}
	}
	
	

	@Override
	protected void prepare() {

	}

}
