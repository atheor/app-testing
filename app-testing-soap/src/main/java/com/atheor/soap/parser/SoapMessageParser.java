package com.atheor.soap.parser;

import com.atheor.common.constant.AppConstants;
import com.atheor.wsdl.CreateScheduleRequest;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import lombok.extern.slf4j.Slf4j;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;

/**
 * Parses a raw SOAP 1.1 envelope string into typed JAXB objects.
 *
 * <p>All XML parsing is hardened against XXE (CWE-611) by explicitly
 * disabling DOCTYPE declarations and external entity/parameter references.
 */
@Slf4j
public class SoapMessageParser {

    private static final JAXBContext JAXB_CTX;

    static {
        try {
            JAXB_CTX = JAXBContext.newInstance(CreateScheduleRequest.class);
        } catch (JAXBException e) {
            throw new ExceptionInInitializerError("Failed to initialise JAXBContext: " + e.getMessage());
        }
    }

    /**
     * Parses a SOAP envelope XML string and returns the {@link CreateScheduleRequest}
     * found in the {@code soap:Body}.
     *
     * @param soapXml raw SOAP 1.1 XML string
     * @return unmarshalled {@link CreateScheduleRequest}
     * @throws IllegalArgumentException if the SOAP envelope is malformed or missing the Body
     */
    public CreateScheduleRequest parseCreateScheduleRequest(String soapXml) {
        log.debug("Parsing SOAP CreateScheduleRequest");

        Document document = parseXmlSafely(soapXml);
        Node bodyContent = extractSoapBodyContent(document);

        try {
            Unmarshaller unmarshaller = JAXB_CTX.createUnmarshaller();
            return unmarshaller.unmarshal(bodyContent, CreateScheduleRequest.class).getValue();
        } catch (JAXBException e) {
            throw new IllegalArgumentException("Failed to unmarshal CreateScheduleRequest from SOAP body", e);
        }
    }

    // ------------------------------------------------------------------
    // Private helpers
    // ------------------------------------------------------------------

    private Document parseXmlSafely(String xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);

            // XXE prevention (OWASP A05 / CWE-611)
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities",  false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);

            DocumentBuilder builder = factory.newDocumentBuilder();
            return builder.parse(new InputSource(new StringReader(xml)));
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid XML in SOAP request", e);
        }
    }

    private Node extractSoapBodyContent(Document document) {
        NodeList bodyNodes = document.getElementsByTagNameNS(AppConstants.SOAP_NAMESPACE, "Body");
        if (bodyNodes.getLength() == 0) {
            throw new IllegalArgumentException("SOAP envelope is missing the <soap:Body> element");
        }

        Node body = bodyNodes.item(0);
        // Skip whitespace text nodes — find first Element child
        Node child = body.getFirstChild();
        while (child != null && child.getNodeType() != Node.ELEMENT_NODE) {
            child = child.getNextSibling();
        }

        if (child == null) {
            throw new IllegalArgumentException("SOAP <soap:Body> element is empty");
        }
        return child;
    }
}
