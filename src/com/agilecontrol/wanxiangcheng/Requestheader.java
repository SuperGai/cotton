/**
 * Requestheader.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.agilecontrol.wanxiangcheng;

public class Requestheader  implements java.io.Serializable {
    private java.lang.String licensekey;

    private java.lang.String username;

    private java.lang.String password;

    private java.lang.String lang;

    private int pagerecords;

    private int pageno;

    private int updatecount;

    private java.lang.String messagetype;

    private java.lang.String messageid;

    private java.lang.String version;

    public Requestheader() {
    }

    public Requestheader(
           java.lang.String licensekey,
           java.lang.String username,
           java.lang.String password,
           java.lang.String lang,
           int pagerecords,
           int pageno,
           int updatecount,
           java.lang.String messagetype,
           java.lang.String messageid,
           java.lang.String version) {
           this.licensekey = licensekey;
           this.username = username;
           this.password = password;
           this.lang = lang;
           this.pagerecords = pagerecords;
           this.pageno = pageno;
           this.updatecount = updatecount;
           this.messagetype = messagetype;
           this.messageid = messageid;
           this.version = version;
    }


    /**
     * Gets the licensekey value for this Requestheader.
     * 
     * @return licensekey
     */
    public java.lang.String getLicensekey() {
        return licensekey;
    }


    /**
     * Sets the licensekey value for this Requestheader.
     * 
     * @param licensekey
     */
    public void setLicensekey(java.lang.String licensekey) {
        this.licensekey = licensekey;
    }


    /**
     * Gets the username value for this Requestheader.
     * 
     * @return username
     */
    public java.lang.String getUsername() {
        return username;
    }


    /**
     * Sets the username value for this Requestheader.
     * 
     * @param username
     */
    public void setUsername(java.lang.String username) {
        this.username = username;
    }


    /**
     * Gets the password value for this Requestheader.
     * 
     * @return password
     */
    public java.lang.String getPassword() {
        return password;
    }


    /**
     * Sets the password value for this Requestheader.
     * 
     * @param password
     */
    public void setPassword(java.lang.String password) {
        this.password = password;
    }


    /**
     * Gets the lang value for this Requestheader.
     * 
     * @return lang
     */
    public java.lang.String getLang() {
        return lang;
    }


    /**
     * Sets the lang value for this Requestheader.
     * 
     * @param lang
     */
    public void setLang(java.lang.String lang) {
        this.lang = lang;
    }


    /**
     * Gets the pagerecords value for this Requestheader.
     * 
     * @return pagerecords
     */
    public int getPagerecords() {
        return pagerecords;
    }


    /**
     * Sets the pagerecords value for this Requestheader.
     * 
     * @param pagerecords
     */
    public void setPagerecords(int pagerecords) {
        this.pagerecords = pagerecords;
    }


    /**
     * Gets the pageno value for this Requestheader.
     * 
     * @return pageno
     */
    public int getPageno() {
        return pageno;
    }


    /**
     * Sets the pageno value for this Requestheader.
     * 
     * @param pageno
     */
    public void setPageno(int pageno) {
        this.pageno = pageno;
    }


    /**
     * Gets the updatecount value for this Requestheader.
     * 
     * @return updatecount
     */
    public int getUpdatecount() {
        return updatecount;
    }


    /**
     * Sets the updatecount value for this Requestheader.
     * 
     * @param updatecount
     */
    public void setUpdatecount(int updatecount) {
        this.updatecount = updatecount;
    }


    /**
     * Gets the messagetype value for this Requestheader.
     * 
     * @return messagetype
     */
    public java.lang.String getMessagetype() {
        return messagetype;
    }


    /**
     * Sets the messagetype value for this Requestheader.
     * 
     * @param messagetype
     */
    public void setMessagetype(java.lang.String messagetype) {
        this.messagetype = messagetype;
    }


    /**
     * Gets the messageid value for this Requestheader.
     * 
     * @return messageid
     */
    public java.lang.String getMessageid() {
        return messageid;
    }


    /**
     * Sets the messageid value for this Requestheader.
     * 
     * @param messageid
     */
    public void setMessageid(java.lang.String messageid) {
        this.messageid = messageid;
    }


    /**
     * Gets the version value for this Requestheader.
     * 
     * @return version
     */
    public java.lang.String getVersion() {
        return version;
    }


    /**
     * Sets the version value for this Requestheader.
     * 
     * @param version
     */
    public void setVersion(java.lang.String version) {
        this.version = version;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof Requestheader)) return false;
        Requestheader other = (Requestheader) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            ((this.licensekey==null && other.getLicensekey()==null) || 
             (this.licensekey!=null &&
              this.licensekey.equals(other.getLicensekey()))) &&
            ((this.username==null && other.getUsername()==null) || 
             (this.username!=null &&
              this.username.equals(other.getUsername()))) &&
            ((this.password==null && other.getPassword()==null) || 
             (this.password!=null &&
              this.password.equals(other.getPassword()))) &&
            ((this.lang==null && other.getLang()==null) || 
             (this.lang!=null &&
              this.lang.equals(other.getLang()))) &&
            this.pagerecords == other.getPagerecords() &&
            this.pageno == other.getPageno() &&
            this.updatecount == other.getUpdatecount() &&
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
        if (getLicensekey() != null) {
            _hashCode += getLicensekey().hashCode();
        }
        if (getUsername() != null) {
            _hashCode += getUsername().hashCode();
        }
        if (getPassword() != null) {
            _hashCode += getPassword().hashCode();
        }
        if (getLang() != null) {
            _hashCode += getLang().hashCode();
        }
        _hashCode += getPagerecords();
        _hashCode += getPageno();
        _hashCode += getUpdatecount();
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
        new org.apache.axis.description.TypeDesc(Requestheader.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://tempurl.org", "requestheader"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("licensekey");
        elemField.setXmlName(new javax.xml.namespace.QName("http://tempurl.org", "licensekey"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("username");
        elemField.setXmlName(new javax.xml.namespace.QName("http://tempurl.org", "username"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("password");
        elemField.setXmlName(new javax.xml.namespace.QName("http://tempurl.org", "password"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("lang");
        elemField.setXmlName(new javax.xml.namespace.QName("http://tempurl.org", "lang"));
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
