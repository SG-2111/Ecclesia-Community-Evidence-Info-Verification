package service;

import exception.*;
import model.Claim;
import model.Review;
import model.User;
import repo.ClaimRepository;
import repo.ReviewRepository;
import utils.AppLogger;

import java.time.LocalDateTime;
import java.util.List;

public class ReviewService {
    private final ReviewRepository reviewRepo = new ReviewRepository();
    private final ClaimRepository claimRepo = new ClaimRepository();
    private final ClaimService claimService;

    public ReviewService(ClaimService claimService) {
        this.claimService = claimService;
    }

    public Review submitReview(int claimId, User reviewer, String text, String verdict)
            throws EntityNotFoundException, ValidationException, DataAccessException {

        // Verify claim exists
        Claim claim = claimService.getClaimById(claimId);

        // Validate reviewer
        if (reviewer == null || !reviewer.isReviewer()) {
            throw new ValidationException("Only reviewers can submit reviews");
        }
        if (text == null || text.trim().isEmpty()) {
            throw new ValidationException("Review text cannot be empty");
        }
        if (verdict == null || verdict.trim().isEmpty()) {
            throw new ValidationException("Verdict is required");
        }

        // Validate verdict value
        String upperVerdict = verdict.trim().toUpperCase();
        if (!upperVerdict.equals("VERIFIED") && !upperVerdict.equals("REJECTED")
                && !upperVerdict.equals("NEEDS_INFO")) {
            throw new ValidationException("Verdict must be VERIFIED, REJECTED, or NEEDS_INFO");
        }

        Review review = new Review();
        review.setClaimId(claimId);
        review.setReviewer(reviewer);
        review.setReviewText(text.trim());
        review.setVerdict(upperVerdict);
        review.setReviewDate(LocalDateTime.now());

        reviewRepo.save(review);

        // Update claim status based on verdict
        claim.setStatus(upperVerdict.equals("NEEDS_INFO") ? "NEEDS_INFO" : upperVerdict);
        claimService.updateClaim(claim);

        AppLogger.log("Review submitted for claim #" + claimId + " by " + reviewer.getUsername()
                + " verdict: " + upperVerdict);
        return review;
    }

    public List<Review> getReviewsForClaim(int claimId) throws DataAccessException {
        return reviewRepo.findByClaimId(claimId);
    }
}