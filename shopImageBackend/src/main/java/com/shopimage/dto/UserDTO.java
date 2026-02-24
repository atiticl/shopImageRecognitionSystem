package com.shopimage.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.shopimage.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    
    private Long id;
    private String username;
    private String email;
    private String realName;
    private String phone;
    private String department;
    private User.Role role;
    private User.Status status;
    private String avatar;
    private String remark;
    private Integer loginCount;
    private Integer taskCount;
    private Integer imageCount;
    private Integer onlineTime;
    private Integer lastActiveDays;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastLoginTime;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
    
    // 从User实体转换为DTO
    public static UserDTO fromEntity(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .realName(user.getRealName())
                .phone(user.getPhone())
                .department(user.getDepartment())
                .role(user.getRole())
                .status(user.getStatus())
                .avatar(user.getAvatar())
                .remark(user.getRemark())
                .loginCount(user.getLoginCount() != null ? user.getLoginCount() : 0)
                .taskCount(user.getTaskCount())
                .imageCount(user.getImageCount())
                .onlineTime(user.getOnlineTime())
                .lastActiveDays(user.getLastActiveDays())
                .lastLoginTime(user.getLastLoginTime())
                .createTime(user.getCreatedAt())
                .updateTime(user.getUpdatedAt())
                .build();
    }
}