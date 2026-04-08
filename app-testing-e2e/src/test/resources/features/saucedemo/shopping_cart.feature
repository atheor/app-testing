@saucedemo @smoke
Feature: SauceDemo Shopping Cart
  As a logged-in user I want to add products to my cart
  so that I can proceed to checkout.

  Background:
    Given I navigate to SauceDemo
    When I login with username "standard_user" and password "secret_sauce"
    Then I should be on the inventory page

  @happy-path
  Scenario: Add a single item to the cart
    When I add "Sauce Labs Backpack" to the cart
    Then the cart badge should show 1 item(s)

  @happy-path
  Scenario: Cart contains the added item
    When I add "Sauce Labs Bike Light" to the cart
    And I go to the cart
    Then the cart should contain "Sauce Labs Bike Light"
    And the cart should have 1 item(s)

  @happy-path
  Scenario: Add multiple items and verify cart count
    When I add "Sauce Labs Backpack" to the cart
    And I add "Sauce Labs Bike Light" to the cart
    Then the cart badge should show 2 item(s)
    And I go to the cart
    And the cart should contain "Sauce Labs Backpack"
    And the cart should contain "Sauce Labs Bike Light"
