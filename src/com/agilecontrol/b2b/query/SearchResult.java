package com.agilecontrol.b2b.query;

import org.json.*;

import java.util.*;

import com.agilecontrol.b2b.schema.*;
import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.nea.util.StringUtils;
import com.agilecontrol.nea.util.Validator;
import com.agilecontrol.phone.Admin;
import com.agilecontrol.phone.CmdHandler;
import com.agilecontrol.phone.CmdResult;
import com.agilecontrol.phone.PhoneConfig;
import com.agilecontrol.phone.PhoneController;
import com.agilecontrol.phone.PhoneUtils;
import com.agilecontrol.phone.UUIDUtils;


/**
 * 存储SearchRequest运行后的结果集合
 
 
{
    total, start, cnt, cachekey, $table+"_s" 
}
total - 当前查询结果计数，最大为2000
start - 起始行,0 开始计数
cnt - 当前结果的行数
cachekey - 格式"list:$table:$uid:$uuid"，是指在redis缓存的查询结果的key，redisk中存放List of Id， redis的一个列表查询结果最多缓存30分钟(inactive时间)，timeout后客户端将报错
$table+"_s" - 是返回的对象类别的json key名
举例:

{
    total:1000, start:0, cnt: 20, cachekey: "list:pdt:2:241p4iafiaf", spo_s:[{id,name,value,img}]}}
}
分页访问结果
当前查询结果是个分页列表，当客户端需要获取列表中某一段数据的时候，可以发起以下请求

  {cmd, cachekey, start, cnt}
cmd - 固定为"SearchResult"
cachekey - 是在Seach返回的cachekey
start - 起始id， start from 0
cnt - 需要的记录数，最后一页不一定有这么多，以返回的结果为准
举例

{cmd: “SearchResult”, cachekey:"list:$table:$uid:$uuid", start: 60, cnt: 20 }
返回的结果:

{
    total, start, cnt, cachekey, $table+"_s" 
}   

 
 * @author yfzhu
 *
 */
@Admin(mail="yfzhu@lifecycle.cn")
public class SearchResult {
	private int total;
	private int start;
	private int count;
	/**
	 * 格式: "list:$table:$uid:$uuid"
	 */
	private String cacheKey;
	private JSONArray data;
	/**
	 * 从cache key中读取
	 */
	private String tableName;
	/**
	 * 传给客户端的对象
	 * @return {
    total:1000, start:0, cnt: 20, cachekey: "list:pdt:2:241p4iafiaf", spo_s:[{id,name,value,img}]}}
}
	比较搞的data，需要翻译成: $table+"_s" 作为key
	 * @throws JSONException 
	 */
	public JSONObject toJSONObject() throws JSONException{
		JSONObject jo=new JSONObject();
		jo.put("total", total);
		jo.put("start", start);
		jo.put("cnt", count);
		jo.put("cachekey", cacheKey);
		jo.put(tableName+"_s", data);
		return jo;
	}
	/**
	 * @return the total
	 */
	public int getTotal() {
		return total;
	}
	/**
	 * @param total the total to set
	 */
	public void setTotal(int total) {
		this.total = total;
	}
	/**
	 * @return the start
	 */
	public int getStart() {
		return start;
	}
	/**
	 * @param start the start to set
	 */
	public void setStart(int start) {
		this.start = start;
	}
	/**
	 * @return 当前返回的data数据行数
	 */
	public int getCount() {
		return count;
	}
	/**
	 * @param data 的行数
	 */
	public void setCount(int count) {
		this.count = count;
	}
	/**
	 * @return the cacheKey
	 */
	public String getCacheKey() {
		return cacheKey;
	}
	/**
	 * @param cacheKey the cacheKey to set
	 * "list:$table:$uid:$uuid"
	 */
	public void setCacheKey(String cacheKey) {
		this.cacheKey = cacheKey;
		tableName=cacheKey.split(":")[1];
	}
	/**
	 * @return the data
	 */
	public JSONArray getData() {
		return data;
	}
	/**
	 * @param data the data to set
	 */
	public void setData(JSONArray data) {
		this.data = data;
	}
	
	
}
