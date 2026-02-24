package com.shopimage.repository;

import com.shopimage.entity.MqMessageLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface MqMessageLogRepository extends JpaRepository<MqMessageLog, Long> {
    
    @Modifying
    @Transactional
    @Query("DELETE FROM MqMessageLog ml WHERE ml.taskId = :taskId")
    void deleteByTaskId(@Param("taskId") Long taskId);
}