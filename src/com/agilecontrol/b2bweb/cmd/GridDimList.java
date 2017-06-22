package com.agilecontrol.b2bweb.cmd;

import java.util.HashMap;
import java.util.Iterator;

import org.json.JSONObject;

import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.nea.util.Validator;
import com.agilecontrol.phone.CmdResult;
import com.agilecontrol.phone.PhoneController;
import com.agilecontrol.phone.PhoneController.SQLWithParams;

/**
 * 
h1. ��ȡ��Ʒ�����嵥(���B2B���ӹ����������������dims����ʾ����)

h2. ����

> {cmd:"b2b.grid.dimlist", selected, actid, isfav, cat, pdtsearch,table,id}

*selected* - jsonobj, key �ǵ�ǰѡ�е�column, value ��column��Ӧ��id, ����: {dim3: 12, dim4:2}
*actid* - int �id, Ĭ��-1���Ƿ����ָ���Ļ�������Թ���
*isfav* - boolean �Ƿ������ղؼн������Թ��ˣ�Ĭ��false, true��ʱ�򲻶�ȡactid
*cat* - ��ǰ�û�ѡ���������ඨ�壬ÿ��cat�����n��dimid��ɣ���b2b:cat:tree_conf���幹��
*pdtsearch* �����������������

h2. ���

> [{column,desc, values}]

*column* - string����ǰ�ֶε�����, ��Ϊkey�ش�������
*desc* - string ��ǰ�ֶε���������ʾ�ڽ�����
*values* - [{k:v}] ���飬ÿ��Ԫ�ض���һ����һ���ԵĶ������Ե�key��ֵid��value��ֵ����ʾ����������

<pre>
[
   {column:"dim3", desc:"����", values:  [{1:"��"}, {2:"��"}, {3:"��"}]},
   {column:"dim4", desc:"�۸��", values:  [{1:"~100"}, {2:"200~1000"}, {3:"1000~"}]}
   {column:"dim14", desc:"Ʒ��", values:  []}
]   
</pre>  

h2. ����˵��

Ĭ��selected Ϊ�յ�ʱ��ϵͳ������ȫ���Ŀ�ѡ�����б�selected��Ӧ���ֶβ����ڽ��д���

���� ad_sql#b2b:dim_conf�����������ã��ṹ["dimname"]������:
> ["dim14", "dim3", "dim5"]
��ʾ��ʹ����Ʒ���dim14,dim3,dim5�ֶν���dim��ʾ��������е�key������selected�У������������������Ҫ��Ե�ǰselected
�����ݽ��й��ˣ�����Ҫ���cat��isfav��actid��
 * 
 * 
 * @author lsh
 */
public class GridDimList extends DimList {

	@Override
	protected HashMap<String, Object> reviseSearchCondition(JSONObject jo) throws Exception {
		
		for(Iterator it=jo.keys();it.hasNext();){
			String key=(String)it.next();
			Object v=jo.get(key);
			vc.put(key, v);
		}
		
		HashMap<String, Object> map=new HashMap<String, Object>();
		
		if(Validator.isNull(jo.optString("table"))) throw new NDSException("@b2bedit-config@"+"ad_sql#grid:"+jo.optString("table")+":meta"+"@b2bedit-found@");
		
		JSONObject gridConf=(JSONObject)PhoneController.getInstance().getValueFromADSQLAsJSON("grid:"+jo.optString("table")+":meta", null, conn);
		
		if(gridConf==null) throw new NDSException("@b2bedit-config@"+"ad_sql#grid:"+jo.optString("table")+":meta"+"@b2bedit-found@");
		
		String conf = gridConf.optString("dimfilter_sql");
		if(Validator.isNotNull(conf)){
			SQLWithParams swp = PhoneController.getInstance().parseADSQL(conf, vc, conn);
			map.put(swp.getSQL(),swp.getParams());
		}
		return map;
	}

	@Override
	public CmdResult execute(JSONObject jo) throws Exception {
		return super.execute(jo);
	}
}
