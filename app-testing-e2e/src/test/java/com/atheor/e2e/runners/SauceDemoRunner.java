package com.atheor.e2e.runners;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;

@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features/saucedemo")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "com.atheor.e2e.saucedemo.stepdefs")
@ConfigurationParameter(
        key = PLUGIN_PROPERTY_NAME,
        value = "pretty,html:target/cucumber-reports/saucedemo/index.html,json:target/cucumber-reports/saucedemo/report.json"
)
public class SauceDemoRunner {
}
