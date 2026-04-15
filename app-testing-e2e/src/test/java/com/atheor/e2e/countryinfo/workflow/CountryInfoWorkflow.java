package com.atheor.e2e.countryinfo.workflow;

import com.atheor.e2e.countryinfo.client.CountryInfoSoapClient;

import java.io.IOException;

public class CountryInfoWorkflow {

    private final CountryInfoSoapClient client;
    private String lastResponse;

    public CountryInfoWorkflow(CountryInfoSoapClient client) {
        this.client = client;
    }

    public String requestFullCountryInfo(String isoCode) throws IOException {
        lastResponse = client.getFullCountryInfo(isoCode);
        return lastResponse;
    }

    public String requestCountryNamesList() throws IOException {
        lastResponse = client.listOfCountryNamesByName();
        return lastResponse;
    }

    public String requestCountryName(String isoCode) throws IOException {
        lastResponse = client.getCountryName(isoCode);
        return lastResponse;
    }

    public String getLastResponse() {
        return lastResponse;
    }
}
