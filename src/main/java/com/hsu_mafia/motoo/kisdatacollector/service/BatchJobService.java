package com.hsu_mafia.motoo.kisdatacollector.service;

import com.hsu_mafia.motoo.kisdatacollector.domain.BatchJob;
import com.hsu_mafia.motoo.kisdatacollector.domain.BatchJobStatus;
import com.hsu_mafia.motoo.kisdatacollector.dto.BatchJobRequest;
import com.hsu_mafia.motoo.kisdatacollector.dto.StockDataResponse;
import com.hsu_mafia.motoo.kisdatacollector.repository.BatchJobRepository;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BatchJobService {

    private final BatchJobRepository batchJobRepository;
    private final KisApiService kisApiService;
    private final StockDataProcessingService dataProcessingService;

    public BatchJob createBatchJob(BatchJobRequest request) {
        BatchJob batchJob = BatchJob.builder()
                .jobName(request.getJobName())
                .stockCode(request.getStockCode())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .periodType(request.getPeriodType())
                .status(BatchJobStatus.PENDING)
                .totalCount(0)
                .processedCount(0)
                .successCount(0)
                .failedCount(0)
                .build();

        return batchJobRepository.save(batchJob);
    }

    @Async
    public CompletableFuture<Void> executeBatchJob(Long batchJobId) {
        BatchJob batchJob = batchJobRepository.findById(batchJobId)
                .orElseThrow(() -> new IllegalArgumentException("배치 작업을 찾을 수 없습니다: " + batchJobId));

        try {
            // 작업 시작
            batchJob.setStatus(BatchJobStatus.RUNNING);
            batchJob.setStartTime(LocalDateTime.now());
            batchJobRepository.save(batchJob);

            // 날짜 범위 생성 (역순)
            List<LocalDate> dateRange = generateDateRange(batchJob.getStartDate(), batchJob.getEndDate());
            batchJob.setTotalCount(dateRange.size());
            batchJobRepository.save(batchJob);

            log.info("배치 작업 시작: {} ({} ~ {}, 총 {}일)",
                    batchJob.getStockCode(), batchJob.getStartDate(), batchJob.getEndDate(), dateRange.size());

            for (LocalDate date : dateRange) {
                if (isWeekday(date)) {
                    String dateStr = date.format(DateTimeFormatter.ofPattern("yyyyMMdd"));

                    try {
                        List<StockDataResponse> dataList = kisApiService.fetchStockData(
                                batchJob.getStockCode(), dateStr, dateStr, batchJob.getPeriodType());

                        if (!dataList.isEmpty()) {
                            dataProcessingService.processAndSaveStockData(dataList);
                            batchJob.setSuccessCount(batchJob.getSuccessCount() + 1);
                        } else {
                            batchJob.setFailedCount(batchJob.getFailedCount() + 1);
                        }

                    } catch (Exception e) {
                        log.error("배치 작업 중 오류 발생: {} {}", batchJob.getStockCode(), dateStr, e);
                        batchJob.setFailedCount(batchJob.getFailedCount() + 1);
                        batchJob.setErrorMessage(e.getMessage());
                    }
                }

                batchJob.setProcessedCount(batchJob.getProcessedCount() + 1);
                batchJobRepository.save(batchJob);
            }

            // 작업 완료
            batchJob.setStatus(BatchJobStatus.COMPLETED);
            batchJob.setEndTime(LocalDateTime.now());
            batchJobRepository.save(batchJob);

            log.info("배치 작업 완료: {} (성공: {}, 실패: {})",
                    batchJob.getStockCode(), batchJob.getSuccessCount(), batchJob.getFailedCount());

        } catch (Exception e) {
            log.error("배치 작업 전체 오류: {}", batchJob.getStockCode(), e);
            batchJob.setStatus(BatchJobStatus.FAILED);
            batchJob.setErrorMessage(e.getMessage());
            batchJob.setEndTime(LocalDateTime.now());
            batchJobRepository.save(batchJob);
        }

        return CompletableFuture.completedFuture(null);
    }

    private List<LocalDate> generateDateRange(String startDateStr, String endDateStr) {
        LocalDate startDate = LocalDate.parse(startDateStr, DateTimeFormatter.ofPattern("yyyyMMdd"));
        LocalDate endDate = LocalDate.parse(endDateStr, DateTimeFormatter.ofPattern("yyyyMMdd"));

        List<LocalDate> dateRange = new ArrayList<>();
        LocalDate current = endDate;

        while (!current.isBefore(startDate)) {
            dateRange.add(current);
            current = current.minusDays(1);
        }

        return dateRange;
    }

    private boolean isWeekday(LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY;
    }

    public List<BatchJob> getPendingJobs() {
        return batchJobRepository.findPendingJobsOrderByCreatedAt(BatchJobStatus.PENDING);
    }

    public List<BatchJob> getJobsByStatus(BatchJobStatus status) {
        return batchJobRepository.findByStatus(status);
    }

    public BatchJob getJobById(Long id) {
        return batchJobRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("배치 작업을 찾을 수 없습니다: " + id));
    }
}
