package com.example.chat_application.config;
import com.example.chat_application.Services.CustomOAuth2UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private CustomOAuth2UserService customOAuth2UserService;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // Public pages & API
                        .requestMatchers(
                                "/", "/index.html", "/register.html", "/chat.html",
                                "/css/**", "/js/**",
                                "/api/auth/**",
                                "/api/messages/history",
                                "/ws/**",
                                "/api/files/download/**"
                        ).permitAll()
                        // Admin endpoints
                        .requestMatchers("/api/admin/**").permitAll() // we check admin in controller
                        // Everything else needs auth
                        .anyRequest().permitAll()
                )
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/index.html")
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService)
                        )
                        .defaultSuccessUrl("/api/auth/oauth2/success", true)
                        .failureUrl("/index.html?error=oauth2")
                )
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .logoutSuccessUrl("/index.html")
                );

        return http.build();
    }
}
