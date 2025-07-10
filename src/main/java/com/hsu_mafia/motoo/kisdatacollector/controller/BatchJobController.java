package com.hsu_mafia.motoo.kisdatacollector.controller;

import com.hsu_mafia.motoo.kisdatacollector.domain.BatchJob;
import com.hsu_mafia.motoo.kisdatacollector.domain.BatchJobStatus;
import com.hsu_mafia.motoo.kisdatacollector.domain.PeriodType;
import com.hsu_mafia.motoo.kisdatacollector.dto.BatchJobRequest;
import com.hsu_mafia.motoo.kisdatacollector.service.BatchJobService;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/batch-jobs")
@RequiredArgsConstructor
@Slf4j
public class BatchJobController {

    private final BatchJobService batchJobService;

    @PostMapping
    public ResponseEntity<BatchJob> createBatchJob(@RequestBody BatchJobRequest request) {
        try {
            BatchJob batchJob = batchJobService.createBatchJob(request);
            return ResponseEntity.ok(batchJob);
        } catch (Exception e) {
            log.error("배치 작업 생성 중 오류 발생", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/{id}/execute")
    public ResponseEntity<String> executeBatchJob(@PathVariable Long id) {
        try {
            batchJobService.executeBatchJob(id);
            return ResponseEntity.ok("배치 작업 실행 시작");
        } catch (Exception e) {
            log.error("배치 작업 실행 중 오류 발생", e);
            return ResponseEntity.internalServerError().body("배치 작업 실행 실패");
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<BatchJob> getBatchJob(@PathVariable Long id) {
        try {
            BatchJob batchJob = batchJobService.getJobById(id);
            return ResponseEntity.ok(batchJob);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping
    public ResponseEntity<List<BatchJob>> getBatchJobs(
            @RequestParam(required = false) BatchJobStatus status) {
        try {
            List<BatchJob> jobs = status != null ?
                    batchJobService.getJobsByStatus(status) :
                    batchJobService.getPendingJobs();
            return ResponseEntity.ok(jobs);
        } catch (Exception e) {
            log.error("배치 작업 조회 중 오류 발생", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/test/{stockCode}")
    public ResponseEntity<BatchJob> createTestBatchJob(@PathVariable String stockCode) {
        try {
            String yesterday = LocalDate.now().minusDays(1).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

            BatchJobRequest request = BatchJobRequest.builder()
                    .jobName("테스트 배치 - " + stockCode)
                    .stockCode(stockCode)
                    .startDate(yesterday)
                    .endDate(today)
                    .periodType(PeriodType.DAILY)
                    .build();

            BatchJob batchJob = batchJobService.createBatchJob(request);
            return ResponseEntity.ok(batchJob);
        } catch (Exception e) {
            log.error("테스트 배치 작업 생성 중 오류 발생", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
