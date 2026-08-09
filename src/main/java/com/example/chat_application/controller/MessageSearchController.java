package com.example.chat_application.controller;

import com.example.chat_application.Services.ChatMessageService;
import com.example.chat_application.model.ChatMessage;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/messages")
public class MessageSearchController {

    private final ChatMessageService chatMessageService;

    public MessageSearchController(ChatMessageService chatMessageService) {
        this.chatMessageService = chatMessageService;
    }

    @GetMapping("/search")
    public ResponseEntity<List<ChatMessage>> search(
            @RequestParam(value = "text", required = false) String text,
            @RequestParam(value = "sender", required = false) String sender,
            @RequestParam(value = "fileType", required = false) String fileType,
            @RequestParam(value = "fromDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(value = "toDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {

        return ResponseEntity.ok(chatMessageService.searchPublicMessages(text, sender, fileType, fromDate, toDate));
    }
}