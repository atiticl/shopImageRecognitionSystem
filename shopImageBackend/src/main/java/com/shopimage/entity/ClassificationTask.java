package com.shopimage.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "classification_task")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassificationTask {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_name", nullable = false)
    private String taskName;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "total_images")
    private Integer totalImages;

    @Column(name = "processed_images")
    private Integer processedImages;

    @Column(name = "status")
    private String status;

    @Column(name = "model_name")
    private String modelName;

    @Column(name = "model_version")
    private String modelVersion;

    @Column(name = "model_id")
    private Long modelId;

    @Column(name = "description")
    private String description;

    @Column(name = "processing_time")
    private String processingTime;

    @Column(name = "success_count")
    private Integer successCount;

    @Column(name = "failed_count")
    private Integer failedCount;

    @Column(name = "accuracy", precision = 5, scale = 2)
    private BigDecimal accuracy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}