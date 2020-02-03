package xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>InterFaceFileType complex type�� Java �ࡣ
 * <p>
 * <p>����ģʽƬ��ָ�������ڴ����е�Ԥ�����ݡ�
 * <p>
 * <pre>
 * &lt;complexType name="InterFaceFileType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="FileHeader" type="{}FileHeaderType"/>
 *         &lt;element name="FileBody" type="{}FileBodyType"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "InterFaceFileType", propOrder = {
        "fileHeader",
        "fileBody"
})
public class InterFaceFileType {

    @XmlElement(name = "FileHeader", required = true)
    protected FileHeaderType fileHeader;
    @XmlElement(name = "FileBody", required = true)
    protected FileBodyType fileBody;

    /**
     * ��ȡfileHeader���Ե�ֵ��
     *
     * @return possible object is
     * {@link FileHeaderType }
     */
    public FileHeaderType getFileHeader() {
        return fileHeader;
    }

    /**
     * ����fileHeader���Ե�ֵ��
     *
     * @param value allowed object is
     *              {@link FileHeaderType }
     */
    public void setFileHeader(FileHeaderType value) {
        this.fileHeader = value;
    }

    /**
     * ��ȡfileBody���Ե�ֵ��
     *
     * @return possible object is
     * {@link FileBodyType }
     */
    public FileBodyType getFileBody() {
        return fileBody;
    }

    /**
     * ����fileBody���Ե�ֵ��
     *
     * @param value allowed object is
     *              {@link FileBodyType }
     */
    public void setFileBody(FileBodyType value) {
        this.fileBody = value;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        sb.append("<InterFaceFile>");

        sb.append("<FileHeader>");

        sb.append("<messageType>").append(fileHeader.getMessageType()).append("</messageType>");
        sb.append("<messageID>").append(fileHeader.getMessageID()).append("</messageID>");
        sb.append("<originatorAddress>").append(fileHeader.getOriginatorAddress()).append("</originatorAddress>");
        sb.append("<recipientAddress>").append(fileHeader.getRecipientAddress()).append("</recipientAddress>");
        sb.append("<creationTime>").append(fileHeader.getCreationTime()).append("</creationTime>");

        sb.append("</FileHeader>");

        sb.append("<FileBody>");

        sb.append("<trPlanID>").append(fileBody.getTrPlanID()).append("</trPlanID>");
        sb.append("<satellite>").append(fileBody.getSatellite()).append("</satellite>");
        sb.append("<tmType>").append(fileBody.getTmType()).append("</tmType>");
        sb.append("<sensorType>").append(fileBody.getSensorType()).append("</sensorType>");
        sb.append("<downlinkChannel>").append(fileBody.getDownlinkChannel()).append("</downlinkChannel>");
        sb.append("<receptionType>").append(fileBody.getReceptionType()).append("</receptionType>");
        sb.append("<orbitID>").append(fileBody.getOrbitID()).append("</orbitID>");
        sb.append("<isQuickView>").append(fileBody.getIsQuickView()).append("</isQuickView>");
        sb.append("<isCloud>").append(fileBody.getIsCloud()).append("</isCloud>");
        sb.append("<receiveStartTime>").append(fileBody.getReceiveStartTime()).append("</receiveStartTime>");
        sb.append("<receiveStopTime>").append(fileBody.getReceiveStopTime()).append("</receiveStopTime>");
        sb.append("<satelliteCaptureStartTime>").append(fileBody.getSatelliteCaptureStartTime()).append("</satelliteCaptureStartTime>");
        sb.append("<satelliteCaptureStopTime>").append(fileBody.getSatelliteCaptureStopTime()).append("</satelliteCaptureStopTime>");
        sb.append("<taskCount>").append(fileBody.getTaskCount()).append("</taskCount>");

        sb.append("</FileBody>");

        sb.append("</InterFaceFile>");

        return sb.toString();
    }

}
