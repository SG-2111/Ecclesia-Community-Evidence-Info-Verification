package server.handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import exception.*;
import model.User;
import service.UserService;
import utils.JsonUtil;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class UserHandler implements HttpHandler {
    private final UserService userService;

    public UserHandler(UserService userService) {
        this.userService = userService;
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
            if (path.endsWith("/register") && "POST".equals(method)) {
                handleRegister(exchange);
            } else if (path.endsWith("/login") && "POST".equals(method)) {
                handleLogin(exchange);
            } else {
                sendResponse(exchange, 404, "{\"error\":\"Not found\"}");
            }
        } catch (Exception e) {
            sendResponse(exchange, 500, "{\"error\":\"" + escape(e.getMessage()) + "\"}");
        }
    }

    private void handleRegister(HttpExchange exchange) throws IOException {
        String body = readBody(exchange);
        Map<String, Object> data = JsonUtil.fromJson(body, HashMap.class);

        try {
            User user = userService.registerUser(
                    (String) data.get("username"),
                    (String) data.get("password"),
                    (String) data.get("fullName"),
                    data.getOrDefault("role", "USER").toString()
            );
            sendResponse(exchange, 201,
                    "{\"message\":\"Registration successful\",\"username\":\"" + user.getUsername() + "\"}");
        } catch (ValidationException | DataAccessException e) {
            sendResponse(exchange, 400, "{\"error\":\"" + escape(e.getMessage()) + "\"}");
        }
    }

    private void handleLogin(HttpExchange exchange) throws IOException {
        String body = readBody(exchange);
        Map<String, Object> data = JsonUtil.fromJson(body, HashMap.class);

        try {
            User user = userService.login(
                    (String) data.get("username"),
                    (String) data.get("password")
            );
            sendResponse(exchange, 200, JsonUtil.toJson(user));
        } catch (EntityNotFoundException | ValidationException | DataAccessException e) {
            sendResponse(exchange, 401, "{\"error\":\"" + escape(e.getMessage()) + "\"}");
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