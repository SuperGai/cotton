package com.agilecontrol.phone;



import java.util.UUID;

import org.apache.commons.codec.binary.Base64;




/**
 * 
 * 目标：减少UUID(默认32字符），缩减到22个字符
 * 
 * Java中的UUID采用RFC 4122的标准，按标准数据按16进制进行表示（36个字符）。如：f81d4fae-7dec-11d0-a765-00a0c91e6bf6
 * 
 * 由于Base64编码使用的字符包括大小写字母各26个，加上10个数字，和加号“+”，斜杠“/”，一共64个字符。所以才有Base64名字的由来。Base64相当于使用64进制来表示数据，相同长度位数的情况下要比16进制表示更多的内容。

由于UUID标准数据总共是128-bit，所以我们就可以对这个128-bit重新进行Base64编码。

128-bit的UUID在Java中表示为两个long型数据，可以采用java.util.UUID中的getLeastSignificantBits与getMostSignificantBits分别获得两个long（64-bit）。再通过Base64转码就可以获得我们所要的UUID。

通过调用UuidUtils.compressedUuid()方法就可以获得我的需要的UUID字符串（22个字符，128-bit的Base64只需要22个字符）。如：BwcyZLfGTACTz9_JUxSnyA

在处理Base64时，这里用到了apache的commons-codec编码工具包，因为它提供了简单的编码转换方法。而且还有encodeBase64URLSafeString方法，采用URL安全方式生成Base64编码。默认的Base64含有+与/字符，如果这种编码出现在URL中将造成混乱。URL安全方式采用了-替换+，_替换/，并去掉了结束==。非常适合Web直接传参。


 * http://my.oschina.net/noahxiao/blog/132277
 *  
 * @author yfzhu
 *
 */
@Admin(mail="yfzhu@lifecycle.cn")
public class UUIDUtils {

	public static String uuid() {
        UUID uuid = UUID.randomUUID();
        return uuid.toString();
    }
 
    public static String compressedUuid() {
        UUID uuid = UUID.randomUUID();
        return compressedUUID(uuid);
    }
 
    protected static String compressedUUID(UUID uuid) {
        byte[] byUuid = new byte[16];
        long least = uuid.getLeastSignificantBits();
        long most = uuid.getMostSignificantBits();
        long2bytes(most, byUuid, 0);
        long2bytes(least, byUuid, 8);
        String compressUUID = Base64.encodeBase64URLSafeString(byUuid);
        return compressUUID;
    }
 
    protected static void long2bytes(long value, byte[] bytes, int offset) {
        for (int i = 7; i > -1; i--) {
            bytes[offset++] = (byte) ((value >> 8 * i) & 0xFF);
        }
    }
 
    public static String compress(String uuidString) {
        UUID uuid = UUID.fromString(uuidString);
        return compressedUUID(uuid);
    }
 
    public static String uncompress(String compressedUuid) {
        if (compressedUuid.length() != 22) {
            throw new IllegalArgumentException("Invalid uuid!");
        }
        byte[] byUuid = Base64.decodeBase64(compressedUuid + "==");
        long most = bytes2long(byUuid, 0);
        long least = bytes2long(byUuid, 8);
        UUID uuid = new UUID(most, least);
        return uuid.toString();
    }
 
    protected static long bytes2long(byte[] bytes, int offset) {
        long value = 0;
        for (int i = 7; i > -1; i--) {
            value |= (((long) bytes[offset++]) & 0xFF) << 8 * i;
        }
        return value;
    }
}