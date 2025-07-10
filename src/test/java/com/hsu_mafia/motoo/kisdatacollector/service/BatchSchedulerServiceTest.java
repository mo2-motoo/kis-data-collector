package com.hsu_mafia.motoo.kisdatacollector.service;

import com.hsu_mafia.motoo.kisdatacollector.domain.BatchJob;
import com.hsu_mafia.motoo.kisdatacollector.domain.BatchJobStatus;
import com.hsu_mafia.motoo.kisdatacollector.domain.PeriodType;
import com.hsu_mafia.motoo.kisdatacollector.dto.BatchJobRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BatchSchedulerServiceTest {

    @Mock
    private BatchJobService batchJobService;
    
    private BatchSchedulerService batchSchedulerService;
    
    @BeforeEach
    void setUp() {
        batchSchedulerService = new BatchSchedulerService(batchJobService);
        ReflectionTestUtils.setField(batchSchedulerService, "stockCodesConfig", "005930,000660,035420");
    }
    
    @Test
    void executePendingBatchJobs_ShouldExecuteAllPendingJobs() {
        // Given
        List<BatchJob> pendingJobs = Arrays.asList(
                BatchJob.builder().id(1L).status(BatchJobStatus.PENDING).build(),
                BatchJob.builder().id(2L).status(BatchJobStatus.PENDING).build(),
                BatchJob.builder().id(3L).status(BatchJobStatus.PENDING).build()
        );
        
        when(batchJobService.getPendingJobs()).thenReturn(pendingJobs);
        
        // When
        batchSchedulerService.executePendingBatchJobs();
        
        // Then
        verify(batchJobService).getPendingJobs();
        verify(batchJobService, times(3)).executeBatchJob(anyLong());
        verify(batchJobService).executeBatchJob(1L);
        verify(batchJobService).executeBatchJob(2L);
        verify(batchJobService).executeBatchJob(3L);
    }
    
    @Test
    void executePendingBatchJobs_ShouldHandleEmptyPendingJobs() {
        // Given
        when(batchJobService.getPendingJobs()).thenReturn(Arrays.asList());
        
        // When
        batchSchedulerService.executePendingBatchJobs();
        
        // Then
        verify(batchJobService).getPendingJobs();
        verify(batchJobService, never()).executeBatchJob(anyLong());
    }
    
    @Test
    void executePendingBatchJobs_ShouldContinueExecution_WhenOneJobFails() {
        // Given
        List<BatchJob> pendingJobs = Arrays.asList(
                BatchJob.builder().id(1L).status(BatchJobStatus.PENDING).build(),
                BatchJob.builder().id(2L).status(BatchJobStatus.PENDING).build(),
                BatchJob.builder().id(3L).status(BatchJobStatus.PENDING).build()
        );
        
        when(batchJobService.getPendingJobs()).thenReturn(pendingJobs);
        doThrow(new RuntimeException("Job execution failed")).when(batchJobService).executeBatchJob(2L);
        
        // When
        batchSchedulerService.executePendingBatchJobs();
        
        // Then
        verify(batchJobService).getPendingJobs();
        verify(batchJobService, times(3)).executeBatchJob(anyLong());
        verify(batchJobService).executeBatchJob(1L);
        verify(batchJobService).executeBatchJob(2L);
        verify(batchJobService).executeBatchJob(3L);
    }
    
    @Test
    void scheduledWeeklyBackfill_ShouldCreateJobsForAllStockCodes() {
        // Given
        BatchJob createdJob = BatchJob.builder().id(1L).build();
        when(batchJobService.createBatchJob(any(BatchJobRequest.class))).thenReturn(createdJob);
        
        // When
        batchSchedulerService.scheduledWeeklyBackfill();
        
        // Then
        ArgumentCaptor<BatchJobRequest> requestCaptor = ArgumentCaptor.forClass(BatchJobRequest.class);
        verify(batchJobService, times(3)).createBatchJob(requestCaptor.capture());
        
        List<BatchJobRequest> capturedRequests = requestCaptor.getAllValues();
        assertThat(capturedRequests).hasSize(3);
        
        // 각 주식 코드에 대해 작업이 생성되었는지 확인
        assertThat(capturedRequests.get(0).getStockCode()).isEqualTo("005930");
        assertThat(capturedRequests.get(1).getStockCode()).isEqualTo("000660");
        assertThat(capturedRequests.get(2).getStockCode()).isEqualTo("035420");
        
        // 모든 작업이 DAILY 타입이고 올바른 기간을 가지는지 확인
        for (BatchJobRequest request : capturedRequests) {
            assertThat(request.getPeriodType()).isEqualTo(PeriodType.DAILY);
            assertThat(request.getJobName()).contains("주간 백필");
            assertThat(request.getStartDate()).isNotNull();
            assertThat(request.getEndDate()).isNotNull();
        }
    }
    
    @Test
    void scheduledWeeklyBackfill_ShouldSetCorrectDateRange() {
        // Given
        BatchJob createdJob = BatchJob.builder().id(1L).build();
        when(batchJobService.createBatchJob(any(BatchJobRequest.class))).thenReturn(createdJob);
        
        // When
        batchSchedulerService.scheduledWeeklyBackfill();
        
        // Then
        ArgumentCaptor<BatchJobRequest> requestCaptor = ArgumentCaptor.forClass(BatchJobRequest.class);
        verify(batchJobService, times(3)).createBatchJob(requestCaptor.capture());
        
        BatchJobRequest request = requestCaptor.getValue();
        
        // 날짜 형식 확인 (YYYYMMDD)
        assertThat(request.getStartDate()).hasSize(8);
        assertThat(request.getEndDate()).hasSize(8);
        assertThat(request.getStartDate()).matches("\\d{8}");
        assertThat(request.getEndDate()).matches("\\d{8}");
        
        // 시작 날짜가 종료 날짜보다 이전인지 확인
        assertThat(request.getStartDate()).isLessThanOrEqualTo(request.getEndDate());
    }
    
    @Test
    void scheduledWeeklyBackfill_ShouldContinueCreation_WhenOneJobCreationFails() {
        // Given
        BatchJob createdJob = BatchJob.builder().id(1L).build();
        when(batchJobService.createBatchJob(any(BatchJobRequest.class)))
                .thenReturn(createdJob)
                .thenThrow(new RuntimeException("Job creation failed"))
                .thenReturn(createdJob);
        
        // When
        batchSchedulerService.scheduledWeeklyBackfill();
        
        // Then
        verify(batchJobService, times(3)).createBatchJob(any(BatchJobRequest.class));
    }
    
    @Test
    void scheduledWeeklyBackfill_ShouldHandleEmptyStockCodes() {
        // Given
        ReflectionTestUtils.setField(batchSchedulerService, "stockCodesConfig", "");
        
        // When
        batchSchedulerService.scheduledWeeklyBackfill();
        
        // Then
        // 빈 문자열로 split하면 하나의 빈 요소가 생기므로 createBatchJob이 1회 호출됨
        verify(batchJobService, times(1)).createBatchJob(any(BatchJobRequest.class));
    }
    
    @Test
    void scheduledWeeklyBackfill_ShouldTrimStockCodes() {
        // Given
        ReflectionTestUtils.setField(batchSchedulerService, "stockCodesConfig", " 005930 , 000660 , 035420 ");
        BatchJob createdJob = BatchJob.builder().id(1L).build();
        when(batchJobService.createBatchJob(any(BatchJobRequest.class))).thenReturn(createdJob);
        
        // When
        batchSchedulerService.scheduledWeeklyBackfill();
        
        // Then
        ArgumentCaptor<BatchJobRequest> requestCaptor = ArgumentCaptor.forClass(BatchJobRequest.class);
        verify(batchJobService, times(3)).createBatchJob(requestCaptor.capture());
        
        List<BatchJobRequest> capturedRequests = requestCaptor.getAllValues();
        assertThat(capturedRequests.get(0).getStockCode()).isEqualTo("005930");
        assertThat(capturedRequests.get(1).getStockCode()).isEqualTo("000660");
        assertThat(capturedRequests.get(2).getStockCode()).isEqualTo("035420");
    }
    
    @Test
    void scheduledWeeklyBackfill_ShouldHandleSingleStockCode() {
        // Given
        ReflectionTestUtils.setField(batchSchedulerService, "stockCodesConfig", "005930");
        BatchJob createdJob = BatchJob.builder().id(1L).build();
        when(batchJobService.createBatchJob(any(BatchJobRequest.class))).thenReturn(createdJob);
        
        // When
        batchSchedulerService.scheduledWeeklyBackfill();
        
        // Then
        ArgumentCaptor<BatchJobRequest> requestCaptor = ArgumentCaptor.forClass(BatchJobRequest.class);
        verify(batchJobService, times(1)).createBatchJob(requestCaptor.capture());
        
        BatchJobRequest request = requestCaptor.getValue();
        assertThat(request.getStockCode()).isEqualTo("005930");
        assertThat(request.getJobName()).contains("주간 백필 - 005930");
    }
    
    @Test
    void scheduledWeeklyBackfill_ShouldUseCorrectPeriodType() {
        // Given
        BatchJob createdJob = BatchJob.builder().id(1L).build();
        when(batchJobService.createBatchJob(any(BatchJobRequest.class))).thenReturn(createdJob);
        
        // When
        batchSchedulerService.scheduledWeeklyBackfill();
        
        // Then
        ArgumentCaptor<BatchJobRequest> requestCaptor = ArgumentCaptor.forClass(BatchJobRequest.class);
        verify(batchJobService, times(3)).createBatchJob(requestCaptor.capture());
        
        List<BatchJobRequest> capturedRequests = requestCaptor.getAllValues();
        for (BatchJobRequest request : capturedRequests) {
            assertThat(request.getPeriodType()).isEqualTo(PeriodType.DAILY);
        }
    }
}