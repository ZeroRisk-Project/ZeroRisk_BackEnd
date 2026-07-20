package com.zerorisk.project.domain.stock.client.kis;

import com.zerorisk.project.domain.stock.client.kis.dto.StockMasterRow;
import java.util.List;

public interface KisStockMasterClient {

    List<StockMasterRow> fetchAll();
}