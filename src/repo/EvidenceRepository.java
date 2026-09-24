package repo;

import exception.DataAccessException;
import exception.ValidationException;
import model.Evidence;
import utils.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EvidenceRepository {


    public void save(Evidence evidence) throws DataAccessException {
        String sql = "INSERT INTO evidence (claim_id, title, url, type, description) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, evidence.getClaimId());
            ps.setString(2, evidence.getTitle());
            ps.setString(3, evidence.getUrl());
            ps.setString(4, evidence.getType());
            ps.setString(5, evidence.getDescription());
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) evidence.setId(rs.getInt(1));
        } catch (SQLException e) {
            throw new DataAccessException("Failed to save evidence: " + e.getMessage(), e);
        }
    }

    public List<Evidence> findByClaimId(int claimId) throws DataAccessException {
        String sql = "SELECT * FROM evidence WHERE claim_id = ?";
        List<Evidence> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, claimId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to fetch evidence: " + e.getMessage(), e);
        }
    }

    public Evidence findById(int id) throws DataAccessException {
        String sql = "SELECT * FROM evidence WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapRow(rs);
            }
            return null;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to find evidence: " + e.getMessage(), e);
        }
    }

    // ---------- Private helper ----------
    private Evidence mapRow(ResultSet rs) throws SQLException, DataAccessException {
        Evidence e = new Evidence();
        try {
            e.setId(rs.getInt("id"));
            e.setClaimId(rs.getInt("claim_id"));
            e.setTitle(rs.getString("title"));
            e.setUrl(rs.getString("url"));
            e.setType(rs.getString("type"));
            e.setDescription(rs.getString("description"));
        } catch (ValidationException ex) {
            throw new DataAccessException("Corrupt evidence row: " + ex.getMessage(), ex);
        }
        return e;
    }
    }
