package com.atheor.e2e.countryinfo.contract;

import com.atheor.e2e.countryinfo.client.CountryInfoSoapClient;
import com.atheor.framework.soap.XsdValidator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Contract tests for the CountryInfoService.
 *
 * <p>The XSD embedded in {@code schemas/CountryInfoService.xsd} (extracted from the
 * service's WSDL) acts as the published contract.  These tests verify that the live
 * service still produces SOAP response bodies whose {@code Body} payloads conform to
 * that schema — i.e. the service has not broken its own contract.</p>
 *
 * <p>Note: SOAP <em>response bodies</em> are validated, not the full envelope, because
 * the XSD defines the business types only (not the SOAP infrastructure elements).</p>
 */
@DisplayName("CountryInfoService — XSD Contract Tests")
class CountryInfoContractTest {

    private static CountryInfoSoapClient client;
    private static XsdValidator validator;

    @BeforeAll
    static void setUp() {
        client = new CountryInfoSoapClient();
        validator = new XsdValidator("schemas/CountryInfoService.xsd");
    }

    @ParameterizedTest(name = "FullCountryInfo response for ISO code [{0}] conforms to XSD contract")
    @ValueSource(strings = {"IT", "US", "DE"})
    @DisplayName("FullCountryInfo response conforms to XSD contract")
    void fullCountryInfoResponseConformsToContract(String isoCode) throws IOException {
        String response = client.getFullCountryInfo(isoCode);
        assertNotNull(response, "Response must not be null");

        // Extract the Body payload from the SOAP envelope for XSD validation
        String payload = extractSoapBodyPayload(response);
        assertDoesNotThrow(() -> validator.validate(payload),
                "XSD contract violation for FullCountryInfo[" + isoCode + "]");
    }

    @Test
    @DisplayName("ListOfCountryNamesByName response conforms to XSD contract")
    void listOfCountryNamesByNameConformsToContract() throws IOException {
        String response = client.listOfCountryNamesByName();
        assertNotNull(response, "Response must not be null");

        String payload = extractSoapBodyPayload(response);
        assertDoesNotThrow(() -> validator.validate(payload),
                "XSD contract violation for ListOfCountryNamesByName");
    }

    // ---- Helpers ----

    /**
     * Strips the SOAP envelope/header wrapper and returns only the inner Body content.
     * The XSD defines the business element types, not {@code soap:Envelope}.
     */
    private static String extractSoapBodyPayload(String soapEnvelope) {
        int bodyStart = soapEnvelope.indexOf("<soap:Body>");
        int bodyEnd   = soapEnvelope.indexOf("</soap:Body>");
        if (bodyStart == -1 || bodyEnd == -1) {
            // Try without namespace prefix
            bodyStart = soapEnvelope.indexOf("<Body>");
            bodyEnd   = soapEnvelope.indexOf("</Body>");
        }
        if (bodyStart == -1 || bodyEnd == -1) {
            return soapEnvelope; // fall back: validate the whole envelope
        }
        String openTag = soapEnvelope.contains("<soap:Body>") ? "<soap:Body>" : "<Body>";
        String payload = soapEnvelope.substring(bodyStart + openTag.length(), bodyEnd).trim();
        // Wrap in a document root for the XSD validator
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
               "<root xmlns=\"http://www.oorsprong.org/websamples.countryinfo\">" +
               payload +
               "</root>";
    }
}
