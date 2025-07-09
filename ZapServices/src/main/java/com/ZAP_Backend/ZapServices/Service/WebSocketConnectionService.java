package com.ZAP_Backend.ZapServices.Service;

import org.springframework.stereotype.Service;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class WebSocketConnectionService {
    private final Set<Long> connectedProviders = ConcurrentHashMap.newKeySet();

    public void addConnectedProvider(Long providerId) {
        connectedProviders.add(providerId);
        System.out.println("👥 Provider connected: " + providerId);
        System.out.println("   └─ Current connected providers: " + connectedProviders);
    }

    public void removeConnectedProvider(Long providerId) {
        connectedProviders.remove(providerId);
        System.out.println("👋 Provider disconnected: " + providerId);
        System.out.println("   └─ Current connected providers: " + connectedProviders);
    }

    public boolean isProviderConnected(Long providerId) {
        boolean isConnected = connectedProviders.contains(providerId);
        System.out.println("🔍 Checking if provider " + providerId + " is connected: " + isConnected);
        return isConnected;
    }

    public Set<Long> getConnectedProviders() {
        System.out.println("📊 Getting all connected providers: " + connectedProviders);
        return connectedProviders;
    }
} 