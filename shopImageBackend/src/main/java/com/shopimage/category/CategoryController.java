package com.shopimage.category;

import com.shopimage.category.dto.CategoryRequest;
import com.shopimage.common.api.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {
    private final com.shopimage.service.CategoryService categoryService;

    @GetMapping
    public ApiResponse<Object> list(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(categoryService.findByConditions(name, isActive, page, size));
    }
    
    @GetMapping("/all")
    public ApiResponse<List<Category>> getAllActive() {
        // 返回所有激活的类别，不分页
        return ApiResponse.ok(categoryService.findByConditions(null, true));
    }

    @GetMapping("/{id}")
    public ApiResponse<Category> getById(@PathVariable Long id) {
        return ApiResponse.ok(categoryService.findById(id));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Category>> create(@Valid @RequestBody CategoryRequest req) {
        Category c = Category.builder()
                .parentId(req.getParentId())
                .name(req.getName())
                .description(req.getDescription())
                .categoryCode(req.getCategoryCode())
                .level(req.getLevel() != null ? req.getLevel() : 1)
                .sortOrder(req.getSortOrder() != null ? req.getSortOrder() : 0)
                .isActive(req.getIsActive() != null ? req.getIsActive() : true)
                .iconUrl(req.getIconUrl())
                .build();
        c = categoryService.create(c);
        return ResponseEntity.created(URI.create("/api/categories/" + c.getId())).body(ApiResponse.ok(c));
    }

    @PutMapping("/{id}")
    public ApiResponse<Category> update(@PathVariable Long id, @Valid @RequestBody CategoryRequest req) {
        Category update = Category.builder()
                .parentId(req.getParentId())
                .name(req.getName())
                .description(req.getDescription())
                .categoryCode(req.getCategoryCode())
                .level(req.getLevel() != null ? req.getLevel() : 1)
                .sortOrder(req.getSortOrder() != null ? req.getSortOrder() : 0)
                .isActive(req.getIsActive() != null ? req.getIsActive() : true)
                .iconUrl(req.getIconUrl())
                .build();
        return ApiResponse.ok(categoryService.update(id, update));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Boolean> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return ApiResponse.ok(true);
    }

    @GetMapping("/tree")
    public ApiResponse<List<Category>> tree() {
        return ApiResponse.ok(categoryService.findTree());
    }
}