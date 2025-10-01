package com.example.database;

import com.example.model.User;

import java.util.List;
import java.util.Optional;

/**
 * Database client interface
 */
public interface DatabaseClient {
    
    /**
     * Insert a new user
     * @return Generated user ID
     */
    Long insert(User user);
    
    /**
     * Find user by email
     */
    Optional<User> findByEmail(String email);
    
    /**
     * Update user information
     * @return true if user was updated
     */
    boolean update(Long userId, User updates);
    
    /**
     * Delete user by ID
     * @return true if user was deleted
     */
    boolean delete(Long userId);
    
    /**
     * Retrieve all users with pagination
     */
    List<User> findAll(int limit, int offset);
    
    /**
     * Search users by term
     */
    List<User> search(String searchTerm);
    
    /**
     * Close database connection
     */
    void close();
}
