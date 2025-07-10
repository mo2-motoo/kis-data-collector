package com.hsu_mafia.motoo.kisdatacollector.service;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitManager {

    // KIS API 제한: 초당 2회, 분당 20회
    private static final int MAX_REQUESTS_PER_SECOND = 2;
    private static final int MAX_REQUESTS_PER_MINUTE = 20;
    private static final int SECOND_WINDOW = 1000;
    private static final int MINUTE_WINDOW = 60000;

    private final Queue<Long> requestTimes = new ConcurrentLinkedQueue<>();
    private final Queue<Long> secondRequestTimes = new ConcurrentLinkedQueue<>();
    private final AtomicLong lastRequestTime = new AtomicLong(0);

    public synchronized void waitForRateLimit() {
        long currentTime = System.currentTimeMillis();

        // 1분 내 요청 정리
        cleanupOldRequests(currentTime);

        // 분당 제한 체크
        if (requestTimes.size() >= MAX_REQUESTS_PER_MINUTE) {
            long oldestRequest = requestTimes.peek();
            long waitTime = MINUTE_WINDOW - (currentTime - oldestRequest);
            if (waitTime > 0) {
                log.info("분당 제한 도달, {}ms 대기", waitTime);
                try {
                    Thread.sleep(waitTime);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                currentTime = System.currentTimeMillis();
                cleanupOldRequests(currentTime);
            }
        }

        // 초당 제한 체크
        cleanupSecondRequests(currentTime);
        if (secondRequestTimes.size() >= MAX_REQUESTS_PER_SECOND) {
            long oldestSecondRequest = secondRequestTimes.peek();
            long waitTime = SECOND_WINDOW - (currentTime - oldestSecondRequest);
            if (waitTime > 0) {
                log.debug("초당 제한 적용, {}ms 대기", waitTime);
                try {
                    Thread.sleep(waitTime);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                currentTime = System.currentTimeMillis();
                cleanupSecondRequests(currentTime);
            }
        }

        // 요청 시간 기록
        requestTimes.offer(currentTime);
        secondRequestTimes.offer(currentTime);
        lastRequestTime.set(currentTime);
    }

    private void cleanupOldRequests(long currentTime) {
        while (!requestTimes.isEmpty() && currentTime - requestTimes.peek() > MINUTE_WINDOW) {
            requestTimes.poll();
        }
    }

    private void cleanupSecondRequests(long currentTime) {
        while (!secondRequestTimes.isEmpty() && currentTime - secondRequestTimes.peek() > SECOND_WINDOW) {
            secondRequestTimes.poll();
        }
    }

    public int getCurrentRequestCount() {
        cleanupOldRequests(System.currentTimeMillis());
        return requestTimes.size();
    }

    public int getCurrentSecondRequestCount() {
        cleanupSecondRequests(System.currentTimeMillis());
        return secondRequestTimes.size();
    }
}
