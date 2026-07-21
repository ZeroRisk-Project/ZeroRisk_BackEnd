package com.zerorisk.project.domain.stock.repository;

import com.zerorisk.project.domain.stock.entity.Stock;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StockRepository extends JpaRepository<Stock, Long> {

    Optional<Stock> findByCode(String code);

    @Query("select s from Stock s where s.active = true "
            + "and (s.code like concat(:keyword, '%') or s.name like concat('%', :keyword, '%'))")
    Page<Stock> search(@Param("keyword") String keyword, Pageable pageable);
}