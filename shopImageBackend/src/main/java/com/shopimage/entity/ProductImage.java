package com.shopimage.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "product_image")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "bucket", nullable = false, length = 128)
    private String bucket;

    @Column(name = "object_name", nullable = false, length = 128)
    private String objectName;

    @Column(name = "file_url", nullable = false, length = 255)
    private String fileUrl;

    @Column(name = "thumb_url", length = 255)
    private String thumbUrl;

    @Column(name = "status", length = 32)
    private String status;

    @Column(name = "image_md5", length = 64)
    private String imageMd5;

    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt;
}