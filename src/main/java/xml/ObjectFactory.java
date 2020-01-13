
package xml;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the xml package. 
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

    private final static QName _InterFaceFile_QNAME = new QName("", "InterFaceFile");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: xml
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link InterFaceFileType }
     * 
     */
    public InterFaceFileType createInterFaceFileType() {
        return new InterFaceFileType();
    }

    /**
     * Create an instance of {@link FileBodyType }
     * 
     */
    public FileBodyType createFileBodyType() {
        return new FileBodyType();
    }

    /**
     * Create an instance of {@link FileHeaderType }
     * 
     */
    public FileHeaderType createFileHeaderType() {
        return new FileHeaderType();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link InterFaceFileType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "InterFaceFile")
    public JAXBElement<InterFaceFileType> createInterFaceFile(InterFaceFileType value) {
        return new JAXBElement<InterFaceFileType>(_InterFaceFile_QNAME, InterFaceFileType.class, null, value);
    }

}
