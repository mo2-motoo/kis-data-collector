package com.hsu_mafia.motoo.kisdatacollector.repository;

import com.hsu_mafia.motoo.kisdatacollector.domain.BatchJob;
import com.hsu_mafia.motoo.kisdatacollector.domain.BatchJobStatus;
import com.hsu_mafia.motoo.kisdatacollector.domain.PeriodType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BatchJobRepository extends JpaRepository<BatchJob, Long> {
    List<BatchJob> findByStatus(BatchJobStatus status);
    List<BatchJob> findByStockCode(String stockCode);
    List<BatchJob> findByStockCodeAndPeriodType(String stockCode, PeriodType periodType);

    @Query("SELECT bj FROM BatchJob bj WHERE bj.status = :status ORDER BY bj.createdAt ASC")
    List<BatchJob> findPendingJobsOrderByCreatedAt(@Param("status") BatchJobStatus status);
}
