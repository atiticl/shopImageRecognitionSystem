package com.shopimage.web;

import com.shopimage.common.api.ApiResponse;
import com.shopimage.entity.ClassificationResult;
import com.shopimage.entity.ProductImage;
import com.shopimage.repository.ClassificationResultRepository;
import com.shopimage.repository.ProductImageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 调试控制器 - 用于排查图片显示问题
 */
@RestController
@RequestMapping("/api/debug")
@RequiredArgsConstructor
@Slf4j
public class DebugController {
    
    private final ClassificationResultRepository resultRepository;
    private final ProductImageRepository imageRepository;
    
    /**
     * 获取分类结果的详细信息（用于调试）
     */
    @GetMapping("/result/{resultId}")
    public ApiResponse<Map<String, Object>> getResultDebugInfo(@PathVariable Long resultId) {
        try {
            ClassificationResult result = resultRepository.findById(resultId)
                    .orElseThrow(() -> new RuntimeException("结果不存在"));
            
            ProductImage image = imageRepository.findById(result.getImageId())
                    .orElseThrow(() -> new RuntimeException("图片不存在"));
            
            Map<String, Object> debugInfo = new HashMap<>();
            debugInfo.put("resultId", result.getId());
            debugInfo.put("imageId", result.getImageId());
            debugInfo.put("taskId", result.getTaskId());
            debugInfo.put("confidence", result.getConfidence());
            
            // 图片信息
            Map<String, Object> imageInfo = new HashMap<>();
            imageInfo.put("id", image.getId());
            imageInfo.put("bucket", image.getBucket());
            imageInfo.put("objectName", image.getObjectName());
            imageInfo.put("fileUrl", image.getFileUrl());
            imageInfo.put("status", image.getStatus());
            debugInfo.put("imageInfo", imageInfo);
            
            // 生成访问URL
            debugInfo.put("viewUrl", "/api/images/view/" + image.getId());
            
            return ApiResponse.ok(debugInfo);
            
        } catch (Exception e) {
            log.error("获取调试信息失败", e);
            return ApiResponse.error("获取调试信息失败: " + e.getMessage());
        }
    }
    
    /**
     * 列出所有图片（用于调试）
     */
    @GetMapping("/images")
    public ApiResponse<List<ProductImage>> listAllImages() {
        try {
            List<ProductImage> images = imageRepository.findAll();
            log.info("找到 {} 张图片", images.size());
            return ApiResponse.ok(images);
        } catch (Exception e) {
            log.error("获取图片列表失败", e);
            return ApiResponse.error("获取图片列表失败: " + e.getMessage());
        }
    }
    
    /**
     * 列出所有分类结果（用于调试）
     */
    @GetMapping("/results")
    public ApiResponse<List<ClassificationResult>> listAllResults() {
        try {
            List<ClassificationResult> results = resultRepository.findAll();
            log.info("找到 {} 条分类结果", results.size());
            return ApiResponse.ok(results);
        } catch (Exception e) {
            log.error("获取分类结果列表失败", e);
            return ApiResponse.error("获取分类结果列表失败: " + e.getMessage());
        }
    }
}
