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

h1. 获取商品分类树

h2. 场景

首页或列表

h2. 输入

> {cmd:"b2b.cat.tree"}

完整的分类获取

h2. 输出

> [{cat}]

cat 对象定义
> {dim,pDim,name,level,pic}
*dim* - int pdtid
*pDim* - string 商品备注 
*name* - string 商品编号
*level* -string 商品图片
*pic* - double 显示的价格

举例
<pre>
[ 
	{ "dim":"-1","pDim":"", name:"全部商品分类",level:0,pic:""},
	{ "dim":"12","pDim":"-1",name:"书写文具",level:1,pic:""},
	{ "dim":"12.7","pDim":"12",name:"钢笔",level:2,pic:""},
	{ "dim":"12.7.103","pDim":"12.7",name:"高级钢笔",level:3,pic:""},
	{ "dim":"3","pDim":"-1",name:"纸品",level:1,pic:""},
	{ "dim":"3.14","pDim":"3",name:"抽纸",level:2,pic:""}
]
</pre>

上述的dim是array 行的唯一键，计算规则

$marketname 表示市场代码，默认为cn
$lang 表示当前用户语言，默认 zh

读取市场代码，查找redis key - "b2b:cat:tree:$marketname:$lang", 如果存在，直接返回

首先，需要在ad_sql#b2b:cat:tree_conf 中定义dim层级，比如: ["dim1", "dim3", "dim14"], 表示维度层级，sql语句的写法：
<pre>
select distinct dim1_id, dim1_name, dim1_imgurl, dim3_id, dim3_name, dim3_imgurl,dim14_id, dim14, dim14_imgurl 
from v_pdtdims pdtdims 
where $sql_when_market_name_not_equals_default
order by dim1_orderno, dim3_orderno, dim14_orderno  
</pre>
v_pdtdims - view 默认是将20个分类全部select出来的大型sql语句，如果市场代码!=市场代码默认值（PhoneConfig.DEFAULT_MARKET="cn"），需要增加条件$sql_when_market_name_not_equals_default:

> and exists(select 1 from B_MK_PDT mpdt where mpdt.m_product_id= pdtdims.pdtid and mpdt.is_active='Y' and mpdt.b_market_id=$marketid) 

resultarray=new JSONArray() of JSONObject
首先建立第0层(level=0)，默认  dim="-1", pdim="null",  name="全部", level=0, pic=""
然后：
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

load 商品属性值翻译文件到 PhoneController#DimTranlator
for each lang in the system
    translate name of resultarray row
    缓存resultarray 在redis中，key: b2b:cat:tree:$marketname:$lang, value: string, resultarray.tostring()
	缓存resulthash to redis: key: "b2b:cat:tree:$marketname:$lang:dim", value: hset, key: $dim，value: list of 下级dimobj {dim, name, pic}（用于subcat）
next


关于DimTranlator 属性翻译表

