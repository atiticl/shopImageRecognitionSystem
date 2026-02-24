package com.shopimage.web;

import com.shopimage.common.api.ApiResponse;
import com.shopimage.entity.SystemLog;
import com.shopimage.service.SystemLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/system-logs")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class SystemLogController {
    
    private final SystemLogService systemLogService;
    
    /**
     * 分页查询系统日志
     */
    @GetMapping
    public ApiResponse<Object> getLogs(
            @RequestParam(required = false) SystemLog.Level level,
            @RequestParam(required = false) SystemLog.Module module,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {
        
        try {
            // 记录查询日志
            String clientIp = getClientIp(request);
            systemLogService.log(SystemLog.Level.INFO, SystemLog.Module.SYSTEM, 
                    "查询系统日志", null, clientIp, request.getHeader("User-Agent"), null);
            
            Map<String, Object> result = systemLogService.findByConditions(
                    level, module, keyword, startTime, endTime, page, size);
            
            return ApiResponse.success(result, "查询成功");
        } catch (Exception e) {
            log.error("查询系统日志失败", e);
            systemLogService.logError(SystemLog.Module.SYSTEM, "查询系统日志失败: " + e.getMessage(), 
                    null, getClientIp(request), e);
            return ApiResponse.error("查询系统日志失败");
        }
    }
    
    /**
     * 获取日志统计信息
     */
    @GetMapping("/statistics")
    public ApiResponse<Map<String, Long>> getStatistics(HttpServletRequest request) {
        try {
            Map<String, Long> statistics = systemLogService.getLogStatistics();
            return ApiResponse.success(statistics, "获取统计成功");
        } catch (Exception e) {
            log.error("获取日志统计失败", e);
            systemLogService.logError(SystemLog.Module.SYSTEM, "获取日志统计失败: " + e.getMessage(), 
                    null, getClientIp(request), e);
            return ApiResponse.error("获取日志统计失败");
        }
    }
    
    /**
     * 清空所有日志
     */
    @DeleteMapping("/clear")
    public ApiResponse<String> clearLogs(HttpServletRequest request) {
        try {
            String clientIp = getClientIp(request);
            systemLogService.log(SystemLog.Level.WARN, SystemLog.Module.SYSTEM, 
                    "执行清空日志操作", "admin", clientIp, request.getHeader("User-Agent"), null);
            
            systemLogService.clearAllLogs();
            return ApiResponse.success("日志清空成功", "操作成功");
        } catch (Exception e) {
            log.error("清空日志失败", e);
            systemLogService.logError(SystemLog.Module.SYSTEM, "清空日志失败: " + e.getMessage(), 
                    "admin", getClientIp(request), e);
            return ApiResponse.error("清空日志失败");
        }
    }
    
    /**
     * 获取日志详情
     */
    @GetMapping("/{id}")
    public ApiResponse<SystemLog> getLogDetail(@PathVariable Long id, HttpServletRequest request) {
        try {
            SystemLog systemLog = systemLogService.findById(id);
            if (systemLog == null) {
                return ApiResponse.error("日志不存在");
            }
            return ApiResponse.success(systemLog, "获取详情成功");
        } catch (Exception e) {
            log.error("获取日志详情失败", e);
            systemLogService.logError(SystemLog.Module.SYSTEM, "获取日志详情失败: " + e.getMessage(), 
                    null, getClientIp(request), e);
            return ApiResponse.error("获取日志详情失败");
        }
    }
    
    /**
     * 手动记录日志（用于测试）
     */
    @PostMapping("/test")
    public ApiResponse<String> testLog(@RequestParam SystemLog.Level level,
                                      @RequestParam SystemLog.Module module,
                                      @RequestParam String message,
                                      HttpServletRequest request) {
        try {
            String clientIp = getClientIp(request);
            systemLogService.log(level, module, message, "test_user", clientIp, 
                    request.getHeader("User-Agent"), null);
            return ApiResponse.success("测试日志记录成功", "操作成功");
        } catch (Exception e) {
            log.error("记录测试日志失败", e);
            return ApiResponse.error("记录测试日志失败");
        }
    }
    
    /**
     * 获取客户端IP地址
     */
    private String getClientIp(HttpServletRequest request) {
        // 优先从代理头获取真实IP
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            // X-Forwarded-For可能包含多个IP，取第一个
            String ip = xForwardedFor.split(",")[0].trim();
            return ip;
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        
        // 尝试其他代理头
        String[] headers = {
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_CLIENT_IP",
            "HTTP_X_FORWARDED_FOR"
        };
        
        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                return ip;
            }
        }
        
        String remoteAddr = request.getRemoteAddr();
        // 如果是localhost地址，尝试获取本机真实IP
        if ("0:0:0:0:0:0:0:1".equals(remoteAddr) || "::1".equals(remoteAddr) || "127.0.0.1".equals(remoteAddr)) {
            return getLocalHostIP();
        }
        
        return remoteAddr;
    }
    
    /**
     * 获取本机IP地址
     */
    private String getLocalHostIP() {
        try {
            java.util.Enumeration<java.net.NetworkInterface> interfaces = java.net.NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                java.net.NetworkInterface ni = interfaces.nextElement();
                if (ni.isLoopback() || ni.isVirtual() || !ni.isUp()) {
                    continue;
                }
                java.util.Enumeration<java.net.InetAddress> addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    java.net.InetAddress addr = addresses.nextElement();
                    if (addr instanceof java.net.Inet4Address && !addr.isLoopbackAddress()) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (Exception e) {
            // 如果获取失败，返回默认值
        }
        return "127.0.0.1";
    }
}