package rpt;

import org.json.JSONArray;
import org.json.JSONObject;

import com.agilecontrol.nea.util.Validator;
import com.agilecontrol.phone.CmdHandler;
import com.agilecontrol.phone.CmdResult;
/**

rpthome 用户进入报表中心的首页，由多个panel构成。针对不同的用户，可以定义不同的panel, 在用户表上需要定义字段 users.rpthome_id， 指向某个ad_sql记录，ad_sql中有rpthome的配置内容。如果未找到rpthome_id字段或未设置，将默认读取 "rpt:home" 的ad_sql 定义

h3. 后台定义

<pre>
 [ panelid ]
</pre>

* panelid - 即ad_sql.name 指定的panel， panel id 格式 "rpt:widget:panel:{{key}}"

h3. 客户端请求

<pre>
{cmd:"rpt.home"}
</pre>

返回举例:
<pre>
["rpt:widget:panel:sale", "rpt:widget:panel:office":"rpt:widget:panel:hr":"rpt:widget:panel:daysale"]
</pre>
客户端接下来发出 {cmd: "rpt.widget.panel.search",  id} 分别获取每一份panel的数据, 详细见[[Rptconfig#panel面板组件|panel面板组件]]

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
