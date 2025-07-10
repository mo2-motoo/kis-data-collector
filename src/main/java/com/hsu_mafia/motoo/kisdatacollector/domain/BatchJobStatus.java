package com.hsu_mafia.motoo.kisdatacollector.domain;

import lombok.Getter;

@Getter
public enum BatchJobStatus {
    PENDING("대기"),
    RUNNING("실행중"),
    COMPLETED("완료"),
    FAILED("실패"),
    CANCELLED("취소");

    private final String description;

    BatchJobStatus(String description) {
        this.description = description;
    }

}
