package com.hsu_mafia.motoo.kisdatacollector;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "kis")
@Data
public class KisConfig {
    private String appKey;
    private String appSecret;
    private String accountNumber;
    private String accountProductCode;
    private String grantType;
    private String scope;
    private String baseUrl;
    private String sandboxUrl;
    private int tokenExpirationMinutes;
}
