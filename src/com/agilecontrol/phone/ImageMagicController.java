package com.agilecontrol.phone;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Random;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.agilecontrol.b2b.qiniu.QiniuManager;
import com.agilecontrol.nea.core.util.ConfigValues;
import com.agilecontrol.nea.core.velocity.VelocityUtils;
import com.agilecontrol.nea.util.FileUtils;
import com.agilecontrol.nea.util.MD5Sum;
import com.agilecontrol.nea.util.NDSException;
import com.agilecontrol.nea.util.Tools;
import com.qiniu.common.QiniuException;
import com.qiniu.http.Response;
import com.qiniu.storage.UploadManager;

/**
 * 
montage "D:\erweima\xxy\xxy001.png" "D:\erweima\xxy\xxy002.png" "D:\erweima\xxy\xxy003.png" "D:\erweima\xxy\xxy004.png" "D:\erweima\xxy\xxy005.png" "D:\erweima\xxy\xxy006.png" "D:\erweima\xxy\xxy007.png" -tile 3x3 -geometry 48x48+1+1 "D:\erweima\xxy1.jpg"

convert -size 200x200 -background lightblue -font Georgia -fill red -gravity center label:"hello" "D:\erweima\hello.jpg" 
可以使用 –size控制大小, 
-gravity 控制位置,
-backgound控制背景颜色,
-fill 控制字体颜色
当要转换的text没有空格时可以不要双引号没有,有空格是双引号是必须的,
当没有 –size时 就没有必要使用 –gravity（center south north east west northeast southeast 等等）了，
它会自动调整位置

convert -size 200x200 -background #e79f58 -font "D:\erweima\simfang.ttf" -fill red -gravity center label:"中文" "D:\erweima\hello.jpg"
--文字头像

convert "D:\erweima\hello.jpg" "D:\erweima\sm.png" -gravity southeast -geometry +5+10 -composite "D:\erweima\3.jpg" 
--图片水印
convert "D:\erweima\hello.jpg" -background transparent -fill green -size 100x30 label:"@daidai" -gravity southeast -geometry +5+10 -composite "D:\erweima\水印.jpg"
--文字水印 

 * ImageMagic图片处理
 * @author chenmengqi
 *
 */
@Admin(mail="chen.mengqi@lifecycle.cn")
public class ImageMagicController {
	private static final Logger logger=LoggerFactory.getLogger(ImageMagicController.class);
    /**
     * 英文名称的图片中文字的Pointsize
     */
    private final static int[] POINTSIZES =new int[]{400,300,240,180,160,140,120,100,90,80,70,60,60,55,50};    
    private final static int POINTSIZE_CHINESE=200;//中文仅取2个字

	private static ImageMagicController instance;
	private static File actneaFile;
	private static String changeCd;
	
	private ImageMagicController(){};
	
	public static ImageMagicController getInstance(){
		if(instance == null){
			instance = new ImageMagicController();
			actneaFile=new File( ConfigValues.DIR_NEA_ROOT);
			//changeMagic="cd /d C:/ImageMagick-6.9.3-Q16 \n";
			changeCd="cd /d C:/ \n";
			//changeCd="cd / \n";
		}
		return instance;
	}
	
