package com.atheor.e2e.test;

import com.atheor.e2e.workflow.SauceDemoLoginWorkflow;
import com.atheor.framework.config.ConfigManager;
import com.atheor.framework.web.DriverFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SauceDemoLoginTest {

    @BeforeEach
    void setUpDriver() {
        DriverFactory.initDriver();
    }

    @AfterEach
    void tearDownDriver() {
        DriverFactory.quit();
    }

    @Test
    void shouldLoginAddItemAndStartCheckout() {
        String baseUrl = ConfigManager.get("saucedemo.base.url");
        String username = ConfigManager.get("saucedemo.username", "standard_user");
        String password = ConfigManager.get("saucedemo.password", "secret_sauce");

        Assumptions.assumeTrue(baseUrl != null && !baseUrl.isBlank(),
                "Set saucedemo.base.url in test.properties or -Dsaucedemo.base.url");

        SauceDemoLoginWorkflow workflow = new SauceDemoLoginWorkflow(DriverFactory.getDriver());
        workflow.loginAddToCartAndStartCheckout(baseUrl, username, password);
    }
}
