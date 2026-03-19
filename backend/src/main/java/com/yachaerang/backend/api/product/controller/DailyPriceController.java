package com.yachaerang.backend.api.product.controller;

import com.yachaerang.backend.api.product.dto.response.DailyPriceResponseDto;
import com.yachaerang.backend.api.product.service.DailyPriceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@RequestMapping("/api/v1/daily-prices")
@RestController
@RequiredArgsConstructor
public class DailyPriceController {

    private final DailyPriceService dailyPriceService;
    /*
    특정 날짜의 기한을 정하여 조회 - 일별
     */
    @GetMapping("/{productCode}")
    public List<DailyPriceResponseDto.PriceRecordDto> getDailyPrice(
            @PathVariable(name = "productCode") String productCode,
            @RequestParam(name = "startDate") LocalDate startDate,
            @RequestParam(name = "endDate")LocalDate endDate
    ) {
        return dailyPriceService.getDailyPrice(productCode, startDate, endDate);
    }

    /*
    어제자 높은 가격 순위 보여주기
     */
    @GetMapping("/rank/high-prices")
    public List<DailyPriceResponseDto.RankDto> getHighPriceRank() {
        LocalDate yesterday = LocalDate.now(ZoneId.of("Asia/Seoul")).minusDays(1);
        return dailyPriceService.getHighPriceRank(yesterday);
    }

    /*
    어제자 낮은 가격 순위 보여주기
     */
    @GetMapping("/rank/low-prices")
    public List<DailyPriceResponseDto.RankDto> getLowPriceRank() {
        LocalDate yesterday = LocalDate.now(ZoneId.of("Asia/Seoul")).minusDays(1);
        return dailyPriceService.getLowPriceRank(yesterday);
    }

}
