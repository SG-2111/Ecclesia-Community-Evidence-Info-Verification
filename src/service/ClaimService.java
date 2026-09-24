package service;

import exception.*;
import model.Claim;
import model.Evidence;
import model.User;
import repo.ClaimRepository;
import repo.EvidenceRepository;
import utils.AppLogger;

import java.util.List;

public class ClaimService {
    private final ClaimRepository claimRepo = new ClaimRepository();
    private final EvidenceRepository evidenceRepo = new EvidenceRepository();

    public Claim submitClaim(String title, String description, String sourceUrl, User submitter)
            throws ValidationException, DataAccessException {
        if (title == null || title.trim().isEmpty()) {
            throw new ValidationException("Claim title is required");
        }
        if (submitter == null) {
            throw new ValidationException("Submitter is required");
        }

        Claim claim = new Claim(title, description, sourceUrl, submitter);
        int id = claimRepo.save(claim);
        claim.setId(id);
        AppLogger.log("Claim submitted: #" + id + " by " + submitter.getUsername());
        return claim;
    }

    public Claim getClaimById(int id) throws EntityNotFoundException, DataAccessException {
        Claim claim = claimRepo.findById(id);
        if (claim == null) {
            throw new EntityNotFoundException("Claim not found: " + id);
        }
        // Load evidence
        claim.setEvidenceList(evidenceRepo.findByClaimId(id));
        return claim;
    }

    public List<Claim> getAllClaims() throws DataAccessException {
        List<Claim> claims = claimRepo.findAll();
        for (Claim c : claims) {
            c.setEvidenceList(evidenceRepo.findByClaimId(c.getId()));
        }
        return claims;
    }

    public List<Claim> getClaimsBySubmitter(String username) throws DataAccessException {
        return claimRepo.findBySubmitter(username);
    }

    public void updateClaim(Claim claim) throws EntityNotFoundException, DataAccessException {
        if (claimRepo.findById(claim.getId()) == null) {
            throw new EntityNotFoundException("Claim not found: " + claim.getId());
        }
        claimRepo.update(claim);
        AppLogger.log("Claim updated: #" + claim.getId() + " status: " + claim.getStatus());
    }



    public Evidence addEvidence(int claimId, String title, String url, String type, String description)
            throws EntityNotFoundException, ValidationException, DataAccessException {
        if (claimRepo.findById(claimId) == null) {
            throw new EntityNotFoundException("Claim not found: " + claimId);
        }
        Evidence evidence = new Evidence(title, url, type, description);
        evidence.setClaimId(claimId);
        evidenceRepo.save(evidence);
        AppLogger.log("Evidence added to claim #" + claimId + ": " + title);
        return evidence;
    }

    public Evidence addEvidence(int claimId, String title, String url, String type)
            throws EntityNotFoundException, ValidationException, DataAccessException {
        // Verify claim exists
        if (claimRepo.findById(claimId) == null) {
            throw new EntityNotFoundException("Claim not found: " + claimId);
        }

        Evidence evidence = new Evidence(title, url, type);
        evidence.setClaimId(claimId);
        evidenceRepo.save(evidence);
        AppLogger.log("Evidence added to claim #" + claimId + ": " + title);
        return evidence;
    }

    public List<Evidence> getEvidenceForClaim(int claimId) throws DataAccessException {
        return evidenceRepo.findByClaimId(claimId);
    }
}