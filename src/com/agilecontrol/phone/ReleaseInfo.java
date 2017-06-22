package com.agilecontrol.phone;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import java.util.Date;

import com.agilecontrol.nea.util.Tools;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import java.util.Date;

import com.agilecontrol.nea.util.Tools;

/**
 * Will read release information from package Manifest.MF
 * @author yfzhu
 *
 */
public class ReleaseInfo {

	private final static String codeName = "Snake";

	/**
	 * These is default value will get real data from package Manifest.MF
	 */
	private static String name = "Lifecycle PadStoreSvr";
	private static String versionDetail= "1.0.1.001";
	private static String version="1.0.1";
	private static String build = "001"; 

	private static int bigversion=2; //
	private static String releaseInfo ;
	private static String serverInfo ;
	
	static {
		//versionDetail
		Package pkg = ReleaseInfo.class.getPackage();
        if (pkg != null && "Lifecycle.cn".equals(pkg.getImplementationVendor()) &&(pkg.getImplementationVersion() != null)){
        	versionDetail = pkg.getImplementationVersion();
        	name=pkg.getImplementationTitle();
        }
        
        //build,//version
        int pdx=versionDetail.lastIndexOf(".");
        if(pdx>0) {
        	build=versionDetail.substring(pdx+1);
        	version= versionDetail.substring(0,pdx);
        }
        
        //big version
        pdx=versionDetail.indexOf(".");
		if(pdx>0)bigversion= Tools.getInt( versionDetail.substring(0, pdx),-1);
			
		
		releaseInfo =name + " " + version + " (" + codeName +" / Build " + build + ")" ;
		serverInfo=name + " / " + version;
		
	}
	
	/**
	 * Big version number in version string. version format:"2.1.0", then
	 * big version will be 2
	 * @return
	 */
	public static final int getBigVersion(){
		
		return bigversion;
	}
	/**
	 * Version+Build like '5.0.1.1039'
	 * @return
	 */
	public static final String getVersionBuild() {
		return versionDetail;
	}
	/**
	 * 5.0.1
	 * @return
	 */
	public static final String getVersion() {
		return version;
	}
	/**
	 * like: Snake
	 * @return
	 */
	public static final String getCodeName() {
		return codeName;
	}

	public static final int getBuildNumber() {
		return Integer.parseInt(build);
	}


	public static final String getReleaseInfo() {
		return releaseInfo;
	}

	public static final String getServerInfo() {
		return serverInfo;
	}

}