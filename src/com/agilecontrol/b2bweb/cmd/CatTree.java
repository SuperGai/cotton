package com.agilecontrol.b2bweb.cmd;

import java.sql.Connection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.aop.framework.HashMapCachingAdvisorChainFactory;

import com.agilecontrol.b2bweb.DimTranslator;
import com.agilecontrol.nea.core.query.QueryEngine;
import com.agilecontrol.nea.core.query.QueryException;
import com.agilecontrol.nea.core.util.MessagesHolder;
import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.nea.util.Validator;
import com.agilecontrol.phone.Admin;
import com.agilecontrol.phone.CmdHandler;
import com.agilecontrol.phone.CmdResult;
import com.agilecontrol.phone.PhoneController;
import com.agilecontrol.phone.UserObj;

import redis.clients.jedis.Jedis;

/**

h1. ��ȡ��Ʒ������

h2. ����

��ҳ���б�

h2. ����

> {cmd:"b2b.cat.tree"}

�����ķ����ȡ

h2. ���

> [{cat}]

cat ������
> {dim,pDim,name,level,pic}
*dim* - int pdtid
*pDim* - string ��Ʒ��ע 
*name* - string ��Ʒ���
*level* -string ��ƷͼƬ
*pic* - double ��ʾ�ļ۸�

����
<pre>
[ 
	{ "dim":"-1","pDim":"", name:"ȫ����Ʒ����",level:0,pic:""},
	{ "dim":"12","pDim":"-1",name:"��д�ľ�",level:1,pic:""},
	{ "dim":"12.7","pDim":"12",name:"�ֱ�",level:2,pic:""},
	{ "dim":"12.7.103","pDim":"12.7",name:"�߼��ֱ�",level:3,pic:""},
	{ "dim":"3","pDim":"-1",name:"ֽƷ",level:1,pic:""},
	{ "dim":"3.14","pDim":"3",name:"��ֽ",level:2,pic:""}
]
</pre>

������dim��array �е�Ψһ�����������

$marketname ��ʾ�г����룬Ĭ��Ϊcn
$lang ��ʾ��ǰ�û����ԣ�Ĭ�� zh

��ȡ�г����룬����redis key - "b2b:cat:tree:$marketname:$lang", ������ڣ�ֱ�ӷ���

���ȣ���Ҫ��ad_sql#b2b:cat:tree_conf �ж���dim�㼶������: ["dim1", "dim3", "dim14"], ��ʾά�Ȳ㼶��sql����д����
<pre>
select distinct dim1_id, dim1_name, dim1_imgurl, dim3_id, dim3_name, dim3_imgurl,dim14_id, dim14, dim14_imgurl 
from v_pdtdims pdtdims 
where $sql_when_market_name_not_equals_default
order by dim1_orderno, dim3_orderno, dim14_orderno  
</pre>
v_pdtdims - view Ĭ���ǽ�20������ȫ��select�����Ĵ���sql��䣬����г�����!=�г�����Ĭ��ֵ��PhoneConfig.DEFAULT_MARKET="cn"������Ҫ��������$sql_when_market_name_not_equals_default:

> and exists(select 1 from B_MK_PDT mpdt where mpdt.m_product_id= pdtdims.pdtid and mpdt.is_active='Y' and mpdt.b_market_id=$marketid) 

resultarray=new JSONArray() of JSONObject
���Ƚ�����0��(level=0)��Ĭ��  dim="-1", pdim="null",  name="ȫ��", level=0, pic=""
Ȼ��
for i=0.. [ad_sql#b2b:cat:tree_conf].length
    if(i==0) conf[i-1]_id="-1"
    sql="select distinct (conf[0]_id..conf[i-1]_id).join(".") pDim, (conf[0]_id..conf[i]_id).join(".") dim,conf[i]_name name,conf[i]_imgurl pic 
    	from v_pdtdims where exists(xxx) order by conf[0]_orderno,...conf[i]_orderno"
    dimarray=doQueryJSONObjectArray(sql)
    for(j in dimarray){
    	if(conf[i]_id is not null){
    		row={dim: dim, pDim: pDim, name: conf[i]_name, level:i+1, pic: conf[i]_imgurl }
    		add row to resultarray
    		hash row.dim to resulthash(key=pdim, value=list of children {dim,name,pic})
    	}
    }
next

load ��Ʒ����ֵ�����ļ��� PhoneController#DimTranlator
for each lang in the system
    translate name of resultarray row
    ����resultarray ��redis�У�key: b2b:cat:tree:$marketname:$lang, value: string, resultarray.tostring()
	����resulthash to redis: key: "b2b:cat:tree:$marketname:$lang:dim", value: hset, key: $dim��value: list of �¼�dimobj {dim, name, pic}������subcat��
next


����DimTranlator ���Է����

��Ӧ���ݿ�M_DIM_TRANS��ȫ�����棬����߷���Ч��
key: m_dim_id+lang, value: attributename,
���key�����ڣ�����ԭֵ 


 * @author yfzhu
 *
 */
