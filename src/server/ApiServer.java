package server;

import com.sun.net.httpserver.HttpServer;
import server.handlers.*;
import service.*;

import java.net.InetSocketAddress;

public class ApiServer {
    private final HttpServer server;
    private final UserService userService;
    private final ClaimService claimService;
    private final ReviewService reviewService;
    private final AIService aiService;

    public ApiServer(int port) throws Exception {
        this.userService = new UserService();
        this.claimService = new ClaimService();
        this.reviewService = new ReviewService(claimService);
        this.aiService = new AIService(claimService);

        server = HttpServer.create(new InetSocketAddress(port), 0);

        // Register handlers
        server.createContext("/api/user", new UserHandler(userService));
        server.createContext("/api/claim", new ClaimHandler(claimService, aiService));
        server.createContext("/api/review", new ReviewHandler(reviewService));

        server.setExecutor(java.util.concurrent.Executors.newFixedThreadPool(10));
    }

    public void start() {
        server.start();
        System.out.println("Server started on port " + server.getAddress().getPort());
    }

    public void stop() {
        server.stop(0);
    }
}
