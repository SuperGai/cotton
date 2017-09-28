/**
 * EsalesSoapStub.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.agilecontrol.wanxiangcheng;

public class EsalesSoapStub extends org.apache.axis.client.Stub implements com.agilecontrol.wanxiangcheng.EsalesSoap {
    private java.util.Vector cachedSerClasses = new java.util.Vector();
    private java.util.Vector cachedSerQNames = new java.util.Vector();
    private java.util.Vector cachedSerFactories = new java.util.Vector();
    private java.util.Vector cachedDeserFactories = new java.util.Vector();

    static org.apache.axis.description.OperationDesc [] _operations;

    static {
        _operations = new org.apache.axis.description.OperationDesc[7];
        _initOperationDesc1();
    }

    private static void _initOperationDesc1(){
        org.apache.axis.description.OperationDesc oper;
        org.apache.axis.description.ParameterDesc param;
        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("getversion");
        oper.setReturnType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        oper.setReturnClass(java.lang.String.class);
        oper.setReturnQName(new javax.xml.namespace.QName("http://tempurl.org", "getversionResult"));
        oper.setStyle(org.apache.axis.constants.Style.WRAPPED);
        oper.setUse(org.apache.axis.constants.Use.LITERAL);
        _operations[0] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("getcompanyname");
        oper.setReturnType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        oper.setReturnClass(java.lang.String.class);
        oper.setReturnQName(new javax.xml.namespace.QName("http://tempurl.org", "getcompanynameResult"));
        oper.setStyle(org.apache.axis.constants.Style.WRAPPED);
        oper.setUse(org.apache.axis.constants.Use.LITERAL);
        _operations[1] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("postposconnectcreate");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://tempurl.org", "astr_request"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://tempurl.org", "postposconnectcreaterequest"), com.agilecontrol.wanxiangcheng.Postposconnectcreaterequest.class, false, false);
        param.setOmittable(true);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://tempurl.org", "postposconnectcreateresponse"));
        oper.setReturnClass(com.agilecontrol.wanxiangcheng.Postposconnectcreateresponse.class);
        oper.setReturnQName(new javax.xml.namespace.QName("http://tempurl.org", "postposconnectcreateResult"));
        oper.setStyle(org.apache.axis.constants.Style.WRAPPED);
        oper.setUse(org.apache.axis.constants.Use.LITERAL);
        _operations[2] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("postesalescreate");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://tempurl.org", "astr_request"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://tempurl.org", "postesalescreaterequest"), com.agilecontrol.wanxiangcheng.Postesalescreaterequest.class, false, false);
        param.setOmittable(true);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://tempurl.org", "postesalescreateresponse"));
        oper.setReturnClass(com.agilecontrol.wanxiangcheng.Postesalescreateresponse.class);
        oper.setReturnQName(new javax.xml.namespace.QName("http://tempurl.org", "postesalescreateResult"));
        oper.setStyle(org.apache.axis.constants.Style.WRAPPED);
        oper.setUse(org.apache.axis.constants.Use.LITERAL);
        _operations[3] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("postsalesbonusadjust");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://tempurl.org", "astr_request"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://tempurl.org", "postsalesbonusadjustrequest"), com.agilecontrol.wanxiangcheng.Postsalesbonusadjustrequest.class, false, false);
        param.setOmittable(true);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://tempurl.org", "postsalesbonusadjustresponse"));
        oper.setReturnClass(com.agilecontrol.wanxiangcheng.Postsalesbonusadjustresponse.class);
        oper.setReturnQName(new javax.xml.namespace.QName("http://tempurl.org", "postsalesbonusadjustResult"));
        oper.setStyle(org.apache.axis.constants.Style.WRAPPED);
        oper.setUse(org.apache.axis.constants.Use.LITERAL);
        _operations[4] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("postvipbonuscreate");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://tempurl.org", "astr_request"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://tempurl.org", "postvipbonuscreaterequest"), com.agilecontrol.wanxiangcheng.Postvipbonuscreaterequest.class, false, false);
        param.setOmittable(true);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://tempurl.org", "postvipbonuscreateresponse"));
        oper.setReturnClass(com.agilecontrol.wanxiangcheng.Postvipbonuscreateresponse.class);
        oper.setReturnQName(new javax.xml.namespace.QName("http://tempurl.org", "postvipbonuscreateResult"));
        oper.setStyle(org.apache.axis.constants.Style.WRAPPED);
        oper.setUse(org.apache.axis.constants.Use.LITERAL);
        _operations[5] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("postsalesbonusadd");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://tempurl.org", "astr_request"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://tempurl.org", "postsalesbonusaddrequest"), com.agilecontrol.wanxiangcheng.Postsalesbonusaddrequest.class, false, false);
        param.setOmittable(true);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://tempurl.org", "postsalesbonusaddresponse"));
        oper.setReturnClass(com.agilecontrol.wanxiangcheng.Postsalesbonusaddresponse.class);
        oper.setReturnQName(new javax.xml.namespace.QName("http://tempurl.org", "postsalesbonusaddResult"));
        oper.setStyle(org.apache.axis.constants.Style.WRAPPED);
        oper.setUse(org.apache.axis.constants.Use.LITERAL);
        _operations[6] = oper;

    }

    public EsalesSoapStub() throws org.apache.axis.AxisFault {
         this(null);
    }

    public EsalesSoapStub(java.net.URL endpointURL, javax.xml.rpc.Service service) throws org.apache.axis.AxisFault {
         this(service);
         super.cachedEndpoint = endpointURL;
    }

    public EsalesSoapStub(javax.xml.rpc.Service service) throws org.apache.axis.AxisFault {
        if (service == null) {
            super.service = new org.apache.axis.client.Service();
        } else {
            super.service = service;
        }
        ((org.apache.axis.client.Service)super.service).setTypeMappingVersion("1.2");
            java.lang.Class cls;
            javax.xml.namespace.QName qName;
            javax.xml.namespace.QName qName2;
            java.lang.Class beansf = org.apache.axis.encoding.ser.BeanSerializerFactory.class;
            java.lang.Class beandf = org.apache.axis.encoding.ser.BeanDeserializerFactory.class;
            java.lang.Class enumsf = org.apache.axis.encoding.ser.EnumSerializerFactory.class;
            java.lang.Class enumdf = org.apache.axis.encoding.ser.EnumDeserializerFactory.class;
            java.lang.Class arraysf = org.apache.axis.encoding.ser.ArraySerializerFactory.class;
            java.lang.Class arraydf = org.apache.axis.encoding.ser.ArrayDeserializerFactory.class;
            java.lang.Class simplesf = org.apache.axis.encoding.ser.SimpleSerializerFactory.class;
            java.lang.Class simpledf = org.apache.axis.encoding.ser.SimpleDeserializerFactory.class;
            java.lang.Class simplelistsf = org.apache.axis.encoding.ser.SimpleListSerializerFactory.class;
            java.lang.Class simplelistdf = org.apache.axis.encoding.ser.SimpleListDeserializerFactory.class;
            qName = new javax.xml.namespace.QName("http://tempurl.org", "ArrayOfEsalesitem");
            cachedSerQNames.add(qName);
            cls = com.agilecontrol.wanxiangcheng.Esalesitem[].class;
            cachedSerClasses.add(cls);
            qName = new javax.xml.namespace.QName("http://tempurl.org", "esalesitem");
            qName2 = new javax.xml.namespace.QName("http://tempurl.org", "esalesitem");
            cachedSerFactories.add(new org.apache.axis.encoding.ser.ArraySerializerFactory(qName, qName2));
            cachedDeserFactories.add(new org.apache.axis.encoding.ser.ArrayDeserializerFactory());

            qName = new javax.xml.namespace.QName("http://tempurl.org", "ArrayOfEsalestender");
            cachedSerQNames.add(qName);
            cls = com.agilecontrol.wanxiangcheng.Esalestender[].class;
            cachedSerClasses.add(cls);
            qName = new javax.xml.namespace.QName("http://tempurl.org", "esalestender");
            qName2 = new javax.xml.namespace.QName("http://tempurl.org", "esalestender");
            cachedSerFactories.add(new org.apache.axis.encoding.ser.ArraySerializerFactory(qName, qName2));
            cachedDeserFactories.add(new org.apache.axis.encoding.ser.ArrayDeserializerFactory());

            qName = new javax.xml.namespace.QName("http://tempurl.org", "ArrayOfPosconnectdtl");
            cachedSerQNames.add(qName);
            cls = com.agilecontrol.wanxiangcheng.Posconnectdtl[].class;
            cachedSerClasses.add(cls);
            qName = new javax.xml.namespace.QName("http://tempurl.org", "posconnectdtl");
            qName2 = new javax.xml.namespace.QName("http://tempurl.org", "posconnectdtl");
            cachedSerFactories.add(new org.apache.axis.encoding.ser.ArraySerializerFactory(qName, qName2));
            cachedDeserFactories.add(new org.apache.axis.encoding.ser.ArrayDeserializerFactory());

            qName = new javax.xml.namespace.QName("http://tempurl.org", "esaleshdr");
            cachedSerQNames.add(qName);
            cls = com.agilecontrol.wanxiangcheng.Esaleshdr.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://tempurl.org", "esalesitem");
            cachedSerQNames.add(qName);
            cls = com.agilecontrol.wanxiangcheng.Esalesitem.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://tempurl.org", "esalestender");
            cachedSerQNames.add(qName);
            cls = com.agilecontrol.wanxiangcheng.Esalestender.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://tempurl.org", "posconnectdtl");
            cachedSerQNames.add(qName);
            cls = com.agilecontrol.wanxiangcheng.Posconnectdtl.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://tempurl.org", "posconnecthdr");
            cachedSerQNames.add(qName);
            cls = com.agilecontrol.wanxiangcheng.Posconnecthdr.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://tempurl.org", "postesalescreaterequest");
            cachedSerQNames.add(qName);
            cls = com.agilecontrol.wanxiangcheng.Postesalescreaterequest.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://tempurl.org", "postesalescreateresponse");
            cachedSerQNames.add(qName);
            cls = com.agilecontrol.wanxiangcheng.Postesalescreateresponse.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://tempurl.org", "postposconnectcreaterequest");
            cachedSerQNames.add(qName);
            cls = com.agilecontrol.wanxiangcheng.Postposconnectcreaterequest.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://tempurl.org", "postposconnectcreateresponse");
            cachedSerQNames.add(qName);
            cls = com.agilecontrol.wanxiangcheng.Postposconnectcreateresponse.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://tempurl.org", "postsalesbonusaddrequest");
            cachedSerQNames.add(qName);
            cls = com.agilecontrol.wanxiangcheng.Postsalesbonusaddrequest.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://tempurl.org", "postsalesbonusaddresponse");
            cachedSerQNames.add(qName);
            cls = com.agilecontrol.wanxiangcheng.Postsalesbonusaddresponse.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://tempurl.org", "postsalesbonusadjustrequest");
            cachedSerQNames.add(qName);
            cls = com.agilecontrol.wanxiangcheng.Postsalesbonusadjustrequest.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://tempurl.org", "postsalesbonusadjustresponse");
            cachedSerQNames.add(qName);
            cls = com.agilecontrol.wanxiangcheng.Postsalesbonusadjustresponse.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://tempurl.org", "postvipbonuscreaterequest");
            cachedSerQNames.add(qName);
            cls = com.agilecontrol.wanxiangcheng.Postvipbonuscreaterequest.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://tempurl.org", "postvipbonuscreateresponse");
            cachedSerQNames.add(qName);
            cls = com.agilecontrol.wanxiangcheng.Postvipbonuscreateresponse.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://tempurl.org", "requestheader");
            cachedSerQNames.add(qName);
            cls = com.agilecontrol.wanxiangcheng.Requestheader.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://tempurl.org", "responseheader");
            cachedSerQNames.add(qName);
            cls = com.agilecontrol.wanxiangcheng.Responseheader.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://tempurl.org", "salesbonusadd");
            cachedSerQNames.add(qName);
            cls = com.agilecontrol.wanxiangcheng.Salesbonusadd.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://tempurl.org", "salesbonusadjust");
            cachedSerQNames.add(qName);
            cls = com.agilecontrol.wanxiangcheng.Salesbonusadjust.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://tempurl.org", "vipbonuscreate");
            cachedSerQNames.add(qName);
            cls = com.agilecontrol.wanxiangcheng.Vipbonuscreate.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

    }

    protected org.apache.axis.client.Call createCall() throws java.rmi.RemoteException {
        try {
            org.apache.axis.client.Call _call = super._createCall();
            if (super.maintainSessionSet) {
                _call.setMaintainSession(super.maintainSession);
            }
            if (super.cachedUsername != null) {
                _call.setUsername(super.cachedUsername);
            }
            if (super.cachedPassword != null) {
                _call.setPassword(super.cachedPassword);
            }
            if (super.cachedEndpoint != null) {
                _call.setTargetEndpointAddress(super.cachedEndpoint);
            }
            if (super.cachedTimeout != null) {
                _call.setTimeout(super.cachedTimeout);
            }
            if (super.cachedPortName != null) {
                _call.setPortName(super.cachedPortName);
            }
            java.util.Enumeration keys = super.cachedProperties.keys();
            while (keys.hasMoreElements()) {
                java.lang.String key = (java.lang.String) keys.nextElement();
                _call.setProperty(key, super.cachedProperties.get(key));
            }
            // All the type mapping information is registered
            // when the first call is made.
            // The type mapping information is actually registered in
            // the TypeMappingRegistry of the service, which
            // is the reason why registration is only needed for the first call.
            synchronized (this) {
                if (firstCall()) {
                    // must set encoding style before registering serializers
                    _call.setEncodingStyle(null);
                    for (int i = 0; i < cachedSerFactories.size(); ++i) {
                        java.lang.Class cls = (java.lang.Class) cachedSerClasses.get(i);
                        javax.xml.namespace.QName qName =
                                (javax.xml.namespace.QName) cachedSerQNames.get(i);
                        java.lang.Object x = cachedSerFactories.get(i);
                        if (x instanceof Class) {
                            java.lang.Class sf = (java.lang.Class)
                                 cachedSerFactories.get(i);
                            java.lang.Class df = (java.lang.Class)
                                 cachedDeserFactories.get(i);
                            _call.registerTypeMapping(cls, qName, sf, df, false);
                        }
                        else if (x instanceof javax.xml.rpc.encoding.SerializerFactory) {
                            org.apache.axis.encoding.SerializerFactory sf = (org.apache.axis.encoding.SerializerFactory)
                                 cachedSerFactories.get(i);
                            org.apache.axis.encoding.DeserializerFactory df = (org.apache.axis.encoding.DeserializerFactory)
                                 cachedDeserFactories.get(i);
                            _call.registerTypeMapping(cls, qName, sf, df, false);
                        }
                    }
                }
            }
            return _call;
        }
        catch (java.lang.Throwable _t) {
            throw new org.apache.axis.AxisFault("Failure trying to get the Call object", _t);
        }
    }

    public java.lang.String getversion() throws java.rmi.RemoteException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[0]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("http://tempurl.org/getversion");
        _call.setEncodingStyle(null);
        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://tempurl.org", "getversion"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            extractAttachments(_call);
            try {
                return (java.lang.String) _resp;
            } catch (java.lang.Exception _exception) {
                return (java.lang.String) org.apache.axis.utils.JavaUtils.convert(_resp, java.lang.String.class);
            }
        }
  } catch (org.apache.axis.AxisFault axisFaultException) {
  throw axisFaultException;
}
    }

    public java.lang.String getcompanyname() throws java.rmi.RemoteException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[1]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("http://tempurl.org/getcompanyname");
        _call.setEncodingStyle(null);
        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://tempurl.org", "getcompanyname"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            extractAttachments(_call);
            try {
                return (java.lang.String) _resp;
            } catch (java.lang.Exception _exception) {
                return (java.lang.String) org.apache.axis.utils.JavaUtils.convert(_resp, java.lang.String.class);
            }
        }
  } catch (org.apache.axis.AxisFault axisFaultException) {
  throw axisFaultException;
}
    }

    public com.agilecontrol.wanxiangcheng.Postposconnectcreateresponse postposconnectcreate(com.agilecontrol.wanxiangcheng.Postposconnectcreaterequest astr_request) throws java.rmi.RemoteException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[2]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("http://tempurl.org/postposconnectcreate");
        _call.setEncodingStyle(null);
        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://tempurl.org", "postposconnectcreate"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {astr_request});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            extractAttachments(_call);
            try {
                return (com.agilecontrol.wanxiangcheng.Postposconnectcreateresponse) _resp;
            } catch (java.lang.Exception _exception) {
                return (com.agilecontrol.wanxiangcheng.Postposconnectcreateresponse) org.apache.axis.utils.JavaUtils.convert(_resp, com.agilecontrol.wanxiangcheng.Postposconnectcreateresponse.class);
            }
        }
  } catch (org.apache.axis.AxisFault axisFaultException) {
  throw axisFaultException;
}
    }

    public com.agilecontrol.wanxiangcheng.Postesalescreateresponse postesalescreate(com.agilecontrol.wanxiangcheng.Postesalescreaterequest astr_request) throws java.rmi.RemoteException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[3]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("http://tempurl.org/postesalescreate");
        _call.setEncodingStyle(null);
        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://tempurl.org", "postesalescreate"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {astr_request});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            extractAttachments(_call);
            try {
                return (com.agilecontrol.wanxiangcheng.Postesalescreateresponse) _resp;
            } catch (java.lang.Exception _exception) {
                return (com.agilecontrol.wanxiangcheng.Postesalescreateresponse) org.apache.axis.utils.JavaUtils.convert(_resp, com.agilecontrol.wanxiangcheng.Postesalescreateresponse.class);
            }
        }
  } catch (org.apache.axis.AxisFault axisFaultException) {
  throw axisFaultException;
}
    }

    public com.agilecontrol.wanxiangcheng.Postsalesbonusadjustresponse postsalesbonusadjust(com.agilecontrol.wanxiangcheng.Postsalesbonusadjustrequest astr_request) throws java.rmi.RemoteException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[4]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("http://tempurl.org/postsalesbonusadjust");
        _call.setEncodingStyle(null);
        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://tempurl.org", "postsalesbonusadjust"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {astr_request});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            extractAttachments(_call);
            try {
                return (com.agilecontrol.wanxiangcheng.Postsalesbonusadjustresponse) _resp;
            } catch (java.lang.Exception _exception) {
                return (com.agilecontrol.wanxiangcheng.Postsalesbonusadjustresponse) org.apache.axis.utils.JavaUtils.convert(_resp, com.agilecontrol.wanxiangcheng.Postsalesbonusadjustresponse.class);
            }
        }
  } catch (org.apache.axis.AxisFault axisFaultException) {
  throw axisFaultException;
}
    }

    public com.agilecontrol.wanxiangcheng.Postvipbonuscreateresponse postvipbonuscreate(com.agilecontrol.wanxiangcheng.Postvipbonuscreaterequest astr_request) throws java.rmi.RemoteException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[5]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("http://tempurl.org/postvipbonuscreate");
        _call.setEncodingStyle(null);
        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://tempurl.org", "postvipbonuscreate"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {astr_request});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            extractAttachments(_call);
            try {
                return (com.agilecontrol.wanxiangcheng.Postvipbonuscreateresponse) _resp;
            } catch (java.lang.Exception _exception) {
                return (com.agilecontrol.wanxiangcheng.Postvipbonuscreateresponse) org.apache.axis.utils.JavaUtils.convert(_resp, com.agilecontrol.wanxiangcheng.Postvipbonuscreateresponse.class);
            }
        }
  } catch (org.apache.axis.AxisFault axisFaultException) {
  throw axisFaultException;
}
    }

    public com.agilecontrol.wanxiangcheng.Postsalesbonusaddresponse postsalesbonusadd(com.agilecontrol.wanxiangcheng.Postsalesbonusaddrequest astr_request) throws java.rmi.RemoteException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[6]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("http://tempurl.org/postsalesbonusadd");
        _call.setEncodingStyle(null);
        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://tempurl.org", "postsalesbonusadd"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {astr_request});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            extractAttachments(_call);
            try {
                return (com.agilecontrol.wanxiangcheng.Postsalesbonusaddresponse) _resp;
            } catch (java.lang.Exception _exception) {
                return (com.agilecontrol.wanxiangcheng.Postsalesbonusaddresponse) org.apache.axis.utils.JavaUtils.convert(_resp, com.agilecontrol.wanxiangcheng.Postsalesbonusaddresponse.class);
            }
        }
  } catch (org.apache.axis.AxisFault axisFaultException) {
  throw axisFaultException;
}
    }

}
