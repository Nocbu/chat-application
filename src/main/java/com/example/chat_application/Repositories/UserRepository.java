package com.example.chat_application.Repositories;

import com.example.chat_application.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;
import java.util.List;

public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findByEmail(String email);
    Optional<User> findByGoogleId(String googleId);
    Optional<User> findByDisplayName(String displayName);
    Optional<User> findByUsernameIgnoreCase(String username);
    List<User> findTop10ByUsernameContainingIgnoreCase(String q);// NEW
    boolean existsByEmail(String email);
    List<User> findByEnabled(boolean enabled);
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
}