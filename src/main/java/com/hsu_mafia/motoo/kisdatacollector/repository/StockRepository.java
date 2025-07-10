package com.hsu_mafia.motoo.kisdatacollector.repository;

import com.hsu_mafia.motoo.kisdatacollector.domain.Stock;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StockRepository extends JpaRepository<Stock, String> {
    List<Stock> findByIsActiveTrue();
    List<Stock> findByMarketType(String marketType);
    Optional<Stock> findByStockName(String stockName);
}
