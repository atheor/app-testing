package com.atheor.e2e.workflow;

import com.atheor.e2e.pom.SauceDemoCartPage;
import com.atheor.e2e.pom.SauceDemoCheckoutInfoPage;
import com.atheor.e2e.pom.SauceDemoInventoryPage;
import com.atheor.e2e.pom.SauceDemoLoginPage;
import org.openqa.selenium.WebDriver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Behavior workflow for SauceDemo authentication.
 */
public class SauceDemoLoginWorkflow {

    private final SauceDemoLoginPage loginPage;
    private final SauceDemoInventoryPage inventoryPage;
    private final SauceDemoCartPage cartPage;
    private final SauceDemoCheckoutInfoPage checkoutInfoPage;

    public SauceDemoLoginWorkflow(WebDriver driver) {
        this.loginPage = new SauceDemoLoginPage(driver);
        this.inventoryPage = new SauceDemoInventoryPage(driver);
        this.cartPage = new SauceDemoCartPage(driver);
        this.checkoutInfoPage = new SauceDemoCheckoutInfoPage(driver);
    }

    public void loginAddToCartAndStartCheckout(String baseUrl, String username, String password) {
        loginPage.open(baseUrl);
        loginPage.login(username, password);

        assertTrue(inventoryPage.isLoaded(),
                "Login should redirect to Products inventory page");

        inventoryPage.addFirstItemToCart();
        assertEquals("1", inventoryPage.getCartBadgeCount(),
                "Cart badge should show one item after adding a product");

        inventoryPage.openCart();
        assertTrue(cartPage.isLoaded(), "Cart page should be loaded");

        cartPage.startCheckout();
        assertTrue(checkoutInfoPage.isLoaded(),
                "Checkout information page should be loaded after clicking Checkout");
    }
}
