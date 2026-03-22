package com.example.chat_application.Utilities;
import java.util.ArrayList;
import java.util.List;

public class PasswordValidator {

    public static List<String> validate(String password) {
        List<String> errors = new ArrayList<>();

        if (password == null || password.length() < 8) {
            errors.add("Password must be at least 8 characters long");
        }
        if (password != null) {
            if (!password.matches(".*[A-Z].*")) {
                errors.add("Password must contain at least 1 uppercase letter");
            }
            if (!password.matches(".*[a-z].*")) {
                errors.add("Password must contain at least 1 lowercase letter");
            }
            if (!password.matches(".*\\d.*")) {
                errors.add("Password must contain at least 1 digit");
            }
            if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{}|;':\",./<>?].*")) {
                errors.add("Password must contain at least 1 special character");
            }
        }
        return errors;
    }

    public static boolean isValid(String password) {
        return validate(password).isEmpty();
    }
}
