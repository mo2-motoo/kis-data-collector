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
public class BatchJobRequest {
    private String stockCode;
    private String startDate;
    private String endDate;
    private PeriodType periodType;
    private String jobName;
}
