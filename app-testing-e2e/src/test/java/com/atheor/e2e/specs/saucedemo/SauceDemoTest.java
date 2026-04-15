package com.atheor.e2e.specs.saucedemo;

import com.atheor.e2e.saucedemo.workflow.SauceDemoWorkflow;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SauceDemo — JUnit 5 Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SauceDemoTest {

    private SauceDemoWorkflow workflow;

    @BeforeEach
    void setUp() {
        workflow = new SauceDemoWorkflow();
        workflow.startSession();
        workflow.openApplication();
    }

    @AfterEach
    void tearDown() {
        workflow.endSession();
    }

    // ---- Login tests ----

    @Test
    @Order(1)
    @DisplayName("Valid credentials navigate to inventory page")
    void validLoginNavigatesToInventory() {
        workflow.login("standard_user", "secret_sauce");
        assertTrue(workflow.isInventoryPageLoaded(),
                "Expected inventory page to be loaded after valid login");
    }

    @Test
    @Order(2)
    @DisplayName("Invalid password shows an error message")
    void invalidPasswordShowsError() {
        workflow.login("standard_user", "wrong_password");
        assertTrue(workflow.isLoginErrorDisplayed(), "Expected error message to be visible");
        assertTrue(workflow.getLoginErrorMessage().contains("Username and password do not match"),
            "Unexpected error message: " + workflow.getLoginErrorMessage());
    }

    @Test
    @Order(3)
    @DisplayName("Locked-out user cannot login")
    void lockedOutUserCannotLogin() {
        workflow.login("locked_out_user", "secret_sauce");
        assertTrue(workflow.isLoginErrorDisplayed(), "Expected error for locked-out user");
        assertTrue(workflow.getLoginErrorMessage().contains("locked out"),
            "Expected 'locked out' in error message: " + workflow.getLoginErrorMessage());
    }

    // ---- Shopping cart tests ----

    @Test
    @Order(4)
    @DisplayName("Adding one item increments cart badge to 1")
    void addOneItemUpdatesCartBadge() {
        workflow.login("standard_user", "secret_sauce");
        workflow.addProductToCart("Sauce Labs Backpack");
        assertEquals(1, workflow.getCartBadgeCount(),
                "Cart badge should show 1 after adding one item");
    }

    @Test
    @Order(5)
    @DisplayName("Added item appears in cart")
    void addedItemAppearsInCart() {
        workflow.login("standard_user", "secret_sauce");
        workflow.addProductToCart("Sauce Labs Bike Light");
        workflow.goToCart();

        List<String> cartItems = workflow.getCartItemNames();
        assertTrue(cartItems.stream().anyMatch(n -> n.equalsIgnoreCase("Sauce Labs Bike Light")),
                "Expected 'Sauce Labs Bike Light' in cart. Actual: " + cartItems);
    }

    @Test
    @Order(6)
    @DisplayName("Adding two items updates cart badge to 2")
    void addTwoItemsUpdatesBadgeToTwo() {
        workflow.login("standard_user", "secret_sauce");
        workflow.addProductToCart("Sauce Labs Backpack");
        workflow.addProductToCart("Sauce Labs Bike Light");
        assertEquals(2, workflow.getCartBadgeCount(),
                "Cart badge should show 2 after adding two items");
    }

    @Test
    @Order(7)
    @DisplayName("Cart page shows correct item count")
    void cartPageShowsCorrectItemCount() {
        workflow.login("standard_user", "secret_sauce");
        workflow.addProductToCart("Sauce Labs Fleece Jacket");
        workflow.addProductToCart("Sauce Labs Onesie");
        workflow.goToCart();
        assertEquals(2, workflow.getCartItemCount(),
                "Cart page should list exactly 2 items");
    }
}
