package model;

import exception.ValidationException;

public class Evidence {
    private int id;
    private int claimId;
    private String title;
    private String url;
    private String type; // "Study", "News", "Report", etc.
    private String description;

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    // Update constructor
    public Evidence(String title, String url, String type, String description) throws ValidationException {
        setTitle(title);
        this.url = url;
        this.type = type;
        this.description = description;
    }

    public Evidence() {}

    public Evidence(String title, String url, String type) throws ValidationException {
        setTitle(title);
        this.url = url;
        this.type = type;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getClaimId() { return claimId; }
    public void setClaimId(int claimId) { this.claimId = claimId; }

    public String getTitle() { return title; }
    public void setTitle(String title) throws ValidationException {
        if (title == null || title.trim().isEmpty()) {
            throw new ValidationException("Evidence title is required");
        }
        this.title = title.trim();
    }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
}