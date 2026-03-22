package com.example.chat_application.controller;
import com.example.chat_application.DTO.AuthResponse;
import com.example.chat_application.DTO.LoginRequest;
import com.example.chat_application.DTO.RegisterRequest;
import com.example.chat_application.Services.UserService;
import com.example.chat_application.model.User;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    //for regitser and validation
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = userService.register(request);
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.badRequest().body(response);
    }

    //login and check credentials
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpSession session) {
        AuthResponse response = userService.login(request);
        if (response.isSuccess()) {
            //using session for storing
            session.setAttribute("userEmail", response.getEmail());
            session.setAttribute("displayName", response.getDisplayName());
            session.setAttribute("role", response.getRole());
            response.setSessionId(session.getId());
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.badRequest().body(response);
    }

    //oath success
    @GetMapping("/oauth2/success")
    public String oauthSuccess(@AuthenticationPrincipal OAuth2User oAuth2User, HttpSession session) {

        if (oAuth2User == null) {
            return "<html><body><script>"
                    + "window.location.href = '/index.html?error=oauth2';"
                    + "</script></body></html>";
        }

        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String googleId = oAuth2User.getAttribute("sub");
        String picture = oAuth2User.getAttribute("picture");

        //create new if new
        User saved = userService.processGoogleUser(email, name, googleId, picture);

        session.setAttribute("userEmail", email);
        session.setAttribute("displayName", name);
        session.setAttribute("role", "USER");

        // agar username na ho toh
        if (saved.getUsername() == null || saved.getUsername().isBlank()) {
            return "<html><body><script>"
                    + "localStorage.setItem('userEmail', '" + email + "');"
                    + "localStorage.setItem('displayName', '" + name + "');"
                    + "localStorage.setItem('role', 'USER');"
                    + "localStorage.setItem('authProvider', 'GOOGLE');"
                    + "window.location.href = '/choose-username.html';"
                    + "</script></body></html>";
        }

        // else chat
        return "<html><body><script>"
                + "localStorage.setItem('userEmail', '" + email + "');"
                + "localStorage.setItem('displayName', '" + name + "');"
                + "localStorage.setItem('role', 'USER');"
                + "localStorage.setItem('authProvider', 'GOOGLE');"
                + "localStorage.setItem('username', '" + saved.getUsername() + "');"
                + "window.location.href = '/chat.html';"
                + "</script></body></html>";
    }

    //delte session after logout
    @PostMapping("/logout")
    public ResponseEntity<AuthResponse> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok(new AuthResponse(true, "Logged out successfully"));
    }

    //session check
    @GetMapping("/session")
    public ResponseEntity<AuthResponse> checkSession(HttpSession session) {
        String email = (String) session.getAttribute("userEmail");
        if (email != null) {
            AuthResponse response = new AuthResponse(true, "Session active");
            response.setEmail(email);
            response.setDisplayName((String) session.getAttribute("displayName"));
            response.setRole((String) session.getAttribute("role"));
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.ok(new AuthResponse(false, "No active session"));
    }
}
