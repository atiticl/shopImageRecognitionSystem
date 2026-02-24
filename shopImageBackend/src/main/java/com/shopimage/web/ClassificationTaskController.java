package com.shopimage.web;

import com.shopimage.common.api.ApiResponse;
import com.shopimage.entity.ClassificationTask;
import com.shopimage.entity.ProductImage;
import com.shopimage.entity.Model;
import com.shopimage.service.MessageProducer;
import com.shopimage.repository.ClassificationTaskRepository;
import com.shopimage.repository.ProductImageRepository;
import com.shopimage.repository.ClassificationResultRepository;
import com.shopimage.repository.MqMessageLogRepository;
import com.shopimage.repository.ModelRepository;
import com.shopimage.utils.LogUtils;
import com.shopimage.entity.SystemLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.bind.annotation.*;

import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/classification-tasks")
@RequiredArgsConstructor
@Slf4j
public class ClassificationTaskController {
    
    private final ClassificationTaskRepository taskRepository;
    private final ProductImageRepository imageRepository;
    private final MessageProducer messageProducer;
    private final ClassificationResultRepository resultRepository;
    private final MqMessageLogRepository mqMessageLogRepository;
    private final ModelRepository modelRepository;

    @GetMapping
    public ApiResponse<Map<String, Object>> getTasks(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String taskName,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        
        try {
            Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            
            Specification<ClassificationTask> spec = (root, query, criteriaBuilder) -> {
                List<Predicate> predicates = new ArrayList<>();
                
                if (status != null && !status.trim().isEmpty()) {
                    predicates.add(criteriaBuilder.equal(root.get("status"), status));
                }
                
                if (taskName != null && !taskName.trim().isEmpty()) {
                    predicates.add(criteriaBuilder.like(root.get("taskName"), "%" + taskName + "%"));
                }
                
                if (startDate != null && !startDate.trim().isEmpty()) {
                    predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), LocalDateTime.parse(startDate + "T00:00:00")));
                }
                
                if (endDate != null && !endDate.trim().isEmpty()) {
                    predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), LocalDateTime.parse(endDate + "T23:59:59")));
                }
                
                return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
            };
            
            Page<ClassificationTask> taskPage = taskRepository.findAll(spec, pageable);
            
            Map<String, Object> result = new HashMap<>();
            result.put("list", taskPage.getContent());  // 改为list以匹配前端期望
            result.put("total", taskPage.getTotalElements());  // 改为total以匹配前端期望
            result.put("totalPages", taskPage.getTotalPages());
            result.put("currentPage", page);
            result.put("pageSize", size);
            
            return ApiResponse.ok(result);
        } catch (Exception e) {
            log.error("获取任务列表失败", e);
            return ApiResponse.error("获取任务列表失败: " + e.getMessage());
        }
    }

    @PostMapping
    public ApiResponse<ClassificationTask> createTask(@RequestBody Map<String, Object> request) {
        try {
            String taskName = (String) request.get("taskName");
            String description = (String) request.get("description");
            String modelId = (String) request.get("modelId"); // 获取前端传递的模型ID
            // 修复类型转换问题：前端传递的可能是Integer数组，需要转换为Long
            List<?> rawImageIds = (List<?>) request.get("imageIds");
            List<Long> imageIds = new ArrayList<>();
            if (rawImageIds != null) {
                for (Object id : rawImageIds) {
                    if (id instanceof Integer) {
                        imageIds.add(((Integer) id).longValue());
                    } else if (id instanceof Long) {
                        imageIds.add((Long) id);
                    } else if (id instanceof String) {
                        imageIds.add(Long.parseLong((String) id));
                    }
                }
            }
            
            if (taskName == null || taskName.trim().isEmpty()) {
                return ApiResponse.error("任务名称不能为空");
            }
            
            if (imageIds == null || imageIds.isEmpty()) {
                return ApiResponse.error("请选择要分类的图片");
            }
            
            // 验证图片是否存在
            List<ProductImage> images = imageRepository.findAllById(imageIds);
            if (images.size() != imageIds.size()) {
                return ApiResponse.error("部分图片不存在");
            }
            
            // 获取指定的模型或默认活跃模型
            Model selectedModel = null;
            if (modelId != null && !modelId.trim().isEmpty()) {
                // 前端指定了模型，创建一个临时模型对象来保存模型信息
                selectedModel = new Model();
                selectedModel.setName(modelId.trim());
                selectedModel.setVersion("latest"); // 设置默认版本
                selectedModel.setId(0L); // 设置一个默认ID，表示这是从前端传递的模型名称
                log.info("使用前端指定的模型: {}", modelId);
            } else {
                // 没有指定模型，使用数据库中的活跃模型作为备选
                try {
                    selectedModel = modelRepository.findFirstByStatusOrderByUploadedAtDesc(Model.ModelStatus.ACTIVE);
                } catch (Exception e) {
                    log.warn("Error retrieving active model: {}", e.getMessage());
                    // Fallback to the original method
                    Optional<Model> activeModelOpt = modelRepository.findActiveModel();
                    if (activeModelOpt.isPresent()) {
                        selectedModel = activeModelOpt.get();
                    }
                }
                log.info("使用数据库中的活跃模型: {}", selectedModel != null ? selectedModel.getName() : "无");
            }
            
            // 创建任务
            ClassificationTask.ClassificationTaskBuilder taskBuilder = ClassificationTask.builder()
                    .taskName(taskName.trim())
                    .userId(1L) // 暂时固定为1
                    .totalImages(imageIds.size())
                    .processedImages(0)
                    .status("PENDING")
                    .successCount(0)
                    .failedCount(0)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now());
            
            // 设置描述
            if (description != null && !description.trim().isEmpty()) {
                taskBuilder.description(description.trim());
            }
            
            // 设置模型信息
            if (selectedModel != null) {
                taskBuilder.modelId(selectedModel.getId())
                          .modelName(selectedModel.getName())
                          .modelVersion(selectedModel.getVersion());
            }
            
            ClassificationTask task = taskBuilder.build();
            task = taskRepository.save(task);
            
            // 发送消息到队列
            for (Long imageId : imageIds) {
                Map<String, Object> payload = new HashMap<>();
                payload.put("taskId", task.getId());
                payload.put("imageId", imageId);
                messageProducer.send(payload, task.getId());
            }
            
            // 更新任务状态为处理中
            task.setStatus("PROCESSING");
            task = taskRepository.save(task);
            
            // 记录日志
            LogUtils.logInfo(SystemLog.Module.TASK, "创建分类任务: " + task.getTaskName(), "system");
            
            return ApiResponse.ok(task);
        } catch (Exception e) {
            log.error("创建任务失败", e);
            return ApiResponse.error("创建任务失败: " + e.getMessage());
        }
    }

    @GetMapping("/{taskId}")
    public ApiResponse<ClassificationTask> getTask(@PathVariable Long taskId) {
        try {
            Optional<ClassificationTask> taskOpt = taskRepository.findById(taskId);
            if (taskOpt.isEmpty()) {
                return ApiResponse.error("任务不存在");
            }
            return ApiResponse.ok(taskOpt.get());
        } catch (Exception e) {
            log.error("获取任务详情失败", e);
            return ApiResponse.error("获取任务详情失败: " + e.getMessage());
        }
    }

    @PutMapping("/{taskId}/status")
    public ApiResponse<ClassificationTask> updateTaskStatus(@PathVariable Long taskId, @RequestBody Map<String, String> request) {
        try {
            String status = request.get("status");
            if (status == null || status.trim().isEmpty()) {
                return ApiResponse.error("状态不能为空");
            }
            
            Optional<ClassificationTask> taskOpt = taskRepository.findById(taskId);
            if (taskOpt.isEmpty()) {
                return ApiResponse.error("任务不存在");
            }
            
            ClassificationTask task = taskOpt.get();
            
            // 验证状态转换的合法性
            String currentStatus = task.getStatus();
            if (!isValidStatusTransition(currentStatus, status)) {
                return ApiResponse.error("无效的状态转换: " + currentStatus + " -> " + status);
            }
            
            task.setStatus(status);
            task.setUpdatedAt(LocalDateTime.now());
            task = taskRepository.save(task);
            
            // 记录日志
            LogUtils.logInfo(SystemLog.Module.TASK, "更新任务状态: " + taskId + " -> " + status, "system");
            
            return ApiResponse.ok(task);
        } catch (Exception e) {
            log.error("更新任务状态失败", e);
            return ApiResponse.error("更新任务状态失败: " + e.getMessage());
        }
    }

    @PostMapping("/{taskId}/start")
    public ApiResponse<ClassificationTask> startTask(@PathVariable Long taskId) {
        try {
            Optional<ClassificationTask> taskOpt = taskRepository.findById(taskId);
            if (taskOpt.isEmpty()) {
                return ApiResponse.error("任务不存在");
            }
            
            ClassificationTask task = taskOpt.get();
            String currentStatus = task.getStatus();
            
            // 验证状态转换的合法性
            if (!"PENDING".equals(currentStatus) && !"PAUSED".equals(currentStatus)) {
                return ApiResponse.error("只有待处理或已暂停的任务才能开始");
            }
            
            task.setStatus("PROCESSING");
            task.setUpdatedAt(LocalDateTime.now());
            task = taskRepository.save(task);
            
            // 记录日志
            LogUtils.logInfo(SystemLog.Module.TASK, "开始任务: " + taskId, "system");
            
            return ApiResponse.ok(task);
        } catch (Exception e) {
            log.error("开始任务失败", e);
            return ApiResponse.error("开始任务失败: " + e.getMessage());
        }
    }

    @PostMapping("/{taskId}/pause")
    public ApiResponse<ClassificationTask> pauseTask(@PathVariable Long taskId) {
        try {
            Optional<ClassificationTask> taskOpt = taskRepository.findById(taskId);
            if (taskOpt.isEmpty()) {
                return ApiResponse.error("任务不存在");
            }
            
            ClassificationTask task = taskOpt.get();
            String currentStatus = task.getStatus();
            
            // 验证状态转换的合法性
            if (!"PROCESSING".equals(currentStatus)) {
                return ApiResponse.error("只有正在处理的任务才能暂停");
            }
            
            task.setStatus("PAUSED");
            task.setUpdatedAt(LocalDateTime.now());
            task = taskRepository.save(task);
            
            // 记录日志
            LogUtils.logInfo(SystemLog.Module.TASK, "暂停任务: " + taskId, "system");
            
            return ApiResponse.ok(task);
        } catch (Exception e) {
            log.error("暂停任务失败", e);
            return ApiResponse.error("暂停任务失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/{taskId}")
    public ApiResponse<Void> deleteTask(@PathVariable Long taskId) {
        try {
            Optional<ClassificationTask> taskOpt = taskRepository.findById(taskId);
            if (taskOpt.isEmpty()) {
                return ApiResponse.error("任务不存在");
            }
            
            ClassificationTask task = taskOpt.get();
            if ("PROCESSING".equals(task.getStatus())) {
                return ApiResponse.error("先暂停任务");
            }
            
            // 先删除相关的消息日志记录
            mqMessageLogRepository.deleteByTaskId(taskId);
            
            // 再删除相关的分类结果记录
            resultRepository.deleteByTaskId(taskId);
            
            // 最后删除任务记录
            taskRepository.deleteById(taskId);
            
            // 记录日志
            LogUtils.logInfo(SystemLog.Module.TASK, "删除任务: " + taskId, "system");
            
            return ApiResponse.ok(null);
        } catch (Exception e) {
            log.error("删除任务失败", e);
            return ApiResponse.error("删除任务失败: " + e.getMessage());
        }
    }

    private boolean isValidStatusTransition(String currentStatus, String newStatus) {
        // 定义有效的状态转换规则
        switch (currentStatus) {
            case "PENDING":
                return "PROCESSING".equals(newStatus) || "CANCELLED".equals(newStatus);
            case "PROCESSING":
                return "PAUSED".equals(newStatus) || "COMPLETED".equals(newStatus) || "CANCELLED".equals(newStatus);
            case "PAUSED":
                return "PROCESSING".equals(newStatus) || "CANCELLED".equals(newStatus);
            case "COMPLETED":
                return false; // 已完成的任务不能再改变状态
            case "CANCELLED":
                return false; // 已取消的任务不能再改变状态
            default:
                return false;
        }
    }
}