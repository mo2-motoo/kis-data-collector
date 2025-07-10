package com.hsu_mafia.motoo.kisdatacollector.service;

import com.hsu_mafia.motoo.kisdatacollector.domain.PeriodType;
import com.hsu_mafia.motoo.kisdatacollector.dto.StockDataResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * 실제 KIS API와의 통합 테스트
 * 실행하려면 시스템 프로퍼티 설정: -Dintegration.test=true
 * 그리고 실제 KIS API 키와 시크릿이 application-test.yml에 설정되어 있어야 함
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "kis.base-url=https://openapi.koreainvestment.com:9443",
    "kis.app-key=${KIS_APP_KEY}",
    "kis.app-secret=${KIS_APP_SECRET}"
})
//@EnabledIfSystemProperty(named = "integration.test", matches = "true")
class KisApiServiceIntegrationTest {

    @Autowired
    private KisApiService kisApiService;

    @Test
    void fetchStockData_ShouldReturnRealData_WhenConnectedToKisApi() {
        // Given
        String appKey = System.getenv("KIS_APP_KEY");
        String appSecret = System.getenv("KIS_APP_SECRET");
        
        // 환경변수 상태 출력 (디버깅용)
        System.out.println("KIS_APP_KEY 환경변수: " + (appKey != null ? "설정됨" : "미설정"));
        System.out.println("KIS_APP_SECRET 환경변수: " + (appSecret != null ? "설정됨" : "미설정"));
        
        // 실제 API 키가 설정되어 있지 않으면 테스트 건너뛰기
        assumeTrue(appKey != null && !appKey.equals("dummy_key"),
                "KIS_APP_KEY 환경변수가 설정되지 않음");
        assumeTrue(appSecret != null && !appSecret.equals("dummy_secret"),
                "KIS_APP_SECRET 환경변수가 설정되지 않음");
        
        String stockCode = "005930"; // 삼성전자
        String startDate = "20240101";
        String endDate = "20240105";
        PeriodType periodType = PeriodType.DAILY;
        
        // When
        List<StockDataResponse> result = kisApiService.fetchStockData(stockCode, startDate, endDate, periodType);
        
        // Then
        assertThat(result).isNotNull();
        // 실제 데이터가 있을 경우 검증
        if (!result.isEmpty()) {
            StockDataResponse firstData = result.get(0);
            assertThat(firstData.getStockCode()).isEqualTo(stockCode);
            assertThat(firstData.getCandleDateTime()).isNotNull();
            assertThat(firstData.getOpenPrice()).isNotNull();
            assertThat(firstData.getClosePrice()).isNotNull();
            assertThat(firstData.getPeriodType()).isEqualTo(periodType);
        }
    }
    
    @Test
    void fetchStockData_ShouldHandleInvalidStockCode() {
        // Given
        String appKey = System.getenv("KIS_APP_KEY");
        String appSecret = System.getenv("KIS_APP_SECRET");

        assumeTrue(appKey != null && !appKey.equals("dummy_key"),
                "KIS_APP_KEY 환경변수가 설정되지 않음");
        assumeTrue(appSecret != null && !appSecret.equals("dummy_secret"),
                "KIS_APP_SECRET 환경변수가 설정되지 않음");
        
        String invalidStockCode = "999999";
        String startDate = "20240101";
        String endDate = "20240105";
        PeriodType periodType = PeriodType.DAILY;
        
        // When
        List<StockDataResponse> result = kisApiService.fetchStockData(invalidStockCode, startDate, endDate, periodType);
        
        // Then
        // 잘못된 종목코드의 경우 빈 리스트나 오류 응답
        assertThat(result).isNotNull();
    }
}
