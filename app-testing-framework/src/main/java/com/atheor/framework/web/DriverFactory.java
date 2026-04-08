package com.atheor.framework.web;

import com.atheor.framework.config.ConfigManager;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Thread-safe WebDriver factory.
 *
 * <p>Use {@link #getDriver()} after calling {@link #initDriver(BrowserType)} (or the
 * auto-resolving {@link #initDriver()}) on the same thread. Always call {@link #quit()}
 * in an {@code @After} hook to prevent browser leaks.</p>
 */
public final class DriverFactory {

    private static final Logger log = LoggerFactory.getLogger(DriverFactory.class);
    private static final ThreadLocal<WebDriver> driverHolder = new ThreadLocal<>();

    private DriverFactory() {}

    /**
     * Initialises a WebDriver instance using the browser configured in
     * {@code test.properties} (key: {@code browser}).  Defaults to Chrome.
     */
    public static void initDriver() {
        String browserName = ConfigManager.get("browser", "chrome").trim().toUpperCase();
        BrowserType type;
        try {
            type = BrowserType.valueOf(browserName);
        } catch (IllegalArgumentException e) {
            log.warn("Unknown browser '{}', falling back to CHROME", browserName);
            type = BrowserType.CHROME;
        }
        initDriver(type);
    }

    /**
     * Initialises a WebDriver instance for the requested {@link BrowserType}.
     */
    public static void initDriver(BrowserType browserType) {
        boolean headless = ConfigManager.getBoolean("browser.headless", false);

        WebDriver driver;
        switch (browserType) {
            case EDGE -> {
                WebDriverManager.edgedriver().setup();
                EdgeOptions edgeOptions = new EdgeOptions();
                if (headless) {
                    edgeOptions.addArguments("--headless=new");
                }
                edgeOptions.addArguments("--no-sandbox", "--disable-dev-shm-usage");
                driver = new EdgeDriver(edgeOptions);
                log.info("Edge WebDriver initialised (headless={})", headless);
            }
            default -> {
                WebDriverManager.chromedriver().setup();
                ChromeOptions chromeOptions = new ChromeOptions();
                if (headless) {
                    chromeOptions.addArguments("--headless=new");
                }
                chromeOptions.addArguments("--no-sandbox", "--disable-dev-shm-usage");
                driver = new ChromeDriver(chromeOptions);
                log.info("Chrome WebDriver initialised (headless={})", headless);
            }
        }

        driverHolder.set(driver);
    }

    /**
     * Returns the WebDriver bound to the current thread.
     *
     * @throws IllegalStateException if {@link #initDriver()} has not been called yet
     */
    public static WebDriver getDriver() {
        WebDriver driver = driverHolder.get();
        if (driver == null) {
            throw new IllegalStateException(
                    "WebDriver not initialised for this thread. Call DriverFactory.initDriver() first.");
        }
        return driver;
    }

    /**
     * Quits the WebDriver and removes it from the thread-local holder.
     */
    public static void quit() {
        WebDriver driver = driverHolder.get();
        if (driver != null) {
            try {
                driver.quit();
                log.info("WebDriver quit successfully.");
            } finally {
                driverHolder.remove();
            }
        }
    }
}
