package com.example.chat_application.Services;

import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PresenceService {

    private final Set<String> onlineUsers = ConcurrentHashMap.newKeySet();

    public void setOnline(String username) {
        if (username != null && !username.isBlank()) {
            onlineUsers.add(username.trim().toLowerCase());
        }
    }

    public void setOffline(String username) {
        if (username != null && !username.isBlank()) {
            onlineUsers.remove(username.trim().toLowerCase());
        }
    }

    public boolean isOnline(String username) {
        if (username == null) return false;
        return onlineUsers.contains(username.trim().toLowerCase());
    }

    public Set<String> getOnlineUsers() {
        return Collections.unmodifiableSet(onlineUsers);
    }
}
