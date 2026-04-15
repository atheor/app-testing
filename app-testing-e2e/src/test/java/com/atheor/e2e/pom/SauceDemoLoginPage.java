package com.atheor.e2e.pom;

import com.atheor.framework.web.BasePage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class SauceDemoLoginPage extends BasePage {

    private static final By USERNAME_FIELD = By.id("user-name");
    private static final By PASSWORD_FIELD = By.id("password");
    private static final By LOGIN_BUTTON = By.id("login-button");

    public SauceDemoLoginPage(WebDriver driver) {
        super(driver);
    }

    public void open(String baseUrl) {
        driver.get(baseUrl);
    }

    public void login(String username, String password) {
        type(USERNAME_FIELD, username);
        type(PASSWORD_FIELD, password);
        click(LOGIN_BUTTON);
    }
}
