//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.11 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2022.03.21 at 02:06:31 PM CET 
//


package iso.std.iso._20022.tech.xsd.camt_054_001;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for BankTransactionCodeStructure6 complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="BankTransactionCodeStructure6"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="Cd" type="{urn:iso:std:iso:20022:tech:xsd:camt.054.001.02}ExternalBankTransactionFamily1Code"/&gt;
 *         &lt;element name="SubFmlyCd" type="{urn:iso:std:iso:20022:tech:xsd:camt.054.001.02}ExternalBankTransactionSubFamily1Code"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "BankTransactionCodeStructure6", propOrder = {
    "cd",
    "subFmlyCd"
})
public class BankTransactionCodeStructure6 {

    @XmlElement(name = "Cd", required = true)
    protected String cd;
    @XmlElement(name = "SubFmlyCd", required = true)
    protected String subFmlyCd;

    /**
     * Gets the value of the cd property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCd() {
        return cd;
    }

    /**
     * Sets the value of the cd property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCd(String value) {
        this.cd = value;
    }

    /**
     * Gets the value of the subFmlyCd property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSubFmlyCd() {
        return subFmlyCd;
    }

    /**
     * Sets the value of the subFmlyCd property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSubFmlyCd(String value) {
        this.subFmlyCd = value;
    }

}
