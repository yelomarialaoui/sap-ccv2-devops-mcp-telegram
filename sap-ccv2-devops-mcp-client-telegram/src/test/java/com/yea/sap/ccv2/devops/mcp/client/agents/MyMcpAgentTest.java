package com.yea.sap.ccv2.devops.mcp.client.agents;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;

class MyMcpAgentTest {

    @Test
    void chatReturnsModelContent() {
        ChatClient.Builder builder = mock(ChatClient.Builder.class, RETURNS_SELF);
        ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        when(builder.build()).thenReturn(chatClient);
        when(chatClient.prompt().user(anyString()).call().content()).thenReturn("Build created");

        MyMcpAgent agent = new MyMcpAgent(builder, mock(ChatMemory.class), toolsProvider());

        assertThat(agent.chat("create a build")).isEqualTo("Build created");
    }

    @Test
    void constructorRegistersToolCallbacksAndSystemPrompt() {
        ChatClient.Builder builder = mock(ChatClient.Builder.class, RETURNS_SELF);
        when(builder.build()).thenReturn(mock(ChatClient.class));
        ToolCallbackProvider tools = mock(ToolCallbackProvider.class);
        when(tools.getToolCallbacks()).thenReturn(new ToolCallback[] {mock(ToolCallback.class)});

        new MyMcpAgent(builder, mock(ChatMemory.class), tools);

        verify(builder).defaultSystem(anyString());
        verify(builder).defaultToolCallbacks(tools);
    }

    private ToolCallbackProvider toolsProvider() {
        ToolCallbackProvider tools = mock(ToolCallbackProvider.class);
        when(tools.getToolCallbacks()).thenReturn(new ToolCallback[0]);
        return tools;
    }
}
