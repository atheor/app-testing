package com.atheor.e2e.adms.pages;

import com.atheor.framework.web.BasePage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/**
 * Page Object for the GE ADMS web portal login page.
 *
 * <p>Update the locators below to match the real portal HTML once available.
 */
public class AdmsLoginPage extends BasePage {

    // Update selectors to match real ADMS portal HTML
    private static final By USERNAME_INPUT = By.cssSelector("input[name='username']");
    private static final By PASSWORD_INPUT = By.cssSelector("input[name='password']");
    private static final By LOGIN_BUTTON   = By.cssSelector("button[type='submit']");
    private static final By USER_MENU      = By.cssSelector(".adms-user-menu, .user-profile-menu");

    public AdmsLoginPage(WebDriver driver) {
        super(driver);
    }

    public void navigateTo(String portalUrl) {
        driver.get(portalUrl);
    }

    public void login(String username, String password) {
        type(USERNAME_INPUT, username);
        type(PASSWORD_INPUT, password);
        click(LOGIN_BUTTON);
    }

    public boolean isLoggedIn() {
        return isPresent(USER_MENU);
    }
}
