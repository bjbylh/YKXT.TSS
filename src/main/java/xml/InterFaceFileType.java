
package xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>InterFaceFileType complex type的 Java 类。
 * 
 * <p>以下模式片段指定包含在此类中的预期内容。
 * 
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
 * 
 * 
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
     * 获取fileHeader属性的值。
     * 
     * @return
     *     possible object is
     *     {@link FileHeaderType }
     *     
     */
    public FileHeaderType getFileHeader() {
        return fileHeader;
    }

    /**
     * 设置fileHeader属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link FileHeaderType }
     *     
     */
    public void setFileHeader(FileHeaderType value) {
        this.fileHeader = value;
    }

    /**
     * 获取fileBody属性的值。
     * 
     * @return
     *     possible object is
     *     {@link FileBodyType }
     *     
     */
    public FileBodyType getFileBody() {
        return fileBody;
    }

    /**
     * 设置fileBody属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link FileBodyType }
     *     
     */
    public void setFileBody(FileBodyType value) {
        this.fileBody = value;
    }

}
