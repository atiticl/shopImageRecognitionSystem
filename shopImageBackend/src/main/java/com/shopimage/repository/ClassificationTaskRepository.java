package com.shopimage.repository;

import com.shopimage.entity.ClassificationTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDateTime;
import java.util.List;

public interface ClassificationTaskRepository extends JpaRepository<ClassificationTask, Long>, JpaSpecificationExecutor<ClassificationTask> {
    List<ClassificationTask> findByCreatedAtBetween(LocalDateTime startTime, LocalDateTime endTime);
    List<ClassificationTask> findTop10ByOrderByCreatedAtDesc();
    List<ClassificationTask> findByStatus(String status);
    long countByStatus(String status);
}
