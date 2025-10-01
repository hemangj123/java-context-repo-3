package com.example.database;

import com.example.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * MySQL database client implementation
 */
public class MySQLClientImpl implements DatabaseClient {
    private static final Logger logger = LoggerFactory.getLogger(MySQLClientImpl.class);
    
    private final String host;
    private final int port;
    private final String database;
    private Connection connection;
    
    public MySQLClientImpl(String host, int port, String database, 
                          String username, String password) {
        this.host = host;
        this.port = port;
        this.database = database;
        connect(username, password);
    }
    
    private void connect(String username, String password) {
        try {
            String url = String.format("jdbc:mysql://%s:%d/%s", host, port, database);
            this.connection = DriverManager.getConnection(url, username, password);
            logger.info("Connected to MySQL database: {}", database);
        } catch (SQLException e) {
            throw new RuntimeException("MySQL connection failed", e);
        }
    }
    
    @Override
    public Long insert(User user) {
        // MySQL uses different syntax than PostgreSQL
        String sql = "INSERT INTO users (username, email, password_hash) VALUES (?, ?, ?)";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getEmail());
            stmt.setString(3, user.getPasswordHash());
            stmt.executeUpdate();
            
            // MySQL-specific: Get auto-generated ID
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getLong(1);
            }
            throw new SQLException("Failed to get generated ID");
            
        } catch (SQLException e) {
            throw new RuntimeException("Insert failed", e);
        }
    }
    
    @Override
    public Optional<User> findByEmail(String email) {
        // Implementation similar to PostgreSQL but with MySQL-specific optimizations
        String sql = "SELECT * FROM users WHERE email = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                User user = new User(
                    rs.getString("username"),
                    rs.getString("email"),
                    rs.getString("password_hash")
                );
                user.setId(rs.getLong("id"));
                return Optional.of(user);
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Query failed", e);
        }
    }
    
    @Override
    public boolean update(Long userId, User updates) {
        String sql = "UPDATE users SET username = ?, email = ? WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, updates.getUsername());
            stmt.setString(2, updates.getEmail());
            stmt.setLong(3, userId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Update failed", e);
        }
    }
    
    @Override
    public boolean delete(Long userId) {
        String sql = "DELETE FROM users WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Delete failed", e);
        }
    }
    
    @Override
    public List<User> findAll(int limit, int offset) {
        String sql = "SELECT * FROM users LIMIT ? OFFSET ?";
        List<User> users = new ArrayList<>();
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, limit);
            stmt.setInt(2, offset);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                User user = new User(
                    rs.getString("username"),
                    rs.getString("email"),
                    rs.getString("password_hash")
                );
                user.setId(rs.getLong("id"));
                users.add(user);
            }
            return users;
        } catch (SQLException e) {
            throw new RuntimeException("Query failed", e);
        }
    }
    
    @Override
    public List<User> search(String searchTerm) {
        // MySQL uses LIKE instead of PostgreSQL's ILIKE
        String sql = "SELECT * FROM users WHERE username LIKE ?";
        List<User> users = new ArrayList<>();
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, "%" + searchTerm + "%");
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                User user = new User(
                    rs.getString("username"),
                    rs.getString("email"),
                    rs.getString("password_hash")
                );
                user.setId(rs.getLong("id"));
                users.add(user);
            }
            return users;
        } catch (SQLException e) {
            throw new RuntimeException("Search failed", e);
        }
    }
    
    @Override
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            logger.error("Failed to close connection", e);
        }
    }
}
