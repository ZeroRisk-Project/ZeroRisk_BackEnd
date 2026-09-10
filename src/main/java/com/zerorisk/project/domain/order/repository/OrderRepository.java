package com.zerorisk.project.domain.order.repository;

import com.zerorisk.project.domain.order.entity.Order;
import com.zerorisk.project.domain.order.entity.OrderStatus;
import com.zerorisk.project.domain.order.entity.OrderType;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Page<Order> findByAccountId(Long accountId, Pageable pageable);
    Page<Order> findByAccountIdAndStatus(Long accountId, OrderStatus status, Pageable pageable);
    List<Order> findByStatusAndOrderType(OrderStatus status, OrderType orderType);
    List<Order> findByAccountIdInAndStatus(List<Long> accountIds, OrderStatus status);

    // 아직 체결 안 된(PENDING) 지정가 매수 주문들이 이미 걸어둔 금액 총합 - 새 주문을 받아줄지 판단할 때
    // "현재 잔고"가 아니라 "현재 잔고 - 이미 약속된 금액"을 봐야 한다. 네이티브 쿼리로 쓰는 이유는
    // JPQL에서 엔티티명 Order가 ORDER BY와 겹치는 예약어라 "FROM Order o" 형태를 피하기 위함.
    @Query(value = "SELECT COALESCE(SUM(LIMIT_PRICE * QUANTITY), 0) FROM ORDERS "
            + "WHERE ACCOUNT_ID = :accountId AND SIDE = 'BUY' AND ORDER_TYPE = 'LIMIT' AND STATUS = 'PENDING'",
            nativeQuery = true)
    BigDecimal sumPendingBuyReservedAmount(Long accountId);

    // 같은 이유로, 아직 체결 안 된 지정가 매도 주문들이 이미 걸어둔 수량 총합(종목별).
    @Query(value = "SELECT COALESCE(SUM(QUANTITY), 0) FROM ORDERS "
            + "WHERE ACCOUNT_ID = :accountId AND STOCK_ID = :stockId AND SIDE = 'SELL' AND ORDER_TYPE = 'LIMIT' AND STATUS = 'PENDING'",
            nativeQuery = true)
    BigDecimal sumPendingSellReservedQuantity(Long accountId, Long stockId);

    // 취소(cancelOrder)와 예약주문 체결 배치(tryFillPendingOrder)가 같은 주문을 동시에 건드리는
    // 레이스를 막기 위해 CRUD findById 자체에 락을 건다("Order"는 JPQL의 ORDER BY와 겹치는 예약어라
    // 커스텀 @Query("FROM Order o ...")는 피하고, 대신 표준 findById를 락 모드로 오버라이드한다).
    // 이 리포지토리에서 findById를 부르는 곳은 지금 cancelOrder/tryFillPendingOrder뿐이라 항상 락이
    // 걸려도 문제없다 - 락이 필요 없는 단순 조회가 나중에 필요해지면 별도 메서드를 새로 추가할 것.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Override
    Optional<Order> findById(Long id);
}