package com.atheor.e2e.adms.workflow;

import com.atheor.e2e.adms.client.AdmsSoapClient;
import com.atheor.e2e.adms.model.OperationResult;
import com.atheor.e2e.adms.model.ScheduleResult;
import com.atheor.e2e.adms.model.WebVerificationResult;
import com.atheor.e2e.adms.pages.AdmsLoginPage;
import com.atheor.e2e.adms.pages.AdmsSchedulePage;
import com.atheor.framework.config.ConfigManager;
import org.openqa.selenium.WebDriver;

import java.io.IOException;

/**
 * Workflow: Create Schedule and Operation via SOAP, then verify both are visible in the
 * ADMS portal web UI.
 *
 * <p>Step 1 — {@code CreateSchedule} SOAP call → captures {@link ScheduleResult}<br>
 * Step 2 — {@code CreateOperation} SOAP call (chained on {@code scheduleId} from Step 1)
 *            → captures {@link OperationResult}<br>
 * Step 3 — Login to ADMS portal, navigate to Schedules, assert schedule and operation
 *            appear in the UI → captures {@link WebVerificationResult}
 *
 * <p>No assertions are made inside this class; the test class is responsible for
 * asserting on the results exposed by the getters.
 */
public class AdmsCreateScheduleWorkflow {

    private final AdmsSoapClient soapClient;
    private final AdmsLoginPage  loginPage;
    private final AdmsSchedulePage schedulePage;

    private final String portalUrl;
    private final String portalUsername;
    private final String portalPassword;

    private ScheduleResult        scheduleResult;
    private OperationResult       operationResult;
    private WebVerificationResult webVerificationResult;

    /**
     * @param soapClient typed SOAP client used for Steps 1 and 2
     * @param driver     WebDriver instance used for Step 3 (page objects are created internally)
     */
    public AdmsCreateScheduleWorkflow(AdmsSoapClient soapClient, WebDriver driver) {
        this.soapClient    = soapClient;
        this.loginPage     = new AdmsLoginPage(driver);
        this.schedulePage  = new AdmsSchedulePage(driver);
        this.portalUrl      = ConfigManager.get("adms.portal.url",      "http://your-adms-host/portal");
        this.portalUsername = ConfigManager.get("adms.portal.username",  "portal_user");
        this.portalPassword = ConfigManager.get("adms.portal.password",  "portal_password");
    }

    /**
     * Executes the full workflow.
     *
     * @param scheduleName  name for the new schedule (also used to locate it in the portal)
     * @param startDate     ISO-8601 start datetime, e.g. {@code "2026-04-15T08:00:00"}
     * @param endDate       ISO-8601 end datetime, e.g. {@code "2026-04-15T17:00:00"}
     * @param operationName name for the switching operation (also used to locate it in the portal)
     * @param deviceId      network device identifier
     * @throws IOException           if any SOAP call fails at the transport level
     * @throws IllegalStateException if a required ID cannot be extracted from a SOAP response
     */
    public void execute(String scheduleName, String startDate, String endDate,
                        String operationName, String deviceId) throws IOException {

        // Step 1: Create Schedule (SOAP)
        String createScheduleResponse = soapClient.createSchedule(scheduleName, startDate, endDate);
        String scheduleId = soapClient.extractElement(createScheduleResponse, "scheduleId");
        if (scheduleId == null || scheduleId.isBlank()) {
            throw new IllegalStateException(
                    "CreateSchedule did not return a scheduleId. Response: " + createScheduleResponse);
        }
        scheduleResult = new ScheduleResult(createScheduleResponse, scheduleId);

        // Step 2: Create Operation (SOAP) — chained on scheduleId from Step 1
        String createOperationResponse = soapClient.createOperation(scheduleId, operationName, deviceId);
        String operationId = soapClient.extractElement(createOperationResponse, "operationId");
        if (operationId == null || operationId.isBlank()) {
            throw new IllegalStateException(
                    "CreateOperation did not return an operationId. Response: " + createOperationResponse);
        }
        operationResult = new OperationResult(createOperationResponse, operationId);

        // Step 3: Verify in ADMS portal (Web) — runs only after both SOAP steps succeed
        loginPage.navigateTo(portalUrl);
        loginPage.login(portalUsername, portalPassword);

        schedulePage.navigateTo();
        boolean scheduleVisible = schedulePage.isSchedulePresent(scheduleName);

        boolean operationVisible = false;
        if (scheduleVisible) {
            schedulePage.openSchedule(scheduleName);
            operationVisible = schedulePage.isOperationPresent(operationName);
        }

        webVerificationResult = new WebVerificationResult(scheduleVisible, operationVisible);
    }

    // ---- Result accessors ----

    /** Result of Step 1 (CreateSchedule SOAP), or {@code null} if not yet executed. */
    public ScheduleResult getScheduleResult() {
        return scheduleResult;
    }

    /** Result of Step 2 (CreateOperation SOAP), or {@code null} if not yet executed. */
    public OperationResult getOperationResult() {
        return operationResult;
    }

    /** Result of Step 3 (portal web verification), or {@code null} if not yet executed. */
    public WebVerificationResult getWebVerificationResult() {
        return webVerificationResult;
    }
}
