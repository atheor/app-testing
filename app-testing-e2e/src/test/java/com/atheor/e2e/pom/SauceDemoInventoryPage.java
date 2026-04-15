package com.atheor.e2e.pom;

import com.atheor.framework.web.BasePage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class SauceDemoInventoryPage extends BasePage {

    private static final By INVENTORY_CONTAINER = By.id("inventory_container");
    private static final By PAGE_TITLE = By.cssSelector("span.title");
    private static final By ADD_FIRST_ITEM_BUTTON = By.cssSelector("button[id^='add-to-cart']");
    private static final By CART_LINK = By.id("shopping_cart_container");
    private static final By CART_BADGE = By.cssSelector("span.shopping_cart_badge");

    public SauceDemoInventoryPage(WebDriver driver) {
        super(driver);
    }

    public boolean isLoaded() {
        return isPresent(INVENTORY_CONTAINER) && getPageTitle().equalsIgnoreCase("Products");
    }

    public String getPageTitle() {
        return getText(PAGE_TITLE);
    }

    public void addFirstItemToCart() {
        click(ADD_FIRST_ITEM_BUTTON);
    }

    public void openCart() {
        click(CART_LINK);
    }

    public String getCartBadgeCount() {
        return getText(CART_BADGE);
    }
}
