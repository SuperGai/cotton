package com.agilecontrol.phone;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * CmdHandler的运行结果
 * @author yfzhu
 *
 */
public class CmdResult {
	public final static CmdResult SUCCESS= new CmdResult();
	
	private final static JSONObject defaultBlank=new JSONObject();  
	/**
	 * 0 for ok, others for error
	 */
	private int code; 
	
	private String message;
	
	private Object object;
	
	/**
	 * code=0, message="@complete@"
	 */
	public CmdResult(){
		this.code=0;
		this.message="@complete@";
		this.object=null;
	}
	/**
	 * code=0, message="@complete@"
	 * @param obj
	 */
	public CmdResult(Object obj){
		this.code=0;
		this.message="@complete@";
		this.object=obj;
	}
	/**
	 * 
	 * @param code
	 * @param message
	 * @param obj any object attached to, will send to client as restResult in ValueHolder 
	 */
	public CmdResult(int code, String message, Object obj){
		this.code=code;
		this.message=message;
		this.object=obj;
	}
	
	public int getCode() {
		return code;
	}
	
	public void setCode(int code) {
		this.code = code;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public Object getObject() {
		
		return object==null? defaultBlank: object;
	}

	public void setObject(Object object) {
		this.object = object;
	}
	
	public JSONObject getRestResult() throws JSONException{
		JSONObject jo=new JSONObject();
		
		if(this.code < 0){
			jo.put("error", message);
		}else{
			jo.put("message", message);
		}
		
		jo.put("code", code);
		jo.put("result", object);
		return jo;
	}
	
	@Override
	public String toString() {
		String message = "{}";
		try {
			message = getRestResult().toString();
		} catch (Exception e) {
			// TODO: handle exception
		}
		return message;
	}
}
