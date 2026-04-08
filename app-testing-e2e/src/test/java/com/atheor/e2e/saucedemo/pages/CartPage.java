package com.atheor.e2e.saucedemo.pages;

import com.atheor.framework.web.BasePage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Page Object for the SauceDemo shopping cart page.
 */
public class CartPage extends BasePage {

    private static final By CART_ITEM_NAMES  = By.cssSelector(".inventory_item_name");
    private static final By CHECKOUT_BUTTON  = By.id("checkout");
    private static final By CONTINUE_BUTTON  = By.id("continue-shopping");

    public CartPage(WebDriver driver) {
        super(driver);
    }

    /**
     * Returns the names of all items currently in the cart.
     */
    public List<String> getCartItemNames() {
        List<WebElement> elements = driver.findElements(CART_ITEM_NAMES);
        return elements.stream()
                .map(WebElement::getText)
                .collect(Collectors.toList());
    }

    public int getCartItemCount() {
        return driver.findElements(CART_ITEM_NAMES).size();
    }

    public void checkout() {
        click(CHECKOUT_BUTTON);
    }

    public void continueShopping() {
        click(CONTINUE_BUTTON);
    }
}
