package com.community.demo.repository;

import com.community.demo.domain.user.EnrollMode;
import com.community.demo.domain.user.EnrollRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface EnrollRecordRepository extends JpaRepository<EnrollRecord, Long> {
    List<EnrollRecord> findTop5ByUserIdAndModeOrderByFinishedAtDesc(Long userId, EnrollMode mode);

    @Query("select avg(r.durationMs) from EnrollRecord r where r.mode = :mode")
    Double averageByMode(EnrollMode mode);

    @Query("select avg(r.durationMs) from EnrollRecord r where r.mode = :mode and r.user.id <> :userId")
    Double averageByModeExcludingUser(EnrollMode mode, Long userId);
}
