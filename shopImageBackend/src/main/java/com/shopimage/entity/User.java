package com.shopimage.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;

@Entity
@Table(name = "user")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String username;

    @Column(nullable = false, length = 100)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(length = 100)
    private String email;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "real_name", length = 100)
    private String realName;

    @Column(length = 20)
    private String phone;

    @Column(length = 100)
    private String department;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginTime;

    @Column(name = "login_count")
    @Builder.Default
    private Integer loginCount = 0;

    @Column(length = 255)
    private String avatar;

    @Column(columnDefinition = "TEXT")
    private String remark;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Status status = Status.ACTIVE;

    @Column(name = "task_count")
    @Builder.Default
    private Integer taskCount = 0;

    @Column(name = "image_count")
    @Builder.Default
    private Integer imageCount = 0;

    @Column(name = "online_time")
    @Builder.Default
    private Integer onlineTime = 0;

    @Column(name = "last_active_days")
    @Builder.Default
    private Integer lastActiveDays = 0;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Spring Security UserDetails 接口实现
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    // 角色枚举
    public enum Role {
        ADMIN,    // 管理员：可以管理用户、模型、系统配置等
        OPERATOR  // 操作员：可以上传图片、查看分类结果等
    }

    // 用户状态枚举
    public enum Status {
        ACTIVE,   // 正常
        INACTIVE, // 禁用
        LOCKED    // 锁定
    }
}