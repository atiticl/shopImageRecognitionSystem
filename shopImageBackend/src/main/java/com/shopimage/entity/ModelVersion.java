package com.shopimage.entity;

import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "model_version")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelVersion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "model_id", nullable = false)
    @JsonIgnore
    private Model model;

    @Column(name = "version_number", nullable = false)
    private String versionNumber;

    @Column(name = "file_url", nullable = false)
    private String fileUrl;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "accuracy")
    private BigDecimal accuracy;

    @Column(name = "precision_score")
    private BigDecimal precisionScore;

    @Column(name = "recall_score")
    private BigDecimal recallScore;

    @Column(name = "f1_score")
    private BigDecimal f1Score;

    @Column(name = "inference_time")
    private Integer inferenceTime;

    @Column(name = "description")
    private String description;

    @Column(name = "is_current")
    private Boolean isCurrent;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}