package com.smartpm.common.config;

import com.smartpm.common.websocket.TaskWebSocketHandler;
import com.smartpm.common.websocket.WebSocketHandshakeInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * 注册 WebSocket 端点：/ws/project/{projectId}
 * WebSocketHandshakeInterceptor 在握手阶段校验 token 参数。
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final TaskWebSocketHandler taskWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(taskWebSocketHandler, "/ws/project/{projectId}")
                .addInterceptors(new WebSocketHandshakeInterceptor())
                .setAllowedOrigins("*");
    }
}
