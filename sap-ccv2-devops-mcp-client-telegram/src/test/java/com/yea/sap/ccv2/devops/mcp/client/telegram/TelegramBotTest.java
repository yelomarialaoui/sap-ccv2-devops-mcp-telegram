package com.yea.sap.ccv2.devops.mcp.client.telegram;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import com.yea.sap.ccv2.devops.mcp.client.agents.MyMcpAgent;

class TelegramBotTest {

    private static final long ALLOWED_USER_ID = 111L;
    private static final long UNKNOWN_USER_ID = 222L;
    private static final long CHAT_ID = 999L;

    private MyMcpAgent agent;
    private TelegramBot bot;

    @BeforeEach
    void setUp() {
        agent = mock(MyMcpAgent.class);
        bot = spy(new TelegramBot(agent));
        ReflectionTestUtils.setField(bot, "allowedClientIds", List.of(String.valueOf(ALLOWED_USER_ID)));
    }

    @Test
    void forwardsTextFromAllowedUserToAgentAndRepliesWithResponse() throws TelegramApiException {
        when(agent.chat("list builds")).thenReturn("Here are your builds");
        doReturn(null).when(bot).execute(any(SendMessage.class));

        bot.onUpdateReceived(textUpdate(ALLOWED_USER_ID, "list builds"));

        verify(agent).chat("list builds");
        verify(bot).execute(argThatSendsText("Here are your builds"));
    }

    @Test
    void rejectsUnknownUserWithoutCallingAgent() throws TelegramApiException {
        doReturn(null).when(bot).execute(any(SendMessage.class));

        bot.onUpdateReceived(textUpdate(UNKNOWN_USER_ID, "list builds"));

        verify(agent, never()).chat(any());
        verify(bot).execute(argThatSendsTextContaining("is not authorized"));
    }

    @Test
    void ignoresUpdatesWithoutText() throws TelegramApiException {
        Update update = new Update();

        bot.onUpdateReceived(update);

        verify(agent, never()).chat(any());
        verify(bot, never()).execute(any(SendMessage.class));
    }

    @Test
    void doesNotPropagateTelegramSendFailures() throws TelegramApiException {
        when(agent.chat(any())).thenReturn("ok");
        doThrow(new TelegramApiException("boom")).when(bot).execute(any(SendMessage.class));

        bot.onUpdateReceived(textUpdate(ALLOWED_USER_ID, "hello"));

        verify(bot).execute(any(SendMessage.class));
    }

    @Test
    void initParsesAllowedClientIdsAndRegistersBot() {
        doNothing().when(bot).registerBot();
        ReflectionTestUtils.setField(bot, "allowedClientIdsProperty", " 111 , 222 ");

        bot.init();

        @SuppressWarnings("unchecked")
        List<String> allowedIds = (List<String>) ReflectionTestUtils.getField(bot, "allowedClientIds");
        assertThat(allowedIds).containsExactly("111", "222");
        verify(bot).registerBot();
    }

    @Test
    void exposesBotUsernameAndToken() {
        ReflectionTestUtils.setField(bot, "telegramBotToken", "token-value");

        assertThat(bot.getBotUsername()).isEqualTo("SAP CCV2 DevOpsBot - YEA");
        assertThat(bot.getBotToken()).isEqualTo("token-value");
    }

    private static Update textUpdate(long userId, String text) {
        User from = new User(userId, "Jane", false);
        from.setLastName("Doe");

        Message message = new Message();
        message.setText(text);
        message.setChat(new Chat(CHAT_ID, "private"));
        message.setFrom(from);

        Update update = new Update();
        update.setMessage(message);
        return update;
    }

    private static SendMessage argThatSendsText(String expected) {
        return argThat(
            sendMessage -> sendMessage != null
                && String.valueOf(CHAT_ID).equals(sendMessage.getChatId())
                && expected.equals(sendMessage.getText()));
    }

    private static SendMessage argThatSendsTextContaining(String fragment) {
        return argThat(
            sendMessage -> sendMessage != null
                && sendMessage.getText() != null
                && sendMessage.getText().contains(fragment));
    }
}
