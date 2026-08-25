package com.zerorisk.project.domain.pricealert.repository;

import com.zerorisk.project.domain.pricealert.entity.PriceAlert;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PriceAlertRepository extends JpaRepository<PriceAlert, Long> {

}