package service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import model.AIAnalysis;
import model.Claim;
import model.Evidence;
import utils.AppLogger;
import utils.ConfigLoader;
import utils.JsonUtil;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.List;

public class AIService {

    private static final String API_KEY = ConfigLoader.get("gemini.api.key", "");
    private static final String API_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.6-flash:generateContent?key=";

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper mapper = JsonUtil.getMapper();
    private final ClaimService claimService;

    public AIService(ClaimService claimService) {
        this.claimService = claimService;
    }

    public void analyseClaimAsync(Claim claim) {
        Runnable task = () -> {
            try {
                // ---------- 1. Gather evidence ----------
                List<Evidence> evidenceList = claimService.getEvidenceForClaim(claim.getId());
                StringBuilder evidenceText = new StringBuilder();

                if (evidenceList.isEmpty()) {
                    evidenceText.append("No evidence provided.\n");
                } else {
                    int i = 1;
                    for (Evidence e : evidenceList) {
                        evidenceText.append(i++).append(". Title: ")
                                .append(e.getTitle() != null ? e.getTitle() : "N/A").append("\n");

                        // --- NEW: include the finding/description so AI can reason about it ---
                        if (e.getDescription() != null && !e.getDescription().trim().isEmpty()) {
                            evidenceText.append("   Finding: ").append(e.getDescription()).append("\n");
                        } else {
                            evidenceText.append("   Finding: (not provided)\n");
                        }

                        evidenceText.append("   Type: ")
                                .append(e.getType() != null ? e.getType() : "N/A")
                                .append(" | URL: ")
                                .append(e.getUrl() != null ? e.getUrl() : "N/A")
                                .append("\n");
                    }
                }

                // ---------- 2. Build prompt ----------
                String prompt = String.format(
                        "You are an expert fact-checker analyzing a claim for a community verification platform.\n\n" +
                                "CLAIM: %s\n" +
                                "DESCRIPTION: %s\n" +
                                "SOURCE: %s\n\n" +
                                "ATTACHED EVIDENCE:\n%s\n\n" +
                                "YOUR TASK:\n" +
                                "Assess how CREDIBLE and TRUSTWORTHY this claim is based on scientific/historical consensus and the attached evidence.\n\n" +
                                "SCORING GUIDE (be strict, use the full range):\n" +
                                "- 95-100: Universal scientific consensus, textbook-level fact (e.g., 'water boils at 100C at sea level')\n" +
                                "- 85-94:  Strong peer-reviewed consensus, minor uncertainty remains\n" +
                                "- 70-84:  Generally supported but with caveats, limitations, or ongoing debate\n" +
                                "- 55-69:  Mixed evidence, contested, or partially true\n" +
                                "- 40-54:  Weak, anecdotal, or unverifiable evidence\n" +
                                "- 25-39:  Mostly false or misleading\n" +
                                "- 10-24:  Clearly false but not dangerous\n" +
                                "- 0-9:    Dangerous misinformation, conspiracy, or health hazard\n\n" +
                                "IMPORTANT RULES:\n" +
                                "- 'relevance' means credibility/trustworthiness, NOT how important the claim is.\n" +
                                "- Be STRICT. Do not reward plausible-sounding claims without strong evidence.\n" +
                                "- Claims about supplements, alternative medicine, or wellness trends should score 40-70 unless backed by multiple high-quality RCTs.\n" +
                                "- Absolute language ('completely', 'always', 'never', '100%%') in a claim should lower the score.\n" +
                                "- If no evidence is attached, base your assessment on your own knowledge.\n\n" +
                                "EVIDENCE HANDLING:\n" +
                                "- The evidence list shows Title, Finding (description), Type, and URL for each item.\n" +
                                "- You CANNOT visit URLs. Rely on the 'Finding' field to know what each source says.\n" +
                                "- If the Finding indicates SUPPORT for the claim: raise score 10-20 points.\n" +
                                "- If the Finding indicates CONTRADICTION: lower score 20-30 points.\n" +
                                "- If Finding says '(not provided)', treat that evidence as neutral.\n\n" +
                                "CRITICAL: Respond with ONLY a JSON object, no markdown, no code fences.\n" +
                                "Use EXACTLY these keys: relevance, contradiction, summary\n" +
                                "- 'relevance'    : integer 0-100\n" +
                                "- 'contradiction': true or false\n" +
                                "- 'summary'      : string under 100 words explaining WHY this score\n\n" +
                                "Example: {\"relevance\": 88, \"contradiction\": false, \"summary\": \"Supported by CDC and WHO consensus on hand hygiene.\"}\n\n" +
                                "Now produce the JSON for the claim above:",
                        claim.getTitle(),
                        claim.getDescription() != null ? claim.getDescription() : "N/A",
                        claim.getSourceUrl() != null ? claim.getSourceUrl() : "N/A",
                        evidenceText.toString()
                );

                // ---------- 3. Call Gemini ----------
                String responseJson = callGemini(prompt);

                // ---------- 4. Parse top-level Gemini response ----------
                JsonNode root = mapper.readTree(responseJson);

                String content = root
                        .path("candidates").get(0)
                        .path("content")
                        .path("parts").get(0)
                        .path("text").asText();

                System.out.println("========== RAW GEMINI RESPONSE ==========");
                System.out.println(content);
                System.out.println("=========================================");

                String jsonPart = extractJson(content);
                System.out.println("[AI] Extracted JSON: " + jsonPart);

                JsonNode aiResult = mapper.readTree(jsonPart);

                // ---------- 5. Robust field reading ----------
                int relevance = 50;

                if (aiResult.has("credibility")) {
                    relevance = aiResult.path("credibility").asInt(50);
                } else if (aiResult.has("relevance")) {
                    relevance = aiResult.path("relevance").asInt(50);
                } else if (aiResult.has("relevanceScore")) {
                    relevance = aiResult.path("relevanceScore").asInt(50);
                } else if (aiResult.has("relevance_score")) {
                    relevance = aiResult.path("relevance_score").asInt(50);
                }

                boolean contradiction = aiResult.path("contradiction").asBoolean(false);

                String summary = "Analysis completed.";
                if (aiResult.has("summary")) {
                    summary = aiResult.path("summary").asText("Analysis completed.");
                } else if (aiResult.has("explanation")) {
                    summary = aiResult.path("explanation").asText("Analysis completed.");
                }

                System.out.println("[AI] Final relevance=" + relevance
                        + " contradiction=" + contradiction
                        + " summary=" + summary);

                // ---------- 6. Build AIAnalysis ----------
                AIAnalysis analysis = new AIAnalysis();
                analysis.setRelevanceScore(relevance);
                analysis.setContradiction(contradiction);
                analysis.setSummary(summary);
                analysis.setAnalysisDate(LocalDateTime.now());

                // ---------- 7. Persist ----------
                claim.setAiAnalysis(analysis);
                claim.setStatus("AI_ANALYSED");
                claimService.updateClaim(claim);

                AppLogger.log("AI analysis completed for claim #" + claim.getId());

            } catch (Exception e) {
                AppLogger.log("AI analysis failed for claim #" + claim.getId() + ": " + e.getMessage());
                e.printStackTrace();
                try {
                    AIAnalysis fallback = new AIAnalysis();
                    fallback.setRelevanceScore(50);
                    fallback.setContradiction(false);
                    fallback.setSummary("AI analysis unavailable. Manual review recommended.");
                    fallback.setAnalysisDate(LocalDateTime.now());
                    claim.setAiAnalysis(fallback);
                    claim.setStatus("AI_ANALYSED");
                    claimService.updateClaim(claim);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        };

        Thread aiThread = new Thread(task);
        aiThread.setDaemon(false);
        aiThread.start();
    }

    // ---------- Gemini HTTP call with model fallback ----------
    // Try these models in order. If one is busy (503) or unavailable (404), move to the next.
    private static final String[] MODELS = {
            "gemini-3.6-flash",
            "gemini-3.5-flash",
            "gemini-3.5-flash-lite",
            "gemini-3.1-flash-lite"
    };

    private String callGemini(String prompt) throws Exception {
        if (API_KEY == null || API_KEY.isEmpty()) {
            throw new RuntimeException("gemini.api.key not set in config.properties");
        }

        String requestBody = JsonUtil.toJson(new java.util.HashMap<String, Object>() {{
            put("contents", new Object[]{
                    new java.util.HashMap<String, Object>() {{
                        put("parts", new Object[]{
                                new java.util.HashMap<String, String>() {{
                                    put("text", prompt);
                                }}
                        });
                    }}
            });
            put("generationConfig", new java.util.HashMap<String, Object>() {{
                put("responseMimeType", "application/json");
                put("temperature", 0.2);
            }});
        }});

        Exception lastException = null;

        for (String model : MODELS) {
            for (int attempt = 1; attempt <= 2; attempt++) {
                try {
                    String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                            + model + ":generateContent?key=" + API_KEY;

                    System.out.println("[AI] Trying " + model + " (attempt " + attempt + ")");

                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(url))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                            .build();

                    HttpResponse<String> response = httpClient.send(request,
                            HttpResponse.BodyHandlers.ofString());

                    int code = response.statusCode();

                    if (code == 200) {
                        System.out.println("[AI] Success with " + model);
                        return response.body();
                    }

                    if (code == 503 || code == 429) {
                        System.out.println("[AI] " + model + " busy (" + code + "), retrying...");
                        Thread.sleep(1500L * attempt);
                        lastException = new RuntimeException("Model " + model + " busy (HTTP " + code + ")");
                        continue;
                    }

                    if (code == 404) {
                        System.out.println("[AI] " + model + " not available (404), trying next model...");
                        lastException = new RuntimeException("Model " + model + " not found");
                        break;
                    }

                    lastException = new RuntimeException("Gemini API error " + code
                            + " on " + model + ": " + response.body());
                    break;

                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw ie;
                } catch (Exception e) {
                    lastException = e;
                    break;
                }
            }
        }

        throw lastException != null
                ? lastException
                : new RuntimeException("All Gemini models unavailable");
    }

    // ---------- Extract JSON from possibly markdown-wrapped text ----------
    private String extractJson(String content) {
        if (content == null) return "{}";

        content = content.replaceAll("(?s)```json\\s*", "")
                .replaceAll("(?s)```\\s*", "")
                .trim();

        int start = content.indexOf('{');
        int end = content.lastIndexOf('}');
        if (start == -1 || end == -1 || end <= start) return "{}";

        String candidate = content.substring(start, end + 1);

        try {
            mapper.readTree(candidate);
            return candidate;
        } catch (Exception ignored) {
            // fall through to balanced scan
        }

        int depth = 0;
        int firstOpen = -1;
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == '{') {
                if (depth == 0) firstOpen = i;
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0 && firstOpen != -1) {
                    return content.substring(firstOpen, i + 1);
                }
            }
        }
        return "{}";
    }
}