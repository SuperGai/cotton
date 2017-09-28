/**
 * Esalesitem.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.agilecontrol.wanxiangcheng;

public class Esalesitem  implements java.io.Serializable {
	 private int lineno;

	    private java.lang.String itemcode;

	    private java.lang.String qty;

	    private java.lang.String discountamount;

	    private java.lang.String netamount;

	    private java.lang.String bonusearn;

	    private java.lang.String extendparam;

	    private java.lang.String salesitemremark;

	    public Esalesitem() {
	    }

	    public Esalesitem(
	           int lineno,
	           java.lang.String itemcode,
	           java.lang.String qty,
	           java.lang.String discountamount,
	           java.lang.String netamount,
	           java.lang.String bonusearn,
	           java.lang.String extendparam,
	           java.lang.String salesitemremark) {
	           this.lineno = lineno;
	           this.itemcode = itemcode;
	           this.qty = qty;
	           this.discountamount = discountamount;
	           this.netamount = netamount;
	           this.bonusearn = bonusearn;
	           this.extendparam = extendparam;
	           this.salesitemremark = salesitemremark;
	    }


	    /**
	     * Gets the lineno value for this Esalesitem.
	     * 
	     * @return lineno
	     */
	    public int getLineno() {
	        return lineno;
	    }


	    /**
	     * Sets the lineno value for this Esalesitem.
	     * 
	     * @param lineno
	     */
	    public void setLineno(int lineno) {
	        this.lineno = lineno;
	    }


	    /**
	     * Gets the itemcode value for this Esalesitem.
	     * 
	     * @return itemcode
	     */
	    public java.lang.String getItemcode() {
	        return itemcode;
	    }


	    /**
	     * Sets the itemcode value for this Esalesitem.
	     * 
	     * @param itemcode
	     */
	    public void setItemcode(java.lang.String itemcode) {
	        this.itemcode = itemcode;
	    }


	    /**
	     * Gets the qty value for this Esalesitem.
	     * 
	     * @return qty
	     */
	    public java.lang.String getQty() {
	        return qty;
	    }


	    /**
	     * Sets the qty value for this Esalesitem.
	     * 
	     * @param qty
	     */
	    public void setQty(java.lang.String qty) {
	        this.qty = qty;
	    }


	    /**
	     * Gets the discountamount value for this Esalesitem.
	     * 
	     * @return discountamount
	     */
	    public java.lang.String getDiscountamount() {
	        return discountamount;
	    }


	    /**
	     * Sets the discountamount value for this Esalesitem.
	     * 
	     * @param discountamount
	     */
	    public void setDiscountamount(java.lang.String discountamount) {
	        this.discountamount = discountamount;
	    }


	    /**
	     * Gets the netamount value for this Esalesitem.
	     * 
	     * @return netamount
	     */
	    public java.lang.String getNetamount() {
	        return netamount;
	    }


	    /**
	     * Sets the netamount value for this Esalesitem.
	     * 
	     * @param netamount
	     */
	    public void setNetamount(java.lang.String netamount) {
	        this.netamount = netamount;
	    }


	    /**
	     * Gets the bonusearn value for this Esalesitem.
	     * 
	     * @return bonusearn
	     */
	    public java.lang.String getBonusearn() {
	        return bonusearn;
	    }


	    /**
	     * Sets the bonusearn value for this Esalesitem.
	     * 
	     * @param bonusearn
	     */
	    public void setBonusearn(java.lang.String bonusearn) {
	        this.bonusearn = bonusearn;
	    }


	    /**
	     * Gets the extendparam value for this Esalesitem.
	     * 
	     * @return extendparam
	     */
	    public java.lang.String getExtendparam() {
	        return extendparam;
	    }


	    /**
	     * Sets the extendparam value for this Esalesitem.
	     * 
	     * @param extendparam
	     */
	    public void setExtendparam(java.lang.String extendparam) {
	        this.extendparam = extendparam;
	    }


	    /**
	     * Gets the salesitemremark value for this Esalesitem.
	     * 
	     * @return salesitemremark
	     */
	    public java.lang.String getSalesitemremark() {
	        return salesitemremark;
	    }


	    /**
	     * Sets the salesitemremark value for this Esalesitem.
	     * 
	     * @param salesitemremark
	     */
	    public void setSalesitemremark(java.lang.String salesitemremark) {
	        this.salesitemremark = salesitemremark;
	    }

	    private java.lang.Object __equalsCalc = null;
	    public synchronized boolean equals(java.lang.Object obj) {
	        if (!(obj instanceof Esalesitem)) return false;
	        Esalesitem other = (Esalesitem) obj;
	        if (obj == null) return false;
	        if (this == obj) return true;
	        if (__equalsCalc != null) {
	            return (__equalsCalc == obj);
	        }
	        __equalsCalc = obj;
	        boolean _equals;
	        _equals = true && 
	            this.lineno == other.getLineno() &&
	            ((this.itemcode==null && other.getItemcode()==null) || 
	             (this.itemcode!=null &&
	              this.itemcode.equals(other.getItemcode()))) &&
	            ((this.qty==null && other.getQty()==null) || 
	             (this.qty!=null &&
	              this.qty.equals(other.getQty()))) &&
	            ((this.discountamount==null && other.getDiscountamount()==null) || 
	             (this.discountamount!=null &&
	              this.discountamount.equals(other.getDiscountamount()))) &&
	            ((this.netamount==null && other.getNetamount()==null) || 
	             (this.netamount!=null &&
	              this.netamount.equals(other.getNetamount()))) &&
	            ((this.bonusearn==null && other.getBonusearn()==null) || 
	             (this.bonusearn!=null &&
	              this.bonusearn.equals(other.getBonusearn()))) &&
	            ((this.extendparam==null && other.getExtendparam()==null) || 
	             (this.extendparam!=null &&
	              this.extendparam.equals(other.getExtendparam()))) &&
	            ((this.salesitemremark==null && other.getSalesitemremark()==null) || 
	             (this.salesitemremark!=null &&
	              this.salesitemremark.equals(other.getSalesitemremark())));
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
	        if (getItemcode() != null) {
	            _hashCode += getItemcode().hashCode();
	        }
	        if (getQty() != null) {
	            _hashCode += getQty().hashCode();
	        }
	        if (getDiscountamount() != null) {
	            _hashCode += getDiscountamount().hashCode();
	        }
	        if (getNetamount() != null) {
	            _hashCode += getNetamount().hashCode();
	        }
	        if (getBonusearn() != null) {
	            _hashCode += getBonusearn().hashCode();
	        }
	        if (getExtendparam() != null) {
	            _hashCode += getExtendparam().hashCode();
	        }
	        if (getSalesitemremark() != null) {
	            _hashCode += getSalesitemremark().hashCode();
	        }
	        __hashCodeCalc = false;
	        return _hashCode;
	    }

	    // Type metadata
	    private static org.apache.axis.description.TypeDesc typeDesc =
	        new org.apache.axis.description.TypeDesc(Esalesitem.class, true);

	    static {
	        typeDesc.setXmlType(new javax.xml.namespace.QName("http://tempurl.org", "esalesitem"));
	        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
	        elemField.setFieldName("lineno");
	        elemField.setXmlName(new javax.xml.namespace.QName("http://tempurl.org", "lineno"));
	        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "int"));
	        elemField.setNillable(false);
	        typeDesc.addFieldDesc(elemField);
	        elemField = new org.apache.axis.description.ElementDesc();
	        elemField.setFieldName("itemcode");
	        elemField.setXmlName(new javax.xml.namespace.QName("http://tempurl.org", "itemcode"));
	        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
	        elemField.setMinOccurs(0);
	        elemField.setNillable(false);
	        typeDesc.addFieldDesc(elemField);
	        elemField = new org.apache.axis.description.ElementDesc();
	        elemField.setFieldName("qty");
	        elemField.setXmlName(new javax.xml.namespace.QName("http://tempurl.org", "qty"));
	        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "decimal"));
	        elemField.setNillable(false);
	        typeDesc.addFieldDesc(elemField);
	        elemField = new org.apache.axis.description.ElementDesc();
	        elemField.setFieldName("discountamount");
	        elemField.setXmlName(new javax.xml.namespace.QName("http://tempurl.org", "discountamount"));
	        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "decimal"));
	        elemField.setNillable(false);
	        typeDesc.addFieldDesc(elemField);
	        elemField = new org.apache.axis.description.ElementDesc();
	        elemField.setFieldName("netamount");
	        elemField.setXmlName(new javax.xml.namespace.QName("http://tempurl.org", "netamount"));
	        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "decimal"));
	        elemField.setNillable(false);
	        typeDesc.addFieldDesc(elemField);
	        elemField = new org.apache.axis.description.ElementDesc();
	        elemField.setFieldName("bonusearn");
	        elemField.setXmlName(new javax.xml.namespace.QName("http://tempurl.org", "bonusearn"));
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
	        elemField.setFieldName("salesitemremark");
	        elemField.setXmlName(new javax.xml.namespace.QName("http://tempurl.org", "salesitemremark"));
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
