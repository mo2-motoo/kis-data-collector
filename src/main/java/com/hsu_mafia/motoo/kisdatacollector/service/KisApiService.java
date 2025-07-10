package com.hsu_mafia.motoo.kisdatacollector.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hsu_mafia.motoo.kisdatacollector.KisConfig;
import com.hsu_mafia.motoo.kisdatacollector.domain.PeriodType;
import com.hsu_mafia.motoo.kisdatacollector.dto.StockDataResponse;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
@Slf4j
public class KisApiService {

    private final KisConfig kisConfig;
    private final RateLimitManager rateLimitManager;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    // 토큰 캐싱용
    private String cachedAccessToken;
    private LocalDateTime tokenExpiredAt;

    private String fetchAccessToken() throws Exception {
        if (cachedAccessToken != null && tokenExpiredAt != null &&
                LocalDateTime.now().isBefore(tokenExpiredAt.minusMinutes(10))) {
            return cachedAccessToken;
        }

        rateLimitManager.waitForRateLimit();

        String url = kisConfig.getBaseUrl() + "/oauth2/tokenP";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> body = Map.of(
                "grant_type", "client_credentials",
                "appkey", kisConfig.getAppKey(),
                "appsecret", kisConfig.getAppSecret()
        );

        HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

        if (response.getStatusCode().is2xxSuccessful()) {
            JsonNode jsonNode = objectMapper.readTree(response.getBody());
            cachedAccessToken = jsonNode.path("access_token").asText();
            tokenExpiredAt = LocalDateTime.now().plusHours(23);

            log.info("KIS API 토큰 획득 성공");
            return cachedAccessToken;
        } else {
            log.error("KIS API 토큰 획득 실패: {}", response.getStatusCode());
            throw new Exception("토큰 획득 실패");
        }
    }

    public List<StockDataResponse> fetchStockData(String stockCode, String startDate, String endDate, PeriodType periodType) {
        try {
            rateLimitManager.waitForRateLimit();

            String accessToken = fetchAccessToken();
            String url = buildApiUrl(periodType);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            headers.set("appkey", kisConfig.getAppKey());
            headers.set("appsecret", kisConfig.getAppSecret());
            headers.set("tr_id", getTrId(periodType));
            headers.set("custtype", "P");

            String fullUrl = UriComponentsBuilder.fromHttpUrl(url)
                    .queryParam("fid_cond_mrkt_div_code", "J")
                    .queryParam("fid_input_iscd", stockCode)
                    .queryParam("fid_input_date_1", startDate)
                    .queryParam("fid_input_date_2", endDate)
                    .queryParam("fid_period_div_code", periodType.getCode())
                    .queryParam("fid_org_adj_prc", "1")
                    .toUriString();

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    fullUrl, HttpMethod.GET, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                return parseStockDataResponse(response.getBody(), stockCode, periodType);
            } else {
                log.error("주식 데이터 API 호출 실패: {} - {}", stockCode, response.getStatusCode());
                return Collections.emptyList();
            }

        } catch (Exception e) {
            log.error("주식 데이터 API 호출 중 오류 발생: {}", stockCode, e);
            return Collections.emptyList();
        }
    }

    private String buildApiUrl(PeriodType periodType) {
        String baseUrl = kisConfig.getBaseUrl();
        return switch (periodType) {
            case MINUTE -> baseUrl + "/uapi/domestic-stock/v1/quotations/inquire-daily-minutechartprice";
            case DAILY, WEEKLY, MONTHLY -> baseUrl + "/uapi/domestic-stock/v1/quotations/inquire-daily-itemchartprice";
        };
    }

    private String getTrId(PeriodType periodType) {
        return switch (periodType) {
            case MINUTE -> "FHKST03030100";
            case DAILY, WEEKLY, MONTHLY -> "FHKST03010100";
        };
    }

    private List<StockDataResponse> parseStockDataResponse(String responseBody, String stockCode, PeriodType periodType) {
        try {
            JsonNode jsonNode = objectMapper.readTree(responseBody);
            JsonNode outputArray = jsonNode.path("output2");

            List<StockDataResponse> dataList = new ArrayList<>();

            if (outputArray.isArray()) {
                for (JsonNode item : outputArray) {
                    StockDataResponse data = StockDataResponse.builder()
                            .stockCode(stockCode)
                            .candleDateTime(item.path("stck_bsop_date").asText() +
                                    (periodType == PeriodType.MINUTE ? item.path("stck_cntg_hour").asText() : "0000"))
                            .openPrice(item.path("stck_oprc").asText())
                            .highPrice(item.path("stck_hgpr").asText())
                            .lowPrice(item.path("stck_lwpr").asText())
                            .closePrice(item.path("stck_clpr").asText())
                            .volume(item.path("acml_vol").asText())
                            .tradeAmount(item.path("acml_tr_pbmn").asText())
                            .periodType(periodType)
                            .build();

                    dataList.add(data);
                }
            }

            log.info("주식 데이터 파싱 완료: {} {} ({}건)", stockCode, periodType, dataList.size());
            return dataList;

        } catch (Exception e) {
            log.error("주식 데이터 파싱 중 오류 발생: {}", stockCode, e);
            return Collections.emptyList();
        }
    }
}
