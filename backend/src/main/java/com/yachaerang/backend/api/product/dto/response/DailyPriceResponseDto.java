package com.yachaerang.backend.api.product.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public class DailyPriceResponseDto {

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PriceRecordDto {
        private LocalDate priceDate;
        private Long price;
        private Long priceChange;
        private BigDecimal priceChangeRate;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RankDto {
        private String productName;
        private String productCode;
        private String itemName;
        private String kindName;
        private String unit;
        private Long price;
        private Long priceChange;
        private BigDecimal priceChangeRate;
    }
}
