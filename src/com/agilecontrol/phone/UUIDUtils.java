package com.agilecontrol.phone;



import java.util.UUID;

import org.apache.commons.codec.binary.Base64;




/**
 * 
 * Ŀ�꣺����UUID(Ĭ��32�ַ�����������22���ַ�
 * 
 * Java�е�UUID����RFC 4122�ı�׼������׼���ݰ�16���ƽ��б�ʾ��36���ַ������磺f81d4fae-7dec-11d0-a765-00a0c91e6bf6
 * 
 * ����Base64����ʹ�õ��ַ�������Сд��ĸ��26��������10�����֣��ͼӺš�+����б�ܡ�/����һ��64���ַ������Բ���Base64���ֵ�������Base64�൱��ʹ��64��������ʾ���ݣ���ͬ����λ���������Ҫ��16���Ʊ�ʾ��������ݡ�

����UUID��׼�����ܹ���128-bit���������ǾͿ��Զ����128-bit���½���Base64���롣

128-bit��UUID��Java�б�ʾΪ����long�����ݣ����Բ���java.util.UUID�е�getLeastSignificantBits��getMostSignificantBits�ֱ�������long��64-bit������ͨ��Base64ת��Ϳ��Ի��������Ҫ��UUID��

ͨ������UuidUtils.compressedUuid()�����Ϳ��Ի���ҵ���Ҫ��UUID�ַ�����22���ַ���128-bit��Base64ֻ��Ҫ22���ַ������磺BwcyZLfGTACTz9_JUxSnyA

�ڴ���Base64ʱ�������õ���apache��commons-codec���빤�߰�����Ϊ���ṩ�˼򵥵ı���ת�����������һ���encodeBase64URLSafeString����������URL��ȫ��ʽ����Base64���롣Ĭ�ϵ�Base64����+��/�ַ���������ֱ��������URL�н���ɻ��ҡ�URL��ȫ��ʽ������-�滻+��_�滻/����ȥ���˽���==���ǳ��ʺ�Webֱ�Ӵ��Ρ�


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