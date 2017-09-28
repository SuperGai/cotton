/**
 * Posconnecthdr.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.agilecontrol.wanxiangcheng;

public class Posconnecthdr  implements java.io.Serializable {
    private java.lang.String localstorecode;

    private java.lang.String mallid;

    public Posconnecthdr() {
    }

    public Posconnecthdr(
           java.lang.String localstorecode,
           java.lang.String mallid) {
           this.localstorecode = localstorecode;
           this.mallid = mallid;
    }


    /**
     * Gets the localstorecode value for this Posconnecthdr.
     * 
     * @return localstorecode
     */
    public java.lang.String getLocalstorecode() {
        return localstorecode;
    }


    /**
     * Sets the localstorecode value for this Posconnecthdr.
     * 
     * @param localstorecode
     */
    public void setLocalstorecode(java.lang.String localstorecode) {
        this.localstorecode = localstorecode;
    }


    /**
     * Gets the mallid value for this Posconnecthdr.
     * 
     * @return mallid
     */
    public java.lang.String getMallid() {
        return mallid;
    }


    /**
     * Sets the mallid value for this Posconnecthdr.
     * 
     * @param mallid
     */
    public void setMallid(java.lang.String mallid) {
        this.mallid = mallid;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof Posconnecthdr)) return false;
        Posconnecthdr other = (Posconnecthdr) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            ((this.localstorecode==null && other.getLocalstorecode()==null) || 
             (this.localstorecode!=null &&
              this.localstorecode.equals(other.getLocalstorecode()))) &&
            ((this.mallid==null && other.getMallid()==null) || 
             (this.mallid!=null &&
              this.mallid.equals(other.getMallid())));
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
        if (getLocalstorecode() != null) {
            _hashCode += getLocalstorecode().hashCode();
        }
        if (getMallid() != null) {
            _hashCode += getMallid().hashCode();
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(Posconnecthdr.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://tempurl.org", "posconnecthdr"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("localstorecode");
        elemField.setXmlName(new javax.xml.namespace.QName("http://tempurl.org", "localstorecode"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("mallid");
        elemField.setXmlName(new javax.xml.namespace.QName("http://tempurl.org", "mallid"));
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
