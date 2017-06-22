package rpt;

import org.json.JSONArray;
import org.json.JSONObject;

import com.agilecontrol.nea.util.Validator;
import com.agilecontrol.phone.CmdHandler;
import com.agilecontrol.phone.CmdResult;
/**

rpthome �û����뱨�����ĵ���ҳ���ɶ��panel���ɡ���Բ�ͬ���û������Զ��岻ͬ��panel, ���û�������Ҫ�����ֶ� users.rpthome_id�� ָ��ĳ��ad_sql��¼��ad_sql����rpthome���������ݡ����δ�ҵ�rpthome_id�ֶλ�δ���ã���Ĭ�϶�ȡ "rpt:home" ��ad_sql ����

h3. ��̨����

<pre>
 [ panelid ]
</pre>

* panelid - ��ad_sql.name ָ����panel�� panel id ��ʽ "rpt:widget:panel:{{key}}"

h3. �ͻ�������

<pre>
{cmd:"rpt.home"}
</pre>

���ؾ���:
<pre>
["rpt:widget:panel:sale", "rpt:widget:panel:office":"rpt:widget:panel:hr":"rpt:widget:panel:daysale"]
</pre>
�ͻ��˽��������� {cmd: "rpt.widget.panel.search",  id} �ֱ��ȡÿһ��panel������, ��ϸ��[[Rptconfig#panel������|panel������]]

 * 
 * @author yfzhu
 *
 */
public class home extends RptCmdHandler {

	@Override
	public CmdResult execute(JSONObject jo) throws Exception {
		String value=engine.doQueryString("select s.value from users u, ad_sql s where u.id=? and s.id=u.rpthome_id", new Object[]{this.usr.getId()}, conn);
		if(Validator.isNull(value))
			value=engine.doQueryString("select s.value from ad_sql s where s.name='rpt:home'", new Object[]{}, conn);
		JSONArray ar;
		if(value==null) ar=new JSONArray();
		else ar=new JSONArray(value);
		return new CmdResult(ar);
	}

}
