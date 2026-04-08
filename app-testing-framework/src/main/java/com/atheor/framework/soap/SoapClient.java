package com.atheor.framework.soap;

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Thin SOAP-over-HTTP client built on top of Apache HttpClient 5.
 * Sends a raw SOAP envelope and returns the response body as a {@code String}.
 */
public class SoapClient {

    private static final Logger log = LoggerFactory.getLogger(SoapClient.class);

    private final String soapAction;

    public SoapClient() {
        this("");
    }

    public SoapClient(String soapAction) {
        this.soapAction = soapAction;
    }

    /**
     * Sends a SOAP 1.1 request.
     *
     * @param endpointUrl the SOAP service endpoint URL
     * @param soapEnvelope the full SOAP envelope XML
     * @return the response body as a String
     * @throws IOException if the HTTP call fails or returns a non-2xx / 500 status
     */
    public String sendRequest(String endpointUrl, String soapEnvelope) throws IOException {
        log.debug("SOAP POST → {}", endpointUrl);
        log.debug("Request envelope:\n{}", soapEnvelope);

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost post = new HttpPost(endpointUrl);
            post.setHeader("Content-Type", "text/xml; charset=UTF-8");
            post.setHeader("SOAPAction", "\"" + soapAction + "\"");
            post.setEntity(new StringEntity(soapEnvelope,
                    ContentType.create("text/xml", StandardCharsets.UTF_8)));

            return httpClient.execute(post, response -> {
                int statusCode = response.getCode();
                String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                log.debug("SOAP response [{}]:\n{}", statusCode, responseBody);

                if (statusCode < 200 || (statusCode >= 300 && statusCode != 500)) {
                    throw new IOException(
                            "SOAP call to " + endpointUrl + " returned HTTP " + statusCode);
                }
                return responseBody;
            });
        }
    }
}
