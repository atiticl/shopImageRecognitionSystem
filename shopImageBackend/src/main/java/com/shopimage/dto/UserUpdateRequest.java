package com.shopimage.dto;

import com.shopimage.entity.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserUpdateRequest {
    
    @Size(min = 3, max = 50, message = "用户名长度必须在3-50个字符之间")
    private String username;
    
    @Size(min = 6, max = 100, message = "密码长度必须在6-100个字符之间")
    private String password;
    
    @Email(message = "邮箱格式不正确")
    private String email;
    
    private String realName;
    private String phone;
    private String department;
    private User.Role role;
    private User.Status status;
    private String avatar;
    private String remark;
}