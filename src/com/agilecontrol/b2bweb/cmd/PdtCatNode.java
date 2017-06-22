package com.agilecontrol.b2bweb.cmd;

import org.json.JSONArray;
import org.json.JSONObject;

import com.agilecontrol.nea.core.query.QueryEngine;
import com.agilecontrol.phone.Admin;
import com.agilecontrol.phone.CmdHandler;
import com.agilecontrol.phone.CmdResult;
import com.agilecontrol.phone.PhoneController;

/**
 *  h1. ��Ʒ�ϼ�dimid

	h2. ����
	
	> {cmd:"b2b.pdt.catnode", pdtid:"6120"}
	
	*pdtid* - int pdt.id
	
	h2. ���
	
	> {dim,value}
	
	*value* int m_dim1_id.m_dim4_id.m_dim14_id (��Ŀǰ�������������)
	
	����
	<pre>
	{
	 "dim": "10808.10810.10815"
	}
	</pre>
	
	h2. ����˵��
	
	#��ȡpdtid
	#����ad_sql���ж�dim�㼶���Ӷ�֪�����ݿ��dimid
	#�ж�key��pdt:$pdtid:cat �Ƿ���ڣ����ڣ�����JSONObject���󡣷��򣬴���key�����浽jedis��
	#���ڲ㼶dimid����һ�����жϣ����sql�õ��Ľ����12.34.56.13���Ĳ�ý��ͣ����Ǿͷ���12.34.56.13���������12.34.-2.13���Ǿͷ���12.34������value��
	#��ps��-2��oracle�е�nvl�����������õ�һ��Ĭ��ֵ������Ϊ�κγ�-1����ĸ�����
	#��󷵻ص���һ��JSONObject
 * 
 * @author lsh
 *
 */
@Admin(mail="li.shuhao@lifecycle.cn")
public class PdtCatNode extends CmdHandler {

	@Override
	public CmdResult execute(JSONObject jo) throws Exception {
		int pdtid = jo.optInt("pdtid");
		JSONObject ja = getDim(pdtid);
		return new CmdResult(ja);
	}
	public JSONObject getDim(int pdtid) throws Exception{
		String key = "pdt:"+pdtid+":cat";
		String ad_sql = "b2b:cat:tree_conf";
		StringBuilder dimid = new StringBuilder();
		if(jedis.exists(key)){
			return new JSONObject().put("cat", jedis.get(key));
		}else{
			JSONArray level = (JSONArray)PhoneController.getInstance().getValueFromADSQLAsJSON(ad_sql, conn);
			for(int i = 0;i < level.length();i++){
				String dim = level.getString(i);
				if(i==0){
					dimid.append(" nvl(m_"+dim+"_id,-2)");
				}else{
					dimid.append("||'.'||nvl(m_"+dim+"_id,-2)");
				}
			}
			String sql = "select "+dimid+"dim from m_product where id = ?";
			JSONObject ja = QueryEngine.getInstance().doQueryObject(sql, new Object[]{pdtid});
			String value = ja.optString("dim");
			String[] str = value.split("\\.");
			//�������ɵ�valueֵ������У�飬����-2����-2֮ǰ��ȫ������value��
			for(int i = 0;i < str.length;i++){
				if(str[i].equals("-2")){
					StringBuilder stb = new StringBuilder();
					for(int j = 0;j < i;j++){
						stb.append(str[j]+".");
					}
					stb.deleteCharAt(stb.length()-1);
					value = stb.toString();
					break;
				}
			}
			jedis.set(key, value);
		}
		return new JSONObject().put("cat", jedis.get(key));
	}

}
