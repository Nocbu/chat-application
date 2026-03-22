package com.example.chat_application.controller;

import com.example.chat_application.Repositories.UserRepository; // adjust
import com.example.chat_application.model.User;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
public class UserSearchController {

    private final UserRepository userRepository;

    public UserSearchController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/search")
    public List<Map<String, Object>> search(@RequestParam("q") String q, HttpSession session) {
        String myUsername = (String) session.getAttribute("username");

        if (q == null || q.trim().isEmpty()) return List.of();

        // You need a repo method like:
        // findTop10ByUsernameContainingIgnoreCase(String q)
        List<User> users = userRepository.findTop10ByUsernameContainingIgnoreCase(q.trim());

        return users.stream()
                .filter(u -> u.getEnabled() == null || u.getEnabled())
                .filter(u -> myUsername == null || !u.getUsername().equalsIgnoreCase(myUsername))
                .map(u -> Map.of(
                        "username", u.getUsername(),
                        "displayName", u.getDisplayName(),
                        "pictureUrl", u.getPictureUrl() // if you have it; otherwise remove
                ))
                .collect(Collectors.toList());
    }
}