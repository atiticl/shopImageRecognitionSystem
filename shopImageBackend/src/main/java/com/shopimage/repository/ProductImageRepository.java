package com.shopimage.repository;

import com.shopimage.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {
    long countByUploadedAtBetween(LocalDateTime startTime, LocalDateTime endTime);
}
