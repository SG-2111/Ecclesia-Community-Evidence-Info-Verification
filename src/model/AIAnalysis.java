package model;

import java.time.LocalDateTime;

public class AIAnalysis {
    private int relevanceScore; // 0-100
    private boolean contradiction;
    private String summary;
    private LocalDateTime analysisDate;

    public AIAnalysis() {}

    public int getRelevanceScore() { return relevanceScore; }
    public void setRelevanceScore(int relevanceScore) { this.relevanceScore = relevanceScore; }

    public boolean isContradiction() { return contradiction; }
    public void setContradiction(boolean contradiction) { this.contradiction = contradiction; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public LocalDateTime getAnalysisDate() { return analysisDate; }
    public void setAnalysisDate(LocalDateTime analysisDate) { this.analysisDate = analysisDate; }
}