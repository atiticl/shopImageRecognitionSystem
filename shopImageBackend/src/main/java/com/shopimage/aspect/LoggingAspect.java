package com.shopimage.aspect;

import com.shopimage.entity.SystemLog;
import com.shopimage.utils.LogUtils;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;

/**
 * 日志记录切面
 * 自动记录用户操作日志
 */
@Aspect
@Component
@Slf4j
public class LoggingAspect {

    /**
     * 定义切点：拦截所有控制器方法
     */
    @Pointcut("execution(* com.shopimage.web.*.*(..)) || execution(* com.shopimage.controller.*.*(..))")
    public void controllerMethods() {}

    /**
     * 环绕通知：记录方法执行前后的日志
     */
    @Around("controllerMethods()")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        
        // 获取请求信息
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = null;
        if (attributes != null) {
            request = attributes.getRequest();
        }
        
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        String operation = getOperationDescription(className, methodName, request);
        
        try {
            // 记录操作开始
            LogUtils.logInfo(SystemLog.Module.SYSTEM, "开始执行: " + operation, getCurrentUsername());
            
            // 执行目标方法
            Object result = joinPoint.proceed();
            
            // 计算执行时间
            long executionTime = System.currentTimeMillis() - startTime;
            
            // 记录操作成功
            LogUtils.logInfo(SystemLog.Module.SYSTEM, "成功执行: " + operation + " (耗时: " + executionTime + "ms)", getCurrentUsername());
            
            return result;
            
        } catch (Exception e) {
            // 计算执行时间
            long executionTime = System.currentTimeMillis() - startTime;
            
            // 记录操作失败
            LogUtils.logError(SystemLog.Module.SYSTEM, "执行失败: " + operation + " (耗时: " + executionTime + "ms), 错误: " + e.getMessage(), getCurrentUsername());
            
            throw e;
        }
    }

    /**
     * 根据类名、方法名和请求信息生成操作描述
     */
    private String getOperationDescription(String className, String methodName, HttpServletRequest request) {
        StringBuilder description = new StringBuilder();
        
        // 根据控制器类名确定模块
        String module = getModuleName(className);
        description.append(module).append(" - ");
        
        // 根据方法名和HTTP方法确定操作类型
        String httpMethod = request != null ? request.getMethod() : "UNKNOWN";
        String operation = getOperationName(methodName, httpMethod);
        description.append(operation);
        
        // 添加请求路径信息
        if (request != null) {
            String requestURI = request.getRequestURI();
            description.append(" (").append(httpMethod).append(" ").append(requestURI).append(")");
        }
        
        return description.toString();
    }

    /**
     * 根据控制器类名获取模块名称
     */
    private String getModuleName(String className) {
        switch (className) {
            case "AuthController":
                return "用户认证";
            case "UserController":
                return "用户管理";
            case "ImageController":
                return "图片管理";
            case "ImageUploadController":
                return "图片上传";
            case "ClassifyController":
                return "图片分类";
            case "CategoryController":
                return "分类管理";
            case "SystemLogController":
                return "系统日志";
            default:
                return className.replace("Controller", "");
        }
    }

    /**
     * 根据方法名和HTTP方法获取操作名称
     */
    private String getOperationName(String methodName, String httpMethod) {
        // 根据HTTP方法判断
        switch (httpMethod) {
            case "GET":
                if (methodName.contains("list") || methodName.contains("get") || methodName.contains("find")) {
                    return "查询";
                }
                return "获取";
            case "POST":
                if (methodName.contains("login")) {
                    return "登录";
                } else if (methodName.contains("register")) {
                    return "注册";
                } else if (methodName.contains("upload")) {
                    return "上传";
                } else if (methodName.contains("classify")) {
                    return "分类";
                }
                return "创建";
            case "PUT":
                return "更新";
            case "DELETE":
                return "删除";
            default:
                // 根据方法名判断
                if (methodName.contains("create") || methodName.contains("add")) {
                    return "创建";
                } else if (methodName.contains("update") || methodName.contains("modify")) {
                    return "更新";
                } else if (methodName.contains("delete") || methodName.contains("remove")) {
                    return "删除";
                } else if (methodName.contains("login")) {
                    return "登录";
                } else if (methodName.contains("register")) {
                    return "注册";
                } else if (methodName.contains("upload")) {
                    return "上传";
                } else if (methodName.contains("classify")) {
                    return "分类";
                } else if (methodName.contains("get") || methodName.contains("find") || methodName.contains("list")) {
                    return "查询";
                }
                return methodName;
        }
    }
    
    /**
     * 获取当前登录用户名
     */
    private String getCurrentUsername() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated() && 
                !authentication.getName().equals("anonymousUser")) {
                return authentication.getName();
            }
        } catch (Exception e) {
            log.warn("获取当前用户名失败: {}", e.getMessage());
        }
        return "anonymous";
    }
}