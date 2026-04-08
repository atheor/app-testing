package com.atheor.e2e.saucedemo.pages;

import com.atheor.framework.web.BasePage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/**
 * Page Object for the SauceDemo login page (https://www.saucedemo.com).
 */
public class LoginPage extends BasePage {

    private static final By USERNAME_INPUT = By.id("user-name");
    private static final By PASSWORD_INPUT = By.id("password");
    private static final By LOGIN_BUTTON   = By.id("login-button");
    private static final By ERROR_MESSAGE  = By.cssSelector("[data-test='error']");

    public LoginPage(WebDriver driver) {
        super(driver);
    }

    public void navigateTo(String baseUrl) {
        driver.get(baseUrl);
    }

    public void login(String username, String password) {
        type(USERNAME_INPUT, username);
        type(PASSWORD_INPUT, password);
        click(LOGIN_BUTTON);
    }

    public boolean isErrorDisplayed() {
        return isPresent(ERROR_MESSAGE);
    }

    public String getErrorMessage() {
        return getText(ERROR_MESSAGE);
    }
}
