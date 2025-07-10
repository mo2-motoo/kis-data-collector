package com.hsu_mafia.motoo.kisdatacollector.service;

import com.hsu_mafia.motoo.kisdatacollector.domain.PeriodType;
import com.hsu_mafia.motoo.kisdatacollector.dto.StockDataResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * IntelliJ에서 직접 실행할 수 있는 수동 테스트
 * 실제 API 키를 아래에 입력하고 테스트하세요
 * 
 * 주의: 실제 API 키를 커밋하지 마세요!
 */
@SpringBootTest
@TestPropertySource(properties = {
    "kis.base-url=https://openapi.koreainvestment.com:9443",
    "kis.app-key=YOUR_ACTUAL_APP_KEY_HERE",
    "kis.app-secret=YOUR_ACTUAL_APP_SECRET_HERE"
})
class KisApiServiceManualTest {

    @Autowired
    private KisApiService kisApiService;

    @Test
    void manualTest_fetchStockData() {
        // 실제 API 키를 위의 @TestPropertySource에 입력하고 테스트하세요
        
        // Given
        String stockCode = "005930"; // 삼성전자
        String startDate = "20240101";
        String endDate = "20240105";
        PeriodType periodType = PeriodType.DAILY;
        
        // When
        List<StockDataResponse> result = kisApiService.fetchStockData(stockCode, startDate, endDate, periodType);
        
        // Then
        System.out.println("=== API 테스트 결과 ===");
        System.out.println("결과 개수: " + (result != null ? result.size() : "null"));
        
        if (result != null && !result.isEmpty()) {
            System.out.println("첫 번째 데이터:");
            StockDataResponse firstData = result.get(0);
            System.out.println("- 종목코드: " + firstData.getStockCode());
            System.out.println("- 날짜시간: " + firstData.getCandleDateTime());
            System.out.println("- 시가: " + firstData.getOpenPrice());
            System.out.println("- 종가: " + firstData.getClosePrice());
            System.out.println("- 거래량: " + firstData.getVolume());
            
            // 검증
            assertThat(result).isNotNull();
            assertThat(firstData.getStockCode()).isEqualTo(stockCode);
            assertThat(firstData.getCandleDateTime()).isNotNull();
            assertThat(firstData.getOpenPrice()).isNotNull();
            assertThat(firstData.getClosePrice()).isNotNull();
        } else {
            System.out.println("데이터가 없습니다. API 키 확인 또는 날짜 범위를 확인하세요.");
        }
    }
    
    @Test
    void manualTest_invalidStockCode() {
        // Given
        String invalidStockCode = "999999";
        String startDate = "20240101";
        String endDate = "20240105";
        PeriodType periodType = PeriodType.DAILY;
        
        // When
        List<StockDataResponse> result = kisApiService.fetchStockData(invalidStockCode, startDate, endDate, periodType);
        
        // Then
        System.out.println("=== 잘못된 종목코드 테스트 ===");
        System.out.println("결과: " + (result != null ? result.size() + "개" : "null"));
        
        assertThat(result).isNotNull();
        // 잘못된 종목코드는 빈 결과를 반환할 것으로 예상
    }
}