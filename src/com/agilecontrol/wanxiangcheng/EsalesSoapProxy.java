package com.agilecontrol.wanxiangcheng;

public class EsalesSoapProxy implements com.agilecontrol.wanxiangcheng.EsalesSoap {
  private String _endpoint = null;
  private com.agilecontrol.wanxiangcheng.EsalesSoap esalesSoap = null;
  
  public EsalesSoapProxy() {
    _initEsalesSoapProxy();
  }
  
  public EsalesSoapProxy(String endpoint) {
    _endpoint = endpoint;
    _initEsalesSoapProxy();
  }
  
  private void _initEsalesSoapProxy() {
    try {
      esalesSoap = (new com.agilecontrol.wanxiangcheng.EsalesLocator()).getesalesSoap();
      if (esalesSoap != null) {
        if (_endpoint != null)
          ((javax.xml.rpc.Stub)esalesSoap)._setProperty("javax.xml.rpc.service.endpoint.address", _endpoint);
        else
          _endpoint = (String)((javax.xml.rpc.Stub)esalesSoap)._getProperty("javax.xml.rpc.service.endpoint.address");
      }
      
    }
    catch (javax.xml.rpc.ServiceException serviceException) {}
  }
  
  public String getEndpoint() {
    return _endpoint;
  }
  
  public void setEndpoint(String endpoint) {
    _endpoint = endpoint;
    if (esalesSoap != null)
      ((javax.xml.rpc.Stub)esalesSoap)._setProperty("javax.xml.rpc.service.endpoint.address", _endpoint);
    
  }
  
  public com.agilecontrol.wanxiangcheng.EsalesSoap getEsalesSoap() {
    if (esalesSoap == null)
      _initEsalesSoapProxy();
    return esalesSoap;
  }
  
  public java.lang.String getversion() throws java.rmi.RemoteException{
    if (esalesSoap == null)
      _initEsalesSoapProxy();
    return esalesSoap.getversion();
  }
  
  public java.lang.String getcompanyname() throws java.rmi.RemoteException{
    if (esalesSoap == null)
      _initEsalesSoapProxy();
    return esalesSoap.getcompanyname();
  }
  
  public com.agilecontrol.wanxiangcheng.Postposconnectcreateresponse postposconnectcreate(com.agilecontrol.wanxiangcheng.Postposconnectcreaterequest astr_request) throws java.rmi.RemoteException{
    if (esalesSoap == null)
      _initEsalesSoapProxy();
    return esalesSoap.postposconnectcreate(astr_request);
  }
  
  public com.agilecontrol.wanxiangcheng.Postesalescreateresponse postesalescreate(com.agilecontrol.wanxiangcheng.Postesalescreaterequest astr_request) throws java.rmi.RemoteException{
    if (esalesSoap == null)
      _initEsalesSoapProxy();
    return esalesSoap.postesalescreate(astr_request);
  }
  
  public com.agilecontrol.wanxiangcheng.Postsalesbonusadjustresponse postsalesbonusadjust(com.agilecontrol.wanxiangcheng.Postsalesbonusadjustrequest astr_request) throws java.rmi.RemoteException{
    if (esalesSoap == null)
      _initEsalesSoapProxy();
    return esalesSoap.postsalesbonusadjust(astr_request);
  }
  
  public com.agilecontrol.wanxiangcheng.Postvipbonuscreateresponse postvipbonuscreate(com.agilecontrol.wanxiangcheng.Postvipbonuscreaterequest astr_request) throws java.rmi.RemoteException{
    if (esalesSoap == null)
      _initEsalesSoapProxy();
    return esalesSoap.postvipbonuscreate(astr_request);
  }
  
  public com.agilecontrol.wanxiangcheng.Postsalesbonusaddresponse postsalesbonusadd(com.agilecontrol.wanxiangcheng.Postsalesbonusaddrequest astr_request) throws java.rmi.RemoteException{
    if (esalesSoap == null)
      _initEsalesSoapProxy();
    return esalesSoap.postsalesbonusadd(astr_request);
  }
  
  
}