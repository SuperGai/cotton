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
 * ��װ��������

�÷�����ʾ��ǰ�̼ҵ�Ա���б�, ���ƺ��С���Ҷ��"��

 SearchRequest req=new SearchRequest("emp");
 req.addParam("com_id", usr.getComId()); //��ǰ�̼�
 req.setQueryString( "��Ҷ��");
 SearchResult res=cmd.search(req);
 return res.toJSONObject();

����
�б���������ͨ��Search������

����
{
    table, exprs, querystr,  maxcnt, mask, usepref, orderby
}
table - String �����ı�������: emp
exprs - Expression ��ȷ���ֶεĹ���������Ԫ��key-valueΪ �����ֶ����Ͷ�Ӧֵ, ���������ʾ����and
querystr - String ͨ�ò�ѯ�ַ�������exprsΪand��ϵ
maxcnt - int ����ѯ���������ʱ��ͻ��˿���1����̨���Ϊ2000��ǰ̨���õ�ֵ���ܳ���2000��Ĭ��2000
mask - string �ֶ�ȡֵ�� ��ѡ"list"|"obj", Ĭ��Ϊ"list", ��ʱ�ͻ����Կ�Ƭ��ʽ��ʾ���������Ҫ����Ϊ obj���Ա��ȡ������ʾ�ֶ����ݣ���TableManager#Column#mask)
usepref - boolean���Ƿ�ʹ�ó��ò�ѯ������Ĭ��Ϊtrue���ڲ��ֱ��������ó��ò�ѯ����������ϴ��������й���
orderby - string, ���ֶ���ҳ������orderbyѡ��Թؼ��ֽ���ƥ��
����:

   { 
        table:"spo", expres: {st_id: 33, emp_id: 20, state: ["V","O", "S"], price: "12~20"}, 
        querystr: "13918891588", usepref: false, orderby: "stocktime_first"}
    }  

Expression
��ʽ

   {key: value}
key - �ֶ����ƣ���Ҫ�ڵ�ǰ������
value - �ֶε�ֵ��֧�������ֵ�������ʾ����һ��ƥ��
����: {"st_id": 13} ��ʾҪ��st_id=13

 * 
 * 
 * @author yfzhu
 *
 */
@Admin(mail="yfzhu@lifecycle.cn")
public class SearchRequest {
	
	private JSONObject jo=null;
	private JSONObject orderbyDef=null;
	/**
	 * ��ʲô��������ѯ
	 * @param table
	 */
	public SearchRequest(String table)throws JSONException {
		jo=new JSONObject();
		setTable(table);
	}
	/**
	 *@param jo  {
    table, exprs, querystr,  maxcnt, mask, usepref, orderby
}
table - String �����ı�������: emp
exprs - Expression ��ȷ���ֶεĹ���������Ԫ��key-valueΪ �����ֶ����Ͷ�Ӧֵ, ���������ʾ����and
querystr - String ͨ�ò�ѯ�ַ�������exprsΪand��ϵ
maxcnt - int ����ѯ���������ʱ��ͻ��˿���1����̨���Ϊ2000��ǰ̨���õ�ֵ���ܳ���2000��Ĭ��2000
mask - string �ֶ�ȡֵ�� ��ѡ"list"|"obj", Ĭ��Ϊ"list", ��ʱ�ͻ����Կ�Ƭ��ʽ��ʾ���������Ҫ����Ϊ obj���Ա��ȡ������ʾ�ֶ����ݣ���TableManager#Column#mask)
usepref - boolean���Ƿ�ʹ�ó��ò�ѯ������Ĭ��Ϊtrue���ڲ��ֱ��������ó��ò�ѯ����������ϴ��������й���
orderby - string, ���ֶ���ҳ������orderbyѡ��Թؼ��ֽ���ƥ��
����:

   { 
        table:"spo", expres: {st_id: 33, emp_id: 20, state: ["V","O", "S"], price: "12~20"}, 
        querystr: "13918891588", usepref: false, orderby: "stocktime_first"}
    }  

	 * 
	 * @throws Exception
	 */
	public SearchRequest(JSONObject jo){
		this.jo=jo;
	}
	/**
	 * ת��ΪJSON����
	 * @return
	 */
	public JSONObject toJSONObject(){
		return jo;
	}
	/**
	 * ��ӹ�������
	 * @param column �ڲ�ѯ���ϵ��ֶ�
	 * @param condition ֧������:
	 *   column ��number���͵ģ����� "a~b" ��ʾ���b������)����Сa(����), a��b����ֻ��1�������û��"~", ֻҪ�����־Ϳ��ԣ���Ҫ��=��
	 *   column ��date�ģ���������"now-3" ��ʾ>=��ǰ�죬ϵͳ����Ĭ���� >=��now��ʾ��ǰʱ��
	 *   column ��str�ģ�Ĭ��ǿƥ�䣬��=��
	 *   column ����չ����
	 * @throws JSONException 
	 */
	public void addParam(String column, Object condition) throws JSONException {
		JSONObject expr=(JSONObject)jo.optJSONObject("expr");
		if(expr==null)expr=new JSONObject();
		expr.put(column, condition);
	}
	/**
	 * �����������
	 * @param s ��Ϊģ����������
	 */
	public void setQueryString(String s )throws JSONException {
		jo.put("querystr", s);
	}
	/**
	 * ��������б�����ֶΣ���mask[1]==true���ֶ�
	 */
	public void addColumnsForListView()throws JSONException {
		jo.put("mask", "list");
	}
	/**
	 * ��������б�����ֶΣ���mask[0]==true���ֶ�
	 */
	public void addColumnsForObjectView()throws JSONException {
		jo.put("mask", "obj");
	}
	/**
	 * �����û��ı����Ϊ��ѯ������ע���addParam��˳�����Ǻ���õ���ͬ�ֶθ���ǰ���
	 * @throws Exception
	 */
	public void setUserPreferenceAsParam() throws JSONException {
		jo.put("usepref", true);
	}
	/**
	 * @param orderByDef �ṹ: {table: string, column: string, asc: boolean, join: string, param:[] } 
		 * ָ���ǻ���ʲô����ֶ�������,  joinָ��������֮�����ӵĹ�ϵ��param����join�г��ֵģ������������֧��$stid,$uid, $empid,$comid
		 * Ŀǰֻ֧�ֲ�ѯģʽ
		 * ����:
		 * ����������Ʒ��ǰ
		 * {table:"stg", column:"stg.samt", asc: false, join: "stg.pdtid=pdt.id and stg.st_id=?", param:["$stid"]}
	 * 
	 */
	public void setOrderBy(JSONObject orderByDef)throws JSONException {
		jo.put("orderby",orderByDef);
	}
	/**
	 * ���ò�ѯ����
	 * @param table
	 * @throws Exception
	 */
	public void setTable(String table) throws JSONException {
		jo.put("table", table);
	}
	/**
	 * ��¼������ȡ���������ܴ���2000
	 * @param cnt
	 */
	public void setMaxCount(int cnt)throws JSONException {
		jo.put("maxcnt", cnt);
	}
	
	
}
