/**
 * Esalestender.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.agilecontrol.wanxiangcheng;

public class Esalestender  implements java.io.Serializable {
	  private int lineno;

	    private java.lang.String tendercode;

	    private java.lang.String payamount;

	    private java.lang.String baseamount;

	    private java.lang.String excessamount;

	    private java.lang.String extendparam;

	    private java.lang.String remark;

	    public Esalestender() {
	    }

	    public Esalestender(
	           int lineno,
	           java.lang.String tendercode,
	           java.lang.String payamount,
	           java.lang.String baseamount,
	           java.lang.String excessamount,
	           java.lang.String extendparam,
	           java.lang.String remark) {
	           this.lineno = lineno;
	           this.tendercode = tendercode;
	           this.payamount = payamount;
	           this.baseamount = baseamount;
	           this.excessamount = excessamount;
	           this.extendparam = extendparam;
	           this.remark = remark;
	    }


	    /**
	     * Gets the lineno value for this Esalestender.
	     * 
	     * @return lineno
	     */
	    public int getLineno() {
	        return lineno;
	    }


	    /**
	     * Sets the lineno value for this Esalestender.
	     * 
	     * @param lineno
	     */
	    public void setLineno(int lineno) {
	        this.lineno = lineno;
	    }


	    /**
	     * Gets the tendercode value for this Esalestender.
	     * 
	     * @return tendercode
	     */
	    public java.lang.String getTendercode() {
	        return tendercode;
	    }


	    /**
	     * Sets the tendercode value for this Esalestender.
	     * 
	     * @param tendercode
	     */
	    public void setTendercode(java.lang.String tendercode) {
	        this.tendercode = tendercode;
	    }


	    /**
	     * Gets the payamount value for this Esalestender.
	     * 
	     * @return payamount
	     */
	    public java.lang.String getPayamount() {
	        return payamount;
	    }


	    /**
	     * Sets the payamount value for this Esalestender.
	     * 
	     * @param payamount
	     */
	    public void setPayamount(java.lang.String payamount) {
	        this.payamount = payamount;
	    }


	    /**
	     * Gets the baseamount value for this Esalestender.
	     * 
	     * @return baseamount
	     */
	    public java.lang.String getBaseamount() {
	        return baseamount;
	    }


	    /**
	     * Sets the baseamount value for this Esalestender.
	     * 
	     * @param baseamount
	     */
	    public void setBaseamount(java.lang.String baseamount) {
	        this.baseamount = baseamount;
	    }


	    /**
	     * Gets the excessamount value for this Esalestender.
	     * 
	     * @return excessamount
	     */
	    public java.lang.String getExcessamount() {
	        return excessamount;
	    }


	    /**
	     * Sets the excessamount value for this Esalestender.
	     * 
	     * @param excessamount
	     */
	    public void setExcessamount(java.lang.String excessamount) {
	        this.excessamount = excessamount;
	    }


	    /**
	     * Gets the extendparam value for this Esalestender.
	     * 
	     * @return extendparam
	     */
	    public java.lang.String getExtendparam() {
	        return extendparam;
	    }


	    /**
	     * Sets the extendparam value for this Esalestender.
	     * 
	     * @param extendparam
	     */
	    public void setExtendparam(java.lang.String extendparam) {
	        this.extendparam = extendparam;
	    }


	    /**
	     * Gets the remark value for this Esalestender.
	     * 
	     * @return remark
	     */
	    public java.lang.String getRemark() {
	        return remark;
	    }


	    /**
	     * Sets the remark value for this Esalestender.
	     * 
	     * @param remark
	     */
	    public void setRemark(java.lang.String remark) {
	        this.remark = remark;
	    }

	    private java.lang.Object __equalsCalc = null;
	    public synchronized boolean equals(java.lang.Object obj) {
	        if (!(obj instanceof Esalestender)) return false;
	        Esalestender other = (Esalestender) obj;
	        if (obj == null) return false;
	        if (this == obj) return true;
	        if (__equalsCalc != null) {
	            return (__equalsCalc == obj);
	        }
	        __equalsCalc = obj;
	        boolean _equals;
	        _equals = true && 
	            this.lineno == other.getLineno() &&
	            ((this.tendercode==null && other.getTendercode()==null) || 
	             (this.tendercode!=null &&
	              this.tendercode.equals(other.getTendercode()))) &&
	            ((this.payamount==null && other.getPayamount()==null) || 
	             (this.payamount!=null &&
	              this.payamount.equals(other.getPayamount()))) &&
	            ((this.baseamount==null && other.getBaseamount()==null) || 
	             (this.baseamount!=null &&
	              this.baseamount.equals(other.getBaseamount()))) &&
	            ((this.excessamount==null && other.getExcessamount()==null) || 
	             (this.excessamount!=null &&
	              this.excessamount.equals(other.getExcessamount()))) &&
	            ((this.extendparam==null && other.getExtendparam()==null) || 
	             (this.extendparam!=null &&
	              this.extendparam.equals(other.getExtendparam()))) &&
	            ((this.remark==null && other.getRemark()==null) || 
	             (this.remark!=null &&
	              this.remark.equals(other.getRemark())));
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
	        _hashCode += getLineno();
	        if (getTendercode() != null) {
	            _hashCode += getTendercode().hashCode();
	        }
	        if (getPayamount() != null) {
	            _hashCode += getPayamount().hashCode();
	        }
	        if (getBaseamount() != null) {
	            _hashCode += getBaseamount().hashCode();
	        }
	        if (getExcessamount() != null) {
	            _hashCode += getExcessamount().hashCode();
	        }
	        if (getExtendparam() != null) {
	            _hashCode += getExtendparam().hashCode();
	        }
	        if (getRemark() != null) {
	            _hashCode += getRemark().hashCode();
	        }
	        __hashCodeCalc = false;
	        return _hashCode;
	    }

	    // Type metadata
	    private static org.apache.axis.description.TypeDesc typeDesc =
	        new org.apache.axis.description.TypeDesc(Esalestender.class, true);

	    static {
	        typeDesc.setXmlType(new javax.xml.namespace.QName("http://tempurl.org", "esalestender"));
	        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
	        elemField.setFieldName("lineno");
	        elemField.setXmlName(new javax.xml.namespace.QName("http://tempurl.org", "lineno"));
	        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "int"));
	        elemField.setNillable(false);
	        typeDesc.addFieldDesc(elemField);
	        elemField = new org.apache.axis.description.ElementDesc();
	        elemField.setFieldName("tendercode");
	        elemField.setXmlName(new javax.xml.namespace.QName("http://tempurl.org", "tendercode"));
	        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
	        elemField.setMinOccurs(0);
	        elemField.setNillable(false);
	        typeDesc.addFieldDesc(elemField);
	        elemField = new org.apache.axis.description.ElementDesc();
	        elemField.setFieldName("payamount");
	        elemField.setXmlName(new javax.xml.namespace.QName("http://tempurl.org", "payamount"));
	        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "decimal"));
	        elemField.setNillable(false);
	        typeDesc.addFieldDesc(elemField);
	        elemField = new org.apache.axis.description.ElementDesc();
	        elemField.setFieldName("baseamount");
	        elemField.setXmlName(new javax.xml.namespace.QName("http://tempurl.org", "baseamount"));
	        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "decimal"));
	        elemField.setNillable(false);
	        typeDesc.addFieldDesc(elemField);
	        elemField = new org.apache.axis.description.ElementDesc();
	        elemField.setFieldName("excessamount");
	        elemField.setXmlName(new javax.xml.namespace.QName("http://tempurl.org", "excessamount"));
	        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "decimal"));
	        elemField.setNillable(false);
	        typeDesc.addFieldDesc(elemField);
	        elemField = new org.apache.axis.description.ElementDesc();
	        elemField.setFieldName("extendparam");
	        elemField.setXmlName(new javax.xml.namespace.QName("http://tempurl.org", "extendparam"));
	        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
	        elemField.setMinOccurs(0);
	        elemField.setNillable(false);
	        typeDesc.addFieldDesc(elemField);
	        elemField = new org.apache.axis.description.ElementDesc();
	        elemField.setFieldName("remark");
	        elemField.setXmlName(new javax.xml.namespace.QName("http://tempurl.org", "remark"));
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
