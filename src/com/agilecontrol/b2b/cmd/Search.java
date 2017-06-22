package com.agilecontrol.b2b.cmd;

import org.json.*;

import java.util.*;

import com.agilecontrol.b2b.query.SearchRequest;
import com.agilecontrol.b2b.query.SearchResult;
import com.agilecontrol.b2b.schema.*;
import com.agilecontrol.phone.UserObj;
import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.nea.util.StringUtils;
import com.agilecontrol.nea.util.Validator;
import com.agilecontrol.phone.CmdHandler;
import com.agilecontrol.phone.CmdResult;
import com.agilecontrol.phone.PhoneConfig;
import com.agilecontrol.phone.PhoneController;
import com.agilecontrol.phone.PhoneUtils;
import com.agilecontrol.phone.UUIDUtils;

/**
 * 
����
�б���������ͨ��Search������

����
{
    table, exprs, querystr,  maxcnt, mask, usepref, orderby,pagesize
}
table - String �����ı�������: emp
exprs - Expression ��ȷ���ֶεĹ���������Ԫ��key-valueΪ �����ֶ����Ͷ�Ӧֵ, ���������ʾ����and
querystr - String ͨ�ò�ѯ�ַ�������exprsΪand��ϵ
maxcnt - int ����ѯ���������ʱ��ͻ��˿���1����̨���Ϊ2000��ǰ̨���õ�ֵ���ܳ���2000��Ĭ��2000
pagesize - ҳ����, ����Ӧ���ص�cnt
mask - string �ֶ�ȡֵ�� ��ѡ"list"|"obj", Ĭ��Ϊ"list", ��ʱ�ͻ����Կ�Ƭ��ʽ��ʾ���������Ҫ����Ϊ obj���Ա��ȡ������ʾ�ֶ����ݣ���TableManager#Column#mask)
usepref - boolean���Ƿ�ʹ�ó��ò�ѯ������Ĭ��Ϊtrue���ڲ��ֱ��������ó��ò�ѯ����������ϴ��������й���
orderby - string, ���ֶ���ҳ������orderbyѡ��Թؼ��ֽ���ƥ��
����:

   { 
        table:"spo", exprs: {st_id: 33, emp_id: 20, state: ["V","O", "S"], price: "12~20"}, 
        querystr: "13918891588", usepref: false, orderby: "stocktime_first"}
    }  

Expression
��ʽ

   {key: value}
key - �ֶ����ƣ���Ҫ�ڵ�ǰ������
value - �ֶε�ֵ��֧�������ֵ�������ʾ����һ��ƥ��
����: {"st_id": 13} ��ʾҪ��st_id=13


����
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
cmd - �̶�Ϊ"Search"
cachekey - ����Seach���ص�cachekey, ���������״�search�ͺ���search�Ĺؼ����ԣ����ʶ�𣬾���Ϊ��ȡ�б�ķ�ʽ
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
public class Search extends CmdHandler {
	/**
	 * ��������SearchResult.toJSON������и�װ���Է��Ͽͻ���Ҫ��
	 * @param ret redis SearchResult.toJSON��ֱ���ٴα༭
	 * @throws Exception
	 */
	protected void postAction(JSONObject ret) throws Exception{
		
	}
	
	/**
	 * ������Ĳ�ѯ���������ع�����֧������Ĳ�ѯ����
	 * @param jo ԭʼ�Ĳ�ѯ�������п����ڴ˵ر��ع�
	 * @return ����Ĳ�ѯ�����������ӵ���ѯwhere�����
	 * key: ��Ҫ���䵽sql where ���ֵ�clause������ݣ����� "emp_id=?"
	 * value ���ʺŶ�Ӧ��ʵ��ֵ��Ŀǰkey��֧��һ���ʺű����Ӧ�����value= ��ǰemp��id
	 * 
	 */
	protected HashMap<String, Object> reviseSearchCondition(JSONObject jo) throws Exception{
		return null;
	}
	
	@Override
	public CmdResult execute(JSONObject jo) throws Exception {
		SearchResult sr=null;
		
		//���û�ж���table, ��ʹ��class�����е�searchǰ�Ĳ���
		Table table=findTable(jo,"Search");
		if(table!=null && Validator.isNull( jo.optString("table"))) 
			jo.put("table", table.getName());
		String cacheKey=jo.optString("cachekey");
		boolean idOnly=jo.optBoolean("idonly",false);
		 
		if(Validator.isNull(cacheKey) || !jedis.exists(cacheKey) /*������key timeoutҲҪ���²�*/) {
			//������������
			HashMap<String, Object> addParams=reviseSearchCondition(jo);
			sr=this.search(jo, addParams);
		}else{
			
			//��֤cacheKey���û�һ��
			String[] keyParts=cacheKey.split(":");
			if(keyParts.length!=6) throw new NDSException("�����cachekey");
			
			if(!"list".equals(keyParts[0]))throw new NDSException("�����cachekey(list)");
			
			if(!keyParts[2].equals(String.valueOf(usr.getId()))) throw new NDSException("�����cachekey(uid)");
			
			int start= jo.optInt("start",0);
			int pageSize= jo.optInt("pagesize", 20);
			sr=this.searchByCache(cacheKey, start, pageSize, table, idOnly);
		}
		JSONObject ret= sr.toJSONObject();
		
		//�������þ����Ƿ���װ����Ϊ���ӽṹ
		JSONArray ja=sr.getData();
		if(!idOnly){
			this.assembleArrayByConfName(ja, "table:"+table.getName() +":assemble:list", table);
			
			//���ÿ��Ԫ�أ��޸�tagcolumn��ֵ, תstringΪjsonarray
			for(int i=0;i<ja.length();i++){
				JSONObject one=ja.getJSONObject(i);
				this.reviseColumnsOfJSONTypeValue(one, table);
			}
		}
		postAction(ret);
		CmdResult res=new CmdResult(ret );

		return res;
	}
	
	
	
	
}
