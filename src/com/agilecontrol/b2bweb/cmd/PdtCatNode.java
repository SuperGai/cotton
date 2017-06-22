package com.agilecontrol.b2bweb.cmd;

import org.json.JSONArray;
import org.json.JSONObject;

import com.agilecontrol.nea.core.query.QueryEngine;
import com.agilecontrol.phone.Admin;
import com.agilecontrol.phone.CmdHandler;
import com.agilecontrol.phone.CmdResult;
import com.agilecontrol.phone.PhoneController;

/**
 *  h1. 商品上级dimid

	h2. 输入
	
	> {cmd:"b2b.pdt.catnode", pdtid:"6120"}
	
	*pdtid* - int pdt.id
	
	h2. 输出
	
	> {dim,value}
	
	*value* int m_dim1_id.m_dim4_id.m_dim14_id (就目前极限情况是这样)
	
	举例
	<pre>
	{
	 "dim": "10808.10810.10815"
	}
	</pre>
	
	h2. 开发说明
	
	#获取pdtid
	#根据ad_sql来判断dim层级，从而知道数据库的dimid
	#判断key：pdt:$pdtid:cat 是否存在，存在，返回JSONObject对象。否则，创建key，缓存到jedis中
	#对于层级dimid进行一定的判断，如果sql得到的结果如12.34.56.13（四层好解释），那就返回12.34.56.13，而结果如12.34.-2.13，那就返回12.34。放入value中
	#（ps，-2是oracle中的nvl函数我们设置的一个默认值，可以为任何除-1以外的负数）
	#最后返回的是一个JSONObject
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
			//对于生成的value值，进行校验，存在-2，把-2之前的全部放入value中
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
