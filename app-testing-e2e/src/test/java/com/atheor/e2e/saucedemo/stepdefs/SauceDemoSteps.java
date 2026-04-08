package com.atheor.e2e.saucedemo.stepdefs;

import com.atheor.e2e.saucedemo.pages.CartPage;
import com.atheor.e2e.saucedemo.pages.InventoryPage;
import com.atheor.e2e.saucedemo.pages.LoginPage;
import com.atheor.framework.config.ConfigManager;
import com.atheor.framework.web.DriverFactory;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.WebDriver;

import java.util.List;

public class SauceDemoSteps {

    private WebDriver driver;
    private LoginPage loginPage;
    private InventoryPage inventoryPage;
    private CartPage cartPage;

    @Before
    public void setUp() {
        DriverFactory.initDriver();
        driver = DriverFactory.getDriver();
        loginPage = new LoginPage(driver);
        inventoryPage = new InventoryPage(driver);
        cartPage = new CartPage(driver);
    }

    @After
    public void tearDown() {
        DriverFactory.quit();
    }

    // ---- Navigation ----

    @Given("I navigate to SauceDemo")
    public void iNavigateToSauceDemo() {
        String url = ConfigManager.get("saucedemo.base.url", "https://www.saucedemo.com");
        loginPage.navigateTo(url);
    }

    // ---- Login ----

    @When("I login with username {string} and password {string}")
    public void iLoginWithUsernameAndPassword(String username, String password) {
        loginPage.login(username, password);
    }

    @Then("I should be on the inventory page")
    public void iShouldBeOnTheInventoryPage() {
        Assertions.assertTrue(inventoryPage.isLoaded(),
                "Expected to be on the inventory/products page");
    }

    @Then("I should see a login error")
    public void iShouldSeeALoginError() {
        Assertions.assertTrue(loginPage.isErrorDisplayed(),
                "Expected a login error message to be displayed");
    }

    @Then("the error message should contain {string}")
    public void theErrorMessageShouldContain(String expected) {
        String actual = loginPage.getErrorMessage();
        Assertions.assertTrue(actual.contains(expected),
                "Expected error to contain '" + expected + "', but was: " + actual);
    }

    // ---- Shopping cart ----

    @When("I add {string} to the cart")
    public void iAddProductToCart(String productName) {
        inventoryPage.addToCart(productName);
    }

    @Then("the cart badge should show {int} item\\(s\\)")
    public void theCartBadgeShouldShow(int expectedCount) {
        Assertions.assertEquals(expectedCount, inventoryPage.getCartItemCount(),
                "Cart badge count mismatch");
    }

    @And("I go to the cart")
    public void iGoToTheCart() {
        inventoryPage.goToCart();
    }

    @Then("the cart should contain {string}")
    public void theCartShouldContain(String productName) {
        List<String> cartItems = cartPage.getCartItemNames();
        Assertions.assertTrue(cartItems.stream()
                .anyMatch(name -> name.equalsIgnoreCase(productName)),
                "Cart does not contain '" + productName + "'. Actual items: " + cartItems);
    }

    @Then("the cart should have {int} item\\(s\\)")
    public void theCartShouldHaveItems(int expectedCount) {
        Assertions.assertEquals(expectedCount, cartPage.getCartItemCount(),
                "Cart item count mismatch");
    }
}
