package com.zerorisk.project.domain.stock.dto;

public record ChartCandleResponse(
        String dateTime,
        Long open,
        Long high,
        Long low,
        Long close,
        Long volume) {
}