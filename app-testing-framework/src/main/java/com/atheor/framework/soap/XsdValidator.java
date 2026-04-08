package com.atheor.framework.soap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;

/**
 * Validates XML content against an XSD schema loaded from the classpath.
 * Used for SOAP contract testing — the XSD extracted from a service's WSDL
 * acts as the published contract.
 */
public class XsdValidator {

    private static final Logger log = LoggerFactory.getLogger(XsdValidator.class);

    private final Schema schema;

    /**
     * Creates a validator for the given classpath XSD resource.
     *
     * @param xsdClasspathResource path to the XSD relative to the classpath root
     *        (e.g. {@code "schemas/CountryInfoService.xsd"})
     */
    public XsdValidator(String xsdClasspathResource) {
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        // Disable external entity / DTD to prevent XXE attacks
        try {
            schemaFactory.setFeature(
                    "http://apache.org/xml/features/disallow-doctype-decl", true);
        } catch (SAXException ignored) {
            // feature may not be supported on all parsers — log and continue
            log.warn("Could not disable DOCTYPE declarations on SchemaFactory");
        }

        try (InputStream xsdStream = XsdValidator.class.getClassLoader()
                .getResourceAsStream(xsdClasspathResource)) {
            if (xsdStream == null) {
                throw new IllegalArgumentException(
                        "XSD resource not found on classpath: " + xsdClasspathResource);
            }
            schema = schemaFactory.newSchema(new StreamSource(xsdStream));
        } catch (SAXException | IOException e) {
            throw new IllegalStateException(
                    "Failed to load XSD schema from " + xsdClasspathResource, e);
        }
    }

    /**
     * Validates an XML string against the loaded schema.
     *
     * @param xml the XML content to validate (may be a full SOAP envelope or extracted payload)
     * @throws AssertionError if the XML does not conform to the schema
     */
    public void validate(String xml) {
        Validator validator = schema.newValidator();
        // Prevent XXE in the validator as well
        try {
            validator.setFeature(
                    "http://apache.org/xml/features/disallow-doctype-decl", true);
        } catch (SAXException ignored) {
            log.warn("Could not disable DOCTYPE declarations on Validator");
        }

        try {
            validator.validate(new StreamSource(new StringReader(xml)));
            log.debug("XSD validation passed.");
        } catch (SAXException e) {
            throw new AssertionError("XSD contract violation: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new AssertionError("IO error during XSD validation: " + e.getMessage(), e);
        }
    }
}
