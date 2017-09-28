/**
 * Postsalesbonusadjustrequest.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.agilecontrol.wanxiangcheng;

public class Postsalesbonusadjustrequest  implements java.io.Serializable {
    private com.agilecontrol.wanxiangcheng.Requestheader header;

    private com.agilecontrol.wanxiangcheng.Salesbonusadjust bonusadjust;

    public Postsalesbonusadjustrequest() {
    }

    public Postsalesbonusadjustrequest(
           com.agilecontrol.wanxiangcheng.Requestheader header,
           com.agilecontrol.wanxiangcheng.Salesbonusadjust bonusadjust) {
           this.header = header;
           this.bonusadjust = bonusadjust;
    }


    /**
     * Gets the header value for this Postsalesbonusadjustrequest.
     * 
     * @return header
     */
    public com.agilecontrol.wanxiangcheng.Requestheader getHeader() {
        return header;
    }


    /**
     * Sets the header value for this Postsalesbonusadjustrequest.
     * 
     * @param header
     */
    public void setHeader(com.agilecontrol.wanxiangcheng.Requestheader header) {
        this.header = header;
    }


    /**
     * Gets the bonusadjust value for this Postsalesbonusadjustrequest.
     * 
     * @return bonusadjust
     */
    public com.agilecontrol.wanxiangcheng.Salesbonusadjust getBonusadjust() {
        return bonusadjust;
    }


    /**
     * Sets the bonusadjust value for this Postsalesbonusadjustrequest.
     * 
     * @param bonusadjust
     */
    public void setBonusadjust(com.agilecontrol.wanxiangcheng.Salesbonusadjust bonusadjust) {
        this.bonusadjust = bonusadjust;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof Postsalesbonusadjustrequest)) return false;
        Postsalesbonusadjustrequest other = (Postsalesbonusadjustrequest) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            ((this.header==null && other.getHeader()==null) || 
             (this.header!=null &&
              this.header.equals(other.getHeader()))) &&
            ((this.bonusadjust==null && other.getBonusadjust()==null) || 
             (this.bonusadjust!=null &&
              this.bonusadjust.equals(other.getBonusadjust())));
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
        if (getHeader() != null) {
            _hashCode += getHeader().hashCode();
        }
        if (getBonusadjust() != null) {
            _hashCode += getBonusadjust().hashCode();
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(Postsalesbonusadjustrequest.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://tempurl.org", "postsalesbonusadjustrequest"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("header");
        elemField.setXmlName(new javax.xml.namespace.QName("http://tempurl.org", "header"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://tempurl.org", "requestheader"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("bonusadjust");
        elemField.setXmlName(new javax.xml.namespace.QName("http://tempurl.org", "bonusadjust"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://tempurl.org", "salesbonusadjust"));
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
