package com.example.chat_application.controller;

import com.example.chat_application.Repositories.UserRepository;
import com.example.chat_application.model.User;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
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

        List<User> users = userRepository.findTop10ByUsernameContainingIgnoreCase(q.trim());

        return users.stream()
                .filter(User::isEnabled)
                .filter(u -> u.getUsername() != null && !u.getUsername().isBlank())
                .filter(u -> myUsername == null || !u.getUsername().equalsIgnoreCase(myUsername))
                .map(u -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("username", u.getUsername());
                    m.put("displayName", u.getDisplayName() == null ? u.getUsername() : u.getDisplayName());
                    m.put("pictureUrl", u.getProfilePicture() == null ? "" : u.getProfilePicture());
                    return m;
                })
                .collect(Collectors.toList());
    }
}