package com.atheor.framework.soap;

import org.w3c.dom.Document;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

/**
 * Utility methods for extracting values from XML content.
 */
public final class XmlValueExtractor {

    private XmlValueExtractor() {
    }

    /**
     * Extracts text content for the first node that matches the provided local name.
     *
     * @param xml XML content to inspect
     * @param localName local-name() to match in XPath
     * @return trimmed value, or {@code null} when no non-blank value is found
     * @throws Exception when XML parsing or XPath evaluation fails
     */
    public static String extractValueByLocalName(String xml, String localName) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);

        Document document = factory.newDocumentBuilder().parse(
                new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

        XPath xPath = XPathFactory.newInstance().newXPath();
        String expression = "//*[local-name()='" + localName + "']/text()";
        String value = (String) xPath.compile(expression).evaluate(document, XPathConstants.STRING);

        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}