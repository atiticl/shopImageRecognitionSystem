package com.shopimage.repository;

import com.shopimage.entity.ClassificationResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface ClassificationResultRepository extends JpaRepository<ClassificationResult, Long>, JpaSpecificationExecutor<ClassificationResult> {
    List<ClassificationResult> findByImageId(Long imageId);
    
    @Modifying
    @Transactional
    @Query("DELETE FROM ClassificationResult cr WHERE cr.taskId = :taskId")
    void deleteByTaskId(@Param("taskId") Long taskId);
}