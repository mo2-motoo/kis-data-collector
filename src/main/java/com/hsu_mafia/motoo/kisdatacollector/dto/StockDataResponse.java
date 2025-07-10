package com.hsu_mafia.motoo.kisdatacollector.dto;

import com.hsu_mafia.motoo.kisdatacollector.domain.PeriodType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockDataResponse {
    private String stockCode;
    private String candleDateTime;
    private String openPrice;
    private String highPrice;
    private String lowPrice;
    private String closePrice;
    private String volume;
    private String tradeAmount;
    private PeriodType periodType;
}
