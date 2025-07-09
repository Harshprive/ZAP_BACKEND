package com.ZAP_Backend.ZapServices.Configurations;

import com.ZAP_Backend.ZapServices.Service.WebSocketConnectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
public class WebSocketEventListener {

    @Autowired
    private WebSocketConnectionService webSocketConnectionService;

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String providerId = headerAccessor.getFirstNativeHeader("providerId");
        if (providerId != null) {
            webSocketConnectionService.addConnectedProvider(Long.parseLong(providerId));
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String providerId = headerAccessor.getFirstNativeHeader("providerId");
        if (providerId != null) {
            webSocketConnectionService.removeConnectedProvider(Long.parseLong(providerId));
        }
    }
} 