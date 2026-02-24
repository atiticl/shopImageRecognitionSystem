package com.shopimage.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "system_log")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Level level;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Module module;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;
    
    @Column(length = 50)
    private String username;
    
    @Column(length = 45)
    private String ip;
    
    @Column(columnDefinition = "TEXT")
    private String userAgent;
    
    @Column(columnDefinition = "TEXT")
    private String stackTrace;
    
    @Column(columnDefinition = "JSON")
    private String requestData;
    
    @Column(name = "create_time")
    private LocalDateTime createTime;
    
    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
    }
    
    public enum Level {
        INFO, WARN, ERROR, DEBUG
    }
    
    public enum Module {
        USER, IMAGE, TASK, SYSTEM, AUTH
    }
}