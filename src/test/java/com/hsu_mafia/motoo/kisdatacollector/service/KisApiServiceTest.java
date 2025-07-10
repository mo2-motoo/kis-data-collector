package com.hsu_mafia.motoo.kisdatacollector.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hsu_mafia.motoo.kisdatacollector.KisConfig;
import com.hsu_mafia.motoo.kisdatacollector.domain.PeriodType;
import com.hsu_mafia.motoo.kisdatacollector.dto.StockDataResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KisApiServiceTest {

    @Mock
    private KisConfig kisConfig;
    
    @Mock
    private RateLimitManager rateLimitManager;
    
    @Mock
    private RestTemplate restTemplate;
    
    private ObjectMapper objectMapper = new ObjectMapper();
    
    private KisApiService kisApiService;
    
    @BeforeEach
    void setUp() {
        kisApiService = new KisApiService(kisConfig, rateLimitManager, restTemplate, objectMapper);
    }
    
    @Test
    void fetchStockData_ShouldReturnStockDataList_WhenApiCallSucceeds() throws Exception {
        // Given
        String stockCode = "005930";
        String startDate = "20240101";
        String endDate = "20240101";
        PeriodType periodType = PeriodType.DAILY;
        
        String mockToken = "mock-token";
        String mockResponseJson = createMockStockDataResponse();
        
        ReflectionTestUtils.setField(kisApiService, "cachedAccessToken", mockToken);
        ReflectionTestUtils.setField(kisApiService, "tokenExpiredAt", LocalDateTime.now().plusHours(1));
        
        when(kisConfig.getBaseUrl()).thenReturn("https://api.example.com");
        when(kisConfig.getAppKey()).thenReturn("app-key");
        when(kisConfig.getAppSecret()).thenReturn("app-secret");
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(mockResponseJson, HttpStatus.OK));
        
        // When
        List<StockDataResponse> result = kisApiService.fetchStockData(stockCode, startDate, endDate, periodType);
        
        // Then
        assertThat(result).isNotEmpty();
        verify(rateLimitManager).waitForRateLimit();
        verify(restTemplate).exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class));
    }
    
    @Test
    void fetchStockData_ShouldReturnEmptyList_WhenApiCallFails() {
        // Given
        String stockCode = "005930";
        String startDate = "20240101";
        String endDate = "20240101";
        PeriodType periodType = PeriodType.DAILY;
        
        String mockToken = "mock-token";
        ReflectionTestUtils.setField(kisApiService, "cachedAccessToken", mockToken);
        ReflectionTestUtils.setField(kisApiService, "tokenExpiredAt", LocalDateTime.now().plusHours(1));
        
        when(kisConfig.getBaseUrl()).thenReturn("https://api.example.com");
        when(kisConfig.getAppKey()).thenReturn("app-key");
        when(kisConfig.getAppSecret()).thenReturn("app-secret");
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(HttpStatus.BAD_REQUEST));
        
        // When
        List<StockDataResponse> result = kisApiService.fetchStockData(stockCode, startDate, endDate, periodType);
        
        // Then
        assertThat(result).isEmpty();
    }
    
    @Test
    void fetchStockData_ShouldReturnEmptyList_WhenExceptionOccurs() {
        // Given
        String stockCode = "005930";
        String startDate = "20240101";
        String endDate = "20240101";
        PeriodType periodType = PeriodType.DAILY;
        
        String mockToken = "mock-token";
        ReflectionTestUtils.setField(kisApiService, "cachedAccessToken", mockToken);
        ReflectionTestUtils.setField(kisApiService, "tokenExpiredAt", LocalDateTime.now().plusHours(1));
        
        when(kisConfig.getBaseUrl()).thenReturn("https://api.example.com");
        when(kisConfig.getAppKey()).thenReturn("app-key");
        when(kisConfig.getAppSecret()).thenReturn("app-secret");
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new RuntimeException("Network error"));
        
        // When
        List<StockDataResponse> result = kisApiService.fetchStockData(stockCode, startDate, endDate, periodType);
        
        // Then
        assertThat(result).isEmpty();
    }
    
    @Test
    void buildApiUrl_ShouldReturnCorrectUrl_ForMinutePeriod() {
        // Given
        when(kisConfig.getBaseUrl()).thenReturn("https://api.example.com");
        PeriodType periodType = PeriodType.MINUTE;
        
        // When
        String result = ReflectionTestUtils.invokeMethod(kisApiService, "buildApiUrl", periodType);
        
        // Then
        assertThat(result).isEqualTo("https://api.example.com/uapi/domestic-stock/v1/quotations/inquire-daily-minutechartprice");
    }
    
    @Test
    void buildApiUrl_ShouldReturnCorrectUrl_ForDailyPeriod() {
        // Given
        when(kisConfig.getBaseUrl()).thenReturn("https://api.example.com");
        PeriodType periodType = PeriodType.DAILY;
        
        // When
        String result = ReflectionTestUtils.invokeMethod(kisApiService, "buildApiUrl", periodType);
        
        // Then
        assertThat(result).isEqualTo("https://api.example.com/uapi/domestic-stock/v1/quotations/inquire-daily-itemchartprice");
    }
    
    @Test
    void getTrId_ShouldReturnCorrectTrId_ForMinutePeriod() {
        // When
        String result = ReflectionTestUtils.invokeMethod(kisApiService, "getTrId", PeriodType.MINUTE);
        
        // Then
        assertThat(result).isEqualTo("FHKST03030100");
    }
    
    @Test
    void getTrId_ShouldReturnCorrectTrId_ForDailyPeriod() {
        // When
        String result = ReflectionTestUtils.invokeMethod(kisApiService, "getTrId", PeriodType.DAILY);
        
        // Then
        assertThat(result).isEqualTo("FHKST03010100");
    }
    
    @Test
    void parseStockDataResponse_ShouldReturnEmptyList_WhenJsonParsingFails() {
        // Given
        String invalidJson = "invalid json";
        String stockCode = "005930";
        PeriodType periodType = PeriodType.DAILY;
        
        // When
        List<StockDataResponse> result = ReflectionTestUtils.invokeMethod(
                kisApiService, "parseStockDataResponse", invalidJson, stockCode, periodType);
        
        // Then
        assertThat(result).isEmpty();
    }
    
    @Test
    void parseStockDataResponse_ShouldParseValidResponse() {
        // Given
        String validJson = createMockStockDataResponse();
        String stockCode = "005930";
        PeriodType periodType = PeriodType.DAILY;
        
        // When
        List<StockDataResponse> result = ReflectionTestUtils.invokeMethod(
                kisApiService, "parseStockDataResponse", validJson, stockCode, periodType);
        
        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStockCode()).isEqualTo(stockCode);
        assertThat(result.get(0).getOpenPrice()).isEqualTo("75000");
        assertThat(result.get(0).getClosePrice()).isEqualTo("75200");
    }
    
    private String createMockStockDataResponse() {
        return """
            {
                "output2": [
                    {
                        "stck_bsop_date": "20240101",
                        "stck_cntg_hour": "0900",
                        "stck_oprc": "75000",
                        "stck_hgpr": "75500",
                        "stck_lwpr": "74500",
                        "stck_clpr": "75200",
                        "acml_vol": "1000000",
                        "acml_tr_pbmn": "75200000000"
                    }
                ]
            }
            """;
    }
}