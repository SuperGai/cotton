/**
 * EsalesSoap.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.agilecontrol.wanxiangcheng;

public interface EsalesSoap extends java.rmi.Remote {
    public java.lang.String getversion() throws java.rmi.RemoteException;
    public java.lang.String getcompanyname() throws java.rmi.RemoteException;
    public com.agilecontrol.wanxiangcheng.Postposconnectcreateresponse postposconnectcreate(com.agilecontrol.wanxiangcheng.Postposconnectcreaterequest astr_request) throws java.rmi.RemoteException;
    public com.agilecontrol.wanxiangcheng.Postesalescreateresponse postesalescreate(com.agilecontrol.wanxiangcheng.Postesalescreaterequest astr_request) throws java.rmi.RemoteException;
    public com.agilecontrol.wanxiangcheng.Postsalesbonusadjustresponse postsalesbonusadjust(com.agilecontrol.wanxiangcheng.Postsalesbonusadjustrequest astr_request) throws java.rmi.RemoteException;
    public com.agilecontrol.wanxiangcheng.Postvipbonuscreateresponse postvipbonuscreate(com.agilecontrol.wanxiangcheng.Postvipbonuscreaterequest astr_request) throws java.rmi.RemoteException;
    public com.agilecontrol.wanxiangcheng.Postsalesbonusaddresponse postsalesbonusadd(com.agilecontrol.wanxiangcheng.Postsalesbonusaddrequest astr_request) throws java.rmi.RemoteException;
}