	/**
	 * 图片拼接处理，现主要用于处理活动图片生成。后期他用，可做修改
	 * @param imgs 				JSONArray图片数组 里面放的JSONObject 取mj_pic字段
	 * @param tableNameAndId 	要上传的图片名称
	 * @return
	 * @throws Exception
	 */
    public String imageMontage(JSONArray imgs,String tableNameAndId) throws Exception{
    	// File actneaFile=new File( ConfigValues.DIR_NEA_ROOT);
    	if(imgs.length()>0){
    		StringBuilder sb=new StringBuilder();
    	//	sb.append("C: \n");
    		sb.append(changeCd);
    		sb.append("montage ");
    		int num=0;
    		for (int i = 0; i < imgs.length(); i++) {
				JSONObject img= imgs.optJSONObject(i);
				String mj_pic=img.optString("mj_pic");
				if(!"".equals(mj_pic)){
					if(num==9){
						break;
					}
					
					String fmkdir=actneaFile.getAbsolutePath()+"/imagemagic/"+tableNameAndId.substring(0, tableNameAndId.lastIndexOf("."));
					//axel 下载
					downloadQiniuImg(fmkdir,mj_pic);
					
					sb.append(fmkdir+"/"+mj_pic.substring(mj_pic.lastIndexOf("/")+1)).append(" ");
			        
					num++;
				}
			}
    		
    		if(num>0){
    		   if(num>6){
    			   sb.append("-tile 3x3").append(" ").append("-geometry 50x50+0+0");
    		   }else if(num>4){
    			   sb.append("-tile 3x2").append(" ").append("-geometry 50x75+0+0");
    		   }else if(num>2){
    			   sb.append("-tile 2x2").append(" ").append("-geometry 75x75+0+0");
    		   }else if(num==2){
    			   sb.append("-tile 2x1").append(" ").append("-geometry 75x150+0+0");
    		   }else{
    			   sb.append("-tile 1x1").append(" ").append("-geometry 150x150+0+0");
    		   }
    		   
   		    String path = actneaFile.getAbsolutePath()+"/imagemagic/"+tableNameAndId;
   		 //   logger.debug("\n *****************path:"+path);
    		   sb.append(" ").append(path);
    		  
    		   //执行cmd task
    		   return runCmd(sb.toString(),path,tableNameAndId,true);
    		   
    		}
    	}
    	
    	return "";
    }
    /**
     * 获取指定文件或url图片的平均色，通过convert函数
     * convert http://img.1688mj.com/txtsanke.jpg -resize 1x1 txt:- 
# ImageMagick pixel enumeration: 1,1,255,srgb
0,0: (234,118,82)  #EA7652  srgb(234,118,82)
	取第二行#后面6位
     * @param fileOrURL
     * @return 返回的#后面的6位（含#）
     * @throws Exception
     */
    public String getAvgColorOfImage(String fileOrURL) throws Exception{
    	String command= PhoneConfig.IMAGE_TINT+ " "+ fileOrURL; //"curl -s "+ fileOrURL+" | convert - -resize 1x1 txt:-";
    	Runtime runtime = Runtime.getRuntime();
        Process process = runtime.exec(command);
        InputStream is = process.getInputStream();
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);

        String line;
        while ((line = br.readLine()) != null) {
        	logger.debug(line);
        	if(line.startsWith("0,0"))break;
        }
        logger.debug("last line of convert "+ command+": "+ line);
        String color="#FFFFFF";
        if(line.startsWith("0,0")){
        	int idx=line.indexOf('#');
        	if(idx>0)
        		color=line.substring(idx, idx+7);
        }
        return color;

    }
    /**
     * 获取指定文件或url图片的平均色，通过convert函数
     * convert http://img.1688mj.com/txtsanke.jpg -resize 1x1 txt:- 
# ImageMagick pixel enumeration: 1,1,255,srgb
0,0: (234,118,82)  #EA7652  srgb(234,118,82)
	取第二行#后面6位
     * @param fileOrURL
     * @return 返回的#后面的6位（含#）
     * @throws Exception
     */
