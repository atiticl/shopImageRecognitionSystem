package com.shopimage.web;

import com.shopimage.entity.Model;
import com.shopimage.repository.ModelRepository;
import com.shopimage.service.MinioService;
import com.shopimage.service.ModelService;

import com.shopimage.common.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

// 缺少的BigDecimal导入，实际代码中需要添加
import java.math.BigDecimal;

@RestController
@RequestMapping({"/api/models", "/models"})
@RequiredArgsConstructor
@Slf4j
public class ModelController {
    private final MinioService minioService;
    private final ModelRepository modelRepository;
    private final ModelService modelService;

    // 支持的模型文件格式
    private static final Pattern MODEL_FILE_PATTERN = Pattern.compile("^.*\\.(onnx|pt|pth|h5|pb)$");
    // 最大文件大小限制(500MB)
    private static final long MAX_FILE_SIZE = 500 * 1024 * 1024;

    /**
     * 上传模型文件（仅上传文件，返回文件URL）
     */
    @PostMapping("/upload")
    public ApiResponse<Map<String, Object>> uploadModel(@RequestParam("file") MultipartFile file) {
        try {
            // 1. 验证文件
            validateUploadRequest(file);

            // 2. 上传文件到MinIO
            String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            String objectName = "models/" + fileName;
            String fileUrl = minioService.upload(objectName, file);

            log.info("Successfully uploaded model file: {}", file.getOriginalFilename());
            return ApiResponse.ok(Map.of(
                    "fileUrl", fileUrl,
                    "fileName", file.getOriginalFilename()
            ));
        } catch (ResponseStatusException e) {
            log.error("Model upload failed: {}", e.getReason());
            return ApiResponse.error(e.getReason());
        } catch (Exception e) {
            log.error("Model upload failed: {}", e.getMessage(), e);
            return ApiResponse.error("Model upload failed: " + e.getMessage());
        }
    }

    /**
     * 验证上传文件请求
     */
    private void validateUploadRequest(MultipartFile file) {
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "文件不能为空");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "文件大小不能超过500MB");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || !MODEL_FILE_PATTERN.matcher(filename.toLowerCase()).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "不支持的文件格式，仅支持: .onnx, .pt, .pth, .h5, .pb");
        }
    }

    /**
     * 将预签名URL转换为minio://格式
     */
    private String normalizeFileUrl(String fileUrl) {
        if (fileUrl == null || fileUrl.trim().isEmpty()) {
            return fileUrl;
        }

        // 如果已经是minio://格式，直接返回
        if (fileUrl.startsWith("minio://")) {
            return fileUrl;
        }

        // 处理完整的HTTP URL（包含预签名参数）
        if (fileUrl.startsWith("http://") || fileUrl.startsWith("https://")) {
            try {
                // 移除URL参数（?之后的部分）
                String urlWithoutParams = fileUrl.split("\\?")[0];
                
                // 提取bucket之后的路径部分
                // URL格式: http://host:port/bucket/object_name
                String[] parts = urlWithoutParams.split("/");
                if (parts.length >= 5) {
                    // 从bucket之后开始拼接对象名称
                    StringBuilder objectName = new StringBuilder();
                    for (int i = 4; i < parts.length; i++) {
                        if (i > 4) {
                            objectName.append("/");
                        }
                        objectName.append(parts[i]);
                    }
                    return "minio://" + objectName.toString();
                }
            } catch (Exception e) {
                log.warn("解析HTTP URL失败，使用原始URL: {}", fileUrl);
            }
        }

        return fileUrl;
    }

    /**
     * 停用其他所有激活的模型
     */
    private void deactivateOtherModels() {
        List<Model> activeModels = modelRepository.findActiveModels();
        for (Model model : activeModels) {
            model.setStatus(Model.ModelStatus.INACTIVE);
            modelRepository.save(model);
        }
    }

    /**
     * 创建新模型
     */
    @PostMapping
    public ApiResponse<Model> createModel(
            @RequestParam("name") String name,
            @RequestParam("version") String version,
            @RequestParam("type") String type,
            @RequestParam("framework") String framework,
            @RequestParam("fileUrl") String fileUrl,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "threshold", defaultValue = "80.0") Double threshold,
            @RequestParam(value = "status", defaultValue = "ACTIVE") String status) {
        try {
            // 验证必填字段
            if (name == null || name.trim().isEmpty()) {
                return ApiResponse.error("模型名称不能为空");
            }
            if (version == null || version.trim().isEmpty()) {
                return ApiResponse.error("模型版本不能为空");
            }
            // 规范化并验证框架取值（与数据库枚举保持一致）
            if (framework == null || framework.trim().isEmpty()) {
                return ApiResponse.error("模型框架不能为空");
            }
            String normalizedFramework = framework.trim().toUpperCase();
            if (!List.of("TENSORFLOW", "PYTORCH", "ONNX").contains(normalizedFramework)) {
                return ApiResponse.error("模型框架不支持，请选择 TENSORFLOW、PYTORCH 或 ONNX");
            }
            if (fileUrl == null || fileUrl.trim().isEmpty()) {
                return ApiResponse.error("文件URL不能为空");
            }

            // 将预签名URL转换为minio://格式
            String normalizedFileUrl = normalizeFileUrl(fileUrl);

            // 如果设置为激活，先停用其他模型
            if ("ACTIVE".equals(status)) {
                deactivateOtherModels();
            }

            // 创建模型对象
            Model model = Model.builder()
                    .name(name.trim())
                    .version(version.trim())
                    .type(Model.ModelType.valueOf(type))
                    .framework(normalizedFramework)
                    .fileUrl(normalizedFileUrl)
                    .description(description != null ? description.trim() : null)
                    .threshold(BigDecimal.valueOf(threshold))
                    .status(Model.ModelStatus.valueOf(status))
                    .createTime(LocalDateTime.now())
                    .updateTime(LocalDateTime.now())
                    .uploadedAt(LocalDateTime.now())
                    .build();

            // 保存到数据库
            model = modelRepository.save(model);
            
            return ApiResponse.success(model, "模型创建成功");
        } catch (Exception e) {
            log.error("创建模型失败", e);
            return ApiResponse.error("创建模型失败: " + e.getMessage());
        }
    }

    /**
     * 分页查询模型列表（支持搜索）
     */
    @GetMapping
    public ApiResponse<Object> getModels(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createTime") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type) {
        try {
            return ApiResponse.ok(modelService.getModels(page, size, sortBy, sortDir, name, status, type));
        } catch (Exception e) {
            log.error("查询模型列表失败", e);
            return ApiResponse.error("查询模型列表失败: " + e.getMessage());
        }
    }

    /**
     * 根据ID查询模型详情
     */
    @GetMapping("/{id}")
    public ApiResponse<Model> getModelById(@PathVariable Long id) {
        try {
            Model model = modelRepository.findById(id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "模型不存在"));
            return ApiResponse.ok(model);
        } catch (Exception e) {
            log.error("查询模型详情失败", e);
            return ApiResponse.error("查询模型详情失败: " + e.getMessage());
        }
    }

    /**
     * 更新模型信息
     */
    @PutMapping("/{id}")
    public ApiResponse<Model> updateModel(
            @PathVariable Long id,
            @RequestParam("name") String name,
            @RequestParam("version") String version,
            @RequestParam("type") String type,
            @RequestParam("framework") String framework,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "threshold", defaultValue = "80.0") Double threshold) {
        try {
            // 规范化并验证框架取值
            if (framework == null || framework.trim().isEmpty()) {
                return ApiResponse.error("模型框架不能为空");
            }
            String normalizedFramework = framework.trim().toUpperCase();
            if (!List.of("TENSORFLOW", "PYTORCH", "ONNX").contains(normalizedFramework)) {
                return ApiResponse.error("模型框架不支持，请选择 TENSORFLOW、PYTORCH 或 ONNX");
            }
            Model model = modelService.updateModel(id, name, version, type, normalizedFramework, description, threshold);
            return ApiResponse.ok(model);
        } catch (Exception e) {
            log.error("更新模型信息失败", e);
            return ApiResponse.error("更新模型信息失败: " + e.getMessage());
        }
    }

    /**
     * 启用/禁用模型
     */
    @PutMapping("/{id}/status")
    public ApiResponse<Model> toggleModelStatus(@PathVariable Long id) {
        try {
            Model model = modelRepository.findById(id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "模型不存在"));
            
            // 切换状态
            if (model.getStatus() == Model.ModelStatus.ACTIVE) {
                model.setStatus(Model.ModelStatus.INACTIVE);
            } else {
                // 如果要激活，先停用其他模型
                deactivateOtherModels();
                model.setStatus(Model.ModelStatus.ACTIVE);
            }
            
            model = modelRepository.save(model);
            String action = model.getStatus() == Model.ModelStatus.ACTIVE ? "启用" : "禁用";
            return ApiResponse.success(model, "模型" + action + "成功");
        } catch (Exception e) {
            log.error("切换模型状态失败", e);
            return ApiResponse.error("切换模型状态失败: " + e.getMessage());
        }
    }

    /**
     * 测试模型
     */
    @PostMapping("/{id}/test")
    public ApiResponse<Object> testModel(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ApiResponse.error("测试图片不能为空");
            }

            // 验证文件类型
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || (!originalFilename.toLowerCase().endsWith(".jpg") && 
                !originalFilename.toLowerCase().endsWith(".jpeg") && 
                !originalFilename.toLowerCase().endsWith(".png"))) {
                return ApiResponse.error("只支持 jpg, jpeg, png 格式的图片文件");
            }

            Object result = modelService.testModel(id, file);
            return ApiResponse.success(result, "模型测试完成");
        } catch (Exception e) {
            log.error("模型测试失败", e);
            return ApiResponse.error("模型测试失败: " + e.getMessage());
        }
    }

    /**
     * 删除模型（同步删除MinIO中的文件）
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Boolean> deleteModel(@PathVariable Long id) {
        try {
            Model model = modelRepository.findById(id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "模型不存在"));

            // 不能删除正在激活的模型
            if (Model.ModelStatus.ACTIVE.equals(model.getStatus())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "不能删除正在使用的模型，请先激活其他模型");
            }

            // 删除MinIO中的文件
            if (model.getFileUrl() != null) {
                modelService.deleteModelFile(model.getFileUrl());
            }
            
            // 删除数据库记录
            modelRepository.delete(model);
            
            return ApiResponse.success(true, "模型删除成功");
        } catch (Exception e) {
            log.error("删除模型失败", e);
            return ApiResponse.error("删除模型失败: " + e.getMessage());
        }
    }

}

