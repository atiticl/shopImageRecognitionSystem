package com.shopimage.web;

import com.shopimage.category.Category;
import com.shopimage.category.CategoryRepository;
import com.shopimage.common.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 类别调试控制器 - 用于排查类别数据问题
 */
@RestController
@RequestMapping("/api/debug/categories")
@RequiredArgsConstructor
@Slf4j
public class CategoryDebugController {
    
    private final CategoryRepository categoryRepository;
    
    /**
     * 获取所有类别（包括未激活的）
     */
    @GetMapping("/all")
    public ApiResponse<List<Category>> getAllCategories() {
        try {
            List<Category> categories = categoryRepository.findAll();
            log.info("找到 {} 个类别", categories.size());
            return ApiResponse.ok(categories);
        } catch (Exception e) {
            log.error("获取类别列表失败", e);
            return ApiResponse.error("获取类别列表失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取类别统计信息
     */
    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> getCategoryStats() {
        try {
            List<Category> allCategories = categoryRepository.findAll();
            long activeCount = allCategories.stream().filter(c -> c.getIsActive() != null && c.getIsActive()).count();
            long inactiveCount = allCategories.size() - activeCount;
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("total", allCategories.size());
            stats.put("active", activeCount);
            stats.put("inactive", inactiveCount);
            stats.put("categories", allCategories);
            
            return ApiResponse.ok(stats);
        } catch (Exception e) {
            log.error("获取类别统计失败", e);
            return ApiResponse.error("获取类别统计失败: " + e.getMessage());
        }
    }
    
    /**
     * 初始化测试类别数据
     */
    @PostMapping("/init-test-data")
    public ApiResponse<String> initTestData() {
        try {
            // 检查是否已有数据
            long count = categoryRepository.count();
            if (count > 0) {
                return ApiResponse.error("数据库中已有 " + count + " 个类别，请先清空或手动添加");
            }
            
            // 创建测试类别
            String[] categoryNames = {
                "文具类", "糖果类", "食品类", "饮料类", "洗漱用品", "其他类"
            };
            
            String[] categoryCodes = {
                "STATIONERY", "CANDY", "FOOD", "BEVERAGE", "TOILETRIES", "OTHER"
            };
            
            String[] descriptions = {
                "各类文具用品", "糖果零食", "食品类商品", "各类饮料", "洗漱护理用品", "其他未分类商品"
            };
            
            for (int i = 0; i < categoryNames.length; i++) {
                Category category = Category.builder()
                        .name(categoryNames[i])
                        .categoryCode(categoryCodes[i])
                        .description(descriptions[i])
                        .level(1)
                        .sortOrder(i)
                        .isActive(true)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();
                categoryRepository.save(category);
            }
            
            log.info("成功初始化 {} 个测试类别", categoryNames.length);
            return ApiResponse.ok("成功初始化 " + categoryNames.length + " 个测试类别");
            
        } catch (Exception e) {
            log.error("初始化测试数据失败", e);
            return ApiResponse.error("初始化测试数据失败: " + e.getMessage());
        }
    }
}
