package com.atheor.e2e.countryinfo.client;

import com.atheor.framework.config.ConfigManager;
import com.atheor.framework.soap.SoapClient;

import java.io.IOException;

/**
 * Typed client for the CountryInfoService SOAP operations used in tests.
 * Endpoint: http://webservices.oorsprong.org/websamples.countryinfo/CountryInfoService.wso
 */
public class CountryInfoSoapClient {

    private static final String DEFAULT_ENDPOINT =
            "http://webservices.oorsprong.org/websamples.countryinfo/CountryInfoService.wso";

    private static final String NAMESPACE = "http://www.oorsprong.org/websamples.countryinfo";

    private final SoapClient soapClient;
    private final String endpoint;

    public CountryInfoSoapClient() {
        this.endpoint = ConfigManager.get("countryinfo.endpoint", DEFAULT_ENDPOINT);
        this.soapClient = new SoapClient("");
    }

    /**
     * Calls the {@code FullCountryInfo} operation for the given ISO country code.
     *
     * @param isoCode 2-letter ISO 3166-1 alpha-2 code, e.g. "IT", "US"
     * @return raw SOAP response body as a String
     */
    public String getFullCountryInfo(String isoCode) throws IOException {
        String envelope = buildEnvelope("FullCountryInfo",
                "<sCountryISOCode>" + escapeXml(isoCode) + "</sCountryISOCode>");
        return soapClient.sendRequest(endpoint, envelope);
    }

    /**
     * Calls the {@code ListOfCountryNamesByName} operation — returns all countries ordered by name.
     *
     * @return raw SOAP response body as a String
     */
    public String listOfCountryNamesByName() throws IOException {
        String envelope = buildEnvelope("ListOfCountryNamesByName", "");
        return soapClient.sendRequest(endpoint, envelope);
    }

    /**
     * Calls the {@code CountryName} operation for the given ISO country code.
     *
     * @param isoCode 2-letter ISO code
     * @return raw SOAP response body as a String
     */
    public String getCountryName(String isoCode) throws IOException {
        String envelope = buildEnvelope("CountryName",
                "<sCountryISOCode>" + escapeXml(isoCode) + "</sCountryISOCode>");
        return soapClient.sendRequest(endpoint, envelope);
    }

    // ---- Helpers ----

    private String buildEnvelope(String operationName, String body) {
        return """
                <?xml version="1.0" encoding="utf-8"?>
                <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/"
                               xmlns:tns="%s">
                    <soap:Header/>
                    <soap:Body>
                        <tns:%s>
                            %s
                        </tns:%s>
                    </soap:Body>
                </soap:Envelope>
                """.formatted(NAMESPACE, operationName, body, operationName);
    }

    /** Minimal XML entity escaping to prevent injection through ISO code values. */
    private static String escapeXml(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&apos;");
    }
}
