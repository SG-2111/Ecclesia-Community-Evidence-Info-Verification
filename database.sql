CREATE DATABASE IF NOT EXISTS evidence_db;
USE evidence_db;

-- Users table
CREATE TABLE user (
                      username VARCHAR(50) PRIMARY KEY,
                      password VARCHAR(255) NOT NULL,
                      full_name VARCHAR(100) NOT NULL,
                      role VARCHAR(20) NOT NULL DEFAULT 'USER' -- 'USER' or 'REVIEWER'
);

-- Claims table
CREATE TABLE claim (
                       id INT PRIMARY KEY AUTO_INCREMENT,
                       title VARCHAR(255) NOT NULL,
                       description TEXT,
                       source_url VARCHAR(500),
                       submitter_username VARCHAR(50),
                       submission_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                       status VARCHAR(30) DEFAULT 'PENDING',
                       ai_relevance_score INT,
                       ai_contradiction BOOLEAN,
                       ai_summary TEXT,
                       ai_analysis_date TIMESTAMP,
                       FOREIGN KEY (submitter_username) REFERENCES user(username) ON DELETE SET NULL
);

-- Evidence table
CREATE TABLE evidence (
                          id INT PRIMARY KEY AUTO_INCREMENT,
                          claim_id INT NOT NULL,
                          title VARCHAR(255) NOT NULL,
                          url VARCHAR(500),
                          type VARCHAR(50),
                          FOREIGN KEY (claim_id) REFERENCES claim(id) ON DELETE CASCADE
);

-- Reviews table
CREATE TABLE review (
                        id INT PRIMARY KEY AUTO_INCREMENT,
                        claim_id INT NOT NULL,
                        reviewer_username VARCHAR(50),
                        review_text TEXT,
                        verdict VARCHAR(30), -- 'VERIFIED', 'REJECTED', 'NEEDS_INFO'
                        review_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        FOREIGN KEY (claim_id) REFERENCES claim(id) ON DELETE CASCADE,
                        FOREIGN KEY (reviewer_username) REFERENCES user(username) ON DELETE SET NULL
);

-- Sample data (optional)
INSERT INTO user (username, password, full_name, role) VALUES
                                                           ('admin', 'admin123', 'Administrator', 'REVIEWER'),
                                                           ('rahul', 'rahul123', 'Rahul Sharma', 'USER');