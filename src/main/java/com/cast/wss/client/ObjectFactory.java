//
// 此文件是由 JavaTM Architecture for XML Binding (JAXB) 引用实现 v2.2.8-b130911.1802 生成的
// 请访问 <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// 在重新编译源模式时, 对此文件的所有修改都将丢失。
// 生成时间: 2019.12.13 时间 09:28:22 AM CST 
//


package com.cast.wss.client;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the com.cast.wss.client package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _Dtplan_QNAME = new QName("", "dtplan");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: com.cast.wss.client
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link DtplanType }
     * 
     */
    public DtplanType createDtplanType() {
        return new DtplanType();
    }

    /**
     * Create an instance of {@link PlanType }
     * 
     */
    public PlanType createPlanType() {
        return new PlanType();
    }

    /**
     * Create an instance of {@link HeadType }
     * 
     */
    public HeadType createHeadType() {
        return new HeadType();
    }

    /**
     * Create an instance of {@link MissionType }
     * 
     */
    public MissionType createMissionType() {
        return new MissionType();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link DtplanType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "dtplan")
    public JAXBElement<DtplanType> createDtplan(DtplanType value) {
        return new JAXBElement<DtplanType>(_Dtplan_QNAME, DtplanType.class, null, value);
    }

}
