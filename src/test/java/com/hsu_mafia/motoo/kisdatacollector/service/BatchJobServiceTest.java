package com.hsu_mafia.motoo.kisdatacollector.service;

import com.hsu_mafia.motoo.kisdatacollector.domain.BatchJob;
import com.hsu_mafia.motoo.kisdatacollector.domain.BatchJobStatus;
import com.hsu_mafia.motoo.kisdatacollector.domain.PeriodType;
import com.hsu_mafia.motoo.kisdatacollector.dto.BatchJobRequest;
import com.hsu_mafia.motoo.kisdatacollector.dto.StockDataResponse;
import com.hsu_mafia.motoo.kisdatacollector.repository.BatchJobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class BatchJobServiceTest {

    @Mock
    private BatchJobRepository batchJobRepository;
    
    @Mock
    private KisApiService kisApiService;
    
    @Mock
    private StockDataProcessingService dataProcessingService;
    
    private BatchJobService batchJobService;
    
    @BeforeEach
    void setUp() {
        batchJobService = new BatchJobService(batchJobRepository, kisApiService, dataProcessingService);
    }
    
    @Test
    void createBatchJob_ShouldCreateAndSaveBatchJob() {
        // Given
        BatchJobRequest request = BatchJobRequest.builder()
                .jobName("Test Job")
                .stockCode("005930")
                .startDate("20240101")
                .endDate("20240105")
                .periodType(PeriodType.DAILY)
                .build();
        
        BatchJob savedJob = BatchJob.builder()
                .id(1L)
                .jobName("Test Job")
                .stockCode("005930")
                .startDate("20240101")
                .endDate("20240105")
                .periodType(PeriodType.DAILY)
                .status(BatchJobStatus.PENDING)
                .build();
        
        when(batchJobRepository.save(any(BatchJob.class))).thenReturn(savedJob);
        
        // When
        BatchJob result = batchJobService.createBatchJob(request);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getJobName()).isEqualTo("Test Job");
        assertThat(result.getStockCode()).isEqualTo("005930");
        assertThat(result.getStatus()).isEqualTo(BatchJobStatus.PENDING);
        
        ArgumentCaptor<BatchJob> jobCaptor = ArgumentCaptor.forClass(BatchJob.class);
        verify(batchJobRepository).save(jobCaptor.capture());
        
        BatchJob capturedJob = jobCaptor.getValue();
        assertThat(capturedJob.getTotalCount()).isEqualTo(0);
        assertThat(capturedJob.getProcessedCount()).isEqualTo(0);
        assertThat(capturedJob.getSuccessCount()).isEqualTo(0);
        assertThat(capturedJob.getFailedCount()).isEqualTo(0);
    }
    
    @Test
    void executeBatchJob_ShouldCompleteSuccessfully_WhenAllDataFetchedSuccessfully() {
        // Given
        Long batchJobId = 1L;
        BatchJob batchJob = BatchJob.builder()
                .id(batchJobId)
                .stockCode("005930")
                .startDate("20240101")
                .endDate("20240102")
                .periodType(PeriodType.DAILY)
                .status(BatchJobStatus.PENDING)
                .totalCount(0)
                .processedCount(0)
                .successCount(0)
                .failedCount(0)
                .build();
        
        when(batchJobRepository.findById(batchJobId)).thenReturn(Optional.of(batchJob));
        when(batchJobRepository.save(any(BatchJob.class))).thenReturn(batchJob);
        
        List<StockDataResponse> mockData = Arrays.asList(
                StockDataResponse.builder()
                        .stockCode("005930")
                        .candleDateTime("202401010000")
                        .openPrice("75000")
                        .closePrice("75500")
                        .periodType(PeriodType.DAILY)
                        .build()
        );
        
        when(kisApiService.fetchStockData(anyString(), anyString(), anyString(), any(PeriodType.class)))
                .thenReturn(mockData);
        
        // When
        CompletableFuture<Void> result = batchJobService.executeBatchJob(batchJobId);
        
        // Then
        assertThat(result).isCompleted();
        verify(batchJobRepository, atLeast(3)).save(any(BatchJob.class));
        verify(kisApiService, times(2)).fetchStockData(anyString(), anyString(), anyString(), any(PeriodType.class));
        verify(dataProcessingService, times(2)).processAndSaveStockData(anyList());
    }
    
    @Test
    void executeBatchJob_ShouldHandleFailure_WhenJobNotFound() {
        // Given
        Long batchJobId = 1L;
        when(batchJobRepository.findById(batchJobId)).thenReturn(Optional.empty());
        
        // When & Then
        assertThatThrownBy(() -> batchJobService.executeBatchJob(batchJobId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("배치 작업을 찾을 수 없습니다");
    }
    
    @Test
    void executeBatchJob_ShouldHandleApiFailure_WhenKisApiThrowsException() {
        // Given
        Long batchJobId = 1L;
        BatchJob batchJob = BatchJob.builder()
                .id(batchJobId)
                .stockCode("005930")
                .startDate("20240101")
                .endDate("20240101")
                .periodType(PeriodType.DAILY)
                .status(BatchJobStatus.PENDING)
                .totalCount(0)
                .processedCount(0)
                .successCount(0)
                .failedCount(0)
                .build();
        
        when(batchJobRepository.findById(batchJobId)).thenReturn(Optional.of(batchJob));
        when(batchJobRepository.save(any(BatchJob.class))).thenReturn(batchJob);
        when(kisApiService.fetchStockData(anyString(), anyString(), anyString(), any(PeriodType.class)))
                .thenThrow(new RuntimeException("API 호출 실패"));
        
        // When
        CompletableFuture<Void> result = batchJobService.executeBatchJob(batchJobId);
        
        // Then
        assertThat(result).isCompleted();
        verify(batchJobRepository, atLeast(2)).save(any(BatchJob.class));
        verify(kisApiService).fetchStockData(anyString(), anyString(), anyString(), any(PeriodType.class));
        verify(dataProcessingService, never()).processAndSaveStockData(anyList());
    }
    
    @Test
    void executeBatchJob_ShouldHandleExceptionGracefully() {
        // Given
        Long batchJobId = 1L;
        BatchJob batchJob = BatchJob.builder()
                .id(batchJobId)
                .stockCode("005930")
                .startDate("20240101")
                .endDate("20240101")
                .periodType(PeriodType.DAILY)
                .status(BatchJobStatus.PENDING)
                .totalCount(0)
                .processedCount(0)
                .successCount(0)
                .failedCount(0)
                .build();
        
        when(batchJobRepository.findById(batchJobId)).thenReturn(Optional.of(batchJob));
        // 첫 번째 save는 성공, 두 번째부터 실패
        when(batchJobRepository.save(any(BatchJob.class)))
                .thenReturn(batchJob)
                .thenReturn(batchJob)
                .thenReturn(batchJob);
        
        when(kisApiService.fetchStockData(anyString(), anyString(), anyString(), any(PeriodType.class)))
                .thenThrow(new RuntimeException("API error"));
        
        // When
        CompletableFuture<Void> result = batchJobService.executeBatchJob(batchJobId);
        
        // Then
        assertThat(result).isCompleted();
        verify(batchJobRepository, atLeast(3)).save(any(BatchJob.class));
    }
    
    @Test
    void generateDateRange_ShouldReturnCorrectDateRange_InReverseOrder() {
        // Given
        String startDate = "20240101";
        String endDate = "20240103";
        
        // When
        List<LocalDate> result = ReflectionTestUtils.invokeMethod(batchJobService, "generateDateRange", startDate, endDate);
        
        // Then
        assertThat(result).hasSize(3);
        assertThat(result.get(0)).isEqualTo(LocalDate.of(2024, 1, 3));
        assertThat(result.get(1)).isEqualTo(LocalDate.of(2024, 1, 2));
        assertThat(result.get(2)).isEqualTo(LocalDate.of(2024, 1, 1));
    }
    
    @Test
    void isWeekday_ShouldReturnTrue_ForWeekdays() {
        // Given
        LocalDate monday = LocalDate.of(2024, 1, 1); // 2024-01-01 is Monday
        LocalDate tuesday = LocalDate.of(2024, 1, 2);
        LocalDate wednesday = LocalDate.of(2024, 1, 3);
        LocalDate thursday = LocalDate.of(2024, 1, 4);
        LocalDate friday = LocalDate.of(2024, 1, 5);
        
        // When & Then
        assertThat((Boolean) ReflectionTestUtils.invokeMethod(batchJobService, "isWeekday", monday)).isTrue();
        assertThat((Boolean) ReflectionTestUtils.invokeMethod(batchJobService, "isWeekday", tuesday)).isTrue();
        assertThat((Boolean) ReflectionTestUtils.invokeMethod(batchJobService, "isWeekday", wednesday)).isTrue();
        assertThat((Boolean) ReflectionTestUtils.invokeMethod(batchJobService, "isWeekday", thursday)).isTrue();
        assertThat((Boolean) ReflectionTestUtils.invokeMethod(batchJobService, "isWeekday", friday)).isTrue();
    }
    
    @Test
    void isWeekday_ShouldReturnFalse_ForWeekends() {
        // Given
        LocalDate saturday = LocalDate.of(2024, 1, 6);
        LocalDate sunday = LocalDate.of(2024, 1, 7);
        
        // When & Then
        assertThat((Boolean) ReflectionTestUtils.invokeMethod(batchJobService, "isWeekday", saturday)).isFalse();
        assertThat((Boolean) ReflectionTestUtils.invokeMethod(batchJobService, "isWeekday", sunday)).isFalse();
    }
    
    @Test
    void getPendingJobs_ShouldReturnPendingJobs() {
        // Given
        List<BatchJob> pendingJobs = Arrays.asList(
                BatchJob.builder().id(1L).status(BatchJobStatus.PENDING).build(),
                BatchJob.builder().id(2L).status(BatchJobStatus.PENDING).build()
        );
        
        when(batchJobRepository.findPendingJobsOrderByCreatedAt(BatchJobStatus.PENDING))
                .thenReturn(pendingJobs);
        
        // When
        List<BatchJob> result = batchJobService.getPendingJobs();
        
        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(1).getId()).isEqualTo(2L);
    }
    
    @Test
    void getJobsByStatus_ShouldReturnJobsWithSpecificStatus() {
        // Given
        BatchJobStatus status = BatchJobStatus.COMPLETED;
        List<BatchJob> completedJobs = Arrays.asList(
                BatchJob.builder().id(1L).status(status).build(),
                BatchJob.builder().id(2L).status(status).build()
        );
        
        when(batchJobRepository.findByStatus(status)).thenReturn(completedJobs);
        
        // When
        List<BatchJob> result = batchJobService.getJobsByStatus(status);
        
        // Then
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(job -> job.getStatus() == status);
    }
    
    @Test
    void getJobById_ShouldReturnJob_WhenJobExists() {
        // Given
        Long jobId = 1L;
        BatchJob job = BatchJob.builder().id(jobId).build();
        
        when(batchJobRepository.findById(jobId)).thenReturn(Optional.of(job));
        
        // When
        BatchJob result = batchJobService.getJobById(jobId);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(jobId);
    }
    
    @Test
    void getJobById_ShouldThrowException_WhenJobNotFound() {
        // Given
        Long jobId = 1L;
        when(batchJobRepository.findById(jobId)).thenReturn(Optional.empty());
        
        // When & Then
        assertThatThrownBy(() -> batchJobService.getJobById(jobId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("배치 작업을 찾을 수 없습니다");
    }
}