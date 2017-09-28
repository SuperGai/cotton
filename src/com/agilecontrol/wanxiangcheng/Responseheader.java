/**
 * Responseheader.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.agilecontrol.wanxiangcheng;

public class Responseheader  implements java.io.Serializable {
    private short responsecode;

    private java.lang.String responsemessage;

    private int pagerecords;

    private int pageno;

    private int updatecount;

    private int maxrecords;

    private int maxpageno;

    private java.lang.String messagetype;

    private java.lang.String messageid;

    private java.lang.String version;

    public Responseheader() {
    }

    public Responseheader(
           short responsecode,
           java.lang.String responsemessage,
           int pagerecords,
           int pageno,
           int updatecount,
           int maxrecords,
           int maxpageno,
           java.lang.String messagetype,
           java.lang.String messageid,
           java.lang.String version) {
           this.responsecode = responsecode;
           this.responsemessage = responsemessage;
           this.pagerecords = pagerecords;
           this.pageno = pageno;
           this.updatecount = updatecount;
           this.maxrecords = maxrecords;
           this.maxpageno = maxpageno;
           this.messagetype = messagetype;
           this.messageid = messageid;
           this.version = version;
    }


    /**
     * Gets the responsecode value for this Responseheader.
     * 
     * @return responsecode
     */
    public short getResponsecode() {
        return responsecode;
    }


    /**
     * Sets the responsecode value for this Responseheader.
     * 
     * @param responsecode
     */
    public void setResponsecode(short responsecode) {
        this.responsecode = responsecode;
    }


    /**
     * Gets the responsemessage value for this Responseheader.
     * 
     * @return responsemessage
     */
    public java.lang.String getResponsemessage() {
        return responsemessage;
    }


    /**
     * Sets the responsemessage value for this Responseheader.
     * 
     * @param responsemessage
     */
    public void setResponsemessage(java.lang.String responsemessage) {
        this.responsemessage = responsemessage;
    }


    /**
     * Gets the pagerecords value for this Responseheader.
     * 
     * @return pagerecords
     */
    public int getPagerecords() {
        return pagerecords;
    }


    /**
     * Sets the pagerecords value for this Responseheader.
     * 
     * @param pagerecords
     */
    public void setPagerecords(int pagerecords) {
        this.pagerecords = pagerecords;
    }


    /**
     * Gets the pageno value for this Responseheader.
     * 
     * @return pageno
     */
    public int getPageno() {
        return pageno;
    }


    /**
     * Sets the pageno value for this Responseheader.
     * 
     * @param pageno
     */
    public void setPageno(int pageno) {
        this.pageno = pageno;
    }


    /**
     * Gets the updatecount value for this Responseheader.
     * 
     * @return updatecount
     */
    public int getUpdatecount() {
        return updatecount;
    }


    /**
     * Sets the updatecount value for this Responseheader.
     * 
     * @param updatecount
     */
    public void setUpdatecount(int updatecount) {
        this.updatecount = updatecount;
    }


    /**
     * Gets the maxrecords value for this Responseheader.
     * 
     * @return maxrecords
     */
    public int getMaxrecords() {
        return maxrecords;
    }


    /**
     * Sets the maxrecords value for this Responseheader.
     * 
     * @param maxrecords
     */
    public void setMaxrecords(int maxrecords) {
        this.maxrecords = maxrecords;
    }


    /**
     * Gets the maxpageno value for this Responseheader.
     * 
     * @return maxpageno
     */
    public int getMaxpageno() {
        return maxpageno;
    }


    /**
     * Sets the maxpageno value for this Responseheader.
     * 
     * @param maxpageno
     */
    public void setMaxpageno(int maxpageno) {
        this.maxpageno = maxpageno;
    }


    /**
     * Gets the messagetype value for this Responseheader.
     * 
     * @return messagetype
     */
    public java.lang.String getMessagetype() {
        return messagetype;
    }


    /**
     * Sets the messagetype value for this Responseheader.
     * 
     * @param messagetype
     */
    public void setMessagetype(java.lang.String messagetype) {
        this.messagetype = messagetype;
    }


    /**
     * Gets the messageid value for this Responseheader.
     * 
     * @return messageid
     */
    public java.lang.String getMessageid() {
        return messageid;
    }


    /**
     * Sets the messageid value for this Responseheader.
     * 
     * @param messageid
     */
    public void setMessageid(java.lang.String messageid) {
        this.messageid = messageid;
    }


    /**
     * Gets the version value for this Responseheader.
     * 
     * @return version
     */
    public java.lang.String getVersion() {
        return version;
    }


    /**
     * Sets the version value for this Responseheader.
     * 
     * @param version
     */
    public void setVersion(java.lang.String version) {
        this.version = version;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof Responseheader)) return false;
        Responseheader other = (Responseheader) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            this.responsecode == other.getResponsecode() &&
            ((this.responsemessage==null && other.getResponsemessage()==null) || 
             (this.responsemessage!=null &&
              this.responsemessage.equals(other.getResponsemessage()))) &&
            this.pagerecords == other.getPagerecords() &&
            this.pageno == other.getPageno() &&
            this.updatecount == other.getUpdatecount() &&
            this.maxrecords == other.getMaxrecords() &&
            this.maxpageno == other.getMaxpageno() &&
            ((this.messagetype==null && other.getMessagetype()==null) || 
             (this.messagetype!=null &&
              this.messagetype.equals(other.getMessagetype()))) &&
            ((this.messageid==null && other.getMessageid()==null) || 
             (this.messageid!=null &&
              this.messageid.equals(other.getMessageid()))) &&
            ((this.version==null && other.getVersion()==null) || 
             (this.version!=null &&
              this.version.equals(other.getVersion())));
        __equalsCalc = null;
        return _equals;
    }

    private boolean __hashCodeCalc = false;
    public synchronized int hashCode() {
        if (__hashCodeCalc) {
            return 0;
        }
        __hashCodeCalc = true;
        int _hashCode = 1;
        _hashCode += getResponsecode();
        if (getResponsemessage() != null) {
            _hashCode += getResponsemessage().hashCode();
        }
        _hashCode += getPagerecords();
        _hashCode += getPageno();
        _hashCode += getUpdatecount();
        _hashCode += getMaxrecords();
        _hashCode += getMaxpageno();
        if (getMessagetype() != null) {
            _hashCode += getMessagetype().hashCode();
        }
        if (getMessageid() != null) {
            _hashCode += getMessageid().hashCode();
        }
        if (getVersion() != null) {
            _hashCode += getVersion().hashCode();
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(Responseheader.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://tempurl.org", "responseheader"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("responsecode");
        elemField.setXmlName(new javax.xml.namespace.QName("http://tempurl.org", "responsecode"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "short"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("responsemessage");
        elemField.setXmlName(new javax.xml.namespace.QName("http://tempurl.org", "responsemessage"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("pagerecords");
        elemField.setXmlName(new javax.xml.namespace.QName("http://tempurl.org", "pagerecords"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "int"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("pageno");
        elemField.setXmlName(new javax.xml.namespace.QName("http://tempurl.org", "pageno"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "int"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("updatecount");
        elemField.setXmlName(new javax.xml.namespace.QName("http://tempurl.org", "updatecount"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "int"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("maxrecords");
        elemField.setXmlName(new javax.xml.namespace.QName("http://tempurl.org", "maxrecords"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "int"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("maxpageno");
        elemField.setXmlName(new javax.xml.namespace.QName("http://tempurl.org", "maxpageno"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "int"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("messagetype");
        elemField.setXmlName(new javax.xml.namespace.QName("http://tempurl.org", "messagetype"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("messageid");
        elemField.setXmlName(new javax.xml.namespace.QName("http://tempurl.org", "messageid"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("version");
        elemField.setXmlName(new javax.xml.namespace.QName("http://tempurl.org", "version"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
    }

    /**
     * Return type metadata object
     */
    public static org.apache.axis.description.TypeDesc getTypeDesc() {
        return typeDesc;
    }

    /**
     * Get Custom Serializer
     */
    public static org.apache.axis.encoding.Serializer getSerializer(
           java.lang.String mechType, 
           java.lang.Class _javaType,  
           javax.xml.namespace.QName _xmlType) {
        return 
          new  org.apache.axis.encoding.ser.BeanSerializer(
            _javaType, _xmlType, typeDesc);
    }

    /**
     * Get Custom Deserializer
     */
    public static org.apache.axis.encoding.Deserializer getDeserializer(
           java.lang.String mechType, 
           java.lang.Class _javaType,  
           javax.xml.namespace.QName _xmlType) {
        return 
          new  org.apache.axis.encoding.ser.BeanDeserializer(
            _javaType, _xmlType, typeDesc);
    }

}
