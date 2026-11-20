package com.yea.sap.ccv2.devops.mcp.client.slack;

import com.yea.sap.ccv2.devops.mcp.client.agents.MyMcpAgent;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(MyMcpAgent.class)
public class SlackClientApplication {
    public static void main(String[] args) {
        SpringApplication.run(SlackClientApplication.class, args);
    }
}
