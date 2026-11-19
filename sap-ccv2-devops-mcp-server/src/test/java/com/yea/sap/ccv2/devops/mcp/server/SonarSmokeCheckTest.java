package com.yea.sap.ccv2.devops.mcp.server;

import org.junit.jupiter.api.Test;

class SonarSmokeCheckTest {

    @Test
    void sonarShouldReportBlockingBug() {
        String value = null;
        System.out.println(value.length());
    }
}
