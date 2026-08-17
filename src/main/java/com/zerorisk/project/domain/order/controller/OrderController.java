package com.zerorisk.project.domain.order.controller;

import com.zerorisk.project.domain.order.dto.OrderCreateRequest;
import com.zerorisk.project.domain.order.dto.OrderResponse;
import com.zerorisk.project.domain.order.service.OrderService;
import com.zerorisk.project.global.security.CurrentUserId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @CurrentUserId Long userId,
            @Valid @RequestBody OrderCreateRequest request) {
        OrderResponse response = orderService.createOrder(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{orderId}")
    public ResponseEntity<Void> cancelOrder(
            @CurrentUserId Long userId,
            @PathVariable Long orderId) {
        orderService.cancelOrder(userId, orderId);
        return ResponseEntity.noContent().build();
    }
}