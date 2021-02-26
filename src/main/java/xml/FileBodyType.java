
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
 *         &lt;element name="sensorType" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="receptionType1" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="receptionType2" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="receiveStartTime" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="receiveStopTime" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="satelliteCaptureStartTime" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="satelliteCaptureStopTime" type="{http://www.w3.org/2001/XMLSchema}string"/>
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
    "sensorType",
    "receptionType1",
    "receptionType2",
    "receiveStartTime",
    "receiveStopTime",
    "satelliteCaptureStartTime",
    "satelliteCaptureStopTime"
})
public class FileBodyType {

    @XmlElement(required = true)
    protected String trPlanID;
    @XmlElement(required = true)
    protected String satellite;
    @XmlElement(required = true)
    protected String sensorType;
    @XmlElement(required = true)
    protected String receptionType1;
    @XmlElement(required = true)
    protected String receptionType2;
    @XmlElement(required = true)
    protected String receiveStartTime;
    @XmlElement(required = true)
    protected String receiveStopTime;
    @XmlElement(required = true)
    protected String satelliteCaptureStartTime;
    @XmlElement(required = true)
    protected String satelliteCaptureStopTime;

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
     * 获取receptionType1属性的值。
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getReceptionType1() {
        return receptionType1;
    }

    /**
     * 设置receptionType1属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setReceptionType1(String value) {
        this.receptionType1 = value;
    }

    /**
     * 获取receptionType2属性的值。
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getReceptionType2() {
        return receptionType2;
    }

    /**
     * 设置receptionType2属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setReceptionType2(String value) {
        this.receptionType2 = value;
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

}
