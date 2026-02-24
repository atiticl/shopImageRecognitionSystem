package com.shopimage.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "classification_result")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassificationResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "image_id", nullable = false)
    private Long imageId;

    @Column(name = "task_id", nullable = false)
    private Long taskId;

    @Column(name = "predicted_category_id")
    private Long predictedCategoryId;

    @Column(name = "confidence")
    private java.math.BigDecimal confidence;

    @Column(name = "corrected_category_id")
    private Long correctedCategoryId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
