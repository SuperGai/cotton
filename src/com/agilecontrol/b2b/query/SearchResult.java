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
 * �洢SearchRequest���к�Ľ������
 
 
{
    total, start, cnt, cachekey, $table+"_s" 
}
total - ��ǰ��ѯ������������Ϊ2000
start - ��ʼ��,0 ��ʼ����
cnt - ��ǰ���������
cachekey - ��ʽ"list:$table:$uid:$uuid"����ָ��redis����Ĳ�ѯ�����key��redisk�д��List of Id�� redis��һ���б��ѯ�����໺��30����(inactiveʱ��)��timeout��ͻ��˽�����
$table+"_s" - �Ƿ��صĶ�������json key��
����:

{
    total:1000, start:0, cnt: 20, cachekey: "list:pdt:2:241p4iafiaf", spo_s:[{id,name,value,img}]}}
}
��ҳ���ʽ��
��ǰ��ѯ����Ǹ���ҳ�б����ͻ�����Ҫ��ȡ�б���ĳһ�����ݵ�ʱ�򣬿��Է�����������

  {cmd, cachekey, start, cnt}
cmd - �̶�Ϊ"SearchResult"
cachekey - ����Seach���ص�cachekey
start - ��ʼid�� start from 0
cnt - ��Ҫ�ļ�¼�������һҳ��һ������ô�࣬�Է��صĽ��Ϊ׼
����

{cmd: ��SearchResult��, cachekey:"list:$table:$uid:$uuid", start: 60, cnt: 20 }
���صĽ��:

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
	 * ��ʽ: "list:$table:$uid:$uuid"
	 */
	private String cacheKey;
	private JSONArray data;
	/**
	 * ��cache key�ж�ȡ
	 */
	private String tableName;
	/**
	 * �����ͻ��˵Ķ���
	 * @return {
    total:1000, start:0, cnt: 20, cachekey: "list:pdt:2:241p4iafiaf", spo_s:[{id,name,value,img}]}}
}
	�Ƚϸ��data����Ҫ�����: $table+"_s" ��Ϊkey
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
	 * @return ��ǰ���ص�data��������
	 */
	public int getCount() {
		return count;
	}
	/**
	 * @param data ������
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
