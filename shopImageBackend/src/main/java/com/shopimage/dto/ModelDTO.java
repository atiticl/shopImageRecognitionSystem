package com.shopimage.dto;

import com.shopimage.entity.Model;
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
public class ModelDTO {
    private Long id;
    private String name;
    private String version;
    private String type;
    private String fileUrl;
    private String fileSize;
    private String framework;
    private BigDecimal accuracy;
    private BigDecimal precision;
    private BigDecimal recall;
    private BigDecimal f1Score;
    private Integer inferenceTime;
    private String description;
    private BigDecimal threshold;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private LocalDateTime uploadedAt;
    
    // 版本相关信息
    private Integer versionCount;
    private String currentVersion;

    // 从实体转换为DTO
    public static ModelDTO fromEntity(Model model) {
        return ModelDTO.builder()
                .id(model.getId())
                .name(model.getName())
                .version(model.getVersion())
                .type(model.getType() != null ? model.getType().name() : null)
                .fileUrl(model.getFileUrl())
                .fileSize(model.getFormattedFileSize())
                .framework(model.getFramework())
                .accuracy(model.getAccuracy())
                .precision(model.getPrecision())
                .recall(model.getRecall())
                .f1Score(model.getF1Score())
                .inferenceTime(model.getInferenceTime())
                .description(model.getDescription())
                .threshold(model.getThreshold())
                .status(model.getStatus() != null ? model.getStatus().name() : null)
                .createTime(model.getCreateTime())
                .updateTime(model.getUpdateTime())
                .uploadedAt(model.getUploadedAt())
                .versionCount(model.getVersions() != null ? model.getVersions().size() : 0)
                .build();
    }

    // 从DTO转换为实体
    public Model toEntity() {
        return Model.builder()
                .id(this.id)
                .name(this.name)
                .version(this.version)
                .type(this.type != null ? Model.ModelType.valueOf(this.type) : null)
                .fileUrl(this.fileUrl)
                .fileSize(this.fileSize != null ? Long.valueOf(this.fileSize) : null)
                .framework(this.framework)
                .accuracy(this.accuracy)
                .precision(this.precision)
                .recall(this.recall)
                .f1Score(this.f1Score)
                .inferenceTime(this.inferenceTime)
                .description(this.description)
                .threshold(this.threshold)
                .status(this.status != null ? Model.ModelStatus.valueOf(this.status) : null)
                .createTime(this.createTime)
                .updateTime(this.updateTime)
                .uploadedAt(this.uploadedAt)
                .build();
    }
}