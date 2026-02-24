package com.shopimage.web;

import com.shopimage.common.api.ApiResponse;
import com.shopimage.dto.UserCreateRequest;
import com.shopimage.dto.UserDTO;
import com.shopimage.dto.UserUpdateRequest;
import com.shopimage.entity.SystemLog;
import com.shopimage.entity.User;
import com.shopimage.service.UserService;
import com.shopimage.service.MinioService;
import com.shopimage.utils.LogUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class UserController {

    private final UserService userService;
    private final MinioService minioService;

    /**
     * 获取用户列表（分页）
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<UserDTO>>> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) User.Role role,
            @RequestParam(required = false) User.Status status) {
        try {
            LogUtils.logInfo(SystemLog.Module.USER, "管理员查询用户列表，页码: " + page + ", 大小: " + size + 
                (keyword != null ? ", 关键词: " + keyword : ""), getCurrentUsername());
            
            Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
            Pageable pageable = PageRequest.of(page, size, sort);
            
            Page<UserDTO> userDTOPage = userService.getUsers(page, size, sortBy, sortDir, keyword, role, status);
            // 为列表中的每个用户转换头像URL为预签名链接
            userDTOPage = userDTOPage.map(dto -> {
                try {
                    String avatar = dto.getAvatar();
                    if (avatar != null && !avatar.isBlank()) {
                        String objectName = null;
                        if (avatar.startsWith("minio://")) {
                            objectName = avatar.substring("minio://".length());
                        } else {
                            String marker = "/" + minioService.getBucket() + "/";
                            int idx = avatar.indexOf(marker);
                            if (idx >= 0) {
                                objectName = avatar.substring(idx + marker.length());
                            }
                        }
                        if (objectName != null && !objectName.isBlank()) {
                            String signed = minioService.presignedGetUrl(objectName, 7 * 24 * 60);
                            dto.setAvatar(signed);
                        }
                    }
                } catch (Exception e) {
                    log.warn("生成列表头像预签名URL失败: {}", e.getMessage());
                }
                return dto;
            });
            
            LogUtils.logInfo(SystemLog.Module.USER, "成功获取用户列表，共 " + userDTOPage.getTotalElements() + " 条记录", getCurrentUsername());
            return ResponseEntity.ok(ApiResponse.ok(userDTOPage));
        } catch (Exception e) {
            log.error("获取用户列表失败: {}", e.getMessage(), e);
            LogUtils.logError(SystemLog.Module.USER, "获取用户列表失败: " + e.getMessage(), getCurrentUsername());
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("获取用户列表失败: " + e.getMessage()));
        }
    }

    /**
     * 根据ID获取用户详情
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")
    public ResponseEntity<ApiResponse<UserDTO>> getUserById(@PathVariable Long id) {
        try {
            UserDTO userDTO = userService.getUserById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
            // 转换详情头像为预签名URL
            try {
                String avatar = userDTO.getAvatar();
                if (avatar != null && !avatar.isBlank()) {
                    String objectName = null;
                    if (avatar.startsWith("minio://")) {
                        objectName = avatar.substring("minio://".length());
                    } else {
                        String marker = "/" + minioService.getBucket() + "/";
                        int idx = avatar.indexOf(marker);
                        if (idx >= 0) {
                            objectName = avatar.substring(idx + marker.length());
                        }
                    }
                    if (objectName != null && !objectName.isBlank()) {
                        String signed = minioService.presignedGetUrl(objectName, 7 * 24 * 60);
                        userDTO.setAvatar(signed);
                    }
                }
            } catch (Exception e) {
                log.warn("生成详情头像预签名URL失败: {}", e.getMessage());
            }
            return ResponseEntity.ok(ApiResponse.ok(userDTO));
        } catch (Exception e) {
            log.error("获取用户详情失败: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("获取用户详情失败: " + e.getMessage()));
        }
    }

    /**
     * 创建新用户
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserDTO>> createUser(@Valid @RequestBody UserCreateRequest request) {
        try {
            log.info("创建用户请求: {}", request.getUsername());
            LogUtils.logInfo(SystemLog.Module.USER, "管理员创建新用户: " + request.getUsername() + 
                ", 角色: " + request.getRole(), getCurrentUsername());
            
            UserDTO userDTO = userService.createUser(request);
            
            LogUtils.logInfo(SystemLog.Module.USER, "成功创建用户: " + request.getUsername() + " (ID: " + userDTO.getId() + ")", getCurrentUsername());
            return ResponseEntity.ok(ApiResponse.ok(userDTO));
        } catch (Exception e) {
            log.error("创建用户失败: {}", e.getMessage(), e);
            LogUtils.logError(SystemLog.Module.USER, "创建用户失败，用户名: " + request.getUsername() + ", 错误: " + e.getMessage(), getCurrentUsername());
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("创建用户失败: " + e.getMessage()));
        }
    }

    /**
     * 更新用户信息
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")
    public ResponseEntity<ApiResponse<UserDTO>> updateUser(
            @PathVariable Long id, 
            @Valid @RequestBody UserUpdateRequest request) {
        try {
            log.info("更新用户请求: {}", id);
            UserDTO userDTO = userService.updateUser(id, request);
            return ResponseEntity.ok(ApiResponse.ok(userDTO));
        } catch (Exception e) {
            log.error("更新用户失败: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("更新用户失败: " + e.getMessage()));
        }
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Boolean>> deleteUser(@PathVariable Long id) {
        try {
            log.info("删除用户请求: {}", id);
            userService.deleteUser(id);
            return ResponseEntity.ok(ApiResponse.ok(true));
        } catch (Exception e) {
            log.error("删除用户失败: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("删除用户失败: " + e.getMessage()));
        }
    }

    /**
     * 批量删除用户
     */
    @DeleteMapping("/batch")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Boolean>> batchDeleteUsers(@RequestBody List<Long> ids) {
        try {
            log.info("批量删除用户请求: {}", ids);
            userService.deleteUsers(ids);
            return ResponseEntity.ok(ApiResponse.ok(true));
        } catch (Exception e) {
            log.error("批量删除用户失败: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("批量删除用户失败: " + e.getMessage()));
        }
    }

    /**
     * 更新用户状态
     */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserDTO>> updateUserStatus(
            @PathVariable Long id, 
            @RequestParam User.Status status) {
        try {
            log.info("更新用户状态请求: {} -> {}", id, status);
            userService.updateUserStatus(id, status);
            UserDTO userDTO = userService.getUserById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
            return ResponseEntity.ok(ApiResponse.ok(userDTO));
        } catch (Exception e) {
            log.error("更新用户状态失败: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("更新用户状态失败: " + e.getMessage()));
        }
    }

    /**
     * 重置用户密码
     */
    @PutMapping("/{id}/password/reset")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Boolean>> resetUserPassword(@PathVariable Long id) {
        try {
            log.info("重置用户密码请求: {}", id);
            userService.resetPassword(id, "123456"); // 默认密码
            return ResponseEntity.ok(ApiResponse.ok(true));
        } catch (Exception e) {
            log.error("重置用户密码失败: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("重置用户密码失败: " + e.getMessage()));
        }
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/current")
    public ResponseEntity<ApiResponse<UserDTO>> getCurrentUser() {
        try {
            String username = getCurrentUsername();
            if ("anonymous".equals(username)) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("用户未登录"));
            }
            
            UserDTO userDTO = userService.getUserByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
            // 转换头像为可访问的预签名URL（如果存储的是对象路径或直链）
            try {
                String avatar = userDTO.getAvatar();
                if (avatar != null && !avatar.isBlank()) {
                    String objectName = null;
                    if (avatar.startsWith("minio://")) {
                        objectName = avatar.substring("minio://".length());
                    } else {
                        String marker = "/" + minioService.getBucket() + "/";
                        int idx = avatar.indexOf(marker);
                        if (idx >= 0) {
                            objectName = avatar.substring(idx + marker.length());
                        }
                    }
                    if (objectName != null && !objectName.isBlank()) {
                        String signed = minioService.presignedGetUrl(objectName, 7 * 24 * 60);
                        userDTO.setAvatar(signed);
                    }
                }
            } catch (Exception e) {
                log.warn("生成头像预签名URL失败: {}", e.getMessage());
            }
            
            return ResponseEntity.ok(ApiResponse.ok(userDTO));
        } catch (Exception e) {
            log.error("获取当前用户信息失败: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("获取当前用户信息失败: " + e.getMessage()));
        }
    }

    /**
     * 更新当前用户信息
     */
    @PutMapping("/current")
    public ResponseEntity<ApiResponse<UserDTO>> updateCurrentUser(@Valid @RequestBody UserUpdateRequest request) {
        try {
            String username = getCurrentUsername();
            if ("anonymous".equals(username)) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("用户未登录"));
            }
            
            UserDTO currentUser = userService.getUserByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
            
            UserDTO userDTO = userService.updateUser(currentUser.getId(), request);
            // 返回更新后的用户信息时也转换头像为预签名URL
            try {
                String avatar = userDTO.getAvatar();
                if (avatar != null && !avatar.isBlank()) {
                    String objectName = null;
                    if (avatar.startsWith("minio://")) {
                        objectName = avatar.substring("minio://".length());
                    } else {
                        String marker = "/" + minioService.getBucket() + "/";
                        int idx = avatar.indexOf(marker);
                        if (idx >= 0) {
                            objectName = avatar.substring(idx + marker.length());
                        }
                    }
                    if (objectName != null && !objectName.isBlank()) {
                        String signed = minioService.presignedGetUrl(objectName, 7 * 24 * 60);
                        userDTO.setAvatar(signed);
                    }
                }
            } catch (Exception e) {
                log.warn("生成头像预签名URL失败: {}", e.getMessage());
            }
            return ResponseEntity.ok(ApiResponse.ok(userDTO));
        } catch (Exception e) {
            log.error("更新当前用户信息失败: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("更新当前用户信息失败: " + e.getMessage()));
        }
    }

    /**
     * 获取用户统计信息
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Object>> getUserStatistics() {
        try {
            Object statistics = userService.getUserStatistics();
            return ResponseEntity.ok(ApiResponse.ok(statistics));
        } catch (Exception e) {
            log.error("获取用户统计信息失败: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("获取用户统计信息失败: " + e.getMessage()));
        }
    }

    /**
     * 获取所有角色
     */
    @GetMapping("/roles")
    public ResponseEntity<ApiResponse<User.Role[]>> getRoles() {
        return ResponseEntity.ok(ApiResponse.ok(User.Role.values()));
    }

    /**
     * 获取所有状态
     */
    @GetMapping("/statuses")
    public ResponseEntity<ApiResponse<User.Status[]>> getStatuses() {
        return ResponseEntity.ok(ApiResponse.ok(User.Status.values()));
    }
    
    /**
     * 上传用户头像
     */
    @PostMapping("/avatar/upload")
    public ResponseEntity<ApiResponse<Map<String, Object>>> uploadAvatar(@RequestParam("file") MultipartFile file) {
        try {
            String currentUsername = getCurrentUsername();
            
            if (file.isEmpty()) {
                LogUtils.logWarn(SystemLog.Module.USER, "用户上传了空的头像文件", currentUsername);
                return ResponseEntity.badRequest().body(ApiResponse.error("文件不能为空"));
            }

            // 验证文件类型
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                LogUtils.logWarn(SystemLog.Module.USER, "用户上传了非图片格式的头像文件: " + contentType, currentUsername);
                return ResponseEntity.badRequest().body(ApiResponse.error("只能上传图片文件"));
            }

            // 验证文件大小 (2MB)
            if (file.getSize() > 2 * 1024 * 1024) {
                LogUtils.logWarn(SystemLog.Module.USER, "用户上传的头像文件过大: " + file.getSize() + " bytes", currentUsername);
                return ResponseEntity.badRequest().body(ApiResponse.error("文件大小不能超过2MB"));
            }

            log.info("开始上传用户头像: {}, 大小: {}", file.getOriginalFilename(), file.getSize());
            LogUtils.logInfo(SystemLog.Module.USER, "开始上传头像: " + file.getOriginalFilename() + ", 大小: " + file.getSize(), currentUsername);

            // 生成文件名
            String originalFilename = file.getOriginalFilename();
            String fileExtension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String objectName = "avatars/" + currentUsername + "_" + System.currentTimeMillis() + fileExtension;

            // 上传到Minio
            String fileUrl = minioService.upload(objectName, file);

            Map<String, Object> result = new HashMap<>();
            result.put("fileUrl", fileUrl);
            result.put("fileName", originalFilename);

            LogUtils.logInfo(SystemLog.Module.USER, "头像上传成功: " + fileUrl, currentUsername);
            log.info("用户头像上传成功: {}", fileUrl);

            return ResponseEntity.ok(ApiResponse.ok(result));

        } catch (Exception e) {
            String currentUsername = getCurrentUsername();
            LogUtils.logError(SystemLog.Module.USER, "头像上传失败: " + e.getMessage(), currentUsername);
            log.error("头像上传失败", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("头像上传失败: " + e.getMessage()));
        }
    }

    /**
     * 获取当前登录用户的用户名
     */
    private String getCurrentUsername() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated() && 
                !"anonymousUser".equals(authentication.getName())) {
                return authentication.getName();
            }
        } catch (Exception e) {
            log.debug("获取当前用户名失败: {}", e.getMessage());
        }
        return "anonymous";
    }
}