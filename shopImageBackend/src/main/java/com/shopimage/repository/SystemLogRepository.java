package com.shopimage.repository;

import com.shopimage.entity.SystemLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Map;

@Repository
public interface SystemLogRepository extends JpaRepository<SystemLog, Long> {
    
    /**
     * 根据条件分页查询系统日志
     */
    @Query("SELECT s FROM SystemLog s WHERE " +
           "(:level IS NULL OR s.level = :level) AND " +
           "(:module IS NULL OR s.module = :module) AND " +
           "(:keyword IS NULL OR s.message LIKE %:keyword% OR s.username LIKE %:keyword%) AND " +
           "(:startTime IS NULL OR s.createTime >= :startTime) AND " +
           "(:endTime IS NULL OR s.createTime <= :endTime) " +
           "ORDER BY s.createTime DESC")
    Page<SystemLog> findByConditions(
            @Param("level") SystemLog.Level level,
            @Param("module") SystemLog.Module module,
            @Param("keyword") String keyword,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            Pageable pageable);
    
    /**
     * 统计各级别日志数量
     */
    @Query("SELECT s.level as level, COUNT(s) as count FROM SystemLog s GROUP BY s.level")
    Map<SystemLog.Level, Long> countByLevel();
    
    /**
     * 统计INFO级别日志数量
     */
    long countByLevel(SystemLog.Level level);
}