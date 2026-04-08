@saucedemo @smoke
Feature: SauceDemo Login
  As a user I want to log in to SauceDemo
  so that I can access the product inventory.

  Background:
    Given I navigate to SauceDemo

  @happy-path
  Scenario: Successful login with valid credentials
    When I login with username "standard_user" and password "secret_sauce"
    Then I should be on the inventory page

  @negative
  Scenario: Login fails with invalid password
    When I login with username "standard_user" and password "wrong_password"
    Then I should see a login error
    And the error message should contain "Username and password do not match"

  @negative
  Scenario: Login fails with locked out user
    When I login with username "locked_out_user" and password "secret_sauce"
    Then I should see a login error
    And the error message should contain "Sorry, this user has been locked out"