@Admin(mail="yfzhu@lifecycle.cn")
public class CatTree extends CmdHandler {
	@Override
	public CmdResult execute(JSONObject jo) throws Exception {
		JSONArray ja = getAllDim(usr.getMarketId(),usr.getLangId());
		return new CmdResult(ja);
	}
	/**
	 * 
	 * @param s if is null, return "@BLANK@"
	 * @return
	 */
	private String nullToBlank(String s){
		if(Validator.isNull(s)){
			return "@empty@";
		}
		return s;
	}
	//���ڱ�����������Ҫ�����ǵĲ㼶��Ҫ��Ϊ���࣬���࣬С�࣬����˵�����Ǳ�����ڵģ������С���ǿ��Բ����ڵģ�����Ŀǰ���ԣ�������������ڣ�С�༴ʹ���ڣ�����Ҳ��������
	public JSONArray getAllDim(int marketid,int langid) throws Exception{
		String key = "b2b:cat:tree:"+marketid+":"+langid;
		String hashkey = "b2b:cat:tree:"+marketid+":"+langid+":dim";
		String ad_sql = "b2b:cat:tree_conf";
		DimTranslator dimtrans=DimTranslator.getInstance();
		if(jedis.exists(key)) {
			return new JSONArray(jedis.get(key));
		}
		else{
			//�ҵ�dim�㼶����ȡlevel�е�ÿһ��dim
			JSONArray level = (JSONArray)PhoneController.getInstance().getValueFromADSQLAsJSON(ad_sql, conn);
			StringBuilder dimsql = new StringBuilder();//���ֶ�
			StringBuilder dimnum = new StringBuilder();//�ֶ�number
			StringBuilder imgsql = new StringBuilder();//ͼƬurl
			StringBuilder namesql = new StringBuilder();//�ֶ�name
			String dim = null;//�㼶����
			JSONArray resultarray = new JSONArray();//δ�����JSONArray�����
			Map<String,String> map = new HashMap();//resulthash
			JSONObject ancestor = new JSONObject();//�㼶Ϊ0��{"dim":"-1","pDim":"", name:"ȫ����Ʒ����",level:0,pic:""}
			//�������ȼ�key
			ancestor.put("dim", "-1");
			ancestor.put("pDim", "");
			ancestor.put("level", "0");
			String str= MessagesHolder.getInstance().getMessage(locale, "all-categories");
			String dimLang=dimtrans.getTranslateName(-1, langid, str, conn);
			ancestor.put("name", nullToBlank(dimLang.replaceAll("\\\\", "")));
			ancestor.put("pic", "");
			resultarray.put(ancestor);
			if(!map.containsKey("-1")){
				map.put("-1", new JSONArray().toString());
			}
			//���ڴ˷������������ò㼶����һ�α����������ɴ��в㼶�����dim�ֶΣ����㷵��������Ҫ��Ķ���������ÿ�β㼶������sql��䣬�ҵ���Ӧ��dim��pdim�ֶ�
			for(int i = 0;i < level.length();i++){
				//ͨ�������ѭ��ƴ�ճ�������Ҫ��sql���
				for(int j = i;j <= i;j++){
					dim = level.getString(i);
//					logger.debug("dim��"+dim);
					if(j == 0){
						dimsql.append(" nvl("+dim+"_id,-2)");
						dimnum.append( ""+dim+"_orderno");
						
					}else{
						dimsql = new StringBuilder().append(dimsql.substring(0, dimsql.lastIndexOf("d")));
						dimsql.append("||'.'||nvl("+dim+"_id,-2)");
						dimnum.append(","+dim+"_orderno");
					}
				}
				dimsql.append(" dim");//Ϊdimid�����Ϊdim
				imgsql.append(dim+"_imgurl imgurl,"+dim+"_picture img");//Ϊdimimgurl�����Ϊimgurl
				namesql.append(dim+" name");//Ϊdimname�����Ϊimgurlname
				String sql = new String();
				sql = "select distinct"
								+dimsql+","+imgsql+","+namesql+","+dimnum+" "
								+"from v_pdtdims pdtdims "
								+"where "
								+"exists(select 1 from B_MK_PDT mpdt where mpdt.m_product_id= pdtdims.pdtid and mpdt.isactive='Y' and mpdt.b_market_id=?) "
								+"order by "+dimnum; 
//				logger.debug("sql�����"+sql);
				JSONArray ja = QueryEngine.getInstance().doQueryObjectArray(sql, new Object[]{marketid},conn);
				namesql = new StringBuilder();
				imgsql = new StringBuilder();
				if(Validator.isNull(ja.toString())) throw new NDSException("sql���Ϊ�� ");
				//ִ��sql��䣬�õ�������Ҫ���ֶ�
				for(int k = 0;k < ja.length();k++){
					//��Ϊ�����Ǳ���Ҫ����ڵģ��������ǾͲ����ж��ˣ����Ҵ����parentdim����-1���������ǵ����ó���
					if(i==0){
						JSONObject jo = ja.getJSONObject(k);
						JSONObject jo1 = new JSONObject();
//						logger.debug("jo��JSONObejct��ʽ"+jo);
						String newdim = jo.optString("dim");
						jo1.put("dim", newdim);
						jo1.put("pDim", "-1");
						jo1.put("level", "1");
						int dimId= Integer.valueOf(newdim.substring(newdim.lastIndexOf(".")+1, newdim.length()));
						String dimLang1=dimtrans.getTranslateName(dimId, langid, jo.getString("name"), conn);
						jo1.put("name",nullToBlank( dimLang1.replaceAll("\\\\", "")));
						String pic= jo.optString("imgurl");
						if(Validator.isNull(pic)){
							jo1.put("pic", "");
						}else{
							jo1.put("pic", pic);
						}
						//�������ͼƬ
						String img= jo.optString("img");
						if(Validator.isNull(img)){
							jo1.put("image", "");
						}else{
							jo1.put("image", img);
						}
						resultarray.put(jo1);
						
						//�����pdim�϶���-1�������ǲ��ܰ��������������˸�langid��map�����Բ����ж�,�������ǿ��԰�dim���������key��JSONArray��
						JSONObject jo2 = new JSONObject();
						jo2.put("dim", newdim);
						jo2.put("name",nullToBlank( dimLang1.replaceAll("\\\\", "")));
						jo2.put("pic", pic);
						map.put("-1",new JSONArray(map.get("-1")).put(jo2).toString());
					}
					//��i>0ʱ��˵������sql�е�dim�ֶ��Ǵ���.����.С��.���������ơ�
					//���������������Ҫ�жϴ�������ĺ�������࣬С�࣬���������ƣ�,if�����ڣ�������Ϊ-2��oracle�е�nvl���������򲻰Ѹö������������
					else{
						JSONObject jo1 = ja.getJSONObject(k);
						JSONObject jo2 = new JSONObject();
						
						//��ȡ�ֶ�dim����������ж�
						String newdim = jo1.optString("dim");
						if(newdim.contains("-2")) continue;
						String pdim = newdim.substring(0, newdim.lastIndexOf("."));
						jo2.put("dim", newdim);
						jo2.put("pDim", pdim);
						jo2.put("level", i+1);
						int dimId= Integer.valueOf(newdim.substring(newdim.lastIndexOf(".")+1, newdim.length()));
						String dimLang2=dimtrans.getTranslateName(dimId, langid, jo1.getString("name"), conn);
						jo2.put("name",nullToBlank( dimLang2.replaceAll("\\\\", "")));
						String pic= jo1.optString("imgurl");
						if(Validator.isNull(pic)){
							jo2.put("pic", "");
						}else{
							jo2.put("pic", pic);
						}
						//�������ͼƬ
						String img= jo1.optString("img");
						if(Validator.isNull(img)){
							jo2.put("image", "");
						}else{
							jo2.put("image", img);
						}
						resultarray.put(jo2);
						
						//���ڴ������ǿ���ֱ�ӽ��в��룬��Ϊsql�������distinct�ֶΣ�����˵�����Ǳ���Ž�ȥ�ģ������࣬С��������ж�һ��pdim�����ھ�ֱ�ӷ���keyΪpdim��Ӧ��value
						JSONObject jo3 = new JSONObject();
						jo3.put("dim", newdim);
						jo3.put("name",nullToBlank( dimLang2.replaceAll("\\\\", "")));
						if(Validator.isNull(pic)){
							jo3.put("pic", "");
						}else{
							jo3.put("pic", pic);
						}
						if(map.containsKey(pdim)){
							map.put(pdim, new JSONArray(map.get(pdim)).put(jo3).toString());
						}
						else{
							map.put(pdim, new JSONArray().toString());
							map.put(pdim, new JSONArray(map.get(pdim)).put(jo3).toString());
						}
					}
				}
			}
			//����jedis��key����Ӧ��value�е�name�ֶ�
			jedis.set(key, resultarray.toString());
			jedis.hmset(hashkey, map);
			
		}
		
		return new JSONArray(jedis.get(key));
	}
}

