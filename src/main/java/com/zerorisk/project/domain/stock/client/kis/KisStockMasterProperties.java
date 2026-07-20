package com.zerorisk.project.domain.stock.client.kis;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "kis.stock-master")
public record KisStockMasterProperties(String kospiUrl, String kosdaqUrl) {

}