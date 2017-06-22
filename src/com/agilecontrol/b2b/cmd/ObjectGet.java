package com.agilecontrol.b2b.cmd;

import org.apache.velocity.VelocityContext;
import org.json.*;

import java.util.*;

import com.agilecontrol.b2b.schema.*;
import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.nea.util.Tools;
import com.agilecontrol.nea.util.Validator;
import com.agilecontrol.phone.CmdHandler;
import com.agilecontrol.phone.CmdResult;
import com.agilecontrol.phone.PhoneController;
import com.agilecontrol.phone.PhoneUtils;

/**
 * 
 获得指定对象，支持自定义符合类型:
 table: String 所在表，例如: usr, com
 id: long 对象id
 items: boolean | string, 
 	false(默认), 即不含明细表
 	true: 含所有的明细表，按照meta的定义来
 	"table1,table2" 指定的明细表，用逗号分隔 
 
 
  将读取配置文件 ad_sql#table:<table>:meta ，格式
  Table
  {
  	name, dk:[column1, column2], perm
  	cols:[{Column}]
  }
  name - string 表名
  dk - 可以是string, 或 [String],  表示当前表作为外键的时候，需要连带显示的字段
  perm - [String] 权限，“com” 表示只有com内的员工可见，要求当前user的comid=当前记录的com_id字段, "admin"表示当前用户需要是管理员身份
  
  Column
  {
  	name, fktable, type, edit
  }
  name - string 字段的数据库名
  fktable - String fk表的名称，将fktable is not null 的字段对象化，并直接塞到字段名对应的属性中
  type - String "string","long", "time", "datenumber"
  edit - boolean 是否允许修改，用于insert/update, 默认为true
  null - boolean 是否允许为空，默认true
  
  返回:
  {key: value}
  key: 当前表的普通字段名， value就是数据库值
  如果是当前表的fk类型的字段，value 是fk字段对应表的所有显示键组成的object，例如: {id,name,img}
  如果定义了items表显示，
  如果当前item是1:1 关联，将以对象形式显示value，name 为refby定义的name
  如果item是1:m关联，将以array形式显示value，结构:[{}] 元素对象是指定表的列表显示字段的内容
  
  
  
  
  {cnt:2000, start:0, range: 20, cachekey:"list:$table:$uid:$uuid", data:[{}]} 
  其中cnt最大2000，不表示查询结果没数据，只是到此为止
  start 当前起始的idx, range: 当前给的数据行数，cachekey: 用于获取当前查询其他行的索引, cachekey对应的查询定义在查询停止使用的30分钟后失效，客户端将报错，需要重新构造查询
  data: 是指定表的列表显示字段的内容
  分页查询的下一个页的请求方法是: 
  {cmd:GetList, cachekey:"list:$table:$uid:$uuid", start: xxx, range: 20 }， 系统将通过cachekey验证用户和获得所在表
  返回:
  {cnt:2000, start:0, range: 20, cachekey:"list:$table:$uid:$uuid", data:[{}]}  
  
  关于redis缓存："list:$table:$uid:$uuid" 是 list of id
  
 * @author yfzhu
 *
 */
public class ObjectGet extends CmdHandler {
	
	/**
	 * redis完成缓存更新后的处理
	 * @param table
	 * @param retObj 正常处理完的，将返回客户端的对象，可以重构, 这里已经是装配完的对象
	 * @throws Exception
	 */
	protected void postAction(Table table, JSONObject retObj) throws Exception{
		
	}
	

	/**
	 * 单层简单对象获取后，方便开发校验权限
	 * @param table
	 * @param fetchObj 可以理解就是当前记录展开 
	 * @throws Exception
	 */
	protected void checkPermission(Table table, JSONObject fetchObj) throws Exception{
		
	}
	
	@Override
	public CmdResult execute(JSONObject jo) throws Exception {

		Table table=findTable(jo,"Get");
		String tableName=table.getName();

		long objectId=getLong(jo,"id");
		JSONObject obj=null;
		Object items=jo.opt("items");
		if(items!=null){
			if(items instanceof Boolean){
				obj=fetchObject(objectId, tableName, (Boolean)items);
			}else if(items instanceof JSONArray){
				String[] its=new String[((JSONArray)items).length()];
				for(int i=0;i<its.length;i++) its[i]=((JSONArray) items).getString(i);
				obj=fetchObject(objectId, tableName,its);
			}else{
				String[] its=items.toString().split(",");
				obj=fetchObject(objectId, tableName,its);
			}
		}else{
			obj=fetchObject(objectId, tableName,null);
		}
		//read from ad_sql#table:$table:assemble
		JSONArray conf=(JSONArray)PhoneController.getInstance().getValueFromADSQLAsJSON("table:"+ tableName+":assemble:obj",new JSONArray(), conn);
		this.assembleObject(obj, conf, manager.getTable(tableName));
		
		checkPermission(table, obj);
		
		postAction(table, obj);
		CmdResult res=new CmdResult(reviseColumnsOfJSONTypeValue(obj, table) );
		return res;
	}
	
	
}
