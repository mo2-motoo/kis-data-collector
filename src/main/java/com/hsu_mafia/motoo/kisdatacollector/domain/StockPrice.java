package com.hsu_mafia.motoo.kisdatacollector.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "stock_prices",
        uniqueConstraints = @UniqueConstraint(columnNames = {"stock_code", "candle_date_time", "period_type"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockPrice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stock_code", nullable = false)
    private String stockCode;

    @Column(name = "candle_date_time", nullable = false)
    private LocalDateTime candleDateTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "period_type", nullable = false)
    private PeriodType periodType; // MINUTE, DAILY, WEEKLY, MONTHLY

    @Column(name = "open_price")
    private String openPrice;

    @Column(name = "high_price")
    private String highPrice;

    @Column(name = "low_price")
    private String lowPrice;

    @Column(name = "close_price")
    private String closePrice;

    @Column(name = "volume")
    private String volume;

    @Column(name = "trade_amount")
    private String tradeAmount;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
