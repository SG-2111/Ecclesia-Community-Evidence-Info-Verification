package service;

import exception.*;
import model.User;
import repo.UserRepository;
import utils.AppLogger;

public class UserService {
    private final UserRepository userRepo = new UserRepository();

    public User registerUser(String username, String password, String fullName, String role)
            throws ValidationException, DataAccessException {
        // Validation
        if (username == null || username.trim().isEmpty()) {
            throw new ValidationException("Username cannot be empty");
        }
        if (password == null || password.length() < 4) {
            throw new ValidationException("Password must be at least 4 characters");
        }
        if (fullName == null || fullName.trim().isEmpty()) {
            throw new ValidationException("Full name cannot be empty");
        }

        if (userRepo.existsByUsername(username)) {
            throw new ValidationException("Username already exists");
        }

        User user = new User(username, password, fullName, role);
        userRepo.save(user);
        AppLogger.log("User registered: " + username + " with role " + role);
        return user;
    }

    public User login(String username, String password)
            throws EntityNotFoundException, ValidationException, DataAccessException {
        if (username == null || password == null) {
            throw new ValidationException("Username and password are required");
        }

        User user = userRepo.findByUsername(username);
        if (user == null) {
            throw new EntityNotFoundException("User not found: " + username);
        }
        if (!user.getPassword().equals(password)) {
            throw new ValidationException("Incorrect password");
        }

        AppLogger.log("User logged in: " + username);
        return user;
    }

    public User findByUsername(String username) throws EntityNotFoundException, DataAccessException {
        User user = userRepo.findByUsername(username);
        if (user == null) {
            throw new EntityNotFoundException("User not found: " + username);
        }
        return user;
    }
}