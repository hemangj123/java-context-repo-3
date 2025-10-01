package com.example.database;

import com.example.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * PostgreSQL database client implementation
 */
public class PostgresClientImpl {
    private static final Logger logger = LoggerFactory.getLogger(PostgresClientImpl.class);
    
    private final String host;
    private final int port;
    private final String database;
    private final String username;
    private final String password;
    private Connection connection;
    
    /**
     * Constructor for PostgreSQL client
     */
    public PostgresClientImpl(String host, int port, String database, 
                              String username, String password) {
        this.host = host;
        this.port = port;
        this.database = database;
        this.username = username;
        this.password = password;
        connect();
    }
    
    private void connect() {
        try {
            String url = String.format("jdbc:postgresql://%s:%d/%s", host, port, database);
            this.connection = DriverManager.getConnection(url, username, password);
            logger.info("Connected to PostgreSQL database: {}", database);
        } catch (SQLException e) {
            logger.error("Failed to connect to PostgreSQL", e);
            throw new RuntimeException("Database connection failed", e);
        }
    }
    
    /**
     * Insert user into database
     * Uses PostgreSQL-specific RETURNING clause
     */
    public Long insert(User user) {
        String sql = "INSERT INTO users (username, email, password_hash) VALUES (?, ?, ?) RETURNING id";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getEmail());
            stmt.setString(3, user.getPasswordHash());
            
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getLong("id");
            }
            throw new SQLException("Failed to get generated ID");
            
        } catch (SQLException e) {
            logger.error("Failed to insert user", e);
            throw new RuntimeException("Insert failed", e);
        }
    }
    
    /**
     * Find user by email
     */
    public Optional<User> findByEmail(String email) {
        String sql = "SELECT * FROM users WHERE email = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return Optional.of(mapRowToUser(rs));
            }
            return Optional.empty();
            
        } catch (SQLException e) {
            logger.error("Failed to find user by email", e);
            throw new RuntimeException("Query failed", e);
        }
    }
    
    /**
     * Update user
     */
    public boolean update(Long userId, User updates) {
        String sql = "UPDATE users SET username = ?, email = ? WHERE id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, updates.getUsername());
            stmt.setString(2, updates.getEmail());
            stmt.setLong(3, userId);
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            logger.error("Failed to update user", e);
            throw new RuntimeException("Update failed", e);
        }
    }
    
    /**
     * Delete user
     */
    public boolean delete(Long userId) {
        String sql = "DELETE FROM users WHERE id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            logger.error("Failed to delete user", e);
            throw new RuntimeException("Delete failed", e);
        }
    }
    
    /**
     * Find all users with pagination
     */
    public List<User> findAll(int limit, int offset) {
        String sql = "SELECT * FROM users LIMIT ? OFFSET ?";
        List<User> users = new ArrayList<>();
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, limit);
            stmt.setInt(2, offset);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                users.add(mapRowToUser(rs));
            }
            return users;
            
        } catch (SQLException e) {
            logger.error("Failed to fetch users", e);
            throw new RuntimeException("Query failed", e);
        }
    }
    
    /**
     * Search users by username
     * Uses PostgreSQL-specific ILIKE operator
     */
    public List<User> search(String searchTerm) {
        String sql = "SELECT * FROM users WHERE username ILIKE ?";  // PostgreSQL-specific!
        List<User> users = new ArrayList<>();
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, "%" + searchTerm + "%");
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                users.add(mapRowToUser(rs));
            }
            return users;
            
        } catch (SQLException e) {
            logger.error("Failed to search users", e);
            throw new RuntimeException("Search failed", e);
        }
    }
    
    private User mapRowToUser(ResultSet rs) throws SQLException {
        User user = new User(
            rs.getString("username"),
            rs.getString("email"),
            rs.getString("password_hash")
        );
        user.setId(rs.getLong("id"));
        return user;
    }
    
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                logger.info("Database connection closed");
            }
        } catch (SQLException e) {
            logger.error("Failed to close connection", e);
        }
    }
}
