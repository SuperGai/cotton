/**
 * Postposconnectcreaterequest.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.agilecontrol.wanxiangcheng;

public class Postposconnectcreaterequest  implements java.io.Serializable {
    private com.agilecontrol.wanxiangcheng.Requestheader header;

    private com.agilecontrol.wanxiangcheng.Posconnecthdr connecthdr;

    private com.agilecontrol.wanxiangcheng.Posconnectdtl[] connectdtls;

    public Postposconnectcreaterequest() {
    }

    public Postposconnectcreaterequest(
           com.agilecontrol.wanxiangcheng.Requestheader header,
           com.agilecontrol.wanxiangcheng.Posconnecthdr connecthdr,
           com.agilecontrol.wanxiangcheng.Posconnectdtl[] connectdtls) {
           this.header = header;
           this.connecthdr = connecthdr;
           this.connectdtls = connectdtls;
    }


    /**
     * Gets the header value for this Postposconnectcreaterequest.
     * 
     * @return header
     */
    public com.agilecontrol.wanxiangcheng.Requestheader getHeader() {
        return header;
    }


    /**
     * Sets the header value for this Postposconnectcreaterequest.
     * 
     * @param header
     */
    public void setHeader(com.agilecontrol.wanxiangcheng.Requestheader header) {
        this.header = header;
    }


    /**
     * Gets the connecthdr value for this Postposconnectcreaterequest.
     * 
     * @return connecthdr
     */
    public com.agilecontrol.wanxiangcheng.Posconnecthdr getConnecthdr() {
        return connecthdr;
    }


    /**
     * Sets the connecthdr value for this Postposconnectcreaterequest.
     * 
     * @param connecthdr
     */
    public void setConnecthdr(com.agilecontrol.wanxiangcheng.Posconnecthdr connecthdr) {
        this.connecthdr = connecthdr;
    }


    /**
     * Gets the connectdtls value for this Postposconnectcreaterequest.
     * 
     * @return connectdtls
     */
    public com.agilecontrol.wanxiangcheng.Posconnectdtl[] getConnectdtls() {
        return connectdtls;
    }


    /**
     * Sets the connectdtls value for this Postposconnectcreaterequest.
     * 
     * @param connectdtls
     */
    public void setConnectdtls(com.agilecontrol.wanxiangcheng.Posconnectdtl[] connectdtls) {
        this.connectdtls = connectdtls;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof Postposconnectcreaterequest)) return false;
        Postposconnectcreaterequest other = (Postposconnectcreaterequest) obj;
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
            ((this.connecthdr==null && other.getConnecthdr()==null) || 
             (this.connecthdr!=null &&
              this.connecthdr.equals(other.getConnecthdr()))) &&
            ((this.connectdtls==null && other.getConnectdtls()==null) || 
             (this.connectdtls!=null &&
              java.util.Arrays.equals(this.connectdtls, other.getConnectdtls())));
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
        if (getConnecthdr() != null) {
            _hashCode += getConnecthdr().hashCode();
        }
        if (getConnectdtls() != null) {
            for (int i=0;
                 i<java.lang.reflect.Array.getLength(getConnectdtls());
                 i++) {
                java.lang.Object obj = java.lang.reflect.Array.get(getConnectdtls(), i);
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
        new org.apache.axis.description.TypeDesc(Postposconnectcreaterequest.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://tempurl.org", "postposconnectcreaterequest"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("header");
        elemField.setXmlName(new javax.xml.namespace.QName("http://tempurl.org", "header"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://tempurl.org", "requestheader"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("connecthdr");
        elemField.setXmlName(new javax.xml.namespace.QName("http://tempurl.org", "connecthdr"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://tempurl.org", "posconnecthdr"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("connectdtls");
        elemField.setXmlName(new javax.xml.namespace.QName("http://tempurl.org", "connectdtls"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://tempurl.org", "posconnectdtl"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        elemField.setItemQName(new javax.xml.namespace.QName("http://tempurl.org", "posconnectdtl"));
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
