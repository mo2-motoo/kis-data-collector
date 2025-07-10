package com.hsu_mafia.motoo.kisdatacollector.service;

import com.hsu_mafia.motoo.kisdatacollector.domain.BatchJob;
import com.hsu_mafia.motoo.kisdatacollector.domain.PeriodType;
import com.hsu_mafia.motoo.kisdatacollector.dto.BatchJobRequest;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
@EnableScheduling
public class BatchSchedulerService {

    private final BatchJobService batchJobService;

    @Value("${stock.codes:005930,000660,035420}")
    private String stockCodesConfig;

    @Scheduled(cron = "0 0 1 * * *")
    public void executePendingBatchJobs() {
        log.info("대기 중인 배치 작업 실행 시작");

        List<BatchJob> pendingJobs = batchJobService.getPendingJobs();

        for (BatchJob job : pendingJobs) {
            try {
                batchJobService.executeBatchJob(job.getId());
                Thread.sleep(5000);
            } catch (Exception e) {
                log.error("배치 작업 실행 중 오류 발생: {}", job.getId(), e);
            }
        }

        log.info("대기 중인 배치 작업 실행 완료");
    }

    @Scheduled(cron = "0 0 2 * * SUN")
    public void scheduledWeeklyBackfill() {
        log.info("주간 백필 작업 시작");

        String weekAgo = LocalDate.now().minusDays(7).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        List<String> stockCodes = Arrays.asList(stockCodesConfig.split(","));

        for (String stockCode : stockCodes) {
            try {
                BatchJobRequest request = BatchJobRequest.builder()
                        .jobName("주간 백필 - " + stockCode)
                        .stockCode(stockCode.trim())
                        .startDate(weekAgo)
                        .endDate(today)
                        .periodType(PeriodType.DAILY)
                        .build();

                batchJobService.createBatchJob(request);
                log.info("주간 백필 작업 생성: {}", stockCode);

            } catch (Exception e) {
                log.error("주간 백필 작업 생성 중 오류 발생: {}", stockCode, e);
            }
        }

        log.info("주간 백필 작업 생성 완료");
    }
}
