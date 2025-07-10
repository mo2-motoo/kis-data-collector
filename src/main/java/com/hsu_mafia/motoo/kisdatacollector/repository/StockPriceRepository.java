package com.hsu_mafia.motoo.kisdatacollector.repository;

import com.hsu_mafia.motoo.kisdatacollector.domain.PeriodType;
import com.hsu_mafia.motoo.kisdatacollector.domain.StockPrice;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface StockPriceRepository extends JpaRepository<StockPrice, Long> {
    List<StockPrice> findByStockCodeAndPeriodType(String stockCode, PeriodType periodType);
    List<StockPrice> findByStockCodeAndPeriodTypeAndCandleDateTimeBetween(
            String stockCode, PeriodType periodType, LocalDateTime start, LocalDateTime end);

    @Query("SELECT MAX(sp.candleDateTime) FROM StockPrice sp WHERE sp.stockCode = :stockCode AND sp.periodType = :periodType")
    Optional<LocalDateTime> findLatestCandleDateTime(@Param("stockCode") String stockCode, @Param("periodType") PeriodType periodType);

    @Query("SELECT MIN(sp.candleDateTime) FROM StockPrice sp WHERE sp.stockCode = :stockCode AND sp.periodType = :periodType")
    Optional<LocalDateTime> findEarliestCandleDateTime(@Param("stockCode") String stockCode, @Param("periodType") PeriodType periodType);

    boolean existsByStockCodeAndCandleDateTimeAndPeriodType(String stockCode, LocalDateTime candleDateTime, PeriodType periodType);

    @Query("SELECT COUNT(sp) FROM StockPrice sp WHERE sp.stockCode = :stockCode AND sp.periodType = :periodType")
    long countByStockCodeAndPeriodType(@Param("stockCode") String stockCode, @Param("periodType") PeriodType periodType);
}
