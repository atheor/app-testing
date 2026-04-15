package com.atheor.e2e.pom;

import com.atheor.framework.web.BasePage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class SauceDemoCheckoutInfoPage extends BasePage {

    private static final By CHECKOUT_INFO_CONTAINER = By.cssSelector("div.checkout_info");
    private static final By FIRST_NAME_FIELD = By.id("first-name");

    public SauceDemoCheckoutInfoPage(WebDriver driver) {
        super(driver);
    }

    public boolean isLoaded() {
        return isPresent(CHECKOUT_INFO_CONTAINER) && isPresent(FIRST_NAME_FIELD);
    }
}
