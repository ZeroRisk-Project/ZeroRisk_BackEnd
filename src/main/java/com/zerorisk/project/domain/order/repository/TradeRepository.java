package com.zerorisk.project.domain.order.repository;

import com.zerorisk.project.domain.order.entity.Trade;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TradeRepository extends JpaRepository<Trade, Long> {

}