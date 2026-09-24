package model;

import exception.ValidationException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Claim {
    private int id;
    private String title;
    private String description;
    private String sourceUrl;
    private User submitter;
    private LocalDateTime submissionDate;
    private String status; // PENDING, AI_ANALYSED, REVIEWED, VERIFIED, REJECTED, NEEDS_INFO
    private AIAnalysis aiAnalysis;
    private List<Evidence> evidenceList = new ArrayList<>();

    public Claim() {}

    public Claim(String title, String description, String sourceUrl, User submitter) throws ValidationException {
        setTitle(title);
        this.description = description;
        this.sourceUrl = sourceUrl;
        this.submitter = submitter;
        this.submissionDate = LocalDateTime.now();
        this.status = "PENDING";
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) throws ValidationException {
        if (title == null || title.trim().isEmpty()) {
            throw new ValidationException("Claim title is required");
        }
        this.title = title.trim();
    }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getSourceUrl() { return sourceUrl; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }

    public User getSubmitter() { return submitter; }
    public void setSubmitter(User submitter) { this.submitter = submitter; }

    public LocalDateTime getSubmissionDate() { return submissionDate; }
    public void setSubmissionDate(LocalDateTime submissionDate) { this.submissionDate = submissionDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public AIAnalysis getAiAnalysis() { return aiAnalysis; }
    public void setAiAnalysis(AIAnalysis aiAnalysis) { this.aiAnalysis = aiAnalysis; }

    public List<Evidence> getEvidenceList() { return evidenceList; }
    public void setEvidenceList(List<Evidence> evidenceList) { this.evidenceList = evidenceList; }
}