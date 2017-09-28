/**
 * Postvipbonuscreateresponse.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.agilecontrol.wanxiangcheng;

public class Postvipbonuscreateresponse  implements java.io.Serializable {
    private com.agilecontrol.wanxiangcheng.Responseheader header;

    private com.agilecontrol.wanxiangcheng.Vipbonuscreate bonuscreate;

    public Postvipbonuscreateresponse() {
    }

    public Postvipbonuscreateresponse(
           com.agilecontrol.wanxiangcheng.Responseheader header,
           com.agilecontrol.wanxiangcheng.Vipbonuscreate bonuscreate) {
           this.header = header;
           this.bonuscreate = bonuscreate;
    }


    /**
     * Gets the header value for this Postvipbonuscreateresponse.
     * 
     * @return header
     */
    public com.agilecontrol.wanxiangcheng.Responseheader getHeader() {
        return header;
    }


    /**
     * Sets the header value for this Postvipbonuscreateresponse.
     * 
     * @param header
     */
    public void setHeader(com.agilecontrol.wanxiangcheng.Responseheader header) {
        this.header = header;
    }


    /**
     * Gets the bonuscreate value for this Postvipbonuscreateresponse.
     * 
     * @return bonuscreate
     */
    public com.agilecontrol.wanxiangcheng.Vipbonuscreate getBonuscreate() {
        return bonuscreate;
    }


    /**
     * Sets the bonuscreate value for this Postvipbonuscreateresponse.
     * 
     * @param bonuscreate
     */
    public void setBonuscreate(com.agilecontrol.wanxiangcheng.Vipbonuscreate bonuscreate) {
        this.bonuscreate = bonuscreate;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof Postvipbonuscreateresponse)) return false;
        Postvipbonuscreateresponse other = (Postvipbonuscreateresponse) obj;
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
            ((this.bonuscreate==null && other.getBonuscreate()==null) || 
             (this.bonuscreate!=null &&
              this.bonuscreate.equals(other.getBonuscreate())));
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
        if (getBonuscreate() != null) {
            _hashCode += getBonuscreate().hashCode();
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(Postvipbonuscreateresponse.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://tempurl.org", "postvipbonuscreateresponse"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("header");
        elemField.setXmlName(new javax.xml.namespace.QName("http://tempurl.org", "header"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://tempurl.org", "responseheader"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("bonuscreate");
        elemField.setXmlName(new javax.xml.namespace.QName("http://tempurl.org", "bonuscreate"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://tempurl.org", "vipbonuscreate"));
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
