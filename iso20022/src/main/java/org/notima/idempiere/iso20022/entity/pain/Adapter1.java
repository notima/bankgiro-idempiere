//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vJAXB 2.1.10 in JDK 6 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2014.03.21 at 02:49:55 em CET 
//


package org.notima.idempiere.iso20022.entity.pain;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.datatype.XMLGregorianCalendar;

public class Adapter1
    extends XmlAdapter<String, XMLGregorianCalendar>
{


    public XMLGregorianCalendar unmarshal(String value) {
        return (org.notima.idempiere.iso20022.Iso20022FileFactory.parseISODate(value));
    }

    public String marshal(XMLGregorianCalendar value) {
        return (org.notima.idempiere.iso20022.Iso20022FileFactory.printISODate(value));
    }

}
