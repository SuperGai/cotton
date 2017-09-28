/**
 * Salesbonusadjust.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.agilecontrol.wanxiangcheng;

public class Salesbonusadjust  implements java.io.Serializable {
    private java.lang.String txdate_yyyymmdd;

    private java.lang.String txtime_hhmmss;

    private java.lang.String mallid;

    private java.lang.String storecode;

    private java.lang.String tillid;

    private java.lang.String txdocno;

    private java.lang.String vipcode;

    private java.math.BigDecimal netamount;

    private java.math.BigDecimal bonus;

    public Salesbonusadjust() {
    }

    public Salesbonusadjust(
           java.lang.String txdate_yyyymmdd,
           java.lang.String txtime_hhmmss,
           java.lang.String mallid,
           java.lang.String storecode,
           java.lang.String tillid,
           java.lang.String txdocno,
           java.lang.String vipcode,
           java.math.BigDecimal netamount,
           java.math.BigDecimal bonus) {
           this.txdate_yyyymmdd = txdate_yyyymmdd;
           this.txtime_hhmmss = txtime_hhmmss;
           this.mallid = mallid;
           this.storecode = storecode;
           this.tillid = tillid;
           this.txdocno = txdocno;
           this.vipcode = vipcode;
           this.netamount = netamount;
           this.bonus = bonus;
    }


    /**
     * Gets the txdate_yyyymmdd value for this Salesbonusadjust.
     * 
     * @return txdate_yyyymmdd
     */
    public java.lang.String getTxdate_yyyymmdd() {
        return txdate_yyyymmdd;
    }


    /**
     * Sets the txdate_yyyymmdd value for this Salesbonusadjust.
     * 
     * @param txdate_yyyymmdd
     */
    public void setTxdate_yyyymmdd(java.lang.String txdate_yyyymmdd) {
        this.txdate_yyyymmdd = txdate_yyyymmdd;
    }


    /**
     * Gets the txtime_hhmmss value for this Salesbonusadjust.
     * 
     * @return txtime_hhmmss
     */
    public java.lang.String getTxtime_hhmmss() {
        return txtime_hhmmss;
    }


    /**
     * Sets the txtime_hhmmss value for this Salesbonusadjust.
     * 
     * @param txtime_hhmmss
     */
    public void setTxtime_hhmmss(java.lang.String txtime_hhmmss) {
        this.txtime_hhmmss = txtime_hhmmss;
    }


    /**
     * Gets the mallid value for this Salesbonusadjust.
     * 
     * @return mallid
     */
    public java.lang.String getMallid() {
        return mallid;
    }


    /**
     * Sets the mallid value for this Salesbonusadjust.
     * 
     * @param mallid
     */
    public void setMallid(java.lang.String mallid) {
        this.mallid = mallid;
    }


    /**
     * Gets the storecode value for this Salesbonusadjust.
     * 
     * @return storecode
     */
    public java.lang.String getStorecode() {
        return storecode;
    }


    /**
     * Sets the storecode value for this Salesbonusadjust.
     * 
     * @param storecode
     */
    public void setStorecode(java.lang.String storecode) {
        this.storecode = storecode;
    }


    /**
     * Gets the tillid value for this Salesbonusadjust.
     * 
     * @return tillid
     */
    public java.lang.String getTillid() {
        return tillid;
    }


    /**
     * Sets the tillid value for this Salesbonusadjust.
     * 
     * @param tillid
     */
    public void setTillid(java.lang.String tillid) {
        this.tillid = tillid;
    }


    /**
     * Gets the txdocno value for this Salesbonusadjust.
     * 
     * @return txdocno
     */
    public java.lang.String getTxdocno() {
        return txdocno;
    }


    /**
     * Sets the txdocno value for this Salesbonusadjust.
     * 
     * @param txdocno
     */
    public void setTxdocno(java.lang.String txdocno) {
        this.txdocno = txdocno;
    }


    /**
     * Gets the vipcode value for this Salesbonusadjust.
     * 
     * @return vipcode
     */
    public java.lang.String getVipcode() {
        return vipcode;
    }


    /**
     * Sets the vipcode value for this Salesbonusadjust.
     * 
     * @param vipcode
     */
    public void setVipcode(java.lang.String vipcode) {
        this.vipcode = vipcode;
    }


    /**
     * Gets the netamount value for this Salesbonusadjust.
     * 
     * @return netamount
     */
    public java.math.BigDecimal getNetamount() {
        return netamount;
    }


    /**
     * Sets the netamount value for this Salesbonusadjust.
     * 
     * @param netamount
     */
    public void setNetamount(java.math.BigDecimal netamount) {
        this.netamount = netamount;
    }


    /**
     * Gets the bonus value for this Salesbonusadjust.
     * 
     * @return bonus
     */
    public java.math.BigDecimal getBonus() {
        return bonus;
    }


    /**
     * Sets the bonus value for this Salesbonusadjust.
     * 
     * @param bonus
     */
    public void setBonus(java.math.BigDecimal bonus) {
        this.bonus = bonus;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof Salesbonusadjust)) return false;
        Salesbonusadjust other = (Salesbonusadjust) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            ((this.txdate_yyyymmdd==null && other.getTxdate_yyyymmdd()==null) || 
             (this.txdate_yyyymmdd!=null &&
              this.txdate_yyyymmdd.equals(other.getTxdate_yyyymmdd()))) &&
            ((this.txtime_hhmmss==null && other.getTxtime_hhmmss()==null) || 
             (this.txtime_hhmmss!=null &&
              this.txtime_hhmmss.equals(other.getTxtime_hhmmss()))) &&
            ((this.mallid==null && other.getMallid()==null) || 
             (this.mallid!=null &&
              this.mallid.equals(other.getMallid()))) &&
            ((this.storecode==null && other.getStorecode()==null) || 
             (this.storecode!=null &&
              this.storecode.equals(other.getStorecode()))) &&
            ((this.tillid==null && other.getTillid()==null) || 
             (this.tillid!=null &&
              this.tillid.equals(other.getTillid()))) &&
            ((this.txdocno==null && other.getTxdocno()==null) || 
             (this.txdocno!=null &&
              this.txdocno.equals(other.getTxdocno()))) &&
            ((this.vipcode==null && other.getVipcode()==null) || 
             (this.vipcode!=null &&
              this.vipcode.equals(other.getVipcode()))) &&
            ((this.netamount==null && other.getNetamount()==null) || 
             (this.netamount!=null &&
              this.netamount.equals(other.getNetamount()))) &&
            ((this.bonus==null && other.getBonus()==null) || 
             (this.bonus!=null &&
              this.bonus.equals(other.getBonus())));
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
        if (getTxdate_yyyymmdd() != null) {
            _hashCode += getTxdate_yyyymmdd().hashCode();
        }
        if (getTxtime_hhmmss() != null) {
            _hashCode += getTxtime_hhmmss().hashCode();
        }
        if (getMallid() != null) {
            _hashCode += getMallid().hashCode();
        }
        if (getStorecode() != null) {
            _hashCode += getStorecode().hashCode();
        }
        if (getTillid() != null) {
            _hashCode += getTillid().hashCode();
        }
        if (getTxdocno() != null) {
            _hashCode += getTxdocno().hashCode();
        }
        if (getVipcode() != null) {
            _hashCode += getVipcode().hashCode();
        }
        if (getNetamount() != null) {
            _hashCode += getNetamount().hashCode();
        }
        if (getBonus() != null) {
            _hashCode += getBonus().hashCode();
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(Salesbonusadjust.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://tempurl.org", "salesbonusadjust"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("txdate_yyyymmdd");
        elemField.setXmlName(new javax.xml.namespace.QName("http://tempurl.org", "txdate_yyyymmdd"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("txtime_hhmmss");
        elemField.setXmlName(new javax.xml.namespace.QName("http://tempurl.org", "txtime_hhmmss"));
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
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("storecode");
        elemField.setXmlName(new javax.xml.namespace.QName("http://tempurl.org", "storecode"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("tillid");
        elemField.setXmlName(new javax.xml.namespace.QName("http://tempurl.org", "tillid"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("txdocno");
        elemField.setXmlName(new javax.xml.namespace.QName("http://tempurl.org", "txdocno"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("vipcode");
        elemField.setXmlName(new javax.xml.namespace.QName("http://tempurl.org", "vipcode"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("netamount");
        elemField.setXmlName(new javax.xml.namespace.QName("http://tempurl.org", "netamount"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "decimal"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("bonus");
        elemField.setXmlName(new javax.xml.namespace.QName("http://tempurl.org", "bonus"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "decimal"));
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
