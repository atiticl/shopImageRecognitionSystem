package com.shopimage.service;

import com.shopimage.dto.AuthResponse;
import com.shopimage.dto.LoginRequest;
import com.shopimage.dto.RegisterRequest;
import com.shopimage.entity.SystemLog;
import com.shopimage.entity.User;
import com.shopimage.repository.UserRepository;
import com.shopimage.security.JwtUtil;
import com.shopimage.utils.LogUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Registering new user: {}", request.getUsername());
        
        try {
            // 检查用户名是否已存在
            if (userRepository.existsByUsername(request.getUsername())) {
                LogUtils.logWarn(SystemLog.Module.AUTH, 
                    String.format("用户注册失败：用户名 %s 已存在", request.getUsername()), 
                    request.getUsername());
                throw new RuntimeException("用户名已存在");
            }
            
            // 检查邮箱是否已存在
            if (request.getEmail() != null && userRepository.existsByEmail(request.getEmail())) {
                LogUtils.logWarn(SystemLog.Module.AUTH, 
                    String.format("用户注册失败：邮箱 %s 已被使用", request.getEmail()), 
                    request.getUsername());
                throw new RuntimeException("邮箱已被使用");
            }
            
            // 创建新用户
            User user = new User();
            user.setUsername(request.getUsername());
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            user.setEmail(request.getEmail());
            user.setRole(request.getRole());
            
            User savedUser = userRepository.save(user);
            log.info("User registered successfully: {}", savedUser.getUsername());
            
            // 记录注册成功日志
            LogUtils.logUserRegister(savedUser.getUsername(), true);
            
            // 生成JWT token
            String token = jwtUtil.generateToken(savedUser);
            
            return new AuthResponse(token, savedUser.getUsername(), savedUser.getRole(), "注册成功");
        } catch (Exception e) {
            // 记录注册失败日志
            LogUtils.logUserRegister(request.getUsername(), false);
            throw e;
        }
    }

    public AuthResponse login(LoginRequest request) {
        log.info("User login attempt: {}", request.getUsername());
        
        try {
            // 使用Spring Security进行认证
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    request.getUsername(),
                    request.getPassword()
                )
            );
            
            User user = (User) authentication.getPrincipal();
            log.info("User login successful: {} with role: {}", user.getUsername(), user.getRole());
            
            // 记录登录成功日志
            LogUtils.logUserLogin(user.getUsername(), true);
            
            // 更新登录信息
            updateLoginInfo(user);
            
            // 生成JWT token
            String token = jwtUtil.generateToken(user);
            
            return new AuthResponse(token, user.getUsername(), user.getRole());
            
        } catch (AuthenticationException e) {
            log.error("Login failed for user: {}, error: {}", request.getUsername(), e.getMessage());
            
            // 记录登录失败日志
            LogUtils.logUserLogin(request.getUsername(), false);
            
            throw new RuntimeException("用户名或密码错误");
        }
    }
    
    /**
     * 更新用户登录信息
     */
    @Transactional
    private void updateLoginInfo(User user) {
        // 更新登录次数
        user.setLoginCount(user.getLoginCount() + 1);
        // 更新最后登录时间
        user.setLastLoginTime(java.time.LocalDateTime.now());
        // 保存到数据库
        userRepository.save(user);
        log.info("Updated login info for user: {}, login count: {}", user.getUsername(), user.getLoginCount());
    }
}