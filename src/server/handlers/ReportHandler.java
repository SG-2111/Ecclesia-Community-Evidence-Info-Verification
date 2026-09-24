package server.handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import exception.DataAccessException;
import exception.EntityNotFoundException;
import model.Report;
import service.ReportService;
import utils.JsonUtil;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class ReportHandler implements HttpHandler {

    private final ReportService reportService;

    public ReportHandler(ReportService reportService) {
        this.reportService = reportService;
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
            if (path.endsWith("/claim") && "GET".equals(method)) {
                int claimId = Integer.parseInt(query.split("=")[1]);
                Report report = reportService.generateClaimReport(claimId);
                sendResponse(exchange, 200, JsonUtil.toJson(report));

            } else if (path.endsWith("/stats") && "GET".equals(method)) {
                String stats = reportService.generateSystemStatistics();
                String json = "{\"stats\":\"" + stats.replace("\n", "\\n")
                        .replace("\"", "\\\"") + "\"}";
                sendResponse(exchange, 200, json);

            } else {
                sendResponse(exchange, 404, "{\"error\":\"Not found\"}");
            }

        } catch (EntityNotFoundException | DataAccessException e) {
            sendResponse(exchange, 404, "{\"error\":\"" + escape(e.getMessage()) + "\"}");

        } catch (Exception e) {
            System.err.println("══════════════════════════════════════");
            System.err.println("REPORT ERROR");
            System.err.println("Error type: " + e.getClass().getName());
            System.err.println("Message: " + e.getMessage());
            System.err.println("══════════════════════════════════════");
            e.printStackTrace();
            sendResponse(exchange, 500, "{\"error\":\"" + escape(e.getMessage()) + "\"}");
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