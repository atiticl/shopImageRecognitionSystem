package com.shopimage.service;

import com.shopimage.entity.SystemLog;
import com.shopimage.repository.SystemLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class SystemLogService {
    
    private final SystemLogRepository systemLogRepository;
    
    /**
     * 记录系统日志
     */
    public void log(SystemLog.Level level, SystemLog.Module module, String message, 
                   String username, String ip, String userAgent, String requestData) {
        SystemLog systemLog = SystemLog.builder()
                .level(level)
                .module(module)
                .message(message)
                .username(username)
                .ip(ip)
                .userAgent(userAgent)
                .requestData(requestData)
                .createTime(LocalDateTime.now())
                .build();
        
        systemLogRepository.save(systemLog);
    }
    
    /**
     * 记录异常日志
     */
    public void logError(SystemLog.Module module, String message, String username, 
                        String ip, Exception exception) {
        String stackTrace = exception != null ? getStackTrace(exception) : null;
        
        SystemLog systemLog = SystemLog.builder()
                .level(SystemLog.Level.ERROR)
                .module(module)
                .message(message)
                .username(username)
                .ip(ip)
                .stackTrace(stackTrace)
                .createTime(LocalDateTime.now())
                .build();
        
        systemLogRepository.save(systemLog);
    }
    
    /**
     * 分页查询系统日志
     */
    public Map<String, Object> findByConditions(SystemLog.Level level, SystemLog.Module module,
                                               String keyword, LocalDateTime startTime, 
                                               LocalDateTime endTime, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<SystemLog> logPage = systemLogRepository.findByConditions(
                level, module, keyword, startTime, endTime, pageable);
        
        Map<String, Object> result = new HashMap<>();
        result.put("content", logPage.getContent());
        result.put("totalElements", logPage.getTotalElements());
        result.put("totalPages", logPage.getTotalPages());
        result.put("currentPage", page);
        result.put("size", size);
        result.put("hasNext", logPage.hasNext());
        result.put("hasPrevious", logPage.hasPrevious());
        
        return result;
    }
    
    /**
     * 获取日志统计信息
     */
    public Map<String, Long> getLogStatistics() {
        Map<String, Long> statistics = new HashMap<>();
        statistics.put("info", systemLogRepository.countByLevel(SystemLog.Level.INFO));
        statistics.put("warn", systemLogRepository.countByLevel(SystemLog.Level.WARN));
        statistics.put("error", systemLogRepository.countByLevel(SystemLog.Level.ERROR));
        statistics.put("debug", systemLogRepository.countByLevel(SystemLog.Level.DEBUG));
        return statistics;
    }
    
    /**
     * 清空所有日志
     */
    @Transactional
    public void clearAllLogs() {
        systemLogRepository.deleteAll();
        log(SystemLog.Level.INFO, SystemLog.Module.SYSTEM, "系统日志已清空", "admin", null, null, null);
    }
    
    /**
     * 根据ID查询日志详情
     */
    public SystemLog findById(Long id) {
        return systemLogRepository.findById(id).orElse(null);
    }
    
    /**
     * 获取异常堆栈信息
     */
    private String getStackTrace(Exception exception) {
        StringBuilder sb = new StringBuilder();
        sb.append(exception.getClass().getName()).append(": ").append(exception.getMessage()).append("\n");
        
        for (StackTraceElement element : exception.getStackTrace()) {
            sb.append("\tat ").append(element.toString()).append("\n");
        }
        
        return sb.toString();
    }
}