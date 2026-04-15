package com.atheor.e2e.countryinfo.stepdefs;

import com.atheor.e2e.countryinfo.client.CountryInfoSoapClient;
import com.atheor.e2e.countryinfo.workflow.CountryInfoWorkflow;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.junit.jupiter.api.Assertions;

import java.io.IOException;

public class CountryInfoSteps {

    private final CountryInfoWorkflow workflow = new CountryInfoWorkflow(new CountryInfoSoapClient());

    private String lastResponse;

    @Given("the CountryInfoService is available")
    public void theCountryInfoServiceIsAvailable() {
        // Endpoint reachability is implicitly checked on the first SOAP call.
        // If the service is unreachable the test will fail with a clear IOException.
    }

    @When("I request full country info for ISO code {string}")
    public void iRequestFullCountryInfoForIsoCode(String isoCode) throws IOException {
        lastResponse = workflow.requestFullCountryInfo(isoCode);
    }

    @When("I request the list of country names")
    public void iRequestTheListOfCountryNames() throws IOException {
        lastResponse = workflow.requestCountryNamesList();
    }

    @Then("the response should contain {string}")
    public void theResponseShouldContain(String expectedFragment) {
        Assertions.assertNotNull(lastResponse, "No SOAP response received");
        Assertions.assertTrue(lastResponse.contains(expectedFragment),
                "Expected response to contain '" + expectedFragment + "'\nActual response:\n" + lastResponse);
    }

    @Then("the response should not be empty")
    public void theResponseShouldNotBeEmpty() {
        Assertions.assertNotNull(lastResponse, "Response was null");
        Assertions.assertFalse(lastResponse.isBlank(), "Response was blank/empty");
    }

    @Then("the response should be a valid SOAP envelope")
    public void theResponseShouldBeAValidSoapEnvelope() {
        Assertions.assertNotNull(lastResponse, "No SOAP response received");
        Assertions.assertTrue(lastResponse.contains("soap:Envelope") ||
                              lastResponse.contains("Envelope"),
                "Response does not appear to be a SOAP envelope:\n" + lastResponse);
    }
}
