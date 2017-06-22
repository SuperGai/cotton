package com.agilecontrol.portal.cmd;

import org.json.JSONObject;

import com.agilecontrol.phone.Admin;
import com.agilecontrol.phone.CmdHandler;
import com.agilecontrol.phone.CmdResult;
import com.agilecontrol.phone.PhoneController;

/**
 * 输入
 * 
 * request 参数 name:用户名
 * 
 * 输出
 * 
 * { success: true/false }
 * 
 * 注册成功时，success 为 true，否则为 false
 * 
 * 
 * 
 * 加盟申请调用顺序，调用 p_cu_apply_insert 创建加盟申请主表得到 返回的主表id，调用 p_st_detail_insert 创建店铺基本情况子表，其 中p_cu_apply_id使用刚才得到的主表id，调用
 * p_dist_apply_insert 创 建商圈调查子表，其中 p_cu_apply_id 使用刚才得到的主表id， 最后调用 p_cu_apply_subsubmit 提交主表，参数为刚才得到的主表id。
 * 然后系统会自动生成一个用户，用户名为用户的邮箱，密码为123456
 * 
 */

/**
 * @author stao
 */
@Admin(mail="sun.tao@lifecycle.cn")
public class JoinRegister extends CmdHandler {
	@Override
	public CmdResult execute(JSONObject jo) throws Exception{
		JSONObject obj = new JSONObject().put( "success", false);
		
		
		
		//申请人信息
		conn.setAutoCommit( false);
		try{
			JSONObject main = jo.optJSONObject( "main");
			//是否已有店铺
			boolean shopCheck = jo.optBoolean( "shop_check", false);
			//店铺基本信息
			JSONObject shopmsg = jo.optJSONObject( "shopmsg");
			//申请人情况
			JSONObject personal = jo.optJSONObject( "personal");
			//投资管理
			JSONObject investment = jo.optJSONObject( "investment");
			//商圈调查
			JSONObject urvey = jo.optJSONObject( "urvey");
			
			//获取主表 id
			String adSql = "p_cu_apply_insert";
			
			// 申请人信息
			String name = main.optString( "truename");
			vc.put( "NAME", name);
			
			String mobile = main.optString( "telphone");
			mobile = mobile + "(str)";
			vc.put( "MOBIL", mobile);
			
			
			String email = main.optString( "email");
			vc.put( "EMAIL", email);
			
			String idno = main.optString( "ID_card");
			idno = idno + "(str)";
			//idno = idno + " ";
			vc.put( "IDNO", idno);
			
			String isstorer = main.optString( "hasShop");
			vc.put( "ISSTORER", isstorer);
			
			String opendate = main.optString( "shoptimeStr");
			vc.put( "OPENDATE", opendate);
			
			String compname = main.optString( "compname");
			vc.put( "COMPNAME", compname);
			
			String cAreaId = main.optString( "area");
			vc.put( "C_AREA_ID", cAreaId);
			
			
			String address = main.optString( "addr");
			vc.put( "ADDRESS", address);
			
			String ismale = main.optString( "gender");
			vc.put( "ISMALE", ismale);
			
			String age = main.optString( "age");
			vc.put( "AGE", age);
			
			String origin = main.optString( "province");
			vc.put( "ORIGIN", origin);
			
			String phone = main.optString( "othercontact");
			phone = phone + " ";
			vc.put( "PHONE", phone);
			
			String fax = main.optString( "fax");
			vc.put( "FAX", fax);
			
			String zip = main.optString( "post");
			vc.put( "ZIP", zip);
			
			//个人申请情况
			
			String ispara1 = personal.optString( "zmd");
			vc.put( "ISPARA1", ispara1);
			
			String ispara2 = personal.optString( "zxxx");
			vc.put( "ISPARA2", ispara2);
			
			String ispara3 = personal.optString( "hpzz");
			vc.put( "ISPARA3", ispara3);
			
			String ispara4 = personal.optString( "yz");
			vc.put( "ISPARA4", ispara4);
			
			String ispara5 = personal.optString( "shfw");
			vc.put( "ISPARA5", ispara5);
			
			String ispara6 = personal.optString( "jg");
			vc.put( "ISPARA6", ispara6);
			
			String ispara7 = personal.optString( "zytg");
			vc.put( "ISPARA7", ispara7);
			
			String markremark = personal.optString( "ndscdkf");
			vc.put( "MARKREMARK", markremark);
			logger.debug( "remark.........."+ markremark);
			
			//投资预算
			String bugGet = investment.optString( "tzys");
			vc.put( "BUDGET_AMT", bugGet);
			
			String cStandId = investment.optString( "mdbz");
			vc.put( "C_STAND_ID", cStandId);
			
			String ismang = investment.optString( "glfs");
			vc.put( "ISMANG", ismang);
			
			String mangcomp = investment.optString( "dzfwgdppmc1");
			vc.put( "MANGCOMP", mangcomp);
			
			String issell = investment.optString( "sfzm1");
			logger.debug( "issell.........."+ issell);
			vc.put( "ISSELL", issell);
			
			String post = investment.optString( "zw1");
			vc.put( "POST", post);
			
			String renaindate = investment.optString( "sj1Str");
			vc.put( "RENAINDATE", renaindate);
			
			String remainamt = investment.optString( "nyye1");
			vc.put( "REMAINAMT", remainamt);
			
			String mangcomp1 = investment.optString( "dzfwgdppmc2");
			vc.put( "MANGCOMP1", mangcomp1);
			
			String issell1 = investment.optString( "sfzm2");
			vc.put( "ISSELL1", issell1);
			
			String post1 = investment.optString( "zw2");
			vc.put( "POST1", post1);
			
			String renaindate1 = investment.optString( "sj2Str");
			vc.put( "RENAINDATE1", renaindate1);
			
			String remainamt1 = investment.optString( "nyye2");
			vc.put( "REMAINAMT1", remainamt1);
			
			String mangcomp2 = investment.optString( "dzfwgdppmc3");
			vc.put( "MANGCOMP2", mangcomp2);
			
			String issell2 = investment.optString( "sfzm3");
			vc.put( "ISSELL2", issell2);
			
			String post2 = investment.optString( "zw3");
			vc.put( "POST2", post2);
			
			String renaindate2 = investment.optString( "sj3Str");
			vc.put( "RENAINDATE2", renaindate2);
			
			String remainamt2 = investment.optString( "nyye3");
			vc.put( "REMAINAMT2", remainamt2);
			
			String idStr = PhoneController.getInstance().executeFunction( adSql, vc, conn);
			long id = Long.parseLong( idStr);
			obj.put( "applyid", id);
			
			//创建店铺基本情况
			if( true == shopCheck) {
				adSql = "p_st_detail_insert";
				
				vc.put( "P_CU_APPLY_ID", id);
				
				String attn1 = shopmsg.optString( "mainfloorgrids");
				vc.put( "ATTN1", attn1);
				
				String attn2 = shopmsg.optString( "spairearea");
				vc.put( "ATTN2", attn2);
				
				String attn3 = shopmsg.optString( "bicyclearea");
				vc.put( "ATTN3", attn3);
				
				String attn4 = shopmsg.optString( "floorage");
				vc.put( "ATTN4", attn4);
				
				String attn5 = shopmsg.optString( "manroadwidth");
				vc.put( "ATTN5", attn5);
				
				String attn6 = shopmsg.optString( "cararea");
				vc.put( "ATTN6", attn6);
				
				String attn7 = shopmsg.optString( "gatewidth");
				vc.put( "ATTN7", attn7);
				
				String attn8 = shopmsg.optString( "gateheight");
				vc.put( "ATTN8", attn8);
				
				String attn9 = shopmsg.optString( "dzg");
				vc.put( "ATTN9", attn9);
				
				String attn10 = shopmsg.optString( "dmk");
				vc.put( "ATTN10", attn10);
				
				String attn11 = shopmsg.optString( "dzc");
				vc.put( "ATTN11", attn11);
				
				String attn12 = shopmsg.optString( "wsj");
				vc.put( "ATTN12", attn12);
				
				String attn13 = shopmsg.optString( "xz");
				vc.put( "ATTN13", attn13);
				
				String attn14 = shopmsg.optString( "ds");
				vc.put( "ATTN14", attn14);
				
				String attn15 = shopmsg.optString( "gds");
				vc.put( "ATTN15", attn15);
				
				String attn16 = shopmsg.optString( "symj");
				vc.put( "ATTN16", attn16);
				
				String attn17 = shopmsg.optString( "dk");
				vc.put( "ATTN17", attn17);
				
				String attn18 = shopmsg.optString( "lzs");
				vc.put( "ATTN18", attn18);
				
				String attn19 = shopmsg.optString( "dpmj");
				vc.put( "ATTN19", attn19);
				
				int attn20 = 0;
				vc.put( "BUILDMARK", attn20);
				
				String attn21 = shopmsg.optString( "yjyxm");
				vc.put( "ATTN21", attn21);
				
				String attn22 = shopmsg.optString( "tzyy");
				vc.put( "ATTN22", attn22);
				
				String attn23 = shopmsg.optString( "zj");
				vc.put( "ATTN23", attn23);
				
				String attn24 = shopmsg.optString( "zq");
				vc.put( "ATTN24", attn24);
				
				String attn25 = shopmsg.optString( "synx");
				vc.put( "ATTN25", attn25);
				
				String attn26 = shopmsg.optString( "mzq");
				vc.put( "ATTN26", attn26);
				
				String attn27 = shopmsg.optString( "zzyd");
				vc.put( "ATTN27", attn27);
				
				String attn28 = shopmsg.optString( "zrf");
				vc.put( "ATTN28", attn28);
				
				String attn29 = shopmsg.optString( "sqyj");
				vc.put( "ATTN29", attn29);
				
				String attn30 = shopmsg.optString( "zjtfyd");
				vc.put( "ATTN30", attn30);
				
				String attn31 = shopmsg.optString( "qtyd");
				vc.put( "ATTN31", attn31);
				
				String attn32 = shopmsg.optString( "ygdyrs");
				vc.put( "ATTN32", attn32);
				
				String attn33 = shopmsg.optString( "xzq");
				vc.put( "ATTN33", attn33);
				
				String attn34 = shopmsg.optString( "yjjfsjStr");
				vc.put( "ATTN34", attn34);
				
				int contractmark = 0;
				vc.put( "CONTRACTMARK", contractmark);
				PhoneController.getInstance().executeFunction( adSql, vc, conn);
			}
			
			//创建商圈调查子表
			
			adSql = "p_dist_apply_insert";
			
			vc.put( "P_CU_APPLY_ID", id);
			
			String distname = urvey.optString( "sqmc", "");
			vc.put( "DISTNAME", distname);
			
			String distefc = urvey.optString( "sqyxl");
			vc.put( "DISTEFC", distefc);
			
			String disttype = urvey.optString( "sqlx");
			vc.put( "DISTTYPE", disttype);
			
			String transfertype = urvey.optString( "sqkl");
			vc.put( "TRANSFERTYPE", transfertype);
			
			String distfix = urvey.optString( "zydw");
			vc.put( "DISTFIX", distfix);
			
			String mallname = urvey.optString( "scmc");
			vc.put( "MALLNAME", mallname);
			
			String mallfix = urvey.optString( "scdw");
			vc.put( "MALLFIX", mallfix);
			
			String mallarea = urvey.optString( "sctl");
			vc.put( "MALLAREA", mallarea);
			
			String mallflow = urvey.optString( "scrkl");
			vc.put( "MALLFLOW", mallflow);
			
			String mallamt = urvey.optString( "scnyj");
			vc.put( "MALLAMT", mallamt);
			
			String mallage = urvey.optString( "klnl");
			vc.put( "MALLAGE", mallage);
			
			String cusrate = urvey.optString( "xfzgc");
			vc.put( "CUSRATE", cusrate);
			
			String opendate1 = urvey.optString( "kysj");
			vc.put( "OPENDATE", opendate1);
			
			String enterrate = urvey.optString( "zs");
			vc.put( "ENTERRATE", enterrate);
			
			String salerate = urvey.optString( "xszb");
			vc.put( "SALERATE", salerate);
			
			String transcont = urvey.optString( "jtqk");
			vc.put( "TRANSCONT", transcont);
			
			String cusatt = urvey.optString( "xfzgz");
			vc.put( "CUSATT", cusatt);
			
			String otherband = urvey.optString( "xldp_jp");
			vc.put( "OTHERBAND", otherband);
			
			String forsale = urvey.optString( "ygxs");
			vc.put( "FORSALE", forsale);
			PhoneController.getInstance().executeFunction( adSql, vc, conn);
			
			
			
			//提交表单
			adSql = "p_cu_apply_subsubmit";
			vc.put( "p_id", id);
			
			PhoneController.getInstance().executeFunction( adSql, vc, conn);
			//logger.debug( obj.toString());
			obj.put( "success", true);
			conn.commit();
			logger.debug( " commit............");
		}
		catch(Exception e){
			logger.debug( " rollback............");
			conn.rollback();
		}
		finally {
			
			conn.setAutoCommit( true);
		}
		
		return new CmdResult( obj);
	}
	
	/**
	 * Guest can execute this task, default to false
	 * @return
	 */
	public boolean allowGuest(){
		return true;
	}
	
}
