package server.handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import exception.*;
import model.Review;
import model.User;
import service.ReviewService;
import utils.JsonUtil;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReviewHandler implements HttpHandler {
    private final ReviewService reviewService;

    public ReviewHandler(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
        exchange.getResponseHeaders().add("Content-Type", "application/json");

        if ("OPTIONS".equals(method)) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        try {
            if (path.endsWith("/submit") && "POST".equals(method)) {
                handleSubmit(exchange);
            } else if (path.contains("/claim") && "GET".equals(method)) {
                handleGetForClaim(exchange);
            } else {
                sendResponse(exchange, 404, "{\"error\":\"Not found\"}");
            }
        } catch (Exception e) {
            sendResponse(exchange, 500, "{\"error\":\"" + escape(e.getMessage()) + "\"}");
        }
    }

    private void handleSubmit(HttpExchange exchange) throws IOException {
        String body = readBody(exchange);
        // ✅ Use Map<String, Object> so numbers and strings both work
        Map<String, Object> data = JsonUtil.fromJson(body, HashMap.class);

        try {
            User reviewer = new User();
            reviewer.setUsername((String) data.get("reviewerUsername"));
            reviewer.setRole("REVIEWER");

            // ✅ Safely convert claimId — works whether JSON sends 1 or "1"
            int claimId = Integer.parseInt(String.valueOf(data.get("claimId")));

            Review review = reviewService.submitReview(
                    claimId,
                    reviewer,
                    (String) data.get("reviewText"),
                    (String) data.get("verdict")
            );
            sendResponse(exchange, 201, JsonUtil.toJson(review));
        } catch (EntityNotFoundException | ValidationException | DataAccessException e) {
            sendResponse(exchange, 400, "{\"error\":\"" + escape(e.getMessage()) + "\"}");
        } catch (Exception e) {
            sendResponse(exchange, 500, "{\"error\":\"" + escape(e.getMessage()) + "\"}");
        }
    }

    private void handleGetForClaim(HttpExchange exchange) throws IOException {
        try {
            String query = exchange.getRequestURI().getQuery();
            int claimId = Integer.parseInt(query.split("=")[1]);
            List<Review> reviews = reviewService.getReviewsForClaim(claimId);
            sendResponse(exchange, 200, JsonUtil.toJson(reviews));
        } catch (DataAccessException e) {
            sendResponse(exchange, 500, "{\"error\":\"" + escape(e.getMessage()) + "\"}");
        }
    }

    private String readBody(HttpExchange exchange) throws IOException {
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            return sb.toString();
        }
    }

    private void sendResponse(HttpExchange exchange, int status, String response) throws IOException {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}