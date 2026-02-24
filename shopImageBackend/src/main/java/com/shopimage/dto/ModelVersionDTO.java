package com.shopimage.dto;

import com.shopimage.entity.ModelVersion;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelVersionDTO {
    private Long id;
    private Long modelId;
    private String versionNumber;
    private String fileUrl;
    private Long fileSize;
    private BigDecimal accuracy;
    private BigDecimal precision;
    private BigDecimal recall;
    private BigDecimal f1Score;
    private Integer inferenceTime;

    private String description;
    private Boolean isCurrent;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 从实体转换为DTO
    public static ModelVersionDTO fromEntity(ModelVersion version) {
        return ModelVersionDTO.builder()
                .id(version.getId())
                .modelId(version.getModel() != null ? version.getModel().getId() : null)
                .versionNumber(version.getVersionNumber())
                .fileUrl(version.getFileUrl())
                .fileSize(version.getFileSize())
                .accuracy(version.getAccuracy())
                .precision(version.getPrecisionScore())
                .recall(version.getRecallScore())
                .f1Score(version.getF1Score())
                .inferenceTime(version.getInferenceTime())
                .description(version.getDescription())
                .isCurrent(version.getIsCurrent())
                .createdAt(version.getCreatedAt())
                .updatedAt(version.getUpdatedAt())
                .build();
    }

    // 从DTO转换为实体
    public ModelVersion toEntity() {
        return ModelVersion.builder()
                .id(this.id)
                .versionNumber(this.versionNumber)
                .fileUrl(this.fileUrl)
                .fileSize(this.fileSize)
                .accuracy(this.accuracy)
                .precisionScore(this.precision)
                .recallScore(this.recall)
                .f1Score(this.f1Score)
                .inferenceTime(this.inferenceTime)
                .description(this.description)
                .isCurrent(this.isCurrent)
                .createdAt(this.createdAt)
                .updatedAt(this.updatedAt)
                .build();
    }
}