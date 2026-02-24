package com.shopimage.category.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class CategoryRequest {
    private Long parentId;
    
    @NotBlank(message = "类别名称不能为空")
    private String name;
    
    private String description;
    
    @Pattern(regexp = "^[A-Z_]+$", message = "类别编码只能包含大写字母和下划线")
    private String categoryCode;
    
    private Integer level = 1;
    
    private Integer sortOrder = 0;
    
    private Boolean isActive = true;
    
    private String iconUrl;
}