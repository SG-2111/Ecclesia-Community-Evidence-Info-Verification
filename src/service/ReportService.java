package service;

import exception.DataAccessException;
import exception.EntityNotFoundException;
import model.AIAnalysis;
import model.Claim;
import model.Evidence;
import model.Report;
import model.Review;
import utils.AppLogger;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

public class ReportService {

    private final ClaimService claimService;
    private final ReviewService reviewService;

    public ReportService(ClaimService claimService, ReviewService reviewService) {
        this.claimService = claimService;
        this.reviewService = reviewService;
    }

    public Report generateClaimReport(int claimId)
            throws EntityNotFoundException, DataAccessException {

        System.out.println("[ReportService] Generating report for claim #" + claimId);

        Claim claim = claimService.getClaimById(claimId);
        System.out.println("[ReportService] Claim loaded: " + claim.getTitle());

        List<Review> reviews;
        try {
            reviews = reviewService.getReviewsForClaim(claimId);
            if (reviews == null) reviews = Collections.emptyList();
            System.out.println("[ReportService] Reviews loaded: " + reviews.size());
        } catch (Exception e) {
            System.err.println("[ReportService] Review load failed: " + e.getMessage());
            reviews = Collections.emptyList();
        }

        List<Evidence> evidenceList = claim.getEvidenceList();
        if (evidenceList == null) evidenceList = Collections.emptyList();

        StringBuilder sb = new StringBuilder();
        sb.append("==================================================\n");
        sb.append("   VERIFICATION REPORT — CLAIM #").append(claimId).append("\n");
        sb.append("==================================================\n\n");

        sb.append("TITLE:        ").append(safe(claim.getTitle())).append("\n");
        sb.append("SUBMITTED BY: ").append(
                claim.getSubmitter() != null ? safe(claim.getSubmitter().getUsername()) : "Unknown"
        ).append("\n");
        sb.append("SUBMITTED AT: ").append(
                claim.getSubmissionDate() != null ? claim.getSubmissionDate().toString() : "N/A"
        ).append("\n");
        sb.append("STATUS:       ").append(safe(claim.getStatus())).append("\n");
        sb.append("SOURCE URL:   ").append(safe(claim.getSourceUrl())).append("\n\n");

        sb.append("DESCRIPTION:\n");
        sb.append("  ").append(safe(claim.getDescription())).append("\n\n");

        // ---- Evidence section ----
        sb.append("--------------------------------------------------\n");
        sb.append("EVIDENCE (").append(evidenceList.size()).append(")\n");
        sb.append("--------------------------------------------------\n");
        if (evidenceList.isEmpty()) {
            sb.append("  No evidence attached.\n");
        } else {
            int i = 1;
            for (Evidence e : evidenceList) {
                sb.append("  ").append(i++).append(". ").append(safe(e.getTitle()))
                        .append(" [").append(safe(e.getType(), "N/A")).append("]\n");
                sb.append("     URL: ").append(safe(e.getUrl(), "N/A")).append("\n");
            }
        }
        sb.append("\n");

        // ---- AI Analysis section ----
        sb.append("--------------------------------------------------\n");
        sb.append("AI ANALYSIS\n");
        sb.append("--------------------------------------------------\n");
        AIAnalysis ai = claim.getAiAnalysis();
        if (ai == null) {
            sb.append("  Not performed yet.\n");
        } else {
            sb.append("  Relevance Score:  ").append(ai.getRelevanceScore()).append("/100\n");
            sb.append("  Contradiction:    ").append(ai.isContradiction() ? "YES" : "NO").append("\n");
            sb.append("  Summary:          ").append(safe(ai.getSummary(), "N/A")).append("\n");
            sb.append("  Analysed At:      ").append(
                    ai.getAnalysisDate() != null ? ai.getAnalysisDate().toString() : "N/A"
            ).append("\n");
        }
        sb.append("\n");

        // ---- Community reviews section ----
        sb.append("--------------------------------------------------\n");
        sb.append("COMMUNITY REVIEWS (").append(reviews.size()).append(")\n");
        sb.append("--------------------------------------------------\n");
        if (reviews.isEmpty()) {
            sb.append("  No community reviews yet.\n");
        } else {
            int i = 1;
            for (Review r : reviews) {
                sb.append("  ").append(i++).append(". Reviewer: ")
                        .append(r.getReviewer() != null ? safe(r.getReviewer().getUsername()) : "Unknown")
                        .append(" | Verdict: ").append(safe(r.getVerdict()))
                        .append(" | ").append(r.getReviewDate() != null ? r.getReviewDate().toString() : "N/A")
                        .append("\n");
                sb.append("     ").append(safe(r.getReviewText())).append("\n\n");
            }
        }

        sb.append("--------------------------------------------------\n");
        sb.append("FINAL VERDICT: ").append(safe(claim.getStatus())).append("\n");
        sb.append("Report generated at: ").append(LocalDateTime.now()).append("\n");
        sb.append("==================================================\n");

        Report report = new Report();
        report.setClaimId(claimId);
        report.setContent(sb.toString());
        report.setGeneratedDate(LocalDateTime.now());
        report.setReviews(reviews);

        AppLogger.log("Report generated for claim #" + claimId);
        System.out.println("[ReportService] Report completed for claim #" + claimId);
        return report;
    }

    public String generateSystemStatistics() throws DataAccessException {
        List<Claim> allClaims = claimService.getAllClaims();

        int totalClaims = allClaims.size();
        int pending = 0, aiAnalysed = 0, verified = 0, rejected = 0, needsInfo = 0;
        int totalReviews = 0;

        for (Claim c : allClaims) {
            String s = c.getStatus();
            if (s == null) continue;
            switch (s) {
                case "PENDING" -> pending++;
                case "AI_ANALYSED" -> aiAnalysed++;
                case "VERIFIED" -> verified++;
                case "REJECTED" -> rejected++;
                case "NEEDS_INFO" -> needsInfo++;
            }
            try {
                totalReviews += reviewService.getReviewsForClaim(c.getId()).size();
            } catch (Exception ignored) {}
        }

        StringBuilder sb = new StringBuilder();
        sb.append("==================================================\n");
        sb.append("   SYSTEM STATISTICS\n");
        sb.append("==================================================\n");
        sb.append("  Total Claims:      ").append(totalClaims).append("\n");
        sb.append("  Pending:           ").append(pending).append("\n");
        sb.append("  AI Analysed:       ").append(aiAnalysed).append("\n");
        sb.append("  Verified:          ").append(verified).append("\n");
        sb.append("  Rejected:          ").append(rejected).append("\n");
        sb.append("  Needs More Info:   ").append(needsInfo).append("\n");
        sb.append("  Total Reviews:     ").append(totalReviews).append("\n");
        sb.append("  Generated At:      ").append(LocalDateTime.now()).append("\n");
        sb.append("==================================================\n");

        return sb.toString();
    }

    private String safe(String s) {
        return s != null ? s : "N/A";
    }

    private String safe(String s, String fallback) {
        return (s != null && !s.isEmpty()) ? s : fallback;
    }
}