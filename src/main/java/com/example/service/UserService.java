package com.example.service;

import com.example.database.PostgresClientImpl;
import com.example.model.User;
import com.example.validator.EmailValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

/**
 * Service for user management operations
 */
public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    
    private final PostgresClientImpl dbClient;
    private final EmailValidator emailValidator;
    
    public UserService(PostgresClientImpl dbClient) {
        this.dbClient = dbClient;
        this.emailValidator = new EmailValidator();
    }
    
    /**
     * Create a new user
     */
    public User createUser(String username, String email, String password) {
        logger.info("Creating user: {}", username);
        
        // Validate email
        if (!emailValidator.isValid(email)) {
            throw new IllegalArgumentException("Invalid email format");
        }
        
        // Check if user exists
        Optional<User> existing = dbClient.findByEmail(email);
        if (existing.isPresent()) {
            throw new IllegalStateException("User already exists");
        }
        
        // Hash password
        String passwordHash = hashPassword(password);
        
        // Create user
        User user = new User(username, email, passwordHash);
        Long userId = dbClient.insert(user);
        user.setId(userId);
        
        logger.info("Created user with ID: {}", userId);
        return user;
    }
    
    /**
     * Find user by email
     */
    public Optional<User> getUserByEmail(String email) {
        return dbClient.findByEmail(email);
    }
    
    /**
     * Update user information
     */
    public boolean updateUser(Long userId, User updates) {
        if (updates.getEmail() != null && !emailValidator.isValid(updates.getEmail())) {
            throw new IllegalArgumentException("Invalid email format");
        }
        
        return dbClient.update(userId, updates);
    }
    
    /**
     * Delete user by ID
     */
    public boolean deleteUser(Long userId) {
        return dbClient.delete(userId);
    }
    
    /**
     * List all users with pagination
     */
    public List<User> listUsers(int limit, int offset) {
        return dbClient.findAll(limit, offset);
    }
    
    /**
     * Search users by partial username match
     */
    public List<User> searchUsers(String searchTerm) {
        return dbClient.search(searchTerm);
    }
    
    private String hashPassword(String password) {
        // Stub: In real implementation use BCrypt
        return "hashed_" + password;
    }
}
