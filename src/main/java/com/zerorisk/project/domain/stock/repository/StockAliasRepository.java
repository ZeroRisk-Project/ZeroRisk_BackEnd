package com.zerorisk.project.domain.stock.repository;

import com.zerorisk.project.domain.stock.entity.StockAlias;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface StockAliasRepository extends JpaRepository<StockAlias, Long> {

    @Query("select a from StockAlias a join fetch a.stock s where s.active = true")
    List<StockAlias> findAllWithActiveStock();
}