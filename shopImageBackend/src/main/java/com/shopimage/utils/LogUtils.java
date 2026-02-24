package com.shopimage.utils;

import com.shopimage.entity.SystemLog;
import com.shopimage.service.SystemLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 日志记录工具类
 * 用于在业务操作中方便地记录系统日志
 */
@Component
@Slf4j
public class LogUtils {
    
    private static SystemLogService systemLogService;
    
    @Autowired
    public void setSystemLogService(SystemLogService systemLogService) {
        LogUtils.systemLogService = systemLogService;
    }
    
    /**
     * 记录信息日志
     */
    public static void logInfo(SystemLog.Module module, String message, String username) {
        logInfo(module, message, username, null);
    }
    
    /**
     * 记录信息日志（带请求数据）
     */
    public static void logInfo(SystemLog.Module module, String message, String username, String requestData) {
        HttpServletRequest request = getCurrentRequest();
        String ip = request != null ? getClientIp(request) : null;
        String userAgent = request != null ? request.getHeader("User-Agent") : null;
        
        try {
            systemLogService.log(SystemLog.Level.INFO, module, message, username, ip, userAgent, requestData);
        } catch (Exception e) {
            log.error("记录INFO日志失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 记录警告日志
     */
    public static void logWarn(SystemLog.Module module, String message, String username) {
        logWarn(module, message, username, null);
    }
    
    /**
     * 记录警告日志（带请求数据）
     */
    public static void logWarn(SystemLog.Module module, String message, String username, String requestData) {
        HttpServletRequest request = getCurrentRequest();
        String ip = request != null ? getClientIp(request) : null;
        String userAgent = request != null ? request.getHeader("User-Agent") : null;
        
        try {
            systemLogService.log(SystemLog.Level.WARN, module, message, username, ip, userAgent, requestData);
        } catch (Exception e) {
            log.error("记录WARN日志失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 记录错误日志
     */
    public static void logError(SystemLog.Module module, String message, String username, Exception exception) {
        HttpServletRequest request = getCurrentRequest();
        String ip = request != null ? getClientIp(request) : null;
        
        try {
            systemLogService.logError(module, message, username, ip, exception);
        } catch (Exception e) {
            log.error("记录ERROR日志失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 记录错误日志（不带异常对象）
     */
    public static void logError(SystemLog.Module module, String message, String username) {
        HttpServletRequest request = getCurrentRequest();
        String ip = request != null ? getClientIp(request) : null;
        String userAgent = request != null ? request.getHeader("User-Agent") : null;
        
        try {
            systemLogService.log(SystemLog.Level.ERROR, module, message, username, ip, userAgent, null);
        } catch (Exception e) {
            log.error("记录ERROR日志失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 记录调试日志
     */
    public static void logDebug(SystemLog.Module module, String message, String username) {
        logDebug(module, message, username, null);
    }
    
    /**
     * 记录调试日志（带请求数据）
     */
    public static void logDebug(SystemLog.Module module, String message, String username, String requestData) {
        HttpServletRequest request = getCurrentRequest();
        String ip = request != null ? getClientIp(request) : null;
        String userAgent = request != null ? request.getHeader("User-Agent") : null;
        
        try {
            systemLogService.log(SystemLog.Level.DEBUG, module, message, username, ip, userAgent, requestData);
        } catch (Exception e) {
            log.error("记录DEBUG日志失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 记录用户登录日志
     */
    public static void logUserLogin(String username, boolean success) {
        String message = success ? 
            String.format("用户 %s 登录成功", username) : 
            String.format("用户 %s 登录失败", username);
        
        SystemLog.Level level = success ? SystemLog.Level.INFO : SystemLog.Level.WARN;
        HttpServletRequest request = getCurrentRequest();
        String ip = request != null ? getClientIp(request) : null;
        String userAgent = request != null ? request.getHeader("User-Agent") : null;
        
        try {
            systemLogService.log(level, SystemLog.Module.AUTH, message, username, ip, userAgent, null);
        } catch (Exception e) {
            log.error("记录用户登录日志失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 记录用户注册日志
     */
    public static void logUserRegister(String username, boolean success) {
        String message = success ? 
            String.format("用户 %s 注册成功", username) : 
            String.format("用户 %s 注册失败", username);
        
        SystemLog.Level level = success ? SystemLog.Level.INFO : SystemLog.Level.WARN;
        HttpServletRequest request = getCurrentRequest();
        String ip = request != null ? getClientIp(request) : null;
        String userAgent = request != null ? request.getHeader("User-Agent") : null;
        
        try {
            systemLogService.log(level, SystemLog.Module.USER, message, username, ip, userAgent, null);
        } catch (Exception e) {
            log.error("记录用户注册日志失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 记录图片上传日志
     */
    public static void logImageUpload(String username, String filename, boolean success) {
        String message = success ? 
            String.format("用户 %s 上传图片 %s 成功", username, filename) : 
            String.format("用户 %s 上传图片 %s 失败", username, filename);
        
        SystemLog.Level level = success ? SystemLog.Level.INFO : SystemLog.Level.ERROR;
        
        try {
            logInfo(SystemLog.Module.IMAGE, message, username, 
                String.format("{\"filename\": \"%s\", \"success\": %s}", filename, success));
        } catch (Exception e) {
            log.error("记录图片上传日志失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 记录图片分类日志
     */
    public static void logImageClassification(String username, String filename, String result, boolean success) {
        String message = success ? 
            String.format("用户 %s 对图片 %s 分类成功，结果: %s", username, filename, result) : 
            String.format("用户 %s 对图片 %s 分类失败", username, filename);
        
        SystemLog.Level level = success ? SystemLog.Level.INFO : SystemLog.Level.ERROR;
        
        try {
            logInfo(SystemLog.Module.TASK, message, username, 
                String.format("{\"filename\": \"%s\", \"result\": \"%s\", \"success\": %s}", 
                    filename, result, success));
        } catch (Exception e) {
            log.error("记录图片分类日志失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 获取当前请求
     */
    private static HttpServletRequest getCurrentRequest() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attributes != null ? attributes.getRequest() : null;
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * 获取客户端IP地址
     */
    private static String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        
        // 处理多个IP的情况，取第一个IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        
        return ip;
    }
    
    // ========== 兼容性方法 - 支持旧的调用方式 ==========
    
    /**
     * 兼容性方法：记录信息日志（旧的两参数方式）
     * @deprecated 请使用 logInfo(SystemLog.Module, String, String) 方法
     */
    @Deprecated
    public static void logInfo(String category, String message) {
        // 尝试根据category推断模块类型
        SystemLog.Module module = inferModuleFromCategory(category);
        logInfo(module, message, "system");
    }
    
    /**
     * 兼容性方法：记录警告日志（旧的两参数方式）
     * @deprecated 请使用 logWarn(SystemLog.Module, String, String) 方法
     */
    @Deprecated
    public static void logWarn(String category, String message) {
        SystemLog.Module module = inferModuleFromCategory(category);
        logWarn(module, message, "system");
    }
    
    /**
     * 兼容性方法：记录错误日志（旧的两参数方式）
     * @deprecated 请使用 logError(SystemLog.Module, String, String) 方法
     */
    @Deprecated
    public static void logError(String category, String message) {
        SystemLog.Module module = inferModuleFromCategory(category);
        logError(module, message, "system");
    }
    
    /**
     * 兼容性方法：记录图片上传日志
     * @deprecated 请使用 logInfo(SystemLog.Module.IMAGE, String, String) 方法
     */
    @Deprecated
    public static void logImageUpload(String filename, long fileSize, String message) {
        String logMessage = "图片上传: " + filename + ", 大小: " + fileSize + " bytes, " + message;
        logInfo(SystemLog.Module.IMAGE, logMessage, "system");
    }
    
    /**
     * 兼容性方法：记录图片分类日志
     * @deprecated 请使用 logInfo(SystemLog.Module.IMAGE, String, String) 方法
     */
    @Deprecated
    public static void logImageClassification(String filename, String topPrediction, String message) {
        String logMessage = "图片分类: " + filename + ", 预测结果: " + topPrediction + ", " + message;
        logInfo(SystemLog.Module.IMAGE, logMessage, "system");
    }
    
    /**
     * 根据旧的category字符串推断模块类型
     */
    private static SystemLog.Module inferModuleFromCategory(String category) {
        if (category == null) {
            return SystemLog.Module.SYSTEM;
        }
        
        String lowerCategory = category.toLowerCase();
        if (lowerCategory.contains("用户") || lowerCategory.contains("user")) {
            return SystemLog.Module.USER;
        } else if (lowerCategory.contains("图片") || lowerCategory.contains("image") || 
                   lowerCategory.contains("分类") || lowerCategory.contains("classification")) {
            return SystemLog.Module.IMAGE;
        } else if (lowerCategory.contains("任务") || lowerCategory.contains("task")) {
            return SystemLog.Module.TASK;
        } else if (lowerCategory.contains("模型") || lowerCategory.contains("model")) {
            return SystemLog.Module.SYSTEM;
        } else {
            return SystemLog.Module.SYSTEM;
        }
    }
}