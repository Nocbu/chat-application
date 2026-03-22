package com.example.chat_application.Services;

import com.example.chat_application.Repositories.UserRepository;
import com.example.chat_application.DTO.AuthResponse;
import com.example.chat_application.DTO.LoginRequest;
import com.example.chat_application.DTO.RegisterRequest;
import com.example.chat_application.model.Role;
import com.example.chat_application.model.User;
import com.example.chat_application.Utilities.PasswordValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${admin.username}")
    private String adminUsername;

    @Value("${admin.password}")
    private String adminPassword;

    // register
    public AuthResponse register(RegisterRequest request) {

        String normalizedEmail = request.getEmail() == null
                ? null
                : request.getEmail().trim().toLowerCase();

        if (normalizedEmail == null || normalizedEmail.isBlank()) {
            return new AuthResponse(false, "Email is required");
        }

        if (!normalizedEmail.endsWith("@gmail.com")) {
            return new AuthResponse(false, "Only Gmail addresses are allowed (example@gmail.com)");
        }

        if (userRepository.existsByEmail(normalizedEmail)) {
            return new AuthResponse(false, "An account with this email already exists. Please login instead.");
        }

        String normalizedUsername = request.getUsername() == null ? null : request.getUsername().trim().toLowerCase();

        if (normalizedUsername == null || normalizedUsername.isBlank()) {
            return new AuthResponse(false, "Username is required");
        }

        // username format check (same rule you use later)
        if (!normalizedUsername.matches("^[a-z0-9._]{3,20}$")) {
            return new AuthResponse(false, "Invalid username format");
        }

        if (userRepository.existsByUsername(normalizedUsername)) {
            return new AuthResponse(false, "Username already taken. Please choose another one.");
        }

        // pass validate
        List<String> passwordErrors = PasswordValidator.validate(request.getPassword());
        if (!passwordErrors.isEmpty()) {
            return new AuthResponse(false, String.join(". ", passwordErrors));
        }

        // create user
        User user = new User();
        user.setEmail(normalizedEmail);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setDisplayName(request.getDisplayName());
        user.setRole(Role.USER);
        user.setUsername(normalizedUsername);
        user.setAuthProvider("LOCAL");
        user.setEnabled(true);
        user.setCreatedAt(LocalDateTime.now());

        userRepository.save(user);

        AuthResponse response = new AuthResponse(true, "Registration successful!");
        response.setDisplayName(user.getDisplayName());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole().name());
        response.setUsername(user.getUsername());
        return response;
    }

    // login
    public AuthResponse login(LoginRequest request) {

        // ADMIN login (username+password)
        if (request.getEmail().equals(adminUsername) && request.getPassword().equals(adminPassword)) {
            AuthResponse response = new AuthResponse(true, "Admin login successful!");
            response.setDisplayName("Admin");
            response.setEmail(adminUsername);
            response.setRole(Role.ADMIN.name());

            // IMPORTANT: set username so DMs can use it
            response.setUsername(adminUsername.toLowerCase());
            return response;
        }

        String normalizedEmail = request.getEmail() == null
                ? null
                : request.getEmail().trim().toLowerCase();

        if (normalizedEmail == null || normalizedEmail.isBlank()) {
            return new AuthResponse(false, "Email is required");
        }

        Optional<User> optionalUser = userRepository.findByEmail(normalizedEmail);

        if (optionalUser.isEmpty()) {
            return new AuthResponse(false, "No account found with this email. Please register first.");
        }

        User user = optionalUser.get();

        if ("GOOGLE".equals(user.getAuthProvider())) {
            return new AuthResponse(false, "This account uses Google Sign-In. Please login with Google.");
        }

        if (!user.isEnabled()) {
            return new AuthResponse(false, "Your account has been banned. Contact admin.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return new AuthResponse(false, "Incorrect password.");
        }

        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        AuthResponse response = new AuthResponse(true, "Login successful!");
        response.setDisplayName(user.getDisplayName());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole().name());

        // IMPORTANT: return username for frontend + session storage
        response.setUsername(user.getUsername());

        return response;
    }

    // google login
    public User processGoogleUser(String email, String name, String googleId, String picture) {

        String normalizedEmail = email == null ? null : email.trim().toLowerCase();
        Optional<User> existingUser = userRepository.findByEmail(normalizedEmail);

        if (existingUser.isPresent()) {
            User user = existingUser.get();
            user.setLastLogin(LocalDateTime.now());
            user.setGoogleId(googleId);
            user.setProfilePicture(picture);
            return userRepository.save(user);
        }

        User user = new User();
        user.setEmail(normalizedEmail);
        user.setDisplayName(name);
        user.setGoogleId(googleId);
        user.setAuthProvider("GOOGLE");
        user.setProfilePicture(picture);
        user.setRole(Role.USER);
        user.setEnabled(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setLastLogin(LocalDateTime.now());

        return userRepository.save(user);
    }

    public boolean setUsernameForEmail(String email, String username) {
        String normalizedEmail = email == null ? null : email.trim().toLowerCase();
        String normalizedUsername = username == null ? null : username.trim().toLowerCase();

        if (normalizedEmail == null || normalizedEmail.isBlank()) return false;
        if (normalizedUsername == null || normalizedUsername.isBlank()) return false;

        if (!normalizedUsername.matches("^[a-z0-9._]{3,20}$")) {
            throw new IllegalArgumentException("Invalid username format");
        }

        if (userRepository.existsByUsername(normalizedUsername)) {
            throw new IllegalArgumentException("Username already taken");
        }

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setUsername(normalizedUsername);
        userRepository.save(user);
        return true;
    }

    // ban/unban etc (unchanged)
    public boolean banUser(String email) {
        Optional<User> user = userRepository.findByEmail(email);
        if (user.isPresent()) {
            user.get().setEnabled(false);
            userRepository.save(user.get());
            return true;
        }
        return false;
    }

    public boolean unbanUser(String email) {
        Optional<User> user = userRepository.findByEmail(email);
        if (user.isPresent()) {
            user.get().setEnabled(true);
            userRepository.save(user.get());
            return true;
        }
        return false;
    }

    public boolean banUserByDisplayName(String displayName) {
        Optional<User> user = userRepository.findByDisplayName(displayName);
        if (user.isPresent()) {
            user.get().setEnabled(false);
            userRepository.save(user.get());
            return true;
        }
        return false;
    }

    public boolean banUserByUsername(String username) {
        Optional<User> user = userRepository.findByUsername(username);
        if (user.isPresent()) {
            user.get().setEnabled(false);
            userRepository.save(user.get());
            return true;
        }
        return false;
    }

    public boolean unbanUserByUsername(String username) {
        Optional<User> user = userRepository.findByUsername(username);
        if (user.isPresent()) {
            user.get().setEnabled(true);
            userRepository.save(user.get());
            return true;
        }
        return false;
    }

    public String getEmailByUsername(String username) {
        return userRepository.findByUsername(username).map(User::getEmail).orElse(null);
    }

    public boolean unbanUserByDisplayName(String displayName) {
        Optional<User> user = userRepository.findByDisplayName(displayName);
        if (user.isPresent()) {
            user.get().setEnabled(true);
            userRepository.save(user.get());
            return true;
        }
        return false;
    }

    public String getEmailByDisplayName(String displayName) {
        Optional<User> user = userRepository.findByDisplayName(displayName);
        return user.map(User::getEmail).orElse(null);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email.toLowerCase());
    }
}