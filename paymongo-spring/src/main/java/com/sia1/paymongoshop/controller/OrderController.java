package com.sia1.paymongoshop.controller;

import com.sia1.paymongoshop.dto.CreateOrderRequest;
import com.sia1.paymongoshop.dto.OrderResponse;
import com.sia1.paymongoshop.model.Order;
import com.sia1.paymongoshop.model.OrderItem;
import com.sia1.paymongoshop.repository.OrderRepository;
import com.sia1.paymongoshop.service.PayMongoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderRepository orderRepository;
    private final PayMongoService payMongoService;

    public OrderController(OrderRepository orderRepository, PayMongoService payMongoService) {
        this.orderRepository = orderRepository;
        this.payMongoService = payMongoService;
    }

    /** POST /api/orders — create an order from the cart, then create a PayMongo checkout session. */
    @PostMapping
    public ResponseEntity<?> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        int total = request.getItems().stream()
                .mapToInt(i -> i.getPriceCentavos() * i.getQuantity())
                .sum();

        Order order = new Order();
        order.setOrderCode("ORD-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase());
        order.setStatus("pending");
        order.setTotalCentavos(total);

        for (CreateOrderRequest.Item i : request.getItems()) {
            OrderItem item = new OrderItem();
            item.setName(i.getName());
            item.setPriceCentavos(i.getPriceCentavos());
            item.setQuantity(i.getQuantity());
            item.setOrder(order);
            order.getItems().add(item);
        }

        order = orderRepository.save(order);

        try {
            PayMongoService.CheckoutResult result = payMongoService.createCheckoutSession(order);
            order.setCheckoutSessionId(result.sessionId());
            orderRepository.save(order);

            return ResponseEntity.ok(new OrderResponse(
                    order.getOrderCode(), order.getStatus(), order.getTotalCentavos(), result.checkoutUrl()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(502).body(Map.of(
                    "error", "Could not create PayMongo checkout session",
                    "detail", e.getMessage()
            ));
        }
    }

    /** GET /api/orders/{code} — fetch current order status. */
    @GetMapping("/{code}")
    public ResponseEntity<?> getOrder(@PathVariable String code) {
        return orderRepository.findByOrderCode(code)
                .<ResponseEntity<?>>map(order -> ResponseEntity.ok(new OrderResponse(
                        order.getOrderCode(), order.getStatus(), order.getTotalCentavos(), null
                )))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /api/orders/{code}/verify — called by success.html after PayMongo redirects back.
     * Re-checks payment status directly with PayMongo (server-to-server) before trusting it.
     */
    @GetMapping("/{code}/verify")
    public ResponseEntity<?> verifyPayment(@PathVariable String code) {
        return orderRepository.findByOrderCode(code).<ResponseEntity<?>>map(order -> {
            try {
                boolean paid = payMongoService.isSessionPaid(order.getCheckoutSessionId());
                if (paid && !"paid".equals(order.getStatus())) {
                    order.setStatus("paid");
                    orderRepository.save(order);
                }
                return ResponseEntity.ok(new OrderResponse(
                        order.getOrderCode(), order.getStatus(), order.getTotalCentavos(), null
                ));
            } catch (Exception e) {
                return ResponseEntity.status(502).body(Map.of("error", e.getMessage()));
            }
        }).orElse(ResponseEntity.notFound().build());
    }

    /** POST /api/orders/{code}/cancel — mark a pending order cancelled (called from cancel.html). */
    @PostMapping("/{code}/cancel")
    public ResponseEntity<?> cancelOrder(@PathVariable String code) {
        return orderRepository.findByOrderCode(code).<ResponseEntity<?>>map(order -> {
            if ("pending".equals(order.getStatus())) {
                order.setStatus("cancelled");
                orderRepository.save(order);
            }
            return ResponseEntity.ok(new OrderResponse(
                    order.getOrderCode(), order.getStatus(), order.getTotalCentavos(), null
            ));
        }).orElse(ResponseEntity.notFound().build());
    }
}