//    public String getAvgColorOfImage(String fileOrURL) throws Exception{
//    	
//    	String args= fileOrURL+" -quiet -resize 1x1 txt:-";
//    	Process process = new ProcessBuilder("convert", fileOrURL, "-resize", "1x1", "txt:-").start();
//        InputStream is = process.getInputStream();
//        InputStreamReader isr = new InputStreamReader(is);
//        BufferedReader br = new BufferedReader(isr);
//        String line;
//        while ((line = br.readLine()) != null) {
//        	logger.debug(line);
//        	if(line.startsWith("0,0"))break;
//        }
//        logger.debug("last line of convert "+ args+": "+ line);
//        String color="#FFFFFF";
//        if(line.startsWith("0,0")){
//        	int idx=line.indexOf('#');
//        	if(idx>0)
//        		color=line.substring(idx, idx+7);
//        }
//        return color;
//
//    }
    /**
     * 判定文件url内容是否存储
     * @param url
     * @return
     * @throws Exception
     */
    private  boolean exists(String URLName){
        try {
//          HttpURLConnection.setFollowRedirects(false);
          // note : you may also need
          //        HttpURLConnection.setInstanceFollowRedirects(false)
          HttpURLConnection con =
             (HttpURLConnection) new URL(URLName).openConnection();
          con.setRequestMethod("HEAD");
          return (con.getResponseCode() == HttpURLConnection.HTTP_OK);
        }
        catch (Exception e) {
           logger.error("Fail to load "+ URLName, e);
           return false;
        }
      }

    
    /**
     * 判定是否含有中文，标志：最后2个字符是中文 Unicode:汉字的Unicode编码范围为
     * u4E00-u9FA5，uF900-uFA2D  
     * @param str
     * @return true if 最后2个字符任何一个是中文，false 如果都不是
     * @throws Exception
     */
    private boolean isChinese(String str) throws Exception{
    	if(str==null || str.length()==0) return false;
    	for(int i=0;i<2 && i<str.length();i++){
    		char oneChar=str.charAt(str.length()-1-i);
    		if((oneChar >= '\u4e00' && oneChar <= '\u9fa5')||(oneChar >= '\uf900' && oneChar <='\ufa2d')){
    			return true;
    		}
    	}
    	return false;
    }
    
    /**
     * 中文名称中，尾巴是没有多少含义的字符去掉，比如：公司，有限公司，有限责任公司，品牌，服饰，饰品等，去掉后，再取最后2个字符
     * @param name
     * @return
     * @throws Exception 
     */
    private String trimNonsense(String name) throws Exception{
    	JSONArray ja=(JSONArray) PhoneController.getInstance().getValueFromADSQLAsJSON("image:nonsense");
    	for(int i=0;i<ja.length();i++){
    		String str=ja.optString(i);
    		name=name.replaceAll(str, "");
    	}
    	int len=name.length();
    	if(len>2) name=name.substring(len-2);
    	if(name.length()==0) name="无名";
    	return name;
    }
    /**
     * 文字头像制作, 基础公式:

中文: 
convert -size 512x512 canvas:#4cd964 -fill white -font /opt/portal6/SimHei.ttf -pointsize 200 -gravity Center -draw 'text 0,0 "叶峰"' result.jpg

英文，视字符个数而定:
 
convert -size 512x512 canvas:#5856d6 -fill white -font /opt/portal6/Tahoma.ttf -pointsize 400 -gravity Center -draw 'text 0,0 "A"' result.jpg
convert -size 512x512 canvas:#5856d6 -fill white -font /opt/portal6/Tahoma.ttf -pointsize 300 -gravity Center -draw 'text 0,0 "Me"' result.jpg
convert -size 512x512 canvas:#5856d6 -fill white -font /opt/portal6/Tahoma.ttf -pointsize 240 -gravity Center -draw 'text 0,0 "Leo"' result.jpg
convert -size 512x512 canvas:#5856d6 -fill white -font /opt/portal6/Tahoma.ttf -pointsize 180 -gravity Center -draw 'text 0,0 "Macco"' result.jpg
convert -size 512x512 canvas:#5856d6 -fill white -font /opt/portal6/Tahoma.ttf -pointsize 160 -gravity Center -draw 'text 0,0 "Water"' result.jpg
convert -size 512x512 canvas:#5856d6 -fill white -font /opt/portal6/Tahoma.ttf -pointsize 140 -gravity Center -draw 'text 0,0 "Wabste"' result.jpg
convert -size 512x512 canvas:#5856d6 -fill white -font /opt/portal6/Tahoma.ttf -pointsize 120 -gravity Center -draw 'text 0,0 "Wabster"' result.jpg
convert -size 512x512 canvas:#5856d6 -fill white -font /opt/portal6/Tahoma.ttf -pointsize 100 -gravity Center -draw 'text 0,0 "Nicohlas"' result.jpg
convert -size 512x512 canvas:#5856d6 -fill white -font /opt/portal6/Tahoma.ttf -pointsize 90 -gravity Center -draw 'text 0,0 "Facebooks"' result.jpg
convert -size 512x512 canvas:#5856d6 -fill white -font /opt/portal6/Tahoma.ttf -pointsize 80 -gravity Center -draw 'text 0,0 "Mamroswfts"' result.jpg
convert -size 512x512 canvas:#5856d6 -fill white -font /opt/portal6/Tahoma.ttf -pointsize 70 -gravity Center -draw 'text 0,0 "Mamroswftsm"' result.jpg
convert -size 512x512 canvas:#5856d6 -fill white -font /opt/portal6/Tahoma.ttf -pointsize 60 -gravity Center -draw 'text 0,0 "Mamroswftsms"' result.jpg
convert -size 512x512 canvas:#5856d6 -fill white -font /opt/portal6/Tahoma.ttf -pointsize 60 -gravity Center -draw 'text 0,0 "Mamroswftsmsm"' result.jpg
convert -size 512x512 canvas:#5856d6 -fill white -font /opt/portal6/Tahoma.ttf -pointsize 55 -gravity Center -draw 'text 0,0 "Mamroswftsmsmw"' result.jpg
convert -size 512x512 canvas:#5856d6 -fill white -font /opt/portal6/Tahoma.ttf -pointsize 50 -gravity Center -draw 'text 0,0 "Mamroswftsmsmww"' result.jpg

     * @param name 文字, 取hashcode/ad_sql#image:bgcolors .length 定位bgcolor, 进入canvas, 然后取 length 映射 pointsize，图片名称按"txthead"+name.md5求输出文件名
     * @return
     * @throws Exception
     */
    public String getHeadImageByText(String name) throws Exception{
    	if(name==null|| name.length()==0) return name;
    	int pointSize=0;
    	int len=name.length();
    	String str=null;
    	String font=null;
    	if(this.isChinese(name)){
    		pointSize=POINTSIZE_CHINESE;
    		if(len<=2) str= name;
    		else str=trimNonsense(name);//
    		font="SimHei.ttf";
    	}else{
    		if(len>=16) {
    			pointSize=POINTSIZES[POINTSIZES.length-1];
    			str=name.substring(len-15);
    		}else {
    			pointSize=POINTSIZES[len-1];
    			str=name;
    		}
    		font="Tahoma.ttf";
    	}
    	logger.debug("name="+ str);
    	String imageFileName="txt"+MD5Sum.toCheckSumStr(str)+".jpg";
    	String imageURL="http://img.1688mj.com/"+ imageFileName;
    	String imageFilePath=ConfigValues.DIR_NEA_ROOT+"/tmp/"+imageFileName;
    	if(exists(imageURL)){
    		return imageURL;
    	}
    	
    	JSONArray colors=(JSONArray)PhoneController.getInstance().getValueFromADSQLAsJSON("image:bgcolors");
    	int hash= Math.abs( str.hashCode())% colors.length();
    	String color= colors.getString(hash);
    	
    	//"convert -size 512x512 canvas:$bgcolor -fill white -font /opt/portal6/$font -pointsize $pointsize -gravity Center -draw 'text 0,0 \"$txt\"' $output"
    	//这里有个processcall的问题：draw后面的参数无法传递给runtime，通过生成sh 命令来解决
    	//http://shchekoldin.com/2009/10/13/imagemagick-convert-problem/
    	VelocityContext vc = VelocityUtils.createContext();
		StringWriter output = new StringWriter();
		vc.put("bgcolor", color);
		vc.put("font", font);
		vc.put("pointsize", pointSize);
		vc.put("txt", str);
		vc.put("output", imageFilePath);
		
		Velocity.evaluate(vc, output, VelocityUtils.class.getName(), PhoneConfig.IMAGE_TXT_HEAD_CONVERT);
		String cmd=output.toString();

		CommandExecuter cmdrunner = new CommandExecuter(null);
		cmdrunner.run(cmd);
		
		File nf=new File(imageFilePath);
		if(nf.exists() && nf.length()>0){
			//ok
			String token=QiniuManager.getInstance().uploadToken(imageFileName, PhoneConfig.QINIU_UPLOAD_EXPIRE );
			Response res=null;
			try{
				long startTime=System.currentTimeMillis();
				res= new UploadManager().put(imageFilePath, imageFileName, token);
				logger.debug("send "+ imageFilePath+" with response: "+ res.bodyString()+", key="+imageFileName+", time="+ (System.currentTimeMillis()-startTime)/1000.0+" seconds");
				
			}catch(QiniuException ex){
				Response r = ex.response;
		        try {
			         
			        logger.error("Fail to upload to qiniu using "+ imageFilePath+": "+ r.bodyString());
		        } catch (QiniuException e1) {
		        	logger.error("Fail to read error message from qiniu", e1);
		        }
			}
			if(res==null || !res.isOK()){
				throw new NDSException("头像生成失败，请稍后再试");
			}
//			if(!exists(imageURL)){
//				logger.error("Fail to upload to qiniu using "+ imageFilePath+": not exists "+ imageURL );
//				throw new NDSException("头像生成失败，请稍后再试");
//			}
			nf.delete();
	 	      
		}else{
			logger.error("Fail to create "+ imageFilePath+", file not found");
			throw new NDSException("头像生成失败，请稍后再试");
		}
		
		return imageURL;
		
    }
    
    /**
     * 图片水印
     * @param from  图片
     * @param logo  水印图片
     * @param tableNameAndId 图片名称
     * @throws Exception
     */
    public String waterMarImgkConvert(String from,String logo,String tableNameAndId) throws Exception{
    	String fmkdir=actneaFile.getAbsolutePath()+"/imagemagic/"+tableNameAndId.substring(0, tableNameAndId.lastIndexOf("."));
		downloadQiniuImg(fmkdir,from);
		
    	StringBuilder sb=new StringBuilder();
    	sb.append(changeCd);
		sb.append("   convert").append(" ");
		
		String  f=fmkdir+"/"+from.substring(from.lastIndexOf("/")+1);
		sb.append(f).append(" ");
		sb.append("\""+logo+"\"").append(" ");
		sb.append("-gravity southeast").append(" ");
		sb.append("-geometry +5+10").append(" ");
		sb.append("-composite").append(" ");
		
		//下载的路径地址
	    //sb.append("\"D:/erweima/watermark.jpg\"");
		String path = actneaFile.getAbsolutePath()+"/imagemagic/"+tableNameAndId;
		sb.append(path);
		
	//	System.out.println(sb.toString());
		return runCmd(sb.toString(),path,tableNameAndId,true);
    }
    
    /**
     * 文字水印
     * @param fromUrl         图片
     * @param name            水印文字
     * @param tableNameAndId  用表名+当前id生成的图片名称
     * @return
     * @throws Exception
     */
	public String  waterMarTxtConvert(String fromUrl,String name,String tableNameAndId) throws Exception{
		/*方法3：可背景完全透明
		mogrify -font msyh.ttf -pointsize 24 -fill black -weight bolder -gravity southeast -annotate +20+20 @"t.txt" src.jpg*/
		
		
		String font = actneaFile.getAbsolutePath()+"/imagemagic/simfang.ttf";
		
		String fmkdir=actneaFile.getAbsolutePath()+"/imagemagic/"+tableNameAndId.substring(0, tableNameAndId.lastIndexOf("."));
		downloadQiniuImg(fmkdir,fromUrl);
		
		StringBuilder sb=new StringBuilder();
	//	sb.append("cd /d C:\\ImageMagick-6.9.3-Q16\n");
		sb.append(changeCd);
		sb.append("   convert").append(" ");
		String  f=fmkdir+"/"+fromUrl.substring(fromUrl.lastIndexOf("/")+1);
		sb.append(f).append(" ");
		sb.append("-background transparent").append(" ");
		sb.append("-fill green").append(" ");
		sb.append("-size 100x30").append(" ");
		sb.append("-font ").append(font).append(" ");
		sb.append("label:").append("\""+name+"\"").append(" ");
		sb.append("-gravity southeast").append(" ");
		//水印的下边缘距原始图片10像素、右边缘距原始图片5像素
		sb.append("-geometry +5+10").append(" ");
		sb.append("-composite").append(" ");
	
		String path = actneaFile.getAbsolutePath()+"/imagemagic/"+tableNameAndId;
		sb.append(path);
	//	sb.append("\"D:/erweima/水印文字.jpg\"");
		
	//	System.out.println(sb.toString());
		return runCmd(sb.toString(),path,tableNameAndId,false);
	}
    
	public static void main(String[] args) throws Exception{
	/*	JSONArray ja=new JSONArray();
		for (int i = 0; i <2; i++) {
			JSONObject jo=new JSONObject();
			jo.put("mj_pic", "\"http://7xrrtv.com1.z0.glb.clouddn.com/3E4CADE4-D378-4733-94FB-24B6C1A076EB.jpg\"");
			ja.put(jo);
		}
		imageMontage(ja);*/
		
		/*String xx=txtHeaderImgConvert("麦+","buding365");
		System.out.println("--------------"+xx);*/
		//waterMarImgkConvert("D:/erweima/hello.jpg","D:/erweima/sm.png");
		
		//waterMarTxtConvert("D:/erweima/xxy1.jpg", "@daidai");
	}
	
	/**
	 * 执行cmd
	 * @param content	组装内容
	 * @param filename	文件路径
	 * @param name		图片名称
	 * @param isDelFile	是否删除下载的文件
	 * @return
	 * @throws Exception
	 */
	public static String runCmd(String content,String filename,String name,boolean isDelFile) throws Exception{
 	    String runFile= actneaFile.getAbsolutePath()+"/imagemagic/imageMagic.cmd";
		Tools.writeFile(runFile, content, "GBK");
		CommandExecuter cmdrunner = new CommandExecuter(null);
		//cmdrunner.setPrintStream(System.out);
		cmdrunner.run(runFile);
		
		//七牛图片处理
		return upload(filename,name,isDelFile);
	}
	
	/**
	 * 执行axel-cmd 下载地址图片
	 * @param content
	 * @throws Exception
	 */
	public void runAxelCmd(String content) throws Exception{
		
		String runFile=actneaFile.getAbsolutePath()+"/imagemagic/axel.cmd";
 	  //  String runFile=ConfigValues.DIR_NEA_ROOT+"/imagemagic/axel.cmd";
		Tools.writeFile(runFile, content, "GBK");
		CommandExecuter cmdrunner = new CommandExecuter(null);
		//cmdrunner.setPrintStream(System.out);
		cmdrunner.run(runFile);
	}
	
	/**
	 * 构造axel 下载图片路径
	 * @param tableNameAndId
	 * @param pic
	 * @throws Exception
	 */
	public void downloadQiniuImg(String fmkdir,String pic)  throws Exception{
		StringBuilder axel=new StringBuilder();
		//logger.debug("\n ********************changeCd:"+changeCd);
		axel.append(changeCd);
		
	//	String fmkdir=ConfigValues.DIR_NEA_ROOT+"/tmp/"+tableNameAndId.substring(0, tableNameAndId.lastIndexOf("."));
	//	String fmkdir="D:/erweima/"+tableNameAndId.substring(0, tableNameAndId.lastIndexOf("."));
		File f=new File(fmkdir);
		if(!f.exists()){
			f.mkdirs();
		}
		axel.append("axel -n 10").append(" ").append("-o "+fmkdir).append(" ").append(pic);
		runAxelCmd(axel.toString());
	}
	
	/**
	 * 上传图片到七牛
	 * @param filename
	 * @return
	 * @throws Exception
	 */
	public static String upload(String filename,String name,boolean isDelFile) throws Exception{
		JSONObject ret=new JSONObject();
	    try {
		/*	 Auth auth=Auth.create("_H9szGv4IddCaVBTOk7qrRcXzNenKRf2ys5jgvrz","oXwKJCXg66SSft6IjVXCiCBqAC0OjDIHSEPRhtG2");
			 String token=auth.uploadToken("devspace",name,60*60,null);*/
	    	
	    /*	CmdHandler cmdHandler=PhoneController.getInstance().createCmdHandler("UploadToken");
	    	JSONObject jo = new JSONObject();
	    	jo.put("file", name);
	    	CmdResult result=cmdHandler.execute(jo);
	    	JSONObject rt=(JSONObject)result.getObject();
			String token=rt.optString("token");*/
	    	
			String token=QiniuManager.getInstance().uploadToken(name, PhoneConfig.QINIU_UPLOAD_EXPIRE);
		    File ss=new File(filename);
			   while(true){
			       if(ss.exists()){
			 	      //调用put方法上传
			 	      Response res = new UploadManager().put(filename, name, token);
			 	      //打印返回的信息
			 	      ret=new JSONObject(res.bodyString());
			 	      //System.out.println(ret); 
			 	      if(isDelFile){
			 	    	  ss.delete();
			 	      }
			 	      FileUtils.delete(ConfigValues.DIR_NEA_ROOT+"/imagemagic/"+ name.substring(0, name.lastIndexOf(".")));
			          break;
			       }else{
			         Thread.sleep(1000);
			       }
			   }
	      }catch (QiniuException e) {
	          Response r = e.response;
	       /*
	         // 请求失败时打印的异常的信息
	            System.out.println(r.toString());
	          try {
	              //响应的文本信息
	            System.out.println(r.bodyString());
	          } catch (QiniuException e1) {
	              //ignore
	          }
	       */
	          throw new NDSException("upload qiniu Error: "+r.toString());
	      } 
	    return ret.optString("key");
	}
	
}
