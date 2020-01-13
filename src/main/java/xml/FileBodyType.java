
package xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>FileBodyType complex type的 Java 类。
 * 
 * <p>以下模式片段指定包含在此类中的预期内容。
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
     * 获取trPlanID属性的值。
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
     * 设置trPlanID属性的值。
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
     * 获取satellite属性的值。
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
     * 设置satellite属性的值。
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
     * 获取tmType属性的值。
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
     * 设置tmType属性的值。
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
     * 获取sensorType属性的值。
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
     * 设置sensorType属性的值。
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
     * 获取downlinkChannel属性的值。
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
     * 设置downlinkChannel属性的值。
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
     * 获取receptionType属性的值。
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
     * 设置receptionType属性的值。
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
     * 获取orbitID属性的值。
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
     * 设置orbitID属性的值。
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
     * 获取isQuickView属性的值。
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
     * 设置isQuickView属性的值。
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
     * 获取isCloud属性的值。
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
     * 设置isCloud属性的值。
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
     * 获取receiveStartTime属性的值。
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
     * 设置receiveStartTime属性的值。
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
     * 获取receiveStopTime属性的值。
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
     * 设置receiveStopTime属性的值。
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
     * 获取satelliteCaptureStartTime属性的值。
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
     * 设置satelliteCaptureStartTime属性的值。
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
     * 获取satelliteCaptureStopTime属性的值。
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
     * 设置satelliteCaptureStopTime属性的值。
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
     * 获取taskCount属性的值。
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
     * 设置taskCount属性的值。
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
