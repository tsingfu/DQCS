//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vhudson-jaxb-ri-2.1-833 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2019.11.12 at 04:01:21 下午 CST 
//


package org.datacleaner.monitor.pentaho.jaxb;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the org.datacleaner.monitor.pentaho.jaxb package. 
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

    private final static QName _PentahoJob_QNAME = new QName("http://eobjects.org/datacleaner/pentaho-job/1.0", "pentaho-job");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: org.datacleaner.monitor.pentaho.jaxb
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link PentahoJobType }
     * 
     */
    public PentahoJobType createPentahoJobType() {
        return new PentahoJobType();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link PentahoJobType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://eobjects.org/datacleaner/pentaho-job/1.0", name = "pentaho-job")
    public JAXBElement<PentahoJobType> createPentahoJob(PentahoJobType value) {
        return new JAXBElement<PentahoJobType>(_PentahoJob_QNAME, PentahoJobType.class, null, value);
    }

}
