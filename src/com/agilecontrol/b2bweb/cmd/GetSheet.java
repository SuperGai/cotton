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
h1. ��ȡ��Ʒ�µ�����

h2. ����

��ȡָ����Ʒ���µ�����
> {cmd:"b2b.pdt.sheet", pdtid, actid, readonly, isphone}

*pdtid* - ��Ʒid
*actid* - �id���ɲ��ṩ����ʾ��ȡ���μӻ����Ʒ����ע��μӻ�Ͳ��μӻ����Ʒsku����ϲ�
*readonly* - �Ƿ�ֻ����Ĭ��false���ڶ���ȷ�Ͻ��������ʾ���󣬽���Ҫ���ô�ֵtrue
*isphone* - �Ƿ��ֻ��棬boolean���ֻ���ľ����PC��Ĳ�һ�£�������������

������ʽ���б仯������ʵ�ֵ�cmdΪ: com.agilecontrol.b2b2web.cmd.GetSheet����ʵ���඼��om.agilecontrol.b2bweb.sheet ���£�Ŀǰʵ���˱�׼��SKUSheet

���þ���Ķ�����ad_sql#sheet_conf��.

h2. ��׼����

����ģʽ��ʾ���󣬶�ɫ�ڶ��С�֧�ֶ���sql���׼���������ݣ�Ŀǰ������д��ad_sql#sheet_conf��,������

<pre>
sheet_conf:
{
	class:"com.agilecontrol.b2bweb.sheet.SKUSheetBuilder",
	phoneclass:"com.agilecontrol.b2bweb.sheet.PhoneSheetBuilder",
	items:[
		{desc:"�ɶ����",  sql: "sheet_available", lang:"@sheet_available@"},
		{desc:"������", sql: "sheet_stock", lang:"@sheet_stock@"},
		{desc:"���鲹��", sql: "sheet_sugg", lang:"@sheet_sugg@"},
		{desc:"����", sql: "sheet_price",key:"price", lang:"@sheet_price@"},
		{desc:"�µ���", sql: "sheet_orderqty", key:"qty", lang:"@sheet_orderqty@"}
	]
}

</pre>

h3. ��ʽ˵��

> {class, items}
*class*: string�� ����������ȫ·��, ����:"com.agilecontrol.b2bweb.sheet.SKUSheetBuilder" �Ǳ�׼ʵ����Ķ��塣
*phoneclass* string �ֻ����class·��
*items* : jsonarray of jsonobj {desc,sql,key,lang}
>*desc*: string ��Ԫ��Ĭ������, ��langΪ�յ�ʱ����Ч
>*lang*:  ����Ĺؼ��֣� ��ͨ��/act.nea/i18n/message_xxx.properties �ļ����룬ѡ��
>*key*: ѡ���������Ҫ��λ��key���̶���keyΪ��"price", "qty", ���"price"δ���壬��ʾʹ����Ʒ��׼��
>*sql*: ad_sql#name,��� ��ͨ��key����Ҫ��sql����ʽ:
> select asi asi, xxx value from xxx where user_id=$uid and $pdtid=m_product_id
> �����price�ֶ�(key="price")�� ��Ҫ��sql����ʽ:
> select asi asi, xxx value, xxx price from xxx where user_id=$uid and $pdtid=m_product_id
>> ���ñ���$uid, $pdtid, $marketid, $actid ע��$actid ����Ϊ-1��ʾû��, ��Ӧ��ͨ�ֶΣ�value��Ϊ��Ӧ����Ŀֵ�����ڼ۸��ֶΣ�value ���ھ����ϵ���ʾ�price Ϊ�۸���Ŀ�������ĩ����۸��塣
>> *asi* ɫ�����ԣ� m_attributesetinstance.user_id
> > *value* �������ͨ��sheet��Ԫ�񣬷��õĶ�������ֵ������ ���ֵ���µ����ȣ����ڵ��ۣ������ǽ��ݼ���Ƶ�sql��䣬��Ҫ�������䷶Χ������12~240���������ݣ�ֱ�ӷ���sheet��

