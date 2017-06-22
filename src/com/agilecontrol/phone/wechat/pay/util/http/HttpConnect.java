package com.agilecontrol.phone.wechat.pay.util.http;




import java.io.IOException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;


public class HttpConnect { 
	 private static HttpConnect httpConnect = new HttpConnect();
	   
	    public static HttpConnect getInstance() {
	        return httpConnect;
	    }
		MultiThreadedHttpConnectionManager connectionManager = new MultiThreadedHttpConnectionManager();
      
       
        public HttpResponse doGetStr(String url) {
    		String CONTENT_CHARSET = "GBK";
    		long time1 = System.currentTimeMillis();
    		HttpClient client = new HttpClient(connectionManager);  
            client.getHttpConnectionManager().getParams().setConnectionTimeout(30000);  
            client.getHttpConnectionManager().getParams().setSoTimeout(55000);
            client.getParams().setParameter(HttpMethodParams.HTTP_CONTENT_CHARSET, CONTENT_CHARSET); 
            HttpMethod method = new GetMethod(url);
            HttpResponse response = new HttpResponse();
            try {
				client.executeMethod(method);
				response.setStringResult(method.getResponseBodyAsString());
			} catch (HttpException e) {
				method.releaseConnection();
				return null;
			} catch (IOException e) {
				method.releaseConnection();
				return null;
			}finally{
				method.releaseConnection();
			}
			return response;
    }
}