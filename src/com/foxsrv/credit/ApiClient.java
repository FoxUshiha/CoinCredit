package com.foxsrv.credit;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ApiClient {

    private final HttpClient http;
    private final String base;

    public ApiClient(String baseUrl) {
        if (!baseUrl.endsWith("/")) baseUrl += "/";
        this.base = baseUrl;
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public static class ApiResponse {
        public boolean success;
        public String txId;
        public String raw;
    }

    /**
     * Calls POST /api/transfer/card with JSON { cardCode, toId, amount }
     * amount must be BigDecimal with up to 8 decimals (we will format with plain string).
     */
    public ApiResponse transferByCard(String cardCode, String toId, BigDecimal amount) throws IOException, InterruptedException {
        String url = base + "api/transfer/card";
        String amountStr = amount.setScale(8, java.math.RoundingMode.DOWN).stripTrailingZeros().toPlainString();
        // ensure there is a decimal point when needed (allow "1" or "0.10000000")
        String json = "{\"cardCode\":\"" + escapeJson(cardCode) + "\",\"toId\":\"" + escapeJson(toId) + "\",\"amount\":" + amountStr + "}";
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() >= 500) return null;
        String body = resp.body();
        ApiResponse out = new ApiResponse();
        out.raw = body;
        // parse success: look for "success": true/false
        Boolean success = parseBooleanField(body, "success");
        if (success == null) {
            // handle legacy: maybe { success: true, txId: ... } or { success: false }
            // try to detect keywords
            if (body.contains("\"success\":true") || body.contains("success:true")) success = true;
            else if (body.contains("\"success\":false") || body.contains("success:false")) success = false;
        }
        out.success = success != null && success;
        // find txId if present (string)
        String tx = parseStringField(body, "txId");
        if (tx == null) tx = parseStringField(body, "txid");
        out.txId = tx;
        return out;
    }

    private static Boolean parseBooleanField(String body, String field) {
        Pattern p = Pattern.compile("\"" + Pattern.quote(field) + "\"\\s*:\\s*(true|false)", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(body);
        if (m.find()) return Boolean.parseBoolean(m.group(1).toLowerCase());
        // also without quotes
        p = Pattern.compile("\\b" + Pattern.quote(field) + "\\b\\s*:\\s*(true|false)", Pattern.CASE_INSENSITIVE);
        m = p.matcher(body);
        if (m.find()) return Boolean.parseBoolean(m.group(1).toLowerCase());
        return null;
    }

    private static String parseStringField(String body, String field) {
        Pattern p = Pattern.compile("\"" + Pattern.quote(field) + "\"\\s*:\\s*\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(body);
        if (m.find()) return m.group(1);
        // try without quotes around field
        p = Pattern.compile("\\b" + Pattern.quote(field) + "\\b\\s*:\\s*\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);
        m = p.matcher(body);
        if (m.find()) return m.group(1);
        return null;
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