h3. redis ˵��

��redis�н�Ϊÿ����Ʒ���ɾ����Ӧ���ݣ�key: *"pdt:$pdtid:sheet"* , value: string, ʵ��Ϊjsonobject
<pre>
{colors, sizes, pdtids, asis}
</pre>
*colors* - jsonarray of {c,n,s}  c - code, n - name, s - [int], �ֱ��ʾÿ������λ�Ƿ���asi ��������{n:'green' , s:[0,1,1,1,0], c:'01'}
*sizes* - jsonarray of string ��ÿ��λ�ö�Ӧ��������ƣ���m_attribute.value
*pdtids* - jsonarray of int, ÿ����Ʒ��id
*asis* - jsonarray of jsonarray of int, ÿ�У�ÿ�е�asi, Ϊ�ձ�ʾ��

������ɫ�����е�n ���ǿ�����չ����ģ�ϵͳ����ad_param#fair.simple_matrix_row_desc ������ɫ�ı�����Ĭ��ֵ�� $color.description
����ʹ�õı�����$color, $pdt, ����
*$color* ����������� {id,value,description} , ��Ӧm_attributevalue����ɫ) ��id,value,name
*$pdt* �����������{m_attributeset_id, name,value,flowno,m_sizegroup_id,stylename,pricelist,shortname,islookbook,column1,column2,column3,column4,documentnote}����Ӧm_product��ͬ���ֶ�

ʹ��vecloticy���б���ת��


h2. ���

> {sheet}

*sheet* jsonobj ��ʽ˵���� http://prj2.lifecycle.cn/redmine/projects/20141006_prductdfn/wiki/GetSheet

h2. Ŀǰ��ѡ��class

h3. com.agilecontrol.b2bweb.sheet.SKUSheetBuilder

!stsheet.jpg!

���ؿͻ��˵ľ�����Ϣ��
*values* - jsonobject, key: "p"+$pdtid+"_"+$asi,  value �Ƕ�Ӧ���µ���

Ϊ֧�ֽ��ݼ۶��壬��Ҫ�ڿͻ��˳���ʵ�� c(x,y) ��ʽ�⣬����Ҫʵ��
<pre><code class="javascript">

  ��������������������۸���������ʽ���̶��ۻ���ݼ�
  @param p - �۸��壬2�ָ�ʽ: ��������һά���飬��������ʾ��һ�̶��۸�һά�����ʾ���ݼۣ�ÿ3��Ԫ�طֱ����������Ͷ�����������߶������۸�
   ���磺[0,400,50,401,1000,30,1001,99999,20] ��ʾ:
  0~400������400�����۸�Ϊ50�� 
  401~1000�����۸�Ϊ30��
  1001~���࣬�۸�Ϊ20
  @param c - ��ǰ��������Դ��c(x,y)�ķ���ֵ
  @return ���ս��

function amt(p, c) {
  return amt;   
 
}
</code></pre>

h3. ����۸���

����۸��Ǻ�̨�Ĵ���ģ�ͣ�����Ʒ�����ڲ�ͬ�۸�
> select asi asi, xxx value, xxx price from xxx where user_id=$uid and $pdtid=m_product_id
*asi* - ָ����Ʒ��m_attributesetinstance.id
*value* - �۸���������Ҫ��ʾ�ڵ�Ԫ���У�����۸�������ʾΪ��100~499��������ʾȫ�μ۸����ֵ����Сֵ
*price* - ��ͨ�̶��ۣ�ֱ����ʾ�۸�����۸���Ҫ���ظ�ʽ"[0,400,50,401,1000,30,1001,99999,20]"ÿ3��Ԫ�طֱ����������Ͷ�����������߶������۸�ʾ���ĺ����ǣ�
  0~400������400�����۸�Ϊ50�� 
  401~1000�����۸�Ϊ30��
  1001~���࣬�۸�Ϊ20



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













