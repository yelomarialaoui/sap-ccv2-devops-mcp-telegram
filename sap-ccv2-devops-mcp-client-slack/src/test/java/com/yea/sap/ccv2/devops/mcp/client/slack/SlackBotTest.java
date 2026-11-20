package com.yea.sap.ccv2.devops.mcp.client.slack;

import com.slack.api.bolt.App;
import com.slack.api.bolt.jakarta_socket_mode.SocketModeApp;
import com.slack.api.methods.MethodsClient;
import com.slack.api.methods.request.chat.ChatPostMessageRequest;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import com.slack.api.model.event.MessageEvent;
import com.slack.api.socket_mode.SocketModeClient;
import com.yea.sap.ccv2.devops.mcp.client.agents.MyMcpAgent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SlackBotTest {
    private MyMcpAgent agent;
    private MethodsClient client;
    private SlackBot bot;

    @BeforeEach
    void setUp() throws Exception {
        agent = mock(MyMcpAgent.class);
        client = mock(MethodsClient.class);
        bot = new SlackBot(agent, "xapp-test", "xoxb-test", " U1, U2 ");
        ChatPostMessageResponse result = new ChatPostMessageResponse();
        result.setOk(true);
        when(client.chatPostMessage(any(ChatPostMessageRequest.class))).thenReturn(result);
    }

    @Test
    void authorizedDirectMessageIsAnsweredInTheSameConversation() throws Exception {
        MessageEvent event = message("U1");
        event.setThreadTs("123");
        when(agent.chat("list builds", "slack:D1:U1")).thenReturn("Builds");

        assertThat(bot.isDirectUserMessage(event)).isTrue();
        bot.handleMessage(event, client);

        verify(client).chatPostMessage(ArgumentMatchers.<ChatPostMessageRequest>argThat(request -> request.getChannel().equals("D1")
            && request.getThreadTs().equals("123") && request.getText().equals("Builds")));
    }

    @Test
    void unauthorizedUserCannotInvokeAgent() throws Exception {
        bot.handleMessage(message("U3"), client);

        verify(agent, never()).chat(any(), any());
        verify(client).chatPostMessage(ArgumentMatchers.<ChatPostMessageRequest>argThat(request -> request.getText().contains("not authorized")));
    }

    @Test
    void ignoresNonDirectBotEditedAndEmptyMessages() {
        MessageEvent event = message("U1");
        event.setChannelType("channel");
        assertThat(bot.isDirectUserMessage(event)).isFalse();
        event.setChannelType("im");
        event.setBotId("B1");
        assertThat(bot.isDirectUserMessage(event)).isFalse();
        event.setBotId(null);
        event.setText(" ");
        assertThat(bot.isDirectUserMessage(event)).isFalse();
        MessageEvent changed = mock(MessageEvent.class);
        when(changed.getChannelType()).thenReturn("im");
        when(changed.getSubtype()).thenReturn("message_changed");
        assertThat(bot.isDirectUserMessage(changed)).isFalse();
    }

    @Test
    void reportsAgentFailureToUser() throws Exception {
        when(agent.chat(any(), any())).thenThrow(new IllegalStateException("failure"));

        bot.handleMessage(message("U1"), client);

        verify(client).chatPostMessage(ArgumentMatchers.<ChatPostMessageRequest>argThat(request -> request.getText().contains("Unable to process")));
    }

    @Test
    void requiresAnAllowListAndTokens() {
        assertThatThrownBy(() -> new SlackBot(agent, "xapp", "xoxb", " , "))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SlackBot(agent, "", "xoxb", "U1"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void clientProcessRemainsAliveAfterSocketStarts() throws Exception {
        String classpath = System.getProperty("surefire.test.class.path", System.getProperty("java.class.path"));
        Process process = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java").toString(),
            "-cp", classpath, ClientProcess.class.getName()).redirectErrorStream(true).start();
        try {
            CompletableFuture<Boolean> started = CompletableFuture.supplyAsync(() ->
                process.inputReader().lines().anyMatch("TEST_CLIENT_STARTED"::equals));
            assertThat(started.get(10, TimeUnit.SECONDS)).isTrue();
            assertThat(process.waitFor(500, TimeUnit.MILLISECONDS)).isFalse();
        } finally {
            process.destroy();
            process.waitFor(5, TimeUnit.SECONDS);
        }
    }

    public static class ClientProcess {
        public static void main(String[] args) throws Exception {
            SlackBot bot = new SlackBot(null, "xapp-test", "xoxb-test", "U1") {
                @Override
                SocketModeApp createSocketModeApp(App app) {
                    return new SocketModeApp((SocketModeClient) null, app) {
                        @Override
                        public void startAsync() {
                        }
                    };
                }
            };
            bot.start();
            System.out.println("TEST_CLIENT_STARTED");
        }
    }

    private MessageEvent message(String user) {
        MessageEvent event = new MessageEvent();
        event.setUser(user);
        event.setChannel("D1");
        event.setChannelType("im");
        event.setText("list builds");
        return event;
    }
}
