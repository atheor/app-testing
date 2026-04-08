package com.atheor.e2e.saucedemo;

import com.atheor.e2e.saucedemo.pages.CartPage;
import com.atheor.e2e.saucedemo.pages.InventoryPage;
import com.atheor.e2e.saucedemo.pages.LoginPage;
import com.atheor.framework.config.ConfigManager;
import com.atheor.framework.web.DriverFactory;
import org.junit.jupiter.api.*;
import org.openqa.selenium.WebDriver;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SauceDemo — JUnit 5 Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SauceDemoTest {

    private static final String BASE_URL =
            ConfigManager.get("saucedemo.base.url", "https://www.saucedemo.com");

    private WebDriver driver;
    private LoginPage loginPage;
    private InventoryPage inventoryPage;
    private CartPage cartPage;

    @BeforeEach
    void setUp() {
        DriverFactory.initDriver();
        driver = DriverFactory.getDriver();
        loginPage     = new LoginPage(driver);
        inventoryPage = new InventoryPage(driver);
        cartPage      = new CartPage(driver);
        loginPage.navigateTo(BASE_URL);
    }

    @AfterEach
    void tearDown() {
        DriverFactory.quit();
    }

    // ---- Login tests ----

    @Test
    @Order(1)
    @DisplayName("Valid credentials navigate to inventory page")
    void validLoginNavigatesToInventory() {
        loginPage.login("standard_user", "secret_sauce");
        assertTrue(inventoryPage.isLoaded(),
                "Expected inventory page to be loaded after valid login");
    }

    @Test
    @Order(2)
    @DisplayName("Invalid password shows an error message")
    void invalidPasswordShowsError() {
        loginPage.login("standard_user", "wrong_password");
        assertTrue(loginPage.isErrorDisplayed(), "Expected error message to be visible");
        assertTrue(loginPage.getErrorMessage().contains("Username and password do not match"),
                "Unexpected error message: " + loginPage.getErrorMessage());
    }

    @Test
    @Order(3)
    @DisplayName("Locked-out user cannot login")
    void lockedOutUserCannotLogin() {
        loginPage.login("locked_out_user", "secret_sauce");
        assertTrue(loginPage.isErrorDisplayed(), "Expected error for locked-out user");
        assertTrue(loginPage.getErrorMessage().contains("locked out"),
                "Expected 'locked out' in error message: " + loginPage.getErrorMessage());
    }

    // ---- Shopping cart tests ----

    @Test
    @Order(4)
    @DisplayName("Adding one item increments cart badge to 1")
    void addOneItemUpdatesCartBadge() {
        loginPage.login("standard_user", "secret_sauce");
        inventoryPage.addToCart("Sauce Labs Backpack");
        assertEquals(1, inventoryPage.getCartItemCount(),
                "Cart badge should show 1 after adding one item");
    }

    @Test
    @Order(5)
    @DisplayName("Added item appears in cart")
    void addedItemAppearsInCart() {
        loginPage.login("standard_user", "secret_sauce");
        inventoryPage.addToCart("Sauce Labs Bike Light");
        inventoryPage.goToCart();

        List<String> cartItems = cartPage.getCartItemNames();
        assertTrue(cartItems.stream().anyMatch(n -> n.equalsIgnoreCase("Sauce Labs Bike Light")),
                "Expected 'Sauce Labs Bike Light' in cart. Actual: " + cartItems);
    }

    @Test
    @Order(6)
    @DisplayName("Adding two items updates cart badge to 2")
    void addTwoItemsUpdatesBadgeToTwo() {
        loginPage.login("standard_user", "secret_sauce");
        inventoryPage.addToCart("Sauce Labs Backpack");
        inventoryPage.addToCart("Sauce Labs Bike Light");
        assertEquals(2, inventoryPage.getCartItemCount(),
                "Cart badge should show 2 after adding two items");
    }

    @Test
    @Order(7)
    @DisplayName("Cart page shows correct item count")
    void cartPageShowsCorrectItemCount() {
        loginPage.login("standard_user", "secret_sauce");
        inventoryPage.addToCart("Sauce Labs Fleece Jacket");
        inventoryPage.addToCart("Sauce Labs Onesie");
        inventoryPage.goToCart();
        assertEquals(2, cartPage.getCartItemCount(),
                "Cart page should list exactly 2 items");
    }
}
