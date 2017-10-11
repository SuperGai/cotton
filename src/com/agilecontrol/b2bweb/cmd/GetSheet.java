package com.agilecontrol.b2bweb.cmd;

import java.sql.Connection;

import org.apache.velocity.VelocityContext;
import org.json.JSONArray;
import org.json.JSONObject;

import redis.clients.jedis.Jedis;

import com.agilecontrol.b2b.cmd.ObjectGet;
import com.agilecontrol.b2b.cmd.Search;
import com.agilecontrol.b2b.schema.Table;
import com.agilecontrol.b2b.schema.TableManager;
import com.agilecontrol.b2bweb.WebController;
import com.agilecontrol.b2bweb.sheet.Sheet;
import com.agilecontrol.b2bweb.sheet.SheetBuilder;
import com.agilecontrol.nea.core.query.QueryEngine;
import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.nea.util.Tools;
import com.agilecontrol.nea.util.Validator;
import com.agilecontrol.phone.Admin;
import com.agilecontrol.phone.CmdHandler;
import com.agilecontrol.phone.CmdResult;
import com.agilecontrol.phone.PhoneConfig;
import com.agilecontrol.phone.PhoneController;
import com.agilecontrol.phone.PhoneUtils;
import com.agilecontrol.phone.PushWangxiangitems;
import com.agilecontrol.phone.UserObj;

