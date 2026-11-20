package com.yea.sap.ccv2.devops.mcp.client.slack;

import com.slack.api.bolt.App;
import com.slack.api.bolt.AppConfig;
import com.slack.api.bolt.jakarta_socket_mode.SocketModeApp;
import com.slack.api.methods.MethodsClient;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.request.chat.ChatPostMessageRequest;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import com.slack.api.model.event.MessageEvent;
import com.yea.sap.ccv2.devops.mcp.client.agents.MyMcpAgent;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
public class SlackBot {
    private static final Logger logger = LoggerFactory.getLogger(SlackBot.class);

    private final MyMcpAgent agent;
    private final String appToken;
    private final String botToken;
    private final Set<String> allowedUsers;
    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS,
        new LinkedBlockingQueue<>(), Thread.ofPlatform().name("slack-message-worker").daemon(false).factory());
    private SocketModeApp socketApp;

    public SlackBot(MyMcpAgent agent,
                    @Value("${slack.app-token}") String appToken,
                    @Value("${slack.bot-token}") String botToken,
                    @Value("${slack.allowed-user-ids}") String allowedUsers) {
        this.agent = agent;
        this.appToken = appToken;
        this.botToken = botToken;
        this.allowedUsers = Arrays.stream(allowedUsers.split(","))
            .map(String::trim).filter(id -> !id.isEmpty()).collect(Collectors.toUnmodifiableSet());
        if (appToken.isBlank() || botToken.isBlank() || this.allowedUsers.isEmpty()) {
            throw new IllegalArgumentException("Slack tokens and at least one authorized user ID are required");
        }
    }

    @PostConstruct
    void start() throws Exception {
        App app = new App(AppConfig.builder().singleTeamBotToken(botToken).build());
        app.event(MessageEvent.class, (request, context) -> {
            MessageEvent event = request.getEvent();
            if (isDirectUserMessage(event)) {
                executor.execute(() -> handleMessage(event, context.client()));
            }
            return context.ack();
        });
        socketApp = createSocketModeApp(app);
        socketApp.startAsync();
        executor.prestartCoreThread();
        logger.info("Slack Socket Mode client started");
    }

    SocketModeApp createSocketModeApp(App app) throws IOException {
        return new SocketModeApp(appToken, app);
    }

    @PreDestroy
    void stop() throws Exception {
        try {
            if (socketApp != null) {
                socketApp.close();
            }
        } finally {
            executor.close();
        }
    }

    boolean isDirectUserMessage(MessageEvent event) {
        return "im".equals(event.getChannelType())
            && event.getSubtype() == null && event.getBotId() == null
            && event.getUser() != null && event.getChannel() != null
            && event.getText() != null && !event.getText().isBlank();
    }

    void handleMessage(MessageEvent event, MethodsClient client) {
        String response;
        if (!allowedUsers.contains(event.getUser())) {
            logger.warn("Unauthorized Slack user attempted access: {}", event.getUser());
            response = "Your Slack user ID " + event.getUser()
                + " is not authorized to use this bot. Please contact the administrator.";
        } else {
            try {
                response = agent.chat(event.getText(), "slack:" + event.getChannel() + ":" + event.getUser());
                if (response == null || response.isBlank()) {
                    throw new IllegalStateException("MCP agent returned an empty response");
                }
            } catch (RuntimeException e) {
                logger.error("MCP agent failed for Slack user {}", event.getUser(), e);
                response = "Unable to process your request. Please try again or contact the administrator.";
            }
        }
        try {
            ChatPostMessageResponse result = client.chatPostMessage(ChatPostMessageRequest.builder()
                .channel(event.getChannel()).threadTs(event.getThreadTs()).text(response).build());
            if (!result.isOk()) {
                logger.error("Failed to send Slack message: {}", result.getError());
            }
        } catch (IOException | SlackApiException e) {
            logger.error("Failed to send Slack message", e);
        }
    }
}
