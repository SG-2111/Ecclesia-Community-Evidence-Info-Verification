package model;

import exception.ValidationException;

public class User {
    private String username;
    private String password;
    private String fullName;
    private String role; // "USER" or "REVIEWER"

    public User() {}

    public User(String username, String password, String fullName, String role) throws ValidationException {
        setUsername(username);
        setPassword(password);
        setFullName(fullName);
        setRole(role);
    }

    // Getters and Setters with validation
    public String getUsername() { return username; }

    public void setUsername(String username) throws ValidationException {
        if (username == null || username.trim().isEmpty()) {
            throw new ValidationException("Username cannot be empty");
        }
        this.username = username.trim();
    }

    public String getPassword() { return password; }

    public void setPassword(String password) throws ValidationException {
        if (password == null || password.trim().isEmpty()) {
            throw new ValidationException("Password cannot be empty");
        }
        this.password = password;
    }

    public String getFullName() { return fullName; }

    public void setFullName(String fullName) throws ValidationException {
        if (fullName == null || fullName.trim().isEmpty()) {
            throw new ValidationException("Full name cannot be empty");
        }
        this.fullName = fullName.trim();
    }

    public String getRole() { return role; }

    public void setRole(String role) {
        this.role = (role == null || role.trim().isEmpty()) ? "USER" : role.trim().toUpperCase();
    }

    public boolean isReviewer() {
        return "REVIEWER".equalsIgnoreCase(this.role);
    }
}