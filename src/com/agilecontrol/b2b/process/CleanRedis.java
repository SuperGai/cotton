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

h3. ÿ���賿���redis�еĹ���ʱ��

���������Redis�й��ڵ�����, ��ad_sql�ж�ȡprocess:cleanredis:*��sql���壬���ж���sql����ʱ��
��name asc���򣬼�������:

<pre>
select value from ad_sql where name like 'process:cleanredis:%' order by name asc
</pre>

���ص�valueÿ������һ��sql��䣬ÿ��sql���ĸ�ʽ�����ڣ�
> wmsys.wm_concat(distinct 'com:'||com_id||':act')  from act where etime<sysdate
ע����Ҫ�ǵ��У����Զ��С�ÿ�е�������֧�ְ�Ӣ�Ķ��ŷָ����ָ����ÿ��Ԫ�ض���Ӧredis�е�keyֵ

��ɾ����ʱ��ʹ����redis��pipeline���������Ҳ�����Ч��У��

ע�����������Ӧ����oracle������׼����ɺ�

������lua����lua:cleanredis, �޲�����������أ�string/jsonobj/jsonarray ����������, ��ʵ�ֶ���

 * 
 * 
 * @author yfzhu
 *
 */
@Admin(mail="yfzhu@lifecycle.cn")
public class CleanRedis  extends SvrProcess {

	/**
     * �ڲ�����ά��transaction, ����дһ����Чһ��
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
				//sql �Ľṹ�����ƣ�wmsys.wm_concat(distinct 'com:'||com_id||':act')  from act where etime<sysdate;
				//�������ÿ�ж���һ�У��а�Ӣ�Ķ��ŷָ���ÿ��Ԫ�ض���Ҫɾ����key
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
			return "�������";
		}finally{
			try{conn.close();}catch(Throwable tx){}
			try{jedis.close();}catch(Throwable tx){}
		}
	}
	
	

	@Override
	protected void prepare() {

	}

}