对应数据库M_DIM_TRANS，全部缓存，以提高翻译效率
key: m_dim_id+lang, value: attributename,
如果key不存在，返回原值 


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
	//基于本方法的现有要求，我们的层级主要分为大类，中类，小类，所以说大类是必须存在的，中类和小类是可以不存在的，但就目前而言，中类如果不存在，小类即使存在，我们也不做考虑
	public JSONArray getAllDim(int marketid,int langid) throws Exception{
		String key = "b2b:cat:tree:"+marketid+":"+langid;
		String hashkey = "b2b:cat:tree:"+marketid+":"+langid+":dim";
		String ad_sql = "b2b:cat:tree_conf";
		DimTranslator dimtrans=DimTranslator.getInstance();
		if(jedis.exists(key)) {
			return new JSONArray(jedis.get(key));
		}
		else{
			//找到dim层级，获取level中的每一个dim
			JSONArray level = (JSONArray)PhoneController.getInstance().getValueFromADSQLAsJSON(ad_sql, conn);
			StringBuilder dimsql = new StringBuilder();//子字段
			StringBuilder dimnum = new StringBuilder();//字段number
			StringBuilder imgsql = new StringBuilder();//图片url
			StringBuilder namesql = new StringBuilder();//字段name
			String dim = null;//层级对象
			JSONArray resultarray = new JSONArray();//未翻译的JSONArray结果集
			Map<String,String> map = new HashMap();//resulthash
			JSONObject ancestor = new JSONObject();//层级为0的{"dim":"-1","pDim":"", name:"全部商品分类",level:0,pic:""}
			//生成祖先级key
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
			//对于此方法，我们利用层级先做一次遍历，来生成带有层级定义的dim字段，方便返回我们所要求的对象，再利用每次层级遍历的sql语句，找到对应的dim，pdim字段
			for(int i = 0;i < level.length();i++){
				//通过下面的循环拼凑成我们想要的sql语句
				for(int j = i;j <= i;j++){
					dim = level.getString(i);
//					logger.debug("dim是"+dim);
					if(j == 0){
						dimsql.append(" nvl("+dim+"_id,-2)");
						dimnum.append( ""+dim+"_orderno");
						
					}else{
						dimsql = new StringBuilder().append(dimsql.substring(0, dimsql.lastIndexOf("d")));
						dimsql.append("||'.'||nvl("+dim+"_id,-2)");
						dimnum.append(","+dim+"_orderno");
					}
				}
				dimsql.append(" dim");//为dimid起别名为dim
				imgsql.append(dim+"_imgurl imgurl,"+dim+"_picture img");//为dimimgurl起别名为imgurl
				namesql.append(dim+" name");//为dimname起别名为imgurlname
				String sql = new String();
				sql = "select distinct"
								+dimsql+","+imgsql+","+namesql+","+dimnum+" "
								+"from v_pdtdims pdtdims "
								+"where "
								+"exists(select 1 from B_MK_PDT mpdt where mpdt.m_product_id= pdtdims.pdtid and mpdt.isactive='Y' and mpdt.b_market_id=?) "
								+"order by "+dimnum; 
//				logger.debug("sql语句是"+sql);
				JSONArray ja = QueryEngine.getInstance().doQueryObjectArray(sql, new Object[]{marketid},conn);
				namesql = new StringBuilder();
				imgsql = new StringBuilder();
				if(Validator.isNull(ja.toString())) throw new NDSException("sql结果为空 ");
				//执行sql语句，得到我们想要的字段
				for(int k = 0;k < ja.length();k++){
					//因为大类是必须要求存在的，所以我们就不做判断了，而且大类的parentdim就是-1，所以我们单独拿出来
					if(i==0){
						JSONObject jo = ja.getJSONObject(k);
						JSONObject jo1 = new JSONObject();
//						logger.debug("jo的JSONObejct形式"+jo);
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
						//新增类别图片
						String img= jo.optString("img");
						if(Validator.isNull(img)){
							jo1.put("image", "");
						}else{
							jo1.put("image", img);
						}
						resultarray.put(jo1);
						
						//大类的pdim肯定是-1，而我们不管包不包含都生成了该langid的map，所以不用判断,所以我们可以把dim放入包含该key的JSONArray中
						JSONObject jo2 = new JSONObject();
						jo2.put("dim", newdim);
						jo2.put("name",nullToBlank( dimLang1.replaceAll("\\\\", "")));
						jo2.put("pic", pic);
						map.put("-1",new JSONArray(map.get("-1")).put(jo2).toString());
					}
					//当i>0时，说明我们sql中的dim字段是大类.中类.小类.孙子类云云。
					//这种情况，我们需要判断大类下面的后代（中类，小类，孙子类云云）,if不存在（我们设为-2，oracle中的nvl函数），则不把该对象放入需求中
					else{
						JSONObject jo1 = ja.getJSONObject(k);
						JSONObject jo2 = new JSONObject();
						
						//获取字段dim，对其进行判断
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
						//新增类别图片
						String img= jo1.optString("img");
						if(Validator.isNull(img)){
							jo2.put("image", "");
						}else{
							jo2.put("image", img);
						}
						resultarray.put(jo2);
						
						//对于大类我们可以直接进行插入，因为sql语句中有distinct字段，所以说大类是必须放进去的，而中类，小类则必须判断一下pdim，存在就直接放入key为pdim对应的value
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
			//翻译jedis中key所对应的value中的name字段
			jedis.set(key, resultarray.toString());
			jedis.hmset(hashkey, map);
			
		}
		
		return new JSONArray(jedis.get(key));
	}
}

