package com.atheor.e2e.saucedemo.pages;

import com.atheor.framework.web.BasePage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

/**
 * Page Object for the SauceDemo inventory (products) page.
 */
public class InventoryPage extends BasePage {

    private static final By PAGE_TITLE       = By.cssSelector(".title");
    private static final By CART_BADGE       = By.cssSelector(".shopping_cart_badge");
    private static final By CART_LINK        = By.cssSelector(".shopping_cart_link");
    private static final By INVENTORY_ITEMS  = By.cssSelector(".inventory_item");

    public InventoryPage(WebDriver driver) {
        super(driver);
    }

    public boolean isLoaded() {
        return isPresent(PAGE_TITLE) && getText(PAGE_TITLE).equalsIgnoreCase("Products");
    }

    /**
     * Adds the first item whose name matches {@code productName} to the cart.
     *
     * @throws IllegalArgumentException if no matching product is found
     */
    public void addToCart(String productName) {
        List<WebElement> items = driver.findElements(INVENTORY_ITEMS);
        for (WebElement item : items) {
            String name = item.findElement(By.cssSelector(".inventory_item_name")).getText();
            if (name.equalsIgnoreCase(productName)) {
                item.findElement(By.cssSelector("button")).click();
                return;
            }
        }
        throw new IllegalArgumentException("Product not found on inventory page: " + productName);
    }

    public int getCartItemCount() {
        if (!isPresent(CART_BADGE)) {
            return 0;
        }
        return Integer.parseInt(getText(CART_BADGE).trim());
    }

    public void goToCart() {
        click(CART_LINK);
    }
}
