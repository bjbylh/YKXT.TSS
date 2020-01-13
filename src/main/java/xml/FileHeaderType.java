
package xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>FileHeaderType complex type�� Java �ࡣ
 * 
 * <p>����ģʽƬ��ָ�������ڴ����е�Ԥ�����ݡ�
 * 
 * <pre>
 * &lt;complexType name="FileHeaderType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="messageType" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="messageID" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="originatorAddress" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="recipientAddress" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="creationTime" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "FileHeaderType", propOrder = {
    "messageType",
    "messageID",
    "originatorAddress",
    "recipientAddress",
    "creationTime"
})
public class FileHeaderType {

    @XmlElement(required = true)
    protected String messageType;
    @XmlElement(required = true)
    protected String messageID;
    @XmlElement(required = true)
    protected String originatorAddress;
    @XmlElement(required = true)
    protected String recipientAddress;
    @XmlElement(required = true)
    protected String creationTime;

    /**
     * ��ȡmessageType���Ե�ֵ��
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getMessageType() {
        return messageType;
    }

    /**
     * ����messageType���Ե�ֵ��
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setMessageType(String value) {
        this.messageType = value;
    }

    /**
     * ��ȡmessageID���Ե�ֵ��
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getMessageID() {
        return messageID;
    }

    /**
     * ����messageID���Ե�ֵ��
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setMessageID(String value) {
        this.messageID = value;
    }

    /**
     * ��ȡoriginatorAddress���Ե�ֵ��
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getOriginatorAddress() {
        return originatorAddress;
    }

    /**
     * ����originatorAddress���Ե�ֵ��
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setOriginatorAddress(String value) {
        this.originatorAddress = value;
    }

    /**
     * ��ȡrecipientAddress���Ե�ֵ��
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getRecipientAddress() {
        return recipientAddress;
    }

    /**
     * ����recipientAddress���Ե�ֵ��
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setRecipientAddress(String value) {
        this.recipientAddress = value;
    }

    /**
     * ��ȡcreationTime���Ե�ֵ��
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCreationTime() {
        return creationTime;
    }

    /**
     * ����creationTime���Ե�ֵ��
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCreationTime(String value) {
        this.creationTime = value;
    }

}
