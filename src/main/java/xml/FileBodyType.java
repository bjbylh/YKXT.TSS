
package xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>FileBodyType complex type�� Java �ࡣ
 * 
 * <p>����ģʽƬ��ָ�������ڴ����е�Ԥ�����ݡ�
 * 
 * <pre>
 * &lt;complexType name="FileBodyType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="trPlanID" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="satellite" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="tmType" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="sensorType" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="downlinkChannel" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="receptionType" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="orbitID" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="isQuickView" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="isCloud" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="receiveStartTime" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="receiveStopTime" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="satelliteCaptureStartTime" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="satelliteCaptureStopTime" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="taskCount" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "FileBodyType", propOrder = {
    "trPlanID",
    "satellite",
    "tmType",
    "sensorType",
    "downlinkChannel",
    "receptionType",
    "orbitID",
    "isQuickView",
    "isCloud",
    "receiveStartTime",
    "receiveStopTime",
    "satelliteCaptureStartTime",
    "satelliteCaptureStopTime",
    "taskCount"
})
public class FileBodyType {

    @XmlElement(required = true)
    protected String trPlanID;
    @XmlElement(required = true)
    protected String satellite;
    @XmlElement(required = true)
    protected String tmType;
    @XmlElement(required = true)
    protected String sensorType;
    @XmlElement(required = true)
    protected String downlinkChannel;
    @XmlElement(required = true)
    protected String receptionType;
    @XmlElement(required = true)
    protected String orbitID;
    @XmlElement(required = true)
    protected String isQuickView;
    @XmlElement(required = true)
    protected String isCloud;
    @XmlElement(required = true)
    protected String receiveStartTime;
    @XmlElement(required = true)
    protected String receiveStopTime;
    @XmlElement(required = true)
    protected String satelliteCaptureStartTime;
    @XmlElement(required = true)
    protected String satelliteCaptureStopTime;
    @XmlElement(required = true)
    protected String taskCount;

    /**
     * ��ȡtrPlanID���Ե�ֵ��
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getTrPlanID() {
        return trPlanID;
    }

    /**
     * ����trPlanID���Ե�ֵ��
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setTrPlanID(String value) {
        this.trPlanID = value;
    }

    /**
     * ��ȡsatellite���Ե�ֵ��
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSatellite() {
        return satellite;
    }

    /**
     * ����satellite���Ե�ֵ��
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSatellite(String value) {
        this.satellite = value;
    }

    /**
     * ��ȡtmType���Ե�ֵ��
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getTmType() {
        return tmType;
    }

    /**
     * ����tmType���Ե�ֵ��
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setTmType(String value) {
        this.tmType = value;
    }

    /**
     * ��ȡsensorType���Ե�ֵ��
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSensorType() {
        return sensorType;
    }

    /**
     * ����sensorType���Ե�ֵ��
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSensorType(String value) {
        this.sensorType = value;
    }

    /**
     * ��ȡdownlinkChannel���Ե�ֵ��
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDownlinkChannel() {
        return downlinkChannel;
    }

    /**
     * ����downlinkChannel���Ե�ֵ��
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDownlinkChannel(String value) {
        this.downlinkChannel = value;
    }

    /**
     * ��ȡreceptionType���Ե�ֵ��
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getReceptionType() {
        return receptionType;
    }

    /**
     * ����receptionType���Ե�ֵ��
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setReceptionType(String value) {
        this.receptionType = value;
    }

    /**
     * ��ȡorbitID���Ե�ֵ��
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getOrbitID() {
        return orbitID;
    }

    /**
     * ����orbitID���Ե�ֵ��
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setOrbitID(String value) {
        this.orbitID = value;
    }

    /**
     * ��ȡisQuickView���Ե�ֵ��
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getIsQuickView() {
        return isQuickView;
    }

    /**
     * ����isQuickView���Ե�ֵ��
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setIsQuickView(String value) {
        this.isQuickView = value;
    }

    /**
     * ��ȡisCloud���Ե�ֵ��
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getIsCloud() {
        return isCloud;
    }

    /**
     * ����isCloud���Ե�ֵ��
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setIsCloud(String value) {
        this.isCloud = value;
    }

    /**
     * ��ȡreceiveStartTime���Ե�ֵ��
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getReceiveStartTime() {
        return receiveStartTime;
    }

    /**
     * ����receiveStartTime���Ե�ֵ��
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setReceiveStartTime(String value) {
        this.receiveStartTime = value;
    }

    /**
     * ��ȡreceiveStopTime���Ե�ֵ��
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getReceiveStopTime() {
        return receiveStopTime;
    }

    /**
     * ����receiveStopTime���Ե�ֵ��
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setReceiveStopTime(String value) {
        this.receiveStopTime = value;
    }

    /**
     * ��ȡsatelliteCaptureStartTime���Ե�ֵ��
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSatelliteCaptureStartTime() {
        return satelliteCaptureStartTime;
    }

    /**
     * ����satelliteCaptureStartTime���Ե�ֵ��
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSatelliteCaptureStartTime(String value) {
        this.satelliteCaptureStartTime = value;
    }

    /**
     * ��ȡsatelliteCaptureStopTime���Ե�ֵ��
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSatelliteCaptureStopTime() {
        return satelliteCaptureStopTime;
    }

    /**
     * ����satelliteCaptureStopTime���Ե�ֵ��
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSatelliteCaptureStopTime(String value) {
        this.satelliteCaptureStopTime = value;
    }

    /**
     * ��ȡtaskCount���Ե�ֵ��
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getTaskCount() {
        return taskCount;
    }

    /**
     * ����taskCount���Ե�ֵ��
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setTaskCount(String value) {
        this.taskCount = value;
    }

}
