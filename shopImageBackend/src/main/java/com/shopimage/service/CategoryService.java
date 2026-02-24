package com.shopimage.service;

import com.shopimage.category.Category;
import com.shopimage.category.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository categoryRepository;

    public List<Category> findAll() {
        return categoryRepository.findAll();
    }

    public List<Category> findByConditions(String name, Boolean isActive) {
        if (name == null && isActive == null) {
            return findAll();
        }
        
        List<Category> categories = findAll();
        return categories.stream()
                .filter(category -> {
                    boolean nameMatch = name == null || 
                        (category.getName() != null && category.getName().toLowerCase().contains(name.toLowerCase()));
                    boolean statusMatch = isActive == null || 
                        Objects.equals(category.getIsActive(), isActive);
                    return nameMatch && statusMatch;
                })
                .collect(Collectors.toList());
    }

    public Map<String, Object> findByConditions(String name, Boolean isActive, int page, int size) {
        // 获取所有符合条件的数据
        List<Category> allCategories = findByConditions(name, isActive);
        
        // 计算分页信息
        int total = allCategories.size();
        int totalPages = (int) Math.ceil((double) total / size);
        int startIndex = (page - 1) * size;
        int endIndex = Math.min(startIndex + size, total);
        
        // 获取当前页数据
        List<Category> pageData = allCategories.subList(startIndex, endIndex);
        
        // 构建返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("content", pageData);
        result.put("totalElements", total);
        result.put("totalPages", totalPages);
        result.put("currentPage", page);
        result.put("size", size);
        result.put("hasNext", page < totalPages);
        result.put("hasPrevious", page > 1);
        
        return result;
    }

    public Category findById(Long id) {
        return categoryRepository.findById(id).orElseThrow(() -> 
            new RuntimeException("Category not found with id: " + id));
    }

    public Category create(Category c) {
        // 根据父类别设置层级
        if (c.getParentId() == null) {
            // 如果没有父类别，设置为顶级类别（层级1）
            c.setLevel(1);
        } else {
            // 如果有父类别，查找父类别的层级，新类别层级为父类别层级+1
            Category parent = categoryRepository.findById(c.getParentId()).orElseThrow(() -> 
                new RuntimeException("Parent category not found with id: " + c.getParentId()));
            c.setLevel(parent.getLevel() + 1);
        }
        
        c.setCreatedAt(LocalDateTime.now());
        c.setUpdatedAt(LocalDateTime.now());
        return categoryRepository.save(c);
    }

    public Category update(Long id, Category update) {
        Category c = categoryRepository.findById(id).orElseThrow();
        c.setName(update.getName());
        
        // 检查父类别是否发生变化，如果变化则重新计算层级
        if (!Objects.equals(c.getParentId(), update.getParentId())) {
            c.setParentId(update.getParentId());
            
            // 根据新的父类别设置层级
            if (update.getParentId() == null) {
                // 如果没有父类别，设置为顶级类别（层级1）
                c.setLevel(1);
            } else {
                // 如果有父类别，查找父类别的层级，新类别层级为父类别层级+1
                Category parent = categoryRepository.findById(update.getParentId()).orElseThrow(() -> 
                    new RuntimeException("Parent category not found with id: " + update.getParentId()));
                c.setLevel(parent.getLevel() + 1);
            }
        }
        
        c.setDescription(update.getDescription());
        c.setCategoryCode(update.getCategoryCode());
        c.setSortOrder(update.getSortOrder());
        c.setIsActive(update.getIsActive());
        c.setIconUrl(update.getIconUrl());
        c.setUpdatedAt(LocalDateTime.now());
        return categoryRepository.save(c);
    }

    public void delete(Long id) {
        categoryRepository.deleteById(id);
    }

    public List<Category> findTree() {
        List<Category> allCategories = categoryRepository.findAll();
        return buildTree(allCategories, null);
    }

    private List<Category> buildTree(List<Category> allCategories, Long parentId) {
        return allCategories.stream()
                .filter(category -> Objects.equals(category.getParentId(), parentId))
                .map(category -> {
                    Category treeNode = new Category();
                    treeNode.setId(category.getId());
                    treeNode.setParentId(category.getParentId());
                    treeNode.setName(category.getName());
                    treeNode.setDescription(category.getDescription());
                    treeNode.setCategoryCode(category.getCategoryCode());
                    treeNode.setLevel(category.getLevel());
                    treeNode.setSortOrder(category.getSortOrder());
                    treeNode.setIsActive(category.getIsActive());
                    treeNode.setIconUrl(category.getIconUrl());
                    treeNode.setCreatedAt(category.getCreatedAt());
                    treeNode.setUpdatedAt(category.getUpdatedAt());
                    
                    List<Category> children = buildTree(allCategories, category.getId());
                    if (!children.isEmpty()) {
                        // 这里需要在Category实体中添加children字段
                    }
                    return treeNode;
                })
                .sorted((a, b) -> Integer.compare(
                    a.getSortOrder() != null ? a.getSortOrder() : 0,
                    b.getSortOrder() != null ? b.getSortOrder() : 0
                ))
                .collect(Collectors.toList());
    }
}