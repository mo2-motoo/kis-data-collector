package com.hsu_mafia.motoo.kisdatacollector.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class RateLimitManagerTest {

    private RateLimitManager rateLimitManager;
    
    @BeforeEach
    void setUp() {
        rateLimitManager = new RateLimitManager();
    }
    
    @Test
    void waitForRateLimit_ShouldNotWait_WhenNoRequestsYet() {
        // Given
        long startTime = System.currentTimeMillis();
        
        // When
        rateLimitManager.waitForRateLimit();
        
        // Then
        long endTime = System.currentTimeMillis();
        assertThat(endTime - startTime).isLessThan(100); // 거의 즉시 완료
        assertThat(rateLimitManager.getCurrentRequestCount()).isEqualTo(1);
        assertThat(rateLimitManager.getCurrentSecondRequestCount()).isEqualTo(1);
    }
    
    @Test
    void waitForRateLimit_ShouldAllowRequestsWithinSecondLimit() {
        // Given & When
        long startTime = System.currentTimeMillis();
        rateLimitManager.waitForRateLimit();
        rateLimitManager.waitForRateLimit();
        long endTime = System.currentTimeMillis();
        
        // Then
        assertThat(endTime - startTime).isLessThan(500); // 초당 2회 허용이므로 빠르게 완료
        assertThat(rateLimitManager.getCurrentRequestCount()).isEqualTo(2);
        assertThat(rateLimitManager.getCurrentSecondRequestCount()).isEqualTo(2);
    }
    
    @Test
    void waitForRateLimit_ShouldWait_WhenSecondLimitExceeded() {
        // Given
        rateLimitManager.waitForRateLimit();
        rateLimitManager.waitForRateLimit();
        
        // When
        long startTime = System.currentTimeMillis();
        rateLimitManager.waitForRateLimit(); // 세 번째 요청 - 초당 제한 초과
        long endTime = System.currentTimeMillis();
        
        // Then
        assertThat(endTime - startTime).isGreaterThan(900); // 최소 1초 대기
        assertThat(rateLimitManager.getCurrentRequestCount()).isEqualTo(3);
    }
    
    @Test
    void waitForRateLimit_ShouldWait_WhenMinuteLimitExceeded() throws InterruptedException {
        // Given - 분당 제한(20회)에 도달하도록 요청 시간 설정
        Queue<Long> requestTimes = (Queue<Long>) ReflectionTestUtils.getField(rateLimitManager, "requestTimes");
        long currentTime = System.currentTimeMillis();
        
        // 20개의 요청을 1분 내에 배치
        for (int i = 0; i < 20; i++) {
            requestTimes.offer(currentTime - 30000 + (i * 1000)); // 30초 전부터 시작
        }
        
        // When
        long startTime = System.currentTimeMillis();
        rateLimitManager.waitForRateLimit();
        long endTime = System.currentTimeMillis();
        
        // Then
        assertThat(endTime - startTime).isGreaterThan(20000); // 최소 20초 대기 (분당 제한 때문에)
    }
    
    @Test
    void getCurrentRequestCount_ShouldReturnCorrectCount() {
        // Given
        rateLimitManager.waitForRateLimit();
        rateLimitManager.waitForRateLimit();
        
        // When
        int count = rateLimitManager.getCurrentRequestCount();
        
        // Then
        assertThat(count).isEqualTo(2);
    }
    
    @Test
    void getCurrentSecondRequestCount_ShouldReturnCorrectCount() {
        // Given
        rateLimitManager.waitForRateLimit();
        rateLimitManager.waitForRateLimit();
        
        // When
        int count = rateLimitManager.getCurrentSecondRequestCount();
        
        // Then
        assertThat(count).isEqualTo(2);
    }
    
    @Test
    void cleanupOldRequests_ShouldRemoveExpiredRequests() throws InterruptedException {
        // Given
        Queue<Long> requestTimes = (Queue<Long>) ReflectionTestUtils.getField(rateLimitManager, "requestTimes");
        long currentTime = System.currentTimeMillis();
        
        // 오래된 요청들을 큐에 추가
        requestTimes.offer(currentTime - 70000); // 70초 전
        requestTimes.offer(currentTime - 65000); // 65초 전
        requestTimes.offer(currentTime - 30000); // 30초 전
        
        // When
        rateLimitManager.waitForRateLimit();
        
        // Then
        assertThat(rateLimitManager.getCurrentRequestCount()).isEqualTo(2); // 30초 전 요청 + 현재 요청
    }
    
    @Test
    void cleanupSecondRequests_ShouldRemoveExpiredRequests() throws InterruptedException {
        // Given
        Queue<Long> secondRequestTimes = (Queue<Long>) ReflectionTestUtils.getField(rateLimitManager, "secondRequestTimes");
        long currentTime = System.currentTimeMillis();
        
        // 오래된 요청들을 큐에 추가
        secondRequestTimes.offer(currentTime - 2000); // 2초 전
        secondRequestTimes.offer(currentTime - 1500); // 1.5초 전
        secondRequestTimes.offer(currentTime - 500);  // 0.5초 전
        
        // When
        rateLimitManager.waitForRateLimit();
        
        // Then
        assertThat(rateLimitManager.getCurrentSecondRequestCount()).isEqualTo(2); // 0.5초 전 요청 + 현재 요청
    }
    
    @Test
    void concurrentRequests_ShouldHandleMultipleThreads() throws InterruptedException {
        // Given
        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger completedRequests = new AtomicInteger(0);
        
        // When
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    rateLimitManager.waitForRateLimit();
                    completedRequests.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }
        
        // Then
        boolean finished = latch.await(30, TimeUnit.SECONDS);
        assertThat(finished).isTrue();
        assertThat(completedRequests.get()).isEqualTo(threadCount);
        
        executor.shutdown();
    }
    
    @Test
    void waitForRateLimit_ShouldHandleInterruption() throws InterruptedException {
        // Given
        rateLimitManager.waitForRateLimit();
        rateLimitManager.waitForRateLimit();
        
        Thread testThread = new Thread(() -> {
            rateLimitManager.waitForRateLimit(); // 이 호출은 대기하게 됨
        });
        
        // When
        testThread.start();
        Thread.sleep(100); // 스레드가 대기 상태에 들어가도록 잠시 대기
        testThread.interrupt();
        
        // Then
        testThread.join(5000); // 5초 내에 종료되어야 함
        assertThat(testThread.isAlive()).isFalse();
    }
    
    @Test
    void rateLimitManager_ShouldMaintainCorrectState_WithVariousOperations() {
        // Given
        rateLimitManager.waitForRateLimit();
        rateLimitManager.waitForRateLimit();
        
        // When
        int initialCount = rateLimitManager.getCurrentRequestCount();
        int initialSecondCount = rateLimitManager.getCurrentSecondRequestCount();
        
        // Then
        assertThat(initialCount).isEqualTo(2);
        assertThat(initialSecondCount).isEqualTo(2);
        
        // When - 시간이 지나면서 정리됨
        try {
            Thread.sleep(1100); // 1.1초 대기
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        int afterDelaySecondCount = rateLimitManager.getCurrentSecondRequestCount();
        
        // Then
        assertThat(afterDelaySecondCount).isEqualTo(0); // 1초 후엔 초단위 카운트가 0이 됨
    }
    
    @Test
    void rateLimitManager_ShouldHandleEdgeCases() {
        // Given
        Queue<Long> requestTimes = (Queue<Long>) ReflectionTestUtils.getField(rateLimitManager, "requestTimes");
        
        // When - 큐가 비어있을 때
        rateLimitManager.waitForRateLimit();
        
        // Then
        assertThat(rateLimitManager.getCurrentRequestCount()).isEqualTo(1);
        
        // When - 큐에 null이 없는지 확인
        assertThat(requestTimes.peek()).isNotNull();
        
        // When - 매우 많은 요청이 있을 때
        for (int i = 0; i < 100; i++) {
            requestTimes.offer(System.currentTimeMillis() - 70000); // 모두 만료된 요청
        }
        
        rateLimitManager.waitForRateLimit();
        
        // Then - 만료된 요청들은 정리되어야 함
        assertThat(rateLimitManager.getCurrentRequestCount()).isEqualTo(1);
    }
}