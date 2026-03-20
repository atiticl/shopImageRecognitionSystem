package com.shopimage.service;

import com.shopimage.dto.UserCreateRequest;
import com.shopimage.dto.UserDTO;
import com.shopimage.dto.UserUpdateRequest;
import com.shopimage.entity.User;
import com.shopimage.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 获取分页用户列表
     */
    public Page<UserDTO> getUsers(int page, int size, String sortBy, String sortDir, String search, User.Role role, User.Status status) {
        log.info("Getting users - page: {}, size: {}, sortBy: {}, sortDir: {}, search: {}, role: {}, status: {}", 
                page, size, sortBy, sortDir, search, role, status);
        
        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<User> userPage = userRepository.findByConditions(search, role, status, pageable);
        
        return userPage.map(UserDTO::fromEntity);
    }

    /**
     * 根据ID获取用户详情
     */
    public Optional<UserDTO> getUserById(Long id) {
        log.info("Getting user by id: {}", id);
        return userRepository.findById(id).map(UserDTO::fromEntity);
    }

    /**
     * 创建新用户
     */
    @Transactional
    public UserDTO createUser(UserCreateRequest request) {
        log.info("Creating new user: {}", request.getUsername());
        
        // 检查用户名是否已存在
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("用户名已存在");
        }
        
        // 检查邮箱是否已存在
        if (request.getEmail() != null && userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("邮箱已被使用");
        }
        
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setRealName(request.getRealName());
        user.setPhone(request.getPhone());
        user.setDepartment(request.getDepartment());
        user.setRole(request.getRole());
        user.setStatus(request.getStatus() != null ? request.getStatus() : User.Status.ACTIVE);
        user.setAvatar(normalizeAvatarForStorage(request.getAvatar()));
        user.setRemark(request.getRemark());
        user.setLoginCount(0);
        user.setTaskCount(0);
        user.setImageCount(0);
        user.setOnlineTime(0);
        user.setLastActiveDays(0);
        
        User savedUser = userRepository.save(user);
        log.info("User created successfully: {}", savedUser.getUsername());
        
        return UserDTO.fromEntity(savedUser);
    }

    /**
     * 更新用户信息
     */
    @Transactional
    public UserDTO updateUser(Long id, UserUpdateRequest request) {
        log.info("Updating user: {}", id);
        
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        // 检查用户名是否被其他用户使用
        if (request.getUsername() != null && !request.getUsername().equals(user.getUsername())) {
            if (userRepository.existsByUsername(request.getUsername())) {
                throw new RuntimeException("用户名已存在");
            }
            user.setUsername(request.getUsername());
        }
        
        // 检查邮箱是否被其他用户使用
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new RuntimeException("邮箱已被使用");
            }
            user.setEmail(request.getEmail());
        }
        
        // 更新密码（如果提供）
        if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        
        // 更新其他字段
        if (request.getRealName() != null) user.setRealName(request.getRealName());
        if (request.getPhone() != null) user.setPhone(request.getPhone());
        if (request.getDepartment() != null) user.setDepartment(request.getDepartment());
        if (request.getRole() != null) user.setRole(request.getRole());
        if (request.getStatus() != null) user.setStatus(request.getStatus());
        if (request.getAvatar() != null) {
            String normalizedAvatar = normalizeAvatarForStorage(request.getAvatar());
            user.setAvatar(normalizedAvatar);
        }
        if (request.getRemark() != null) user.setRemark(request.getRemark());
        
        user.setUpdatedAt(LocalDateTime.now());
        
        User savedUser = userRepository.save(user);
        log.info("User updated successfully: {}", savedUser.getUsername());
        
        return UserDTO.fromEntity(savedUser);
    }

    /**
     * 删除用户
     */
    @Transactional
    public void deleteUser(Long id) {
        log.info("Deleting user: {}", id);
        
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("用户不存在");
        }
        
        userRepository.deleteById(id);
        log.info("User deleted successfully: {}", id);
    }

    /**
     * 批量删除用户
     */
    @Transactional
    public void deleteUsers(List<Long> ids) {
        log.info("Batch deleting users: {}", ids);
        
        List<User> users = userRepository.findAllById(ids);
        if (users.size() != ids.size()) {
            throw new RuntimeException("部分用户不存在");
        }
        
        userRepository.deleteAllById(ids);
        log.info("Users deleted successfully: {}", ids);
    }

    /**
     * 更新用户状态
     */
    @Transactional
    public void updateUserStatus(Long id, User.Status status) {
        log.info("Updating user status - id: {}, status: {}", id, status);
        
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        user.setStatus(status);
        user.setUpdatedAt(LocalDateTime.now());
        
        userRepository.save(user);
        log.info("User status updated successfully: {}", id);
    }

    /**
     * 重置用户密码
     */
    @Transactional
    public void resetPassword(Long id, String newPassword) {
        log.info("Resetting password for user: {}", id);
        
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        
        userRepository.save(user);
        log.info("Password reset successfully for user: {}", id);
    }

    /**
     * 获取用户统计信息
     */
    public Map<String, Object> getUserStatistics() {
        log.info("Getting user statistics");
        
        long totalUsers = userRepository.count();
        
        // 这里可以扩展更多统计信息
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", totalUsers);
        stats.put("activeUsers", totalUsers); // TODO: 实现活跃用户统计
        stats.put("inactiveUsers", 0); // TODO: 实现非活跃用户统计
        stats.put("lockedUsers", 0); // TODO: 实现锁定用户统计
        
        return stats;
    }

    /**
     * 获取所有可用的用户角色
     */
    public User.Role[] getAvailableRoles() {
        return User.Role.values();
    }

    /**
     * 获取所有可用的用户状态
     */
    public User.Status[] getAvailableStatuses() {
        return User.Status.values();
    }

    /**
     * 根据用户名查找用户
     */
    public Optional<UserDTO> getUserByUsername(String username) {
        log.info("Getting user by username: {}", username);
        return userRepository.findByUsername(username).map(UserDTO::fromEntity);
    }

    /**
     * 检查用户名是否存在
     */
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    /**
     * 检查邮箱是否存在
     */
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    private String normalizeAvatarForStorage(String avatar) {
        if (avatar == null) {
            return null;
        }
        String value = avatar.trim();
        if (value.isEmpty()) {
            return null;
        }
        if (value.startsWith("minio://")) {
            return value.length() <= 255 ? value : null;
        }
        if (value.startsWith("http://") || value.startsWith("https://")) {
            String urlWithoutParams = value.split("\\?")[0];
            try {
                java.net.URI uri = java.net.URI.create(urlWithoutParams);
                String path = uri.getPath();
                if (path != null && !path.isBlank()) {
                    String normalizedPath = path.startsWith("/") ? path.substring(1) : path;
                    int slashIndex = normalizedPath.indexOf('/');
                    if (slashIndex >= 0 && slashIndex < normalizedPath.length() - 1) {
                        String objectName = normalizedPath.substring(slashIndex + 1);
                        String minioPath = "minio://" + objectName;
                        if (minioPath.length() <= 255) {
                            return minioPath;
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Avatar URL parse failed: {}", e.getMessage());
            }
        }
        if (value.length() <= 255) {
            return value;
        }
        log.warn("Avatar value too long and skipped. length={}", value.length());
        return null;
    }
}
