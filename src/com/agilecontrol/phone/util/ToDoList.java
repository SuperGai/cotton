package com.agilecontrol.phone.util;

import java.sql.Connection;
import java.text.SimpleDateFormat;
import java.util.*;

import org.apache.velocity.VelocityContext;
import org.json.*;

import com.agilecontrol.nea.core.control.web.UserWebImpl;
import com.agilecontrol.nea.core.query.QueryUtils;
import com.agilecontrol.nea.core.velocity.DateUtil;
import com.agilecontrol.nea.core.velocity.VelocityUtils;
import com.agilecontrol.nea.util.Tools;
import com.agilecontrol.phone.PhoneController;

/**

  [ {text: "��ȥ", num:2, datebegin: 0, dateend:20150305},
 		{text: "����",num:4,datebegin: 20150306, dateend:20150306},
 		{text: "����",num:0,datebegin: 20150307, dateend:20150307},
 		{text: "����",num:0,datebegin: 20150308, dateend:20150308},
 		{text: "����",num:0,datebegin: 20150309, dateend:20150309},
 		{text: "����",num:0,datebegin: 20150310, dateend:20160310}
 	  ] text��ʾ�ı���num ��ʾ��������datebegin���ǵ��������

 * @author yfzhu
 *
 */
public class ToDoList {
	private UserWebImpl userWeb;
	private Connection conn;
	private VelocityContext vc;

	public ToDoList(UserWebImpl userWeb, Connection conn){
		this.userWeb=userWeb;
		this.conn=conn;
	}
	
	public JSONArray getList() throws Exception{
		
		vc=VelocityUtils.createContext();
		vc.put("conn",conn);
		vc.put("c", this);
		vc.put("userid", this.userWeb.getUserId());
		vc.put("username", this.userWeb.getUserName());
		
		
		Date today=new Date();
		SimpleDateFormat dateFm = new SimpleDateFormat("EEEE");//EEEE�������ڣ��硰�����ġ�
		
		JSONArray rows=new JSONArray();
		rows.put( calcTasks("��ȥ", -365, -1));
		rows.put( calcTasks("����", 0, 0));

		Date date= DateUtil.getInstance().offsetDateInDay(today, 1);
		String name=dateFm.format(date).replace("����", "��");
		rows.put( calcTasks(name, 1, 1));
		
		date= DateUtil.getInstance().offsetDateInDay(today, 2);
		name=dateFm.format(date).replace("����", "��");
		rows.put( calcTasks(name, 2, 2));
		
		date= DateUtil.getInstance().offsetDateInDay(today, 3);
		name=dateFm.format(date).replace("����", "��");
		rows.put( calcTasks(name, 3, 3));

		date= DateUtil.getInstance().offsetDateInDay(today, 4);
		name=dateFm.format(date).replace("����", "��");
		rows.put( calcTasks(name, 4, 4));

		rows.put( calcTasks("����", 5, 365));
		
		return rows;
	}
	
	
	
	/**
	 * ����ָ��ʱ����ڵ�������
	 * @param name ��ǩ
	 * @param dateBegin ��Խ����ƫ��
	 * @param dateEnd ��Խ����ƫ��
	 * @return {num, datebegin,dateend} 
	 * @throws Exception
	 */
	private JSONObject calcTasks(String name, int dateBegin, int dateEnd) throws Exception{
		Date today= new Date();
		Date date1= DateUtil.getInstance().offsetDateInDay(today, dateBegin);
		Date date2=DateUtil.getInstance().offsetDateInDay(today, dateEnd);
		SimpleDateFormat df=QueryUtils.dateNumberFormatter.get();
		int d1= Tools.getInt( df.format(date1),-1);
		int d2= Tools.getInt( df.format(date2),-1);
		
		vc.put("datebegin", d1);
		vc.put("dateend", d2);
		
		JSONArray data=PhoneController.getInstance().getDataArrayByADSQL("user_calendar_events_cnt", vc, conn, false);
		int cnt= data.getInt(0);
		
		JSONObject jo=new JSONObject();
		jo.put("datebegin", d1);
		jo.put("dateend", d2);
		jo.put("num", cnt);
		jo.put("name" ,name);
		
		return jo;
		
	}
}
