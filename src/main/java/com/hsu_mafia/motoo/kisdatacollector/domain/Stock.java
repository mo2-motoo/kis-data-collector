package com.hsu_mafia.motoo.kisdatacollector.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "stocks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Stock {
    @Id
    @Column(name = "stock_code")
    private String stockCode;

    @Column(name = "stock_name")
    private String stockName;

    @Column(name = "market_type")
    private String marketType; // KOSPI, KOSDAQ, KONEX

    @Column(name = "sector")
    private String sector;

    @Column(name = "is_active")
    private boolean isActive;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (!this.isActive) {
            this.isActive = true;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
