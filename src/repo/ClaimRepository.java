package repo;

import exception.DataAccessException;
import exception.ValidationException;
import model.AIAnalysis;
import model.Claim;
import model.User;
import utils.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ClaimRepository {

    public int save(Claim claim) throws DataAccessException {
        String sql = "INSERT INTO claim (title, description, source_url, submitter_username, status) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, claim.getTitle());
            ps.setString(2, claim.getDescription());
            ps.setString(3, claim.getSourceUrl());
            ps.setString(4, claim.getSubmitter().getUsername());
            ps.setString(5, claim.getStatus());
            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }
            throw new DataAccessException("Failed to get generated claim ID");
        } catch (SQLException e) {
            throw new DataAccessException("Failed to save claim: " + e.getMessage(), e);
        }
    }

    public void update(Claim claim) throws DataAccessException {
        String sql = "UPDATE claim SET title=?, description=?, source_url=?, status=?, " +
                "ai_relevance_score=?, ai_contradiction=?, ai_summary=?, ai_analysis_date=? WHERE id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, claim.getTitle());
            ps.setString(2, claim.getDescription());
            ps.setString(3, claim.getSourceUrl());
            ps.setString(4, claim.getStatus());

            if (claim.getAiAnalysis() != null) {
                ps.setInt(5, claim.getAiAnalysis().getRelevanceScore());
                ps.setBoolean(6, claim.getAiAnalysis().isContradiction());
                ps.setString(7, claim.getAiAnalysis().getSummary());
                ps.setTimestamp(8, Timestamp.valueOf(claim.getAiAnalysis().getAnalysisDate()));
            } else {
                ps.setNull(5, Types.INTEGER);
                ps.setNull(6, Types.BOOLEAN);
                ps.setNull(7, Types.VARCHAR);
                ps.setNull(8, Types.TIMESTAMP);
            }
            ps.setInt(9, claim.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update claim: " + e.getMessage(), e);
        }
    }

    public Claim findById(int id) throws DataAccessException {
        String sql = "SELECT c.*, u.full_name, u.role FROM claim c " +
                "LEFT JOIN user u ON c.submitter_username = u.username WHERE c.id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapRow(rs);
            }
            return null;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to find claim: " + e.getMessage(), e);
        }
    }

    public List<Claim> findAll() throws DataAccessException {
        String sql = "SELECT c.*, u.full_name, u.role FROM claim c " +
                "LEFT JOIN user u ON c.submitter_username = u.username " +
                "ORDER BY c.submission_date DESC";
        List<Claim> claims = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                claims.add(mapRow(rs));
            }
            return claims;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to fetch claims: " + e.getMessage(), e);
        }
    }

    public List<Claim> findBySubmitter(String username) throws DataAccessException {
        String sql = "SELECT c.*, u.full_name, u.role FROM claim c " +
                "LEFT JOIN user u ON c.submitter_username = u.username " +
                "WHERE c.submitter_username = ? ORDER BY c.submission_date DESC";
        List<Claim> claims = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                claims.add(mapRow(rs));
            }
            return claims;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to fetch user claims: " + e.getMessage(), e);
        }
    }

    // ---------- Private helper ----------
    // This is where the fix lives: catch ValidationException from setters
    // and rethrow it as DataAccessException (which the repository is allowed to throw).
    private Claim mapRow(ResultSet rs) throws SQLException, DataAccessException {
        Claim claim = new Claim();

        try {
            claim.setId(rs.getInt("id"));
            claim.setTitle(rs.getString("title"));        // ← throws ValidationException
            claim.setDescription(rs.getString("description"));
            claim.setSourceUrl(rs.getString("source_url"));
            claim.setStatus(rs.getString("status"));

            User submitter = new User();
            submitter.setUsername(rs.getString("submitter_username"));  // ← throws
            submitter.setFullName(rs.getString("full_name"));            // ← throws
            submitter.setRole(rs.getString("role"));
            claim.setSubmitter(submitter);

            Timestamp ts = rs.getTimestamp("submission_date");
            if (ts != null) claim.setSubmissionDate(ts.toLocalDateTime());

            int score = rs.getInt("ai_relevance_score");
            if (!rs.wasNull()) {
                AIAnalysis ai = new AIAnalysis();
                ai.setRelevanceScore(score);
                ai.setContradiction(rs.getBoolean("ai_contradiction"));
                ai.setSummary(rs.getString("ai_summary"));
                Timestamp aiTs = rs.getTimestamp("ai_analysis_date");
                if (aiTs != null) ai.setAnalysisDate(aiTs.toLocalDateTime());
                claim.setAiAnalysis(ai);
            }
        } catch (ValidationException e) {
            // Data in the DB violated our model's rules — surface as a data access problem.
            throw new DataAccessException("Corrupt claim row: " + e.getMessage(), e);
        }

        return claim;
    }
}