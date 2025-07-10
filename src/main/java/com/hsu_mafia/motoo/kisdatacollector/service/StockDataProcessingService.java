package com.hsu_mafia.motoo.kisdatacollector.service;

import com.hsu_mafia.motoo.kisdatacollector.domain.PeriodType;
import com.hsu_mafia.motoo.kisdatacollector.domain.StockPrice;
import com.hsu_mafia.motoo.kisdatacollector.dto.StockDataResponse;
import com.hsu_mafia.motoo.kisdatacollector.repository.StockPriceRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class StockDataProcessingService {

    private final StockPriceRepository stockPriceRepository;

    public void processAndSaveStockData(List<StockDataResponse> dataList) {
        int savedCount = 0;
        int updatedCount = 0;
        int skippedCount = 0;

        for (StockDataResponse data : dataList) {
            try {
                LocalDateTime candleDateTime = parseCandleDateTime(data.getCandleDateTime(), data.getPeriodType());
                if (candleDateTime == null) {
                    skippedCount++;
                    continue;
                }

                boolean exists = stockPriceRepository.existsByStockCodeAndCandleDateTimeAndPeriodType(
                        data.getStockCode(), candleDateTime, data.getPeriodType());

                if (exists) {
                    // 기존 데이터 업데이트는 생략 (과거 데이터는 변경되지 않음)
                    skippedCount++;
                } else {
                    StockPrice stockPrice = StockPrice.builder()
                            .stockCode(data.getStockCode())
                            .candleDateTime(candleDateTime)
                            .periodType(data.getPeriodType())
                            .openPrice(data.getOpenPrice())
                            .highPrice(data.getHighPrice())
                            .lowPrice(data.getLowPrice())
                            .closePrice(data.getClosePrice())
                            .volume(data.getVolume())
                            .tradeAmount(data.getTradeAmount())
                            .build();

                    stockPriceRepository.save(stockPrice);
                    savedCount++;
                }

            } catch (Exception e) {
                log.error("주식 데이터 처리 중 오류 발생: {} {}",
                        data.getStockCode(), data.getCandleDateTime(), e);
                skippedCount++;
            }
        }

        log.info("주식 데이터 처리 완료 - 신규: {}, 업데이트: {}, 건너뜀: {}",
                savedCount, updatedCount, skippedCount);
    }

    private LocalDateTime parseCandleDateTime(String candleDateTime, PeriodType periodType) {
        try {
            if (periodType == PeriodType.MINUTE && candleDateTime.length() == 12) {
                // 분봉: YYYYMMDDHHMM
                return LocalDateTime.of(
                        Integer.parseInt(candleDateTime.substring(0, 4)),
                        Integer.parseInt(candleDateTime.substring(4, 6)),
                        Integer.parseInt(candleDateTime.substring(6, 8)),
                        Integer.parseInt(candleDateTime.substring(8, 10)),
                        Integer.parseInt(candleDateTime.substring(10, 12))
                );
            } else if (candleDateTime.length() >= 8) {
                // 일봉/주봉/월봉: YYYYMMDD
                return LocalDate.parse(candleDateTime.substring(0, 8), DateTimeFormatter.ofPattern("yyyyMMdd"))
                        .atStartOfDay();
            }
            return null;
        } catch (Exception e) {
            log.error("날짜 파싱 오류: {}", candleDateTime, e);
            return null;
        }
    }
}
