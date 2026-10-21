package com.sia1.paymongoshop.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sia1.paymongoshop.model.Order;
import com.sia1.paymongoshop.model.OrderItem;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * PayMongoService — the ONLY class in this app that talks to PayMongo.
 * Everything else (controller, entities) stays ignorant of PayMongo's API shape.
 */
@Service
public class PayMongoService {

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${paymongo.secret-key}")
    private String secretKey;

    @Value("${app.success-url}")
    private String successUrl;

    @Value("${app.cancel-url}")
    private String cancelUrl;

    private String authHeader() {
        String creds = secretKey + ":";
        return "Basic " + Base64.getEncoder().encodeToString(creds.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Creates a PayMongo Checkout Session for the given order and returns the
     * hosted checkout URL the customer should be redirected to, plus the
     * session id (save this on the order so you can verify payment later).
     */
    public CheckoutResult createCheckoutSession(Order order) throws Exception {
        List<Map<String, Object>> lineItems = new ArrayList<>();
        for (OrderItem item : order.getItems()) {
            lineItems.add(Map.of(
                    "name", item.getName(),
                    "quantity", item.getQuantity(),
                    "amount", item.getPriceCentavos(), // centavos
                    "currency", "PHP"
            ));
        }

        Map<String, Object> attributes = Map.of(
                "line_items", lineItems,
                "payment_method_types", List.of("card", "gcash", "paymaya"),
                "description", "Order #" + order.getOrderCode(),
                "send_email_receipt", false,
                "show_line_items", true,
                "success_url", successUrl + "?order=" + order.getOrderCode(),
                "cancel_url", cancelUrl + "?order=" + order.getOrderCode()
        );
        Map<String, Object> payload = Map.of("data", Map.of("attributes", attributes));

        String body = mapper.writeValueAsString(payload);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.paymongo.com/v1/checkout_sessions"))
                .header("Content-Type", "application/json")
                .header("Authorization", authHeader())
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode json = mapper.readTree(response.body());

        if (response.statusCode() != 200) {
            String detail = json.path("errors").path(0).path("detail").asText("Unknown PayMongo error");
            throw new RuntimeException("PayMongo error: " + detail);
        }

        String checkoutUrl = json.path("data").path("attributes").path("checkout_url").asText();
        String sessionId = json.path("data").path("id").asText();
        return new CheckoutResult(checkoutUrl, sessionId);
    }

    /**
     * Re-checks a checkout session's payments with PayMongo directly (server-to-server)
     * instead of trusting the browser's redirect. Returns true if a payment is "paid".
     */
    public boolean isSessionPaid(String checkoutSessionId) throws Exception {
        if (checkoutSessionId == null) return false;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.paymongo.com/v1/checkout_sessions/" + checkoutSessionId))
                .header("Authorization", authHeader())
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode json = mapper.readTree(response.body());
        JsonNode payments = json.path("data").path("attributes").path("payments");

        for (JsonNode payment : payments) {
            if ("paid".equals(payment.path("attributes").path("status").asText())) {
                return true;
            }
        }
        return false;
    }

    public record CheckoutResult(String checkoutUrl, String sessionId) {}
}
