import java.util.TimeZone;
import server.ApiServer;
import service.CredibilityService;

public class Main {
    public static void main(String[] args) {
        // Force JVM to Indian Standard Time
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
        System.setProperty("user.timezone", "Asia/Kolkata");

        try {
            int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
            CredibilityService credService = new CredibilityService();
            Thread daemon = new Thread(credService);
            daemon.setDaemon(true);
            daemon.start();

            ApiServer server = new ApiServer(PORT);
            server.start();

            System.out.println("=== Community Evidence Verification Platform ===");
            System.out.println("Server running on port " + port);
            System.out.println("Open web/index.html in your browser");
            System.out.println("Press Ctrl+C to stop");

            Thread.currentThread().join();

        } catch (Exception e) {
            System.err.println("Failed to start server: " + e.getMessage());
            e.printStackTrace();
        }
    }
}