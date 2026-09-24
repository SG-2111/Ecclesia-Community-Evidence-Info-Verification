package repo;

import exception.DataAccessException;
import exception.ValidationException;
import model.User;
import utils.DatabaseConnection;

import java.sql.*;

public class UserRepository {

    public void save(User user) throws DataAccessException {
        String sql = "INSERT INTO user (username, password, full_name, role) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPassword());
            ps.setString(3, user.getFullName());
            ps.setString(4, user.getRole());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Failed to save user: " + e.getMessage(), e);
        }
    }

    public User findByUsername(String username) throws DataAccessException {
        String sql = "SELECT * FROM user WHERE username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapRow(rs);
            }
            return null;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to find user: " + e.getMessage(), e);
        }
    }

    public boolean existsByUsername(String username) throws DataAccessException {
        String sql = "SELECT 1 FROM user WHERE username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            throw new DataAccessException("Failed to check user existence: " + e.getMessage(), e);
        }
    }

    // ---------- Private helper ----------
    // Catch ValidationException from model setters and rethrow as DataAccessException.
    private User mapRow(ResultSet rs) throws SQLException, DataAccessException {
        User user = new User();
        try {
            user.setUsername(rs.getString("username"));    // ← throws ValidationException
            user.setPassword(rs.getString("password"));    // ← throws ValidationException
            user.setFullName(rs.getString("full_name"));   // ← throws ValidationException
            user.setRole(rs.getString("role"));            // no validation, safe
        } catch (ValidationException e) {
            throw new DataAccessException("Corrupt user row: " + e.getMessage(), e);
        }
        return user;
    }
}