package com.example.chat_application.controller;

import com.example.chat_application.Repositories.UserRepository;
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

        List<User> users = userRepository.findTop10ByUsernameContainingIgnoreCase(q.trim());

        return users.stream()
                // enabled is boolean => just check isEnabled()
                .filter(User::isEnabled)

                // don't show self
                .filter(u -> myUsername == null || u.getUsername() == null || !u.getUsername().equalsIgnoreCase(myUsername))

                // map to response
                .map(u -> Map.<String, Object>of(
                        "username", u.getUsername(),
                        "displayName", u.getDisplayName(),
                        // use profilePicture you already have
                        "pictureUrl", u.getProfilePicture()
                ))
                .collect(Collectors.toList());
    }
}