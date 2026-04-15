package com.atheor.e2e.saucedemo.stepdefs;

import com.atheor.e2e.saucedemo.workflow.SauceDemoWorkflow;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.junit.jupiter.api.Assertions;

import java.util.List;

public class SauceDemoSteps {

    private SauceDemoWorkflow workflow;

    @Before
    public void setUp() {
        workflow = new SauceDemoWorkflow();
        workflow.startSession();
    }

    @After
    public void tearDown() {
        workflow.endSession();
    }

    // ---- Navigation ----

    @Given("I navigate to SauceDemo")
    public void iNavigateToSauceDemo() {
        workflow.openApplication();
    }

    // ---- Login ----

    @When("I login with username {string} and password {string}")
    public void iLoginWithUsernameAndPassword(String username, String password) {
        workflow.login(username, password);
    }

    @Then("I should be on the inventory page")
    public void iShouldBeOnTheInventoryPage() {
        Assertions.assertTrue(workflow.isInventoryPageLoaded(),
                "Expected to be on the inventory/products page");
    }

    @Then("I should see a login error")
    public void iShouldSeeALoginError() {
        Assertions.assertTrue(workflow.isLoginErrorDisplayed(),
                "Expected a login error message to be displayed");
    }

    @Then("the error message should contain {string}")
    public void theErrorMessageShouldContain(String expected) {
        String actual = workflow.getLoginErrorMessage();
        Assertions.assertTrue(actual.contains(expected),
                "Expected error to contain '" + expected + "', but was: " + actual);
    }

    // ---- Shopping cart ----

    @When("I add {string} to the cart")
    public void iAddProductToCart(String productName) {
        workflow.addProductToCart(productName);
    }

    @Then("the cart badge should show {int} item\\(s\\)")
    public void theCartBadgeShouldShow(int expectedCount) {
        Assertions.assertEquals(expectedCount, workflow.getCartBadgeCount(),
                "Cart badge count mismatch");
    }

    @And("I go to the cart")
    public void iGoToTheCart() {
        workflow.goToCart();
    }

    @Then("the cart should contain {string}")
    public void theCartShouldContain(String productName) {
        List<String> cartItems = workflow.getCartItemNames();
        Assertions.assertTrue(cartItems.stream()
                .anyMatch(name -> name.equalsIgnoreCase(productName)),
                "Cart does not contain '" + productName + "'. Actual items: " + cartItems);
    }

    @Then("the cart should have {int} item\\(s\\)")
    public void theCartShouldHaveItems(int expectedCount) {
        Assertions.assertEquals(expectedCount, workflow.getCartItemCount(),
                "Cart item count mismatch");
    }
}
