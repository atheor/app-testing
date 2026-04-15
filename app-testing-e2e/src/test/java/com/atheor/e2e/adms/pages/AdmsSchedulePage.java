package com.atheor.e2e.adms.pages;

import com.atheor.framework.web.BasePage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/**
 * Page Object for the GE ADMS portal Schedules section.
 *
 * <p>Covers two views:
 * <ol>
 *   <li>Schedule list — table of all schedules</li>
 *   <li>Schedule detail — table of operations belonging to an opened schedule</li>
 * </ol>
 *
 * <p>Update the locators below to match the real portal HTML once available.
 */
public class AdmsSchedulePage extends BasePage {

    // Navigation (sidebar / top nav)
    // Update selector to match real ADMS portal nav link for Schedules
    private static final By SCHEDULES_NAV_LINK = By.cssSelector(
            "a[href*='schedule'], nav-item[data-view='schedules'], .nav-schedules");

    // Schedule list table
    // Update selector to match real ADMS schedule list table
    private static final By SCHEDULE_LIST_TABLE = By.cssSelector(
            "table.adms-schedule-list, [data-testid='schedule-list']");

    // Operation list table (visible after opening a schedule)
    // Update selector to match real ADMS operation list table
    private static final By OPERATION_LIST_TABLE = By.cssSelector(
            "table.adms-operation-list, [data-testid='operation-list']");

    public AdmsSchedulePage(WebDriver driver) {
        super(driver);
    }

    /**
     * Clicks the Schedules navigation link and waits for the list table to appear.
     */
    public void navigateTo() {
        click(SCHEDULES_NAV_LINK);
        waitForElement(SCHEDULE_LIST_TABLE);
    }

    /**
     * Returns {@code true} if a row with the exact {@code scheduleName} is present
     * in the schedule list table.
     */
    public boolean isSchedulePresent(String scheduleName) {
        return isPresent(scheduleRowLocator(scheduleName));
    }

    /**
     * Clicks the schedule row matching {@code scheduleName} to open its detail view,
     * then waits for the operations table to appear.
     */
    public void openSchedule(String scheduleName) {
        click(scheduleRowLocator(scheduleName));
        waitForElement(OPERATION_LIST_TABLE);
    }

    /**
     * Returns {@code true} if a row with the exact {@code operationName} is present
     * in the operation list table of the currently open schedule.
     */
    public boolean isOperationPresent(String operationName) {
        return isPresent(operationRowLocator(operationName));
    }

    // ---- Private helpers ----

    private static By scheduleRowLocator(String scheduleName) {
        return By.xpath(
                "//table[contains(@class,'adms-schedule-list') or @data-testid='schedule-list']"
                + "//td[normalize-space(text())='" + scheduleName.replace("'", "\\'") + "']");
    }

    private static By operationRowLocator(String operationName) {
        return By.xpath(
                "//table[contains(@class,'adms-operation-list') or @data-testid='operation-list']"
                + "//td[normalize-space(text())='" + operationName.replace("'", "\\'") + "']");
    }
}
