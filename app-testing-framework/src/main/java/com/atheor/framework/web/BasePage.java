package com.atheor.framework.web;

import com.atheor.framework.config.ConfigManager;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * Base class for all Page Objects.
 *
 * <p>Provides a pre-configured {@link WebDriverWait} and convenience wrappers
 * for the most common interactions.  Subclasses receive the driver through the
 * constructor so that the factory / step-def layer controls the lifecycle.</p>
 */
public abstract class BasePage {

    protected final WebDriver driver;
    protected final WebDriverWait wait;

    protected BasePage(WebDriver driver) {
        this.driver = driver;
        int timeoutSeconds = ConfigManager.getInt("wait.timeout.seconds", 10);
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
    }

    /**
     * Waits until the element identified by {@code locator} is visible and returns it.
     */
    protected WebElement waitForElement(By locator) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    /**
     * Waits for the element to be clickable, then clicks it.
     */
    protected void click(By locator) {
        wait.until(ExpectedConditions.elementToBeClickable(locator)).click();
    }

    /**
     * Clears the field, then types the given text.
     */
    protected void type(By locator, String text) {
        WebElement element = waitForElement(locator);
        element.clear();
        element.sendKeys(text);
    }

    /**
     * Returns the visible text content of the element.
     */
    protected String getText(By locator) {
        return waitForElement(locator).getText();
    }

    /**
     * Returns {@code true} if at least one element matching {@code locator} is present in the DOM.
     */
    protected boolean isPresent(By locator) {
        return !driver.findElements(locator).isEmpty();
    }
}