/**
h1. 获取商品下单矩阵

h2. 输入

获取指定商品的下单矩阵
> {cmd:"b2b.pdt.sheet", pdtid, actid, readonly, isphone}

*pdtid* - 商品id
*actid* - 活动id，可不提供，表示获取不参加活动的商品矩阵，注意参加活动和不参加活动的商品sku不会合并
*readonly* - 是否只读，默认false，在订单确认界面如果显示矩阵，将需要设置此值true
*isphone* - 是否手机版，boolean，手机版的矩阵和PC版的不一致，看矩阵配置了

矩阵形式多有变化，具体实现的cmd为: com.agilecontrol.b2b2web.cmd.GetSheet，但实现类都在om.agilecontrol.b2bweb.sheet 包下，目前实现了标准的SKUSheet

配置矩阵的定义在ad_sql#sheet_conf中.

h2. 标准矩阵

按款模式显示矩阵，多色在多行。支持多条sql语句准备所需数据，目前的配置写在ad_sql#sheet_conf中,举例：

<pre>
sheet_conf:
{
	class:"com.agilecontrol.b2bweb.sheet.SKUSheetBuilder",
	phoneclass:"com.agilecontrol.b2bweb.sheet.PhoneSheetBuilder",
	items:[
		{desc:"可定库存",  sql: "sheet_available", lang:"@sheet_available@"},
		{desc:"本店库存", sql: "sheet_stock", lang:"@sheet_stock@"},
		{desc:"建议补货", sql: "sheet_sugg", lang:"@sheet_sugg@"},
		{desc:"单价", sql: "sheet_price",key:"price", lang:"@sheet_price@"},
		{desc:"下单量", sql: "sheet_orderqty", key:"qty", lang:"@sheet_orderqty@"}
	]
}

</pre>

h3. 格式说明

> {class, items}
*class*: string， 处理类名，全路径, 比如:"com.agilecontrol.b2bweb.sheet.SKUSheetBuilder" 是标准实现类的定义。
*phoneclass* string 手机版的class路径
*items* : jsonarray of jsonobj {desc,sql,key,lang}
>*desc*: string 单元格默认描述, 当lang为空的时候有效
>*lang*:  翻译的关键字， 将通过/act.nea/i18n/message_xxx.properties 文件翻译，选填
>*key*: 选填，界面上需要定位的key，固定的key为："price", "qty", 如果"price"未定义，表示使用商品标准价
>*sql*: ad_sql#name,必填， 普通的key，需要的sql语句格式:
> select asi asi, xxx value from xxx where user_id=$uid and $pdtid=m_product_id
> 如果是price字段(key="price")， 需要的sql语句格式:
> select asi asi, xxx value, xxx price from xxx where user_id=$uid and $pdtid=m_product_id
>> 可用变量$uid, $pdtid, $marketid, $actid 注意$actid 可能为-1表示没有, 对应普通字段，value即为对应的项目值，对于价格字段，value 是在矩阵上的显示项，price 为价格项目。详见文末区间价格定义。
>> *asi* 色码属性， m_attributesetinstance.user_id
> > *value* 数据项，普通的sheet单元格，放置的都是数据值，比如 库存值，下单量等，对于单价，尤其是阶梯价设计的sql语句，需要给出区间范围，比如12~240这样的内容，直接放在sheet中

h3. redis 说明

在redis中将为每个商品生成矩阵对应数据，key: *"pdt:$pdtid:sheet"* , value: string, 实际为jsonobject
<pre>
{colors, sizes, pdtids, asis}
</pre>
*colors* - jsonarray of {c,n,s}  c - code, n - name, s - [int], 分别表示每个尺码位是否有asi ，举例：{n:'green' , s:[0,1,1,1,0], c:'01'}
*sizes* - jsonarray of string ，每个位置对应尺码的名称，是m_attribute.value
*pdtids* - jsonarray of int, 每行商品的id
*asis* - jsonarray of jsonarray of int, 每行，每列的asi, 为空表示无

关于颜色对象中的n ，是可以扩展定义的：系统参数ad_param#fair.simple_matrix_row_desc 决定颜色的表述，默认值： $color.description
可以使用的变量：$color, $pdt, 其中
*$color* 对象具有属性 {id,value,description} , 对应m_attributevalue（颜色) 的id,value,name
*$pdt* 对象具有属性{m_attributeset_id, name,value,flowno,m_sizegroup_id,stylename,pricelist,shortname,islookbook,column1,column2,column3,column4,documentnote}，对应m_product的同名字段

使用vecloticy进行表述转换


h2. 输出

> {sheet}

*sheet* jsonobj 格式说明见 http://prj2.lifecycle.cn/redmine/projects/20141006_prductdfn/wiki/GetSheet

h2. 目前可选的class

h3. com.agilecontrol.b2bweb.sheet.SKUSheetBuilder

!stsheet.jpg!

返回客户端的矩阵信息：
*values* - jsonobject, key: "p"+$pdtid+"_"+$asi,  value 是对应的下单量

为支持阶梯价定义，需要在客户端除了实现 c(x,y) 公式外，还需要实现
<pre><code class="javascript">

  根据输入数量，计算金额，价格有两种形式：固定价或阶梯价
  @param p - 价格定义，2种格式: 浮点数或一维数组，浮点数表示单一固定价格，一维数组表示阶梯价，每3个元素分别代表，区间最低订量，区间最高订量，价格。
   例如：[0,400,50,401,1000,30,1001,99999,20] 表示:
  0~400件（含400件）价格为50， 
  401~1000件，价格为30，
  1001~更多，价格为20
  @param c - 当前订量，来源于c(x,y)的返回值
  @return 最终金额

function amt(p, c) {
  return amt;   
 
}
</code></pre>

h3. 区间价格定义

区间价格是后台的促销模型，即商品订量在不同价格
> select asi asi, xxx value, xxx price from xxx where user_id=$uid and $pdtid=m_product_id
*asi* - 指定商品的m_attributesetinstance.id
*value* - 价格描述，需要显示在单元格中，建议价格区间显示为：100~499字样，显示全段价格最大值和最小值
*price* - 普通固定价，直接显示价格，区间价格，需要还回格式"[0,400,50,401,1000,30,1001,99999,20]"每3个元素分别代表，区间最低订量，区间最高订量，价格。示例的含义是：
  0~400件（含400件）价格为50， 
  401~1000件，价格为30，
  1001~更多，价格为20



 * @author yfzhu
 *
 */
@Admin(mail="yfzhu@lifecycle.cn")
public class GetSheet extends CmdHandler {
	
	
	/**
	 * 
	 */
	public CmdResult execute(JSONObject jo) throws Exception {
		
		JSONObject conf=(JSONObject)PhoneController.getInstance().getValueFromADSQLAsJSON("sheet_conf", conn, false);
		boolean isPhone=jo.optBoolean("isphone", false);
		String classConf=isPhone?"phoneclass":"class";
		String clazz=getString(conf,classConf);
		SheetBuilder builder=(SheetBuilder)Class.forName(clazz).newInstance();		
		builder.init(usr, jo,conf, event, vc, jedis, conn);
		builder.build(); 
		JSONObject ret=builder.getSheet();
		return new CmdResult(ret);
	}

}













