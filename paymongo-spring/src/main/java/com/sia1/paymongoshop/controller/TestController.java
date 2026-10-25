package com.sia1.paymongoshop.controller;

import com.sia1.paymongoshop.repository.OrderRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Test-only endpoints used by the Cypress E2E tests.
 * Only active when app.test-mode=true in application.properties.
 */
@RestController
@RequestMapping("/api/test")
@ConditionalOnProperty(name = "app.test-mode", havingValue = "true")
public class TestController {

    private final OrderRepository orderRepository;

    public TestController(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    /** Wipe all orders so every test starts from a clean database. */
    @PostMapping("/reset")
    public Map<String, Object> reset() {
        orderRepository.deleteAll();
        return Map.of("ok", true);
    }

    /** How many orders exist (optionally only those with a given status). */
    @GetMapping("/orders/count")
    public Map<String, Object> count(@RequestParam(required = false) String status) {
        long count = (status == null)
                ? orderRepository.count()
                : orderRepository.findAll().stream().filter(o -> status.equals(o.getStatus())).count();
        return Map.of("count", count);
    }

    /** Pretend PayMongo confirmed the payment (Cypress cannot make a real test payment). */
    @PostMapping("/orders/{code}/force-paid")
    public ResponseEntity<?> forcePaid(@PathVariable String code) {
        return orderRepository.findByOrderCode(code).<ResponseEntity<?>>map(order -> {
            order.setStatus("paid");
            orderRepository.save(order);
            return ResponseEntity.ok(Map.of("status", "paid"));
        }).orElse(ResponseEntity.notFound().build());
    }
}
