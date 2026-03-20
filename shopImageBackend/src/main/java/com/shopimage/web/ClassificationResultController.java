package com.shopimage.web;

import com.shopimage.common.api.ApiResponse;
import com.shopimage.entity.ClassificationResult;
import com.shopimage.entity.ProductImage;
import com.shopimage.category.Category;
import com.shopimage.category.CategoryRepository;
import com.shopimage.repository.ClassificationResultRepository;
import com.shopimage.repository.ProductImageRepository;
import com.shopimage.entity.SystemLog;
import com.shopimage.utils.LogUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.bind.annotation.*;

import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/classification-results")
@RequiredArgsConstructor
@Slf4j
public class ClassificationResultController {
    
    private final ClassificationResultRepository resultRepository;
    private final CategoryRepository categoryRepository;
    private final ProductImageRepository imageRepository;
    
    /**
     * 获取分类结果列表
     */
    @GetMapping
    public ApiResponse<Map<String, Object>> getResults(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "24") int size,
            @RequestParam(required = false) Long taskId,
            @RequestParam(required = false) Long predictedCategoryId,
            @RequestParam(required = false) String predictedCategory,
            @RequestParam(required = false) Double minConfidence,
            @RequestParam(required = false) Double maxConfidence,
            @RequestParam(required = false) Boolean isCorrected) {
        
        try {
            // 创建分页对象
            Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            Set<Long> fuzzyCategoryIds = null;
            if (predictedCategory != null && !predictedCategory.trim().isEmpty()) {
                fuzzyCategoryIds = categoryRepository.findAll().stream()
                        .filter(category -> category.getName() != null && category.getName().contains(predictedCategory.trim()))
                        .map(Category::getId)
                        .collect(Collectors.toSet());
            }
            
            // 构建查询条件
            Set<Long> finalFuzzyCategoryIds = fuzzyCategoryIds;
            Specification<ClassificationResult> spec = (root, query, criteriaBuilder) -> {
                List<Predicate> predicates = new ArrayList<>();
                
                if (taskId != null) {
                    predicates.add(criteriaBuilder.equal(root.get("taskId"), taskId));
                }

                if (predictedCategoryId != null) {
                    predicates.add(criteriaBuilder.equal(root.get("predictedCategoryId"), predictedCategoryId));
                } else if (finalFuzzyCategoryIds != null) {
                    if (finalFuzzyCategoryIds.isEmpty()) {
                        predicates.add(criteriaBuilder.disjunction());
                    } else {
                        predicates.add(root.get("predictedCategoryId").in(finalFuzzyCategoryIds));
                    }
                }
                
                if (minConfidence != null) {
                    predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("confidence"), BigDecimal.valueOf(minConfidence)));
                }
                
                if (maxConfidence != null) {
                    predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("confidence"), BigDecimal.valueOf(maxConfidence)));
                }
                
                if (isCorrected != null) {
                    if (isCorrected) {
                        predicates.add(criteriaBuilder.isNotNull(root.get("correctedCategoryId")));
                    } else {
                        predicates.add(criteriaBuilder.isNull(root.get("correctedCategoryId")));
                    }
                }
                
                return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
            };
            
            Page<ClassificationResult> resultPage = resultRepository.findAll(spec, pageable);
            
            // 获取所有分类信息
            Map<Long, Category> categoryMap = categoryRepository.findAll().stream()
                    .collect(Collectors.toMap(Category::getId, category -> category));
            
            // 获取所有图片信息
            Set<Long> imageIds = resultPage.getContent().stream()
                    .map(ClassificationResult::getImageId)
                    .collect(Collectors.toSet());
            Map<Long, ProductImage> imageMap = imageRepository.findAllById(imageIds).stream()
                    .collect(Collectors.toMap(ProductImage::getId, image -> image));
            
            // 构建返回数据
            List<Map<String, Object>> resultList = resultPage.getContent().stream()
                    .map(result -> {
                        Map<String, Object> item = new HashMap<>();
                        item.put("id", result.getId());
                        item.put("imageId", result.getImageId());
                        item.put("taskId", result.getTaskId());
                        item.put("confidence", result.getConfidence());
                        item.put("createdAt", result.getCreatedAt());
                        
                        // 获取图片信息
                        ProductImage image = imageMap.get(result.getImageId());
                        if (image != null) {
                            // 返回原始文件名（从objectName中提取）
                            String fileName = image.getObjectName();
                            if (fileName != null && fileName.contains("_")) {
                                // 去掉时间戳前缀，例如 "1234567890_image.jpg" -> "image.jpg"
                                fileName = fileName.substring(fileName.indexOf("_") + 1);
                            }
                            item.put("fileName", fileName);
                            item.put("fileUrl", image.getFileUrl());
                            // 注意：前端会使用 convertToImageUrl 函数将 fileUrl 和 imageId 转换为实际访问URL
                            // 即 /api/images/view/{imageId}
                        }
                        
                        // 获取预测分类信息
                        if (result.getPredictedCategoryId() != null) {
                            Category predCategory = categoryMap.get(result.getPredictedCategoryId());
                            if (predCategory != null) {
                                item.put("predictedCategoryId", predCategory.getId());
                                item.put("predictedCategoryName", predCategory.getName());
                            }
                        }
                        
                        // 获取修正分类信息
                        if (result.getCorrectedCategoryId() != null) {
                            Category correctedCategory = categoryMap.get(result.getCorrectedCategoryId());
                            if (correctedCategory != null) {
                                item.put("correctedCategoryId", correctedCategory.getId());
                                item.put("correctedCategoryName", correctedCategory.getName());
                            }
                            item.put("isCorrected", true);
                        } else {
                            item.put("isCorrected", false);
                        }
                        
                        return item;
                    })
                    .collect(Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("data", resultList);
            response.put("total", resultPage.getTotalElements());
            response.put("totalPages", resultPage.getTotalPages());
            response.put("currentPage", page);
            response.put("pageSize", size);
            
            LogUtils.logInfo(SystemLog.Module.TASK, 
                "查询分类结果 - 任务ID: " + taskId + ", 总数: " + resultPage.getTotalElements(), 
                getCurrentUsername());
            
            return ApiResponse.ok(response);
            
        } catch (Exception e) {
            log.error("获取分类结果失败", e);
            LogUtils.logError(SystemLog.Module.TASK, 
                "分类结果查询失败: " + e.getMessage(), getCurrentUsername());
            return ApiResponse.error("获取分类结果失败: " + e.getMessage());
        }
    }
    
    /**
     * 更新分类结果的修正分类
     */
    @PutMapping("/{resultId}/correct")
    public ApiResponse<ClassificationResult> correctResult(
            @PathVariable Long resultId,
            @RequestBody Map<String, Object> request) {
        
        try {
            Long correctedCategoryId = Long.valueOf(request.get("correctedCategoryId").toString());
            String note = (String) request.get("note");
            
            // 查找分类结果
            Optional<ClassificationResult> optionalResult = resultRepository.findById(resultId);
            if (optionalResult.isEmpty()) {
                return ApiResponse.error("分类结果不存在");
            }
            
            ClassificationResult result = optionalResult.get();
            
            // 验证分类是否存在
            Optional<Category> optionalCategory = categoryRepository.findById(correctedCategoryId);
            if (optionalCategory.isEmpty()) {
                return ApiResponse.error("指定的分类不存在");
            }
            
            // 更新修正分类
            result.setCorrectedCategoryId(correctedCategoryId);
            result = resultRepository.save(result);
            
            LogUtils.logInfo(SystemLog.Module.TASK, 
                "修正分类结果 - 结果ID: " + resultId + ", 修正分类ID: " + correctedCategoryId, 
                getCurrentUsername());
            
            return ApiResponse.ok(result);
            
        } catch (Exception e) {
            log.error("修正分类结果失败", e);
            LogUtils.logError(SystemLog.Module.TASK, 
                "修正分类结果失败 - 结果ID: " + resultId + ", 错误: " + e.getMessage(), 
                getCurrentUsername());
            return ApiResponse.error("修正分类结果失败: " + e.getMessage());
        }
    }
    
    /**
     * 批量修正分类结果
     */
    @PutMapping("/batch-correct")
    public ApiResponse<String> batchCorrectResults(@RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<Long> resultIds = (List<Long>) request.get("resultIds");
            Long correctedCategoryId = Long.valueOf(request.get("correctedCategoryId").toString());
            String note = (String) request.get("note");
            
            if (resultIds == null || resultIds.isEmpty()) {
                return ApiResponse.error("请选择要修正的结果");
            }
            
            // 验证分类是否存在
            Optional<Category> optionalCategory = categoryRepository.findById(correctedCategoryId);
            if (optionalCategory.isEmpty()) {
                return ApiResponse.error("指定的分类不存在");
            }
            
            // 批量更新
            List<ClassificationResult> results = resultRepository.findAllById(resultIds);
            for (ClassificationResult result : results) {
                result.setCorrectedCategoryId(correctedCategoryId);
            }
            resultRepository.saveAll(results);
            
            LogUtils.logInfo(SystemLog.Module.TASK, 
                "批量修正分类结果 - 数量: " + results.size() + ", 修正分类ID: " + correctedCategoryId, 
                getCurrentUsername());
            
            return ApiResponse.ok("批量修正成功，共修正 " + results.size() + " 条记录");
            
        } catch (Exception e) {
            log.error("批量修正分类结果失败", e);
            LogUtils.logError(SystemLog.Module.TASK, 
                "批量修正分类结果失败: " + e.getMessage(), getCurrentUsername());
            return ApiResponse.error("批量修正分类结果失败: " + e.getMessage());
        }
    }
    
    private String getCurrentUsername() {
        // 这里应该从Spring Security上下文中获取当前用户名
        // 暂时返回默认值
        return "system";
    }
}
