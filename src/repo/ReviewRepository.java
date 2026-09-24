package repo;

import exception.DataAccessException;
import exception.ValidationException;
import model.Review;
import model.User;
import utils.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReviewRepository {

    public void save(Review review) throws DataAccessException {
        String sql = "INSERT INTO review (claim_id, reviewer_username, review_text, verdict) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, review.getClaimId());
            ps.setString(2, review.getReviewer().getUsername());
            ps.setString(3, review.getReviewText());
            ps.setString(4, review.getVerdict());
            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                review.setId(rs.getInt(1));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to save review: " + e.getMessage(), e);
        }
    }

    public List<Review> findByClaimId(int claimId) throws DataAccessException {
        String sql = "SELECT r.*, u.full_name FROM review r " +
                "LEFT JOIN user u ON r.reviewer_username = u.username WHERE r.claim_id = ?";
        List<Review> reviews = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, claimId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                reviews.add(mapRow(rs));
            }
            return reviews;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to fetch reviews: " + e.getMessage(), e);
        }
    }

    public Review findById(int id) throws DataAccessException {
        String sql = "SELECT r.*, u.full_name FROM review r " +
                "LEFT JOIN user u ON r.reviewer_username = u.username WHERE r.id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapRow(rs);
            }
            return null;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to find review: " + e.getMessage(), e);
        }
    }

    public int countByReviewer(String username) throws DataAccessException {
        String sql = "SELECT COUNT(*) FROM review WHERE reviewer_username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
            return 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to count reviews: " + e.getMessage(), e);
        }
    }

    // ---------- Private helper ----------
    // Catch ValidationException from User setters (and any Review setters that validate),
    // rethrow as DataAccessException.
    private Review mapRow(ResultSet rs) throws SQLException, DataAccessException {
        Review review = new Review();
        try {
            review.setId(rs.getInt("id"));
            review.setClaimId(rs.getInt("claim_id"));
            review.setReviewText(rs.getString("review_text"));   // ← may throw
            review.setVerdict(rs.getString("verdict"));           // ← may throw

            Timestamp ts = rs.getTimestamp("review_date");
            if (ts != null) review.setReviewDate(ts.toLocalDateTime());

            User reviewer = new User();
            reviewer.setUsername(rs.getString("reviewer_username"));  // ← throws
            reviewer.setFullName(rs.getString("full_name"));            // ← throws
            review.setReviewer(reviewer);
        } catch (ValidationException e) {
            throw new DataAccessException("Corrupt review row: " + e.getMessage(), e);
        }
        return review;
    }
}