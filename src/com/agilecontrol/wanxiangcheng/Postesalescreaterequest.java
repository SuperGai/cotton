/**
 * Postesalescreaterequest.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.agilecontrol.wanxiangcheng;

public class Postesalescreaterequest  implements java.io.Serializable {
    private com.agilecontrol.wanxiangcheng.Requestheader header;

    private com.agilecontrol.wanxiangcheng.Esaleshdr esalestotal;

    private com.agilecontrol.wanxiangcheng.Esalesitem[] esalesitems;

    private com.agilecontrol.wanxiangcheng.Esalestender[] esalestenders;

    public Postesalescreaterequest() {
    }

    public Postesalescreaterequest(
           com.agilecontrol.wanxiangcheng.Requestheader header,
           com.agilecontrol.wanxiangcheng.Esaleshdr esalestotal,
           com.agilecontrol.wanxiangcheng.Esalesitem[] esalesitems,
           com.agilecontrol.wanxiangcheng.Esalestender[] esalestenders) {
           this.header = header;
           this.esalestotal = esalestotal;
           this.esalesitems = esalesitems;
           this.esalestenders = esalestenders;
    }


    /**
     * Gets the header value for this Postesalescreaterequest.
     * 
     * @return header
     */
    public com.agilecontrol.wanxiangcheng.Requestheader getHeader() {
        return header;
    }


    /**
     * Sets the header value for this Postesalescreaterequest.
     * 
     * @param header
     */
    public void setHeader(com.agilecontrol.wanxiangcheng.Requestheader header) {
        this.header = header;
    }


    /**
     * Gets the esalestotal value for this Postesalescreaterequest.
     * 
     * @return esalestotal
     */
    public com.agilecontrol.wanxiangcheng.Esaleshdr getEsalestotal() {
        return esalestotal;
    }


    /**
     * Sets the esalestotal value for this Postesalescreaterequest.
     * 
     * @param esalestotal
     */
    public void setEsalestotal(com.agilecontrol.wanxiangcheng.Esaleshdr esalestotal) {
        this.esalestotal = esalestotal;
    }


    /**
     * Gets the esalesitems value for this Postesalescreaterequest.
     * 
     * @return esalesitems
     */
    public com.agilecontrol.wanxiangcheng.Esalesitem[] getEsalesitems() {
        return esalesitems;
    }


    /**
     * Sets the esalesitems value for this Postesalescreaterequest.
     * 
     * @param esalesitems
     */
    public void setEsalesitems(com.agilecontrol.wanxiangcheng.Esalesitem[] esalesitems) {
        this.esalesitems = esalesitems;
    }


    /**
     * Gets the esalestenders value for this Postesalescreaterequest.
     * 
     * @return esalestenders
     */
    public com.agilecontrol.wanxiangcheng.Esalestender[] getEsalestenders() {
        return esalestenders;
    }


    /**
     * Sets the esalestenders value for this Postesalescreaterequest.
     * 
     * @param esalestenders
     */
    public void setEsalestenders(com.agilecontrol.wanxiangcheng.Esalestender[] esalestenders) {
        this.esalestenders = esalestenders;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof Postesalescreaterequest)) return false;
        Postesalescreaterequest other = (Postesalescreaterequest) obj;
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
            ((this.esalestotal==null && other.getEsalestotal()==null) || 
             (this.esalestotal!=null &&
              this.esalestotal.equals(other.getEsalestotal()))) &&
            ((this.esalesitems==null && other.getEsalesitems()==null) || 
             (this.esalesitems!=null &&
              java.util.Arrays.equals(this.esalesitems, other.getEsalesitems()))) &&
            ((this.esalestenders==null && other.getEsalestenders()==null) || 
             (this.esalestenders!=null &&
              java.util.Arrays.equals(this.esalestenders, other.getEsalestenders())));
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
        if (getEsalestotal() != null) {
            _hashCode += getEsalestotal().hashCode();
        }
        if (getEsalesitems() != null) {
            for (int i=0;
                 i<java.lang.reflect.Array.getLength(getEsalesitems());
                 i++) {
                java.lang.Object obj = java.lang.reflect.Array.get(getEsalesitems(), i);
                if (obj != null &&
                    !obj.getClass().isArray()) {
                    _hashCode += obj.hashCode();
                }
            }
        }
        if (getEsalestenders() != null) {
            for (int i=0;
                 i<java.lang.reflect.Array.getLength(getEsalestenders());
                 i++) {
                java.lang.Object obj = java.lang.reflect.Array.get(getEsalestenders(), i);
                if (obj != null &&
                    !obj.getClass().isArray()) {
                    _hashCode += obj.hashCode();
                }
            }
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(Postesalescreaterequest.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://tempurl.org", "postesalescreaterequest"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("header");
        elemField.setXmlName(new javax.xml.namespace.QName("http://tempurl.org", "header"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://tempurl.org", "requestheader"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("esalestotal");
        elemField.setXmlName(new javax.xml.namespace.QName("http://tempurl.org", "esalestotal"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://tempurl.org", "esaleshdr"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("esalesitems");
        elemField.setXmlName(new javax.xml.namespace.QName("http://tempurl.org", "esalesitems"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://tempurl.org", "esalesitem"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        elemField.setItemQName(new javax.xml.namespace.QName("http://tempurl.org", "esalesitem"));
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("esalestenders");
        elemField.setXmlName(new javax.xml.namespace.QName("http://tempurl.org", "esalestenders"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://tempurl.org", "esalestender"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        elemField.setItemQName(new javax.xml.namespace.QName("http://tempurl.org", "esalestender"));
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
