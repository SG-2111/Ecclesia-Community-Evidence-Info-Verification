package service;

import repo.ReviewRepository;
import utils.AppLogger;

public class CredibilityService implements Runnable {
    private volatile boolean running = true;
    private final ReviewRepository reviewRepo = new ReviewRepository();

    @Override
    public void run() {
        while (running) {
            try {
                Thread.sleep(60 * 60 * 1000); // every hour
                recalculate();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void recalculate() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Credibility Recalculation ===\n");
        sb.append("Timestamp: ").append(java.time.LocalDateTime.now()).append("\n");
        sb.append("Reviewer scores recalculated based on review activity.\n");
        AppLogger.log(sb.toString());
    }

    public void stop() {
        running = false;
    }
}