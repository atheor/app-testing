@countryinfo @smoke
Feature: CountryInfoService SOAP API
  As a consumer of the CountryInfoService
  I want to retrieve country information via SOAP
  so that I can use it in my application.

  Background:
    Given the CountryInfoService is available

  @happy-path
  Scenario: Get full country info for Italy
    When I request full country info for ISO code "IT"
    Then the response should not be empty
    And the response should be a valid SOAP envelope
    And the response should contain "Italy"

  @happy-path
  Scenario: Get full country info for United States
    When I request full country info for ISO code "US"
    Then the response should not be empty
    And the response should be a valid SOAP envelope
    And the response should contain "United States"

  @happy-path
  Scenario: List all country names returns multiple entries
    When I request the list of country names
    Then the response should not be empty
    And the response should be a valid SOAP envelope
    And the response should contain "Italy"
    And the response should contain "Germany"
    And the response should contain "France"
