package com.atheor.e2e.saucedemo.workflow;

import com.atheor.e2e.saucedemo.pages.CartPage;
import com.atheor.e2e.saucedemo.pages.InventoryPage;
import com.atheor.e2e.saucedemo.pages.LoginPage;
import com.atheor.framework.config.ConfigManager;
import com.atheor.framework.web.DriverFactory;

import java.util.List;

public class SauceDemoWorkflow {

    private LoginPage loginPage;
    private InventoryPage inventoryPage;
    private CartPage cartPage;

    public void startSession() {
        DriverFactory.initDriver();
        loginPage = new LoginPage(DriverFactory.getDriver());
        inventoryPage = new InventoryPage(DriverFactory.getDriver());
        cartPage = new CartPage(DriverFactory.getDriver());
    }

    public void endSession() {
        DriverFactory.quit();
    }

    public void openApplication() {
        String url = ConfigManager.get("saucedemo.base.url", "https://www.saucedemo.com");
        loginPage.navigateTo(url);
    }

    public void login(String username, String password) {
        loginPage.login(username, password);
    }

    public boolean isInventoryPageLoaded() {
        return inventoryPage.isLoaded();
    }

    public boolean isLoginErrorDisplayed() {
        return loginPage.isErrorDisplayed();
    }

    public String getLoginErrorMessage() {
        return loginPage.getErrorMessage();
    }

    public void addProductToCart(String productName) {
        inventoryPage.addToCart(productName);
    }

    public int getCartBadgeCount() {
        return inventoryPage.getCartItemCount();
    }

    public void goToCart() {
        inventoryPage.goToCart();
    }

    public List<String> getCartItemNames() {
        return cartPage.getCartItemNames();
    }

    public int getCartItemCount() {
        return cartPage.getCartItemCount();
    }
}
