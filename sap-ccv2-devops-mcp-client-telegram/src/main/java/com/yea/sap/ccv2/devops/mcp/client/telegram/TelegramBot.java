package com.yea.sap.ccv2.devops.mcp.client.telegram;

import com.yea.sap.ccv2.devops.mcp.client.agents.MyMcpAgent;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Component
public class TelegramBot extends TelegramLongPollingBot
{
    @Value("${api.telegram.token}")
    private String telegramBotToken;

    private final MyMcpAgent mcpAgent;

    public TelegramBot(final MyMcpAgent mcpAgent)
    {
        this.mcpAgent = mcpAgent;
    }

    @PostConstruct
    public void registerBot()
    {
        try
        {
            final TelegramBotsApi api = new TelegramBotsApi(DefaultBotSession.class);
            api.registerBot(this);
        }
        catch (final TelegramApiException e)
        {
            throw new RuntimeException(e);
        }

    }

    @Override
    public void onUpdateReceived(final Update telegramMessage)
    {

        if (telegramMessage.hasMessage() && telegramMessage.getMessage().hasText())
        {
            final String userMessage = telegramMessage.getMessage().getText();
            final Long chatId = telegramMessage.getMessage().getChatId();

            // Get the Telegram user ID (client ID)
            final Long userId = telegramMessage.getMessage().getFrom().getId();
            final String firstName = telegramMessage.getMessage().getFrom().getFirstName();
            final String lastName = telegramMessage.getMessage().getFrom().getLastName();


            final String botResponse = mcpAgent.chat(userMessage);
            System.out.println("===========================================");
            System.out.println("Telegram user ID: " + userId + ": " + firstName + " - " + lastName + " sent message: " + userMessage);
            System.out.println("Response: " + botResponse);
            System.out.println("===========================================");
            try
            {
                execute(new SendMessage(chatId.toString(), botResponse));
            }
            catch (final TelegramApiException e)
            {
                e.printStackTrace();
            }
        }
    }

    @Override
    public String getBotUsername()
    {
        return "SAP CCV2 DevOpsBot - YEA";
    }

    @Override
    public String getBotToken()
    {
        return telegramBotToken;
    }
}