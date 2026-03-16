package com.example.chat_application.controller;

import com.example.chat_application.Services.UserService;
import com.example.chat_application.DTO.SetUsernameRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/username")
    public ResponseEntity<Map<String, Object>> setUsername(@RequestBody SetUsernameRequest req,
                                                           @AuthenticationPrincipal OAuth2User oAuth2User) {
        Map<String, Object> resp = new HashMap<>();

        if (oAuth2User == null) {
            resp.put("success", false);
            resp.put("message", "Not authenticated");
            return ResponseEntity.status(401).body(resp);
        }

        String email = oAuth2User.getAttribute("email");

        try {
            userService.setUsernameForEmail(email, req.getUsername());
            resp.put("success", true);
            resp.put("message", "Username saved");
            resp.put("username", req.getUsername().trim().toLowerCase());
            return ResponseEntity.ok(resp);
        } catch (IllegalArgumentException ex) {
            resp.put("success", false);
            resp.put("message", ex.getMessage());
            return ResponseEntity.badRequest().body(resp);
        }
    }
}