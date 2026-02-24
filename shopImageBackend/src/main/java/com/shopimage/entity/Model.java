package com.shopimage.entity;

import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "model")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Model {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "version")
    private String version;

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    private ModelType type;

    @Column(name = "file_url", nullable = false)
    private String fileUrl;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "framework")
    private String framework;

    @Column(name = "accuracy")
    private BigDecimal accuracy;

    @Column(name = "precision_score")
    private BigDecimal precision;

    @Column(name = "recall_score")
    private BigDecimal recall;

    @Column(name = "f1_score")
    private BigDecimal f1Score;

    @Column(name = "inference_time")
    private Integer inferenceTime;

    @Column(name = "description")
    private String description;

    @Column(name = "threshold")
    private BigDecimal threshold;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private ModelStatus status;

    @Column(name = "created_at")
    private LocalDateTime createTime;

    @Column(name = "updated_at")
    private LocalDateTime updateTime;

    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt;

    // 关联关系
    @OneToMany(mappedBy = "model", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<ModelVersion> versions;

    // 枚举定义
    public enum ModelType {
        GENERAL("通用分类"),
        CLOTHING("服装分类"),
        ELECTRONICS("电子产品");

        private final String description;

        ModelType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    public enum ModelStatus {
        ACTIVE("启用"),
        INACTIVE("禁用");

        private final String description;

        ModelStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    // 便捷方法
    public String getFormattedFileSize() {
        if (fileSize == null) return "-";
        if (fileSize < 1024) return fileSize + " B";
        if (fileSize < 1024 * 1024) return String.format("%.1f KB", fileSize / 1024.0);
        if (fileSize < 1024 * 1024 * 1024) return String.format("%.1f MB", fileSize / (1024.0 * 1024));
        return String.format("%.1f GB", fileSize / (1024.0 * 1024 * 1024));
    }
}
