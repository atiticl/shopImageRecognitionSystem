package com.shopimage.dto;

import com.shopimage.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthResponse {
    
    private String token;
    private String username;
    private User.Role role;
    private String message;
    
    public AuthResponse(String token, String username, User.Role role) {
        this.token = token;
        this.username = username;
        this.role = role;
        this.message = "登录成功";
    }
}