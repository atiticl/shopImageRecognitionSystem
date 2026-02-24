package com.shopimage.web;

import com.shopimage.category.Category;
import com.shopimage.category.CategoryRepository;
import com.shopimage.common.api.ApiResponse;
import com.shopimage.entity.ClassificationResult;
import com.shopimage.entity.ClassificationTask;
import com.shopimage.entity.ProductImage;
import com.shopimage.entity.SystemLog;
import com.shopimage.repository.ClassificationResultRepository;
import com.shopimage.repository.ClassificationTaskRepository;
import com.shopimage.repository.ProductImageRepository;
import com.shopimage.service.MessageProducer;
import com.shopimage.service.MinioService;
import com.shopimage.utils.LogUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import java.io.InputStream;

@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
@Slf4j
public class ImageController {
    private final MinioService minioService;
    private final ProductImageRepository imageRepository;
    private final ClassificationTaskRepository taskRepository;
    private final MessageProducer producer;
    private final ClassificationResultRepository resultRepository;
    private final CategoryRepository categoryRepository;

    @PostMapping("/upload")
    public ApiResponse<Map<String, Object>> upload(@RequestParam("file") MultipartFile file,
            @RequestParam(value = "taskId", required = false) Long taskId,
            @RequestParam(value = "enqueue", defaultValue = "false") boolean enqueue) {
        try {
            if (file.isEmpty()) {
                LogUtils.logWarn(SystemLog.Module.IMAGE, "用户上传了空文件", getCurrentUsername());
                return ApiResponse.error("文件上传为空");
            }

            log.info("Uploading file: {}, size: {}", file.getOriginalFilename(), file.getSize());
            LogUtils.logInfo(SystemLog.Module.IMAGE, "开始上传图片到存储: " + file.getOriginalFilename() + ", 大小: " + file.getSize(), getCurrentUsername());

            String objectName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            String url = minioService.upload(objectName, file);
            String md5 = DigestUtils.md5DigestAsHex(file.getBytes());

            ProductImage img = imageRepository.save(ProductImage.builder()
                    .userId(1L)
                    .bucket(minioService.getBucket())
                    .objectName(objectName)
                    .fileUrl(url)
                    .status("UPLOADED")
                    .imageMd5(md5)
                    .uploadedAt(LocalDateTime.now())
                    .build());

            // 修复：单图片上传时不自动创建任务，只有在指定taskId或enqueue=true时才处理任务逻辑
            ClassificationTask task = null;
            if (taskId != null) {
                task = taskRepository.findById(taskId).orElseThrow();
                task.setTotalImages(task.getTotalImages() + 1);
                task.setUpdatedAt(LocalDateTime.now());
                taskRepository.save(task);
                LogUtils.logInfo(SystemLog.Module.TASK, "任务 " + task.getTaskName() + " 添加图片，总数: " + task.getTotalImages(), getCurrentUsername());
            } else if (enqueue) {
                // 只有在明确要求入队时才创建任务（用于批量上传等场景）
                task = taskRepository.save(ClassificationTask.builder()
                        .taskName("task-" + System.currentTimeMillis())
                        .userId(1L)
                        .totalImages(1)
                        .processedImages(0)
                        .status("PENDING")
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build());
                LogUtils.logInfo(SystemLog.Module.TASK, "创建新任务: " + task.getTaskName() + " (ID: " + task.getId() + ")", getCurrentUsername());
            }

            if (enqueue && task != null) {
                Map<String, Object> payload = new HashMap<>();
                payload.put("taskId", task.getId());
                payload.put("imageId", img.getId());
                producer.send(payload, task.getId());
                if ("PENDING".equals(task.getStatus())) {
                    task.setStatus("PROCESSING");
                    taskRepository.save(task);
                }
                LogUtils.logInfo(SystemLog.Module.IMAGE, "图片 " + file.getOriginalFilename() + " 已加入处理队列", getCurrentUsername());
            }

            Map<String, Object> resp = new HashMap<>();
            resp.put("imageId", img.getId());
            if (task != null) {
                resp.put("taskId", task.getId());
            }
            // 生成预签名URL供前端直接访问
            String presignedUrl = minioService.presignedGetUrl(objectName, 60); // 60分钟有效期
            resp.put("url", presignedUrl);
            resp.put("md5", md5);
            
            LogUtils.logInfo(SystemLog.Module.IMAGE, "图片上传成功: " + file.getOriginalFilename() + ", 大小: " + file.getSize() + ", ID: " + img.getId(), getCurrentUsername());
            return ApiResponse.ok(resp);

        } catch (Exception e) {
            log.error("Upload failed: {}", e.getMessage(), e);
            LogUtils.logError(SystemLog.Module.IMAGE, "图片上传失败，文件: " + file.getOriginalFilename() + ", 错误: " + e.getMessage(), getCurrentUsername());
            return ApiResponse.error("Upload failed: " + e.getMessage());
        }
    }
    
    @PostMapping("/upload-batch")
    public ApiResponse<Map<String, Object>> uploadBatch(@RequestParam("file") MultipartFile file,
            @RequestParam(value = "taskName", required = false) String taskName) {
        try {
            if (file.isEmpty()) {
                LogUtils.logWarn(SystemLog.Module.IMAGE, "用户上传了空文件", getCurrentUsername());
                return ApiResponse.error("文件上传为空");
            }

            // 检查文件类型是否为压缩包
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || (!originalFilename.toLowerCase().endsWith(".zip") && 
                !originalFilename.toLowerCase().endsWith(".rar") && 
                !originalFilename.toLowerCase().endsWith(".7z"))) {
                return ApiResponse.error("只支持zip、rar、7z格式的压缩包");
            }

            log.info("Uploading batch file: {}, size: {}", file.getOriginalFilename(), file.getSize());
            LogUtils.logInfo(SystemLog.Module.IMAGE, "开始上传压缩包到存储: " + file.getOriginalFilename() + ", 大小: " + file.getSize(), getCurrentUsername());

            // 上传压缩包到MinIO的test-images文件夹
            String objectName = "test-images/" + System.currentTimeMillis() + "_" + file.getOriginalFilename();
            String url = minioService.upload(objectName, file);

            // 创建分类任务
            String finalTaskName = taskName != null && !taskName.trim().isEmpty() ? 
                taskName : "批量分类任务-" + System.currentTimeMillis();
            
            ClassificationTask task = taskRepository.save(ClassificationTask.builder()
                    .taskName(finalTaskName)
                    .userId(1L)
                    .totalImages(0) // 初始为0，解压后更新
                    .processedImages(0)
                    .status("PENDING")
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build());

            // 发送消息到队列进行解压和处理
            Map<String, Object> message = new HashMap<>();
            message.put("taskId", task.getId());
            message.put("zipObjectName", objectName);
            message.put("zipUrl", url);
            producer.sendBatchProcessMessage(message);

            Map<String, Object> result = new HashMap<>();
            result.put("taskId", task.getId());
            result.put("taskName", task.getTaskName());
            result.put("zipUrl", url);
            result.put("status", "PENDING");

            LogUtils.logInfo(SystemLog.Module.IMAGE, "压缩包上传成功，任务ID: " + task.getId(), getCurrentUsername());
            return ApiResponse.ok(result);

        } catch (Exception e) {
            log.error("Batch upload failed: {}", e.getMessage(), e);
            LogUtils.logError(SystemLog.Module.IMAGE, "压缩包上传失败: " + e.getMessage(), getCurrentUsername());
            return ApiResponse.error("压缩包上传失败: " + e.getMessage());
        }
    }

    /**
     * 从MinIO获取图片并渲染到前端
     */
    @GetMapping("/view/{imageId}")
    public ResponseEntity<byte[]> viewImage(@PathVariable Long imageId) {
        try {
            LogUtils.logInfo(SystemLog.Module.IMAGE, "请求查看图片ID: " + imageId, getCurrentUsername());
            
            // 查找图片记录
            Optional<ProductImage> imageOpt = imageRepository.findById(imageId);
            if (imageOpt.isEmpty()) {
                LogUtils.logWarn(SystemLog.Module.IMAGE, "未找到图片ID: " + imageId, getCurrentUsername());
                return ResponseEntity.notFound().build();
            }
            
            ProductImage image = imageOpt.get();
            
            // 从MinIO获取图片数据
            // objectName是实际存储在MinIO中的对象名称
            String objectName = image.getObjectName();
            
            try (InputStream inputStream = minioService.getObject(objectName)) {
                byte[] imageBytes = inputStream.readAllBytes();
                
                // 根据文件扩展名设置Content-Type
                String contentType = getContentTypeFromFileName(objectName);
                
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.parseMediaType(contentType));
                headers.setContentLength(imageBytes.length);
                headers.setCacheControl("max-age=3600"); // 缓存1小时
                // 添加CORS头，允许跨域访问
                headers.add("Access-Control-Allow-Origin", "*");
                headers.add("Access-Control-Allow-Methods", "GET, OPTIONS");
                headers.add("Access-Control-Allow-Headers", "*");
                
                LogUtils.logInfo(SystemLog.Module.IMAGE, "成功获取图片: " + objectName + 
                    ", 大小: " + imageBytes.length + " bytes", getCurrentUsername());
                
                return new ResponseEntity<>(imageBytes, headers, HttpStatus.OK);
                
            } catch (Exception e) {
                log.error("从MinIO获取图片失败 - 对象名: {}, 错误: {}", objectName, e.getMessage(), e);
                LogUtils.logError(SystemLog.Module.IMAGE, "从MinIO获取图片失败 - 图片ID: " + imageId + 
                    ", 对象名: " + objectName + ", 错误: " + e.getMessage(), getCurrentUsername());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
            
        } catch (Exception e) {
            log.error("查看图片失败: {}", e.getMessage(), e);
            LogUtils.logError(SystemLog.Module.IMAGE, "查看图片失败 - 图片ID: " + imageId + ", 错误: " + e.getMessage(), getCurrentUsername());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 根据文件名获取Content-Type
     */
    private String getContentTypeFromFileName(String fileName) {
        if (fileName == null) {
            return "application/octet-stream";
        }
        
        String lowerFileName = fileName.toLowerCase();
        if (lowerFileName.endsWith(".jpg") || lowerFileName.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (lowerFileName.endsWith(".png")) {
            return "image/png";
        } else if (lowerFileName.endsWith(".gif")) {
            return "image/gif";
        } else if (lowerFileName.endsWith(".bmp")) {
            return "image/bmp";
        } else if (lowerFileName.endsWith(".webp")) {
            return "image/webp";
        } else {
            return "image/jpeg"; // 默认为JPEG
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
    @GetMapping("/{imageId}/result")
    public ApiResponse<Map<String, Object>> getClassificationResult(@PathVariable Long imageId) {
        try {
            LogUtils.logInfo(SystemLog.Module.IMAGE, "查询图片ID: " + imageId + " 的分类结果", getCurrentUsername());
            
            // 查询分类结果
            List<ClassificationResult> results = resultRepository.findByImageId(imageId);
            if (results.isEmpty()) {
                LogUtils.logWarn(SystemLog.Module.IMAGE, "未找到图片 " + imageId + " 的分类结果", getCurrentUsername());
                return ApiResponse.error("未找到图片分类结果: " + imageId);
            }

            // 获取最新的分类结果
            ClassificationResult latestResult = results.stream()
                    .max(Comparator.comparing(ClassificationResult::getCreatedAt))
                    .orElseThrow();

            Map<String, Object> resp = new HashMap<>();
            resp.put("imageId", imageId);
            resp.put("resultId", latestResult.getId());
            resp.put("confidence", latestResult.getConfidence());

            // 如果有分类ID，查询分类名称
            if (latestResult.getPredictedCategoryId() != null) {
                Category category = categoryRepository.findById(latestResult.getPredictedCategoryId()).orElseThrow();
                resp.put("categoryId", category.getId());
                // 处理分类名称可能为空的情况
                String categoryName = category.getName();
                if (categoryName == null || categoryName.trim().isEmpty() || "???".equals(categoryName)) {
                    categoryName = "未知分类";
                }
                resp.put("categoryName", categoryName);
                LogUtils.logInfo(SystemLog.Module.IMAGE, "图片 " + imageId + " 分类为: " + categoryName + 
                    " (置信度: " + latestResult.getConfidence() + ")", getCurrentUsername());
            } else {
                resp.put("categoryId", null);
                resp.put("categoryName", "未分类");
                LogUtils.logInfo(SystemLog.Module.IMAGE, "图片 " + imageId + " 未分类", getCurrentUsername());
            }

            return ApiResponse.ok(resp);
        } catch (Exception e) {
            log.error("获取分类结果失败: {}", e.getMessage(), e);
            LogUtils.logError(SystemLog.Module.IMAGE, "分类结果查询失败 - 图片ID: " + imageId + ", 错误: " + e.getMessage(), getCurrentUsername());
            return ApiResponse.error("获取分类结果失败: " + e.getMessage());
        }
    }
}
