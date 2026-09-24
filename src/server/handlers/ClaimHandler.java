package server.handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import exception.*;
import model.Claim;
import model.Evidence;
import model.User;
import service.AIService;
import service.ClaimService;
import utils.JsonUtil;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClaimHandler implements HttpHandler {
    private final ClaimService claimService;
    private final AIService aiService;

    public ClaimHandler(ClaimService claimService, AIService aiService) {
        this.claimService = claimService;
        this.aiService = aiService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        String query = exchange.getRequestURI().getQuery();

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
            } else if (path.endsWith("/all") && "GET".equals(method)) {
                handleGetAll(exchange);
            } else if (path.contains("/detail") && "GET".equals(method)) {
                handleGetDetail(exchange, query);
            } else if (path.endsWith("/evidence") && "POST".equals(method)) {
                handleAddEvidence(exchange);
            } else {
                sendResponse(exchange, 404, "{\"error\":\"Not found\"}");
            }
        } catch (Exception e) {
            sendResponse(exchange, 500, "{\"error\":\"" + escape(e.getMessage()) + "\"}");
        }
    }

    private void handleSubmit(HttpExchange exchange) throws IOException {
        String body = readBody(exchange);
        // ✅ Use Map<String, Object> everywhere for safety
        Map<String, Object> data = JsonUtil.fromJson(body, HashMap.class);

        try {
            User submitter = new User();
            submitter.setUsername((String) data.get("username"));

            Claim claim = claimService.submitClaim(
                    (String) data.get("title"),
                    (String) data.get("description"),
                    (String) data.get("sourceUrl"),
                    submitter
            );

            aiService.analyseClaimAsync(claim);
            sendResponse(exchange, 201, JsonUtil.toJson(claim));
        } catch (ValidationException | DataAccessException e) {
            sendResponse(exchange, 400, "{\"error\":\"" + escape(e.getMessage()) + "\"}");
        }
    }

    private void handleGetAll(HttpExchange exchange) throws IOException {
        try {
            List<Claim> claims = claimService.getAllClaims();
            sendResponse(exchange, 200, JsonUtil.toJson(claims));
        } catch (DataAccessException e) {
            sendResponse(exchange, 500, "{\"error\":\"" + escape(e.getMessage()) + "\"}");
        }
    }

    private void handleGetDetail(HttpExchange exchange, String query) throws IOException {
        try {
            int id = Integer.parseInt(query.split("=")[1]);
            Claim claim = claimService.getClaimById(id);
            sendResponse(exchange, 200, JsonUtil.toJson(claim));
        } catch (EntityNotFoundException | DataAccessException e) {
            sendResponse(exchange, 404, "{\"error\":\"" + escape(e.getMessage()) + "\"}");
        }
    }

    private void handleAddEvidence(HttpExchange exchange) throws IOException {
        String body = readBody(exchange);
        Map<String, Object> data = JsonUtil.fromJson(body, HashMap.class);

        try {
            int claimId = Integer.parseInt(String.valueOf(data.get("claimId")));
            Evidence evidence = claimService.addEvidence(
                    claimId,
                    (String) data.get("title"),
                    (String) data.get("url"),
                    (String) data.get("type"),
                    (String) data.get("description")
            );
            sendResponse(exchange, 201, JsonUtil.toJson(evidence));
        } catch (EntityNotFoundException | ValidationException | DataAccessException e) {
            sendResponse(exchange, 400, "{\"error\":\"" + escape(e.getMessage()) + "\"}");
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