package com.shopimage.web;

import com.shopimage.category.Category;
import com.shopimage.category.CategoryRepository;
import com.shopimage.common.api.ApiResponse;
import com.shopimage.entity.ClassificationResult;
import com.shopimage.entity.ProductImage;
import com.shopimage.repository.ClassificationResultRepository;
import com.shopimage.repository.ProductImageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/public/exhibition")
@RequiredArgsConstructor
@Slf4j
public class ExhibitionController {

    private final ClassificationResultRepository resultRepository;
    private final ProductImageRepository imageRepository;
    private final CategoryRepository categoryRepository;

    @GetMapping("/categories")
    public ApiResponse<List<Map<String, Object>>> listCategories() {
        try {
            List<ClassificationResult> allResults = resultRepository.findAll().stream()
                    .filter(r -> effectiveCategoryId(r) != null)
                    .collect(Collectors.toList());

            Map<Long, Long> countByCategory = allResults.stream()
                    .collect(Collectors.groupingBy(
                            this::effectiveCategoryId,
                            Collectors.counting()));

            List<Category> categories = categoryRepository.findAll();
            Map<Long, Category> catMap = categories.stream()
                    .collect(Collectors.toMap(Category::getId, c -> c));

            List<Map<String, Object>> result = new ArrayList<>();
            Map<String, Object> allItem = new LinkedHashMap<>();
            allItem.put("id", 0L);
            allItem.put("name", "全部");
            allItem.put("count", (long) allResults.size());
            result.add(allItem);

            countByCategory.forEach((categoryId, count) -> {
                Category cat = catMap.get(categoryId);
                if (cat == null) return;
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", cat.getId());
                item.put("name", cat.getName());
                item.put("description", cat.getDescription());
                item.put("count", count);
                result.add(item);
            });

            result.sort((a, b) -> {
                Long idA = (Long) a.get("id");
                Long idB = (Long) b.get("id");
                if (idA == 0L) return -1;
                if (idB == 0L) return 1;
                return Long.compare(idA, idB);
            });

            return ApiResponse.ok(result);
        } catch (Exception e) {
            log.error("获取展示分类失败", e);
            return ApiResponse.error("获取分类失败: " + e.getMessage());
        }
    }

    @GetMapping("/images")
    public ApiResponse<Map<String, Object>> listImages(
            @RequestParam(defaultValue = "0") Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "24") int size) {
        try {
            List<ClassificationResult> allResults = resultRepository.findAll().stream()
                    .filter(r -> effectiveCategoryId(r) != null)
                    .collect(Collectors.toList());

            Map<Long, ClassificationResult> latestByImage = new HashMap<>();
            for (ClassificationResult r : allResults) {
                ClassificationResult prev = latestByImage.get(r.getImageId());
                if (prev == null || (r.getCreatedAt() != null && prev.getCreatedAt() != null
                        && r.getCreatedAt().isAfter(prev.getCreatedAt()))) {
                    latestByImage.put(r.getImageId(), r);
                }
            }

            List<ClassificationResult> filtered = latestByImage.values().stream()
                    .filter(r -> categoryId == null || categoryId == 0L
                            || categoryId.equals(effectiveCategoryId(r)))
                    .collect(Collectors.toList());

            List<Long> imageIds = filtered.stream()
                    .map(ClassificationResult::getImageId)
                    .collect(Collectors.toList());

            Map<Long, ProductImage> imageMap = imageRepository.findAllById(imageIds).stream()
                    .collect(Collectors.toMap(ProductImage::getId, i -> i));

            Map<Long, Category> catMap = categoryRepository.findAll().stream()
                    .collect(Collectors.toMap(Category::getId, c -> c));

            List<Map<String, Object>> items = filtered.stream()
                    .map(r -> {
                        ProductImage img = imageMap.get(r.getImageId());
                        if (img == null) return null;
                        if (keyword != null && !keyword.trim().isEmpty()
                                && img.getObjectName() != null
                                && !img.getObjectName().toLowerCase().contains(keyword.toLowerCase().trim())) {
                            return null;
                        }
                        Category cat = catMap.get(effectiveCategoryId(r));
                        Map<String, Object> item = new LinkedHashMap<>();
                        item.put("imageId", img.getId());
                        item.put("fileName", stripTimestamp(img.getObjectName()));
                        item.put("uploadedAt", img.getUploadedAt());
                        item.put("categoryId", cat != null ? cat.getId() : null);
                        item.put("categoryName", cat != null ? cat.getName() : "未分类");
                        item.put("confidence", r.getConfidence());
                        item.put("corrected", r.getCorrectedCategoryId() != null);
                        item.put("imageUrl", "/api/images/view/" + img.getId());
                        return item;
                    })
                    .filter(Objects::nonNull)
                    .sorted((a, b) -> {
                        Object ta = a.get("uploadedAt");
                        Object tb = b.get("uploadedAt");
                        if (ta == null && tb == null) return 0;
                        if (ta == null) return 1;
                        if (tb == null) return -1;
                        return tb.toString().compareTo(ta.toString());
                    })
                    .collect(Collectors.toList());

            int total = items.size();
            int from = Math.max(0, (page - 1) * size);
            int to = Math.min(from + size, total);
            List<Map<String, Object>> pageItems = from >= to ? Collections.emptyList() : items.subList(from, to);

            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("list", pageItems);
            resp.put("total", total);
            resp.put("page", page);
            resp.put("size", size);
            return ApiResponse.ok(resp);
        } catch (Exception e) {
            log.error("获取展示图片失败", e);
            return ApiResponse.error("获取图片失败: " + e.getMessage());
        }
    }

    private Long effectiveCategoryId(ClassificationResult r) {
        return r.getCorrectedCategoryId() != null ? r.getCorrectedCategoryId() : r.getPredictedCategoryId();
    }

    private String stripTimestamp(String objectName) {
        if (objectName == null) return "";
        String name = objectName.contains("/") ? objectName.substring(objectName.lastIndexOf('/') + 1) : objectName;
        int idx = name.indexOf('_');
        if (idx > 0 && idx < name.length() - 1) {
            String head = name.substring(0, idx);
            if (head.chars().allMatch(Character::isDigit)) {
                return name.substring(idx + 1);
            }
        }
        return name;
    }
}
