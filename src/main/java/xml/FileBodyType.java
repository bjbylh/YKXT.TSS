
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
     * ��ȡreceptionType1���Ե�ֵ��
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
     * ����receptionType1���Ե�ֵ��
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
     * ��ȡreceptionType2���Ե�ֵ��
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
     * ����receptionType2���Ե�ֵ��
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

}
