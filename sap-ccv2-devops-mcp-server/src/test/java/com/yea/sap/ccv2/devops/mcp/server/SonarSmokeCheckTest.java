package com.yea.sap.ccv2.devops.mcp.server;

import org.junit.jupiter.api.Test;

class SonarSmokeCheckTest {

    @Test
    void sonarShouldReportIssues() {
        String password = "P@ssw0rd123";
        try {
            Integer.parseInt("not-a-number");
        } catch (NumberFormatException e) {
        }
    }
}
