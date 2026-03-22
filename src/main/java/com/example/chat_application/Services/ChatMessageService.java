package com.example.chat_application.Services;

import com.example.chat_application.Repositories.ChatMessageRepository;
import com.example.chat_application.model.ChatMessage;
import com.example.chat_application.security.cryptoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class ChatMessageService {

    @Autowired
    private ChatMessageRepository messageRepository;

    @Autowired
    private cryptoService cryptoService;

    public ChatMessage saveMessage(ChatMessage message) {
        if (message.getContent() != null && !message.getContent().isBlank()) {
            message.setContent(cryptoService.encryptToString(message.getContent()));
        }
        return messageRepository.save(message);
    }

    public List<ChatMessage> getRecentMessages() {
        List<ChatMessage> messages = messageRepository.findTop100ByOrderByTimestampDesc();
        Collections.reverse(messages);

        //decrypt
        for (ChatMessage m : messages) {
            if (m.getContent() != null && !m.getContent().isBlank()) {
                m.setContent(cryptoService.decryptToString(m.getContent()));
            }
        }
        return messages;
    }

    public Optional<ChatMessage> getMessageById(String id) {
        Optional<ChatMessage> msg = messageRepository.findById(id);
        msg.ifPresent(m -> {
            if (m.getContent() != null && !m.getContent().isBlank()) {
                m.setContent(cryptoService.decryptToString(m.getContent()));
            }
        });
        return msg;
    }

    public boolean deleteMessage(String messageId) {
        Optional<ChatMessage> message = messageRepository.findById(messageId);
        if (message.isPresent()) {
            messageRepository.deleteById(messageId);
            return true;
        }
        return false;
    }

    public void clearAllMessages() {
        messageRepository.deleteAll();
    }
}