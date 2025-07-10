package com.hsu_mafia.motoo.kisdatacollector.controller;

import com.hsu_mafia.motoo.kisdatacollector.domain.PeriodType;
import com.hsu_mafia.motoo.kisdatacollector.domain.Stock;
import com.hsu_mafia.motoo.kisdatacollector.domain.StockPrice;
import com.hsu_mafia.motoo.kisdatacollector.repository.StockPriceRepository;
import com.hsu_mafia.motoo.kisdatacollector.repository.StockRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stocks")
@RequiredArgsConstructor
@Slf4j
public class StockDataController {

    private final StockRepository stockRepository;
    private final StockPriceRepository stockPriceRepository;

    @GetMapping
    public ResponseEntity<List<Stock>> getAllStocks() {
        try {
            List<Stock> stocks = stockRepository.findByIsActiveTrue();
            return ResponseEntity.ok(stocks);
        } catch (Exception e) {
            log.error("주식 목록 조회 중 오류 발생", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{stockCode}")
    public ResponseEntity<Stock> getStock(@PathVariable String stockCode) {
        try {
            Stock stock = stockRepository.findById(stockCode)
                    .orElseThrow(() -> new IllegalArgumentException("주식을 찾을 수 없습니다: " + stockCode));
            return ResponseEntity.ok(stock);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{stockCode}/prices")
    public ResponseEntity<List<StockPrice>> getStockPrices(
            @PathVariable String stockCode,
            @RequestParam PeriodType periodType,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        try {
            List<StockPrice> prices;

            if (startDate != null && endDate != null) {
                LocalDateTime start = LocalDate.parse(startDate, DateTimeFormatter.ofPattern("yyyyMMdd")).atStartOfDay();
                LocalDateTime end = LocalDate.parse(endDate, DateTimeFormatter.ofPattern("yyyyMMdd")).atTime(23, 59, 59);

                prices = stockPriceRepository.findByStockCodeAndPeriodTypeAndCandleDateTimeBetween(
                        stockCode, periodType, start, end);
            } else {
                prices = stockPriceRepository.findByStockCodeAndPeriodType(stockCode, periodType);
            }

            return ResponseEntity.ok(prices);
        } catch (Exception e) {
            log.error("주식 가격 조회 중 오류 발생", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{stockCode}/prices/latest")
    public ResponseEntity<LocalDateTime> getLatestPriceDate(
            @PathVariable String stockCode,
            @RequestParam PeriodType periodType) {
        try {
            Optional<LocalDateTime> latestDate = stockPriceRepository.findLatestCandleDateTime(stockCode, periodType);
            return latestDate.map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("최신 가격 날짜 조회 중 오류 발생", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{stockCode}/prices/count")
    public ResponseEntity<Long> getStockPriceCount(
            @PathVariable String stockCode,
            @RequestParam PeriodType periodType) {
        try {
            long count = stockPriceRepository.countByStockCodeAndPeriodType(stockCode, periodType);
            return ResponseEntity.ok(count);
        } catch (Exception e) {
            log.error("주식 가격 개수 조회 중 오류 발생", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
