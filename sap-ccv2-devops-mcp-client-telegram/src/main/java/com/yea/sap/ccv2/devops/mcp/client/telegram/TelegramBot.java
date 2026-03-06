package com.yea.sap.ccv2.devops.mcp.client.telegram;

import com.yea.sap.ccv2.devops.mcp.client.agents.MyMcpAgent;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class TelegramBot extends TelegramLongPollingBot {

    private static final Logger logger = LoggerFactory.getLogger(TelegramBot.class);

    @Value("${api.telegram.token}")
    private String telegramBotToken;

    @Value("${allowed.client.ids}")
    private String allowedClientIdsProperty;

    private List<String> allowedClientIds;

    private final MyMcpAgent mcpAgent;

    public TelegramBot(final MyMcpAgent mcpAgent) {
        this.mcpAgent = mcpAgent;
    }

    @PostConstruct
    public void init() {
        // Parse allowed client IDs from property
        allowedClientIds = Arrays.stream(allowedClientIdsProperty.split(","))
            .map(String::trim)
            .collect(Collectors.toList());

        registerBot();
    }

    private void registerBot() {
        try {
            final TelegramBotsApi api = new TelegramBotsApi(DefaultBotSession.class);
            api.registerBot(this);
            logger.info("Telegram bot registered successfully.");
        } catch (final TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onUpdateReceived(final Update telegramMessage) {

        if (telegramMessage.hasMessage() && telegramMessage.getMessage().hasText()) {
            final String userMessage = telegramMessage.getMessage().getText();
            final Long chatId = telegramMessage.getMessage().getChatId();

            // Get the Telegram user ID (client ID)
            final Long userId = telegramMessage.getMessage().getFrom().getId();
            final String firstName = telegramMessage.getMessage().getFrom().getFirstName();
            final String lastName = telegramMessage.getMessage().getFrom().getLastName();

            if (allowedClientIds.contains(userId.toString())) {
                final String botResponse = mcpAgent.chat(userMessage);

                logger.info("===========================================");
                logger.info("Telegram user ID: {} - {} {} sent message: {}", userId, firstName, lastName, userMessage);
                logger.info("Response: {}", botResponse);
                logger.info("===========================================");

                try {
                    execute(new SendMessage(chatId.toString(), botResponse));
                } catch (final TelegramApiException e) {
                    logger.error("Failed to send Telegram message", e);
                }
            } else {
                logger.warn("Unauthorized user ID attempted access: {}", userId);

                // Send a message to the user telling them to contact the administrator
                try {
                    String message = "Your Dear "+firstName +" "+lastName+",\n Your Telegram ID : "+userId+" is not authorized to use this bot. " +
                        "Please contact the administrator to gain access.";
                    execute(new SendMessage(chatId.toString(), message));
                } catch (final TelegramApiException e) {
                    logger.error("Failed to send unauthorized message to Telegram user", e);
                }
            }
        }
    }

    @Override
    public String getBotUsername() {
        return "SAP CCV2 DevOpsBot - YEA";
    }

    @Override
    public String getBotToken() {
        return telegramBotToken;
    }
}