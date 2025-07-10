package com.hsu_mafia.motoo.kisdatacollector.domain;

import lombok.Getter;

@Getter
public enum PeriodType {
    MINUTE("M", "분봉"),
    DAILY("D", "일봉"),
    WEEKLY("W", "주봉"),
    MONTHLY("M", "월봉");

    private final String code;
    private final String description;

    PeriodType(String code, String description) {
        this.code = code;
        this.description = description;
    }

}
