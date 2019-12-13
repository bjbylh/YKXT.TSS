//
// ���ļ����� JavaTM Architecture for XML Binding (JAXB) ����ʵ�� v2.2.8-b130911.1802 ���ɵ�
// ����� <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// �����±���Դģʽʱ, �Դ��ļ��������޸Ķ�����ʧ��
// ����ʱ��: 2019.12.13 ʱ�� 09:28:22 AM CST 
//


package com.cast.wss.client;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.util.List;


/**
 * <p>dtplanType complex type�� Java �ࡣ
 * 
 * <p>����ģʽƬ��ָ�������ڴ����е�Ԥ�����ݡ�
 * 
 * <pre>
 * &lt;complexType name="dtplanType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="head" type="{}headType"/>
 *         &lt;element name="plan" type="{}planType"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "dtplanType", propOrder = {
    "head",
    "plan"
})
public class DtplanType {

    @XmlElement(required = true)
    protected HeadType head;
    @XmlElement(required = true)
    protected PlanType plan;

    /**
     * ��ȡhead���Ե�ֵ��
     * 
     * @return
     *     possible object is
     *     {@link HeadType }
     *     
     */
    public HeadType getHead() {
        return head;
    }

    /**
     * ����head���Ե�ֵ��
     * 
     * @param value
     *     allowed object is
     *     {@link HeadType }
     *     
     */
    public void setHead(HeadType value) {
        this.head = value;
    }

    /**
     * ��ȡplan���Ե�ֵ��
     * 
     * @return
     *     possible object is
     *     {@link PlanType }
     *     
     */
    public PlanType getPlan() {
        return plan;
    }

    /**
     * ����plan���Ե�ֵ��
     * 
     * @param value
     *     allowed object is
     *     {@link PlanType }
     *     
     */
    public void setPlan(PlanType value) {
        this.plan = value;
    }

    public String toString(){
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<?xml version='1.0' encoding=\"gb2312\" ?>");
        stringBuilder.append("<dtplan>");
        stringBuilder.append("<head>").append("<creationTime>").append(getHead().getCreationTime()).append("</creationTime>").append("</head>");
        stringBuilder.append("<plan>");

        List<MissionType> mts =  getPlan().mission;
        for(MissionType missionType : mts){
            stringBuilder.append("<mission>");

            stringBuilder.append("<satelliteID>").append(missionType.getSatelliteID()).append("</satelliteID>");
            stringBuilder.append("<stationID>").append(missionType.getStationID()).append("</stationID>");
            stringBuilder.append("<tplanID>").append(missionType.getTplanID()).append("</tplanID>");
            stringBuilder.append("<startTime>").append(missionType.getStartTime()).append("</startTime>");
            stringBuilder.append("<endTime>").append(missionType.getEndTime()).append("</endTime>");

            stringBuilder.append("</mission>");
        }

        stringBuilder.append("</plan>").append("</dtplan>");

        return stringBuilder.toString();
    }

}
