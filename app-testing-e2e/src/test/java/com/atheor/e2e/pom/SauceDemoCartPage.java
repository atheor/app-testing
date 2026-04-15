package com.atheor.e2e.pom;

import com.atheor.framework.web.BasePage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class SauceDemoCartPage extends BasePage {

    private static final By CART_LIST = By.cssSelector("div.cart_list");
    private static final By CHECKOUT_BUTTON = By.id("checkout");

    public SauceDemoCartPage(WebDriver driver) {
        super(driver);
    }

    public boolean isLoaded() {
        return isPresent(CART_LIST) && isPresent(CHECKOUT_BUTTON);
    }

    public void startCheckout() {
        click(CHECKOUT_BUTTON);
    }
}
