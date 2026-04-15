package com.atheor.e2e.specs.countryinfo;

import com.atheor.e2e.countryinfo.client.CountryInfoSoapClient;
import com.atheor.e2e.countryinfo.workflow.CountryInfoWorkflow;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CountryInfoService — JUnit 5 Functional Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CountryInfoServiceTest {

        private static CountryInfoWorkflow workflow;

    @BeforeAll
    static void setUp() {
                workflow = new CountryInfoWorkflow(new CountryInfoSoapClient());
    }

    // ---- FullCountryInfo ----

    @ParameterizedTest(name = "FullCountryInfo [{0}] contains country name [{1}]")
    @CsvSource({
            "IT, Italy",
            "US, United States",
            "DE, Germany",
            "FR, France",
            "JP, Japan"
    })
    @Order(1)
    @DisplayName("FullCountryInfo returns correct country name for known ISO codes")
    void fullCountryInfoReturnsCountryName(String isoCode, String expectedName) throws IOException {
                String response = workflow.requestFullCountryInfo(isoCode);

        assertNotNull(response, "Response must not be null for ISO code: " + isoCode);
        assertFalse(response.isBlank(), "Response must not be blank for ISO code: " + isoCode);
        assertTrue(response.contains(expectedName),
                "Expected response to contain '" + expectedName + "' for ISO code '" + isoCode + "'");
    }

    @Test
    @Order(2)
    @DisplayName("FullCountryInfo for Italy contains capital city Rome")
    void fullCountryInfoItalyContainsCapitalRome() throws IOException {
        String response = workflow.requestFullCountryInfo("IT");
        assertTrue(response.contains("Rome") || response.contains("Roma"),
                "Expected capital city Rome/Roma in response: " + response);
    }

    @Test
    @Order(3)
    @DisplayName("FullCountryInfo for Italy contains phone code 39")
    void fullCountryInfoItalyContainsPhoneCode() throws IOException {
        String response = workflow.requestFullCountryInfo("IT");
        assertTrue(response.contains("39"),
                "Expected phone code '39' for Italy in response: " + response);
    }

    @Test
    @Order(4)
    @DisplayName("FullCountryInfo response is a valid SOAP envelope")
    void fullCountryInfoResponseIsValidSoapEnvelope() throws IOException {
        String response = workflow.requestFullCountryInfo("US");
        assertTrue(response.contains("Envelope"),
                "Response does not appear to be a SOAP envelope: " + response);
        assertTrue(response.contains("Body"),
                "Response is missing SOAP Body element: " + response);
    }

    // ---- ListOfCountryNamesByName ----

    @Test
    @Order(5)
    @DisplayName("ListOfCountryNamesByName returns a non-empty list")
    void listOfCountryNamesByNameIsNotEmpty() throws IOException {
        String response = workflow.requestCountryNamesList();
        assertNotNull(response, "Response must not be null");
        assertFalse(response.isBlank(), "Response must not be blank");
        assertTrue(response.contains("tCountryCodeAndName"),
                "Expected tCountryCodeAndName elements in response");
    }

    @Test
    @Order(6)
    @DisplayName("ListOfCountryNamesByName contains well-known countries")
    void listOfCountryNamesByNameContainsKnownCountries() throws IOException {
        String response = workflow.requestCountryNamesList();
        assertAll("Well-known countries must be present",
                () -> assertTrue(response.contains("Italy"),   "Missing Italy"),
                () -> assertTrue(response.contains("Germany"), "Missing Germany"),
                () -> assertTrue(response.contains("France"),  "Missing France"),
                () -> assertTrue(response.contains("Japan"),   "Missing Japan")
        );
    }

    // ---- CountryName ----

    @ParameterizedTest(name = "CountryName [{0}] = [{1}]")
    @CsvSource({
            "IT, Italy",
            "US, United States",
            "GB, United Kingdom"
    })
    @Order(7)
    @DisplayName("CountryName returns correct name for known codes")
    void countryNameReturnsCorrectName(String isoCode, String expectedName) throws IOException {
        String response = workflow.requestCountryName(isoCode);
        assertTrue(response.contains(expectedName),
                "Expected '" + expectedName + "' for ISO code '" + isoCode + "'. Actual: " + response);
    }
}
