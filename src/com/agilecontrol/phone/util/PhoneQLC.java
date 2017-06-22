package com.agilecontrol.phone.util;

import java.util.ArrayList;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.agilecontrol.nea.core.query.ColumnLink;
import com.agilecontrol.nea.core.schema.Column;
import com.agilecontrol.nea.core.schema.DisplaySetting;
import com.agilecontrol.nea.core.schema.Table;
import com.agilecontrol.nea.core.schema.TableManager;
import com.agilecontrol.nea.core.web.config.QueryListConfig;
import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.nea.util.Validator;
import com.agilecontrol.phone.PhoneConfig;

/**
 * 根据手机的特定需要构建默认的qlc
 * 
	 * 针对meta级别的config, 需要在selection部分的字段设置listtype, 
	 * 识别规则: 
	 * title: ak, 
	 * description: ak2,或者名称为description/name的字段, 但不可以是ak, 
	 * status: status 字段优选，再取第一个下拉类型的字段, 如果存在的话, 如果一个字段都没有，取第一个fk字段status字段
	 * pic：第一个image类型的字段 

 * @author yfzhu
 *
 */
public class PhoneQLC {
	
	private static Logger logger=LoggerFactory.getLogger(PhoneQLC.class);

	private int tableId;
	private Locale locale;
	
	private QueryListConfig qlc;
	
	public PhoneQLC(int tableId, Locale locale) throws Exception{
		this.tableId=tableId;
		this.locale=locale;
		init();
	}
	
	public QueryListConfig getQLC(){
		return qlc;
	}
	
	
	private void init() throws Exception{
    	
    	TableManager manager=TableManager.getInstance();
    	Table table=manager.getTable(tableId);
    	
    	qlc=new QueryListConfig();
    	qlc.setLocale(locale);
    	qlc.setId(-1); // not in db
    	qlc.setName("phone");
    	qlc.setTableId(tableId);
    	qlc.setDefault(false);
    	//conditions
    	ArrayList<ColumnLink> cls=new ArrayList<ColumnLink>();
    	ArrayList al=table.getIndexedColumns();
    	for(int i=0;i<al.size();i++){
    		Column col=(Column) al.get(i);
    		//if(col.getSecurityGrade()> sgrade) continue;
    		ColumnLink cl=new ColumnLink(new int[]{col.getId()});
    		cls.add(cl);
    	}
    	qlc.setConditions(cls);
    	//selections
    	cls=new ArrayList<ColumnLink>();
    	al=table.getColumns(new int[]{Column.MASK_QUERY_LIST},false, Integer.MAX_VALUE ); //default sgrade
    	for(int i=0;i<al.size();i++){
    		Column col=(Column) al.get(i);
    		//这里是和meta不一样的地方，直接获取reftable.ak的值
    		ColumnLink cl;
    		
    		if(col.getReferenceTable()!=null)cl=new ColumnLink(new int[]{col.getId(), col.getReferenceTable().getAlternateKey().getId()});
    		else cl=new ColumnLink(new int[]{col.getId()});
    		cls.add(cl);
    	}
    	updateSelectionWithListType(cls, table);
    	qlc.setSelections(cls);
    	
    	//orderbys
    	cls=new ArrayList<ColumnLink>();
    	JSONArray orderby=null;
    	if( table.getJSONProps()!=null) orderby=table.getJSONProps().optJSONArray("orderby");
    	if(orderby!=null){
    		for(int i=0;i<orderby.length();i++){
    			try{
        			JSONObject od= orderby.getJSONObject(i);
    				ColumnLink cl= new ColumnLink(table.getName()+"."+ od.getString("column"));
    				cl.setTag(od.optBoolean("asc",true));
    				cls.add(cl);
    			}catch(Throwable t){
    				logger.error("fail to load order by of "+ table.getName()+", pos="+i , t);
    				//throw new NDSException("order by column error:"+ od.optString("column"));
    			}
    			
    		}
        	
    		
    	}else if( table.getColumn("orderno")!=null){
    		ColumnLink cl= new ColumnLink(new int[]{table.getColumn("orderno").getId()});
    		cl.setTag(true);
    		cls.add(cl);
    	}
    	qlc.setOrderBys(cls);
    	
    	qlc.setRange(PhoneConfig.QUERY_RANGE_DEFAULT);
    	
    }
	 /**
	 * 针对meta级别的config, 需要在selection部分的字段设置listtype, 
	 * 识别规则: 
	 * title: ak, 
	 * description: ak2,或者名称为description/name的字段, 但不可以是ak, 
	 * status: status 字段优选，再取第一个下拉类型的字段, 如果存在的话, 如果一个字段都没有，取第一个fk字段status字段
	 * pic：第一个image类型的字段 
	     * 
	     */
	    private void updateSelectionWithListType(ArrayList<ColumnLink> cls, Table table) throws NDSException{
			ColumnLink listType_pic=null, listType_title=null, listType_status=null,fkClink=null;
			boolean foundStatus=false;
			Column ak= table.getAlternateKey();
			Column ak2=table.getAlternateKey2();
			
	    	for(ColumnLink clink: cls){
	    		Column col=clink.getLastColumn();
	    		Column firstCol=clink.getColumns()[0];
	    		if( col.equals(ak)){
	    			clink.setTag("title");
	    			listType_title= clink;
	    		}else if(firstCol.getName().equals("DESCRIPTION")|| firstCol.getName().equals("NAME") || col.equals(ak2) ){
	    			clink.setTag("description");
	    		}

	    		if(col.getDisplaySetting().getObjectType()==DisplaySetting.OBJ_IMAGE && listType_pic==null){
	    			listType_pic=clink;
	    			clink.setTag("image");
	    		}
	    		if( "STATUS".equals(col.getName())){
	    			//忽略listType_status
					clink.setTag("status");
					foundStatus=true;
				}else if(listType_status==null && col.getDisplaySetting().getObjectType()==DisplaySetting.OBJ_SELECT){
	    			listType_status=clink;
	    			clink.setTag("status");
	    			foundStatus=true;
	    		}
	    		if(fkClink==null && firstCol.getReferenceTable()!=null){
	    			fkClink= clink;
	    		}
	    		
	    	}
	    	if(listType_title==null){
	    		//将第一个description字段设置为title
	    		for(ColumnLink clink: cls){
	    			if("description".equals(clink.getTag())){
	    				clink.setTag("title");
	    			}
	    		}
	    	}
	    	if(!foundStatus&& fkClink!=null){
	    		fkClink.setTag("status");
	    	}
	    	//删除所有没有tag的clink
	    	for(int i=cls.size()-1;i>=0;i--){
	    		ColumnLink clink=cls.get(i);
	    		Object t=clink.getTag();
	    		if(t==null || Validator.isNull(t.toString()) ) cls.remove(i);
	    	}
	    }
}
