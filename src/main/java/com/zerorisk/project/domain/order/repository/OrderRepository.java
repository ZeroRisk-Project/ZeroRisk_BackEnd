package com.zerorisk.project.domain.order.repository;

import com.zerorisk.project.domain.order.entity.Order;
import com.zerorisk.project.domain.order.entity.OrderStatus;
import com.zerorisk.project.domain.order.entity.OrderType;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Page<Order> findByAccountId(Long accountId, Pageable pageable);
    Page<Order> findByAccountIdAndStatus(Long accountId, OrderStatus status, Pageable pageable);
    List<Order> findByStatusAndOrderType(OrderStatus status, OrderType orderType);
    List<Order> findByAccountIdInAndStatus(List<Long> accountIds, OrderStatus status);
}