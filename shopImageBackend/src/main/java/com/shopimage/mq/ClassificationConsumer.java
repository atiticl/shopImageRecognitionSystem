package com.shopimage.mq;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopimage.entity.ClassificationResult;
import com.shopimage.entity.ClassificationTask;
import com.shopimage.entity.Model;
import com.shopimage.entity.ProductImage;
import com.shopimage.repository.ClassificationResultRepository;
import com.shopimage.repository.ClassificationTaskRepository;
import com.shopimage.repository.ModelRepository;
import com.shopimage.repository.ProductImageRepository;
import com.shopimage.service.ai.InferenceService;
import com.shopimage.service.ai.ModelLoader;
import com.shopimage.service.ai.PreprocessService;
import com.shopimage.service.MinioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class ClassificationConsumer {
    private final ClassificationResultRepository resultRepository;
    private final ClassificationTaskRepository taskRepository;
    private final ProductImageRepository imageRepository;
    private final ModelRepository modelRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final MinioService minioService;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Value("${python.api.url:http://localhost:5000}")
    private String pythonApiUrl;
    
    // 预测结果类
    public static class PredictionResult {
        private boolean success;
        private Long predictedCategoryId;
        private double maxConfidence;
        
        public PredictionResult(boolean success, Long predictedCategoryId, double maxConfidence) {
            this.success = success;
            this.predictedCategoryId = predictedCategoryId;
            this.maxConfidence = maxConfidence;
        }
        
        public boolean isSuccess() { return success; }
        public Long getPredictedCategoryId() { return predictedCategoryId; }
        public double getMaxConfidence() { return maxConfidence; }
    }

    @RabbitListener(queues = "cls.queue")
    public void onMessage(String body) throws Exception {
        Map<String, Object> payload = objectMapper.readValue(body, Map.class);
        Long taskId = Long.valueOf(payload.get("taskId").toString());
        Long imageId = Long.valueOf(payload.get("imageId").toString());

        log.info("Processing image {} for task {}", imageId, taskId);

        try {
            // 获取图片信息
            ProductImage image = imageRepository.findById(imageId).orElseThrow();
            
            // 获取任务信息，从任务中获取指定的模型
            ClassificationTask task = taskRepository.findById(taskId).orElseThrow();
            String taskModelName = task.getModelName(); // 获取任务指定的模型名称
            
            log.info("Task {} specifies model: {}", taskId, taskModelName);

            // 调用Python预测API
            String bucket = image.getBucket();
            String objectName = image.getObjectName();

            // Validate object name
            if (objectName == null || objectName.isEmpty()) {
                log.error("Invalid object name extracted for image {}", imageId);
                return;
            }

            // 调用Python预测API
            Long predictedCategoryId = null;
            BigDecimal confidence = null;
            
            try {
                // 从MinIO下载图片到临时文件
                String tempImagePath = downloadImageFromMinio(bucket, objectName);
                
                // 调用Python预测API，传递任务指定的模型名称
                PredictionResult predictionResult = callPythonPredictAPI(tempImagePath, taskModelName);
                
                if (predictionResult != null && predictionResult.isSuccess()) {
                    // 使用默认阈值，因为我们不再依赖数据库中的模型记录
                    double threshold = 0.3; // 默认阈值
                    
                    // 检查是否有满足阈值的预测结果
                    if (predictionResult.getMaxConfidence() >= threshold) {
                        predictedCategoryId = predictionResult.getPredictedCategoryId();
                        confidence = BigDecimal.valueOf(predictionResult.getMaxConfidence());
                    }
                }
                
                // 清理临时文件
                cleanupTempFile(tempImagePath);
                
            } catch (Exception e) {
                log.error("Error calling Python predict API for image {}: {}", imageId, e.getMessage(), e);
            }

            // Save result
            // 判断预测是否成功：有预测结果且置信度达到阈值
            boolean isSuccess = predictedCategoryId != null && confidence != null && confidence.doubleValue() >= 0.3;
            resultRepository.save(ClassificationResult.builder()
                    .imageId(imageId)
                    .taskId(taskId)
                    .predictedCategoryId(predictedCategoryId)
                    .confidence(confidence)
                    .createdAt(LocalDateTime.now())
                    .build());

            // Update task progress and statistics
            int processed = task.getProcessedImages() == null ? 0 : task.getProcessedImages();
            int successCount = task.getSuccessCount() == null ? 0 : task.getSuccessCount();
            int failedCount = task.getFailedCount() == null ? 0 : task.getFailedCount();
            
            // 累计置信度用于计算平均准确率
            BigDecimal totalConfidence = task.getAccuracy() != null ? 
                task.getAccuracy().multiply(BigDecimal.valueOf(processed)) : BigDecimal.ZERO;
            
            // 更新统计信息：成功表示有高置信度的预测结果，失败表示无预测结果或置信度过低
            if (isSuccess) {
                successCount++;
                // 累加实际的置信度值
                totalConfidence = totalConfidence.add(confidence.multiply(BigDecimal.valueOf(100)));
                log.info("Image {} classified successfully with confidence {}", imageId, confidence);
            } else {
                failedCount++;
                // 失败的情况下置信度为0
                totalConfidence = totalConfidence.add(BigDecimal.ZERO);
                log.info("Image {} classification failed - predictedCategoryId: {}, confidence: {}", 
                        imageId, predictedCategoryId, confidence);
            }
            
            task.setProcessedImages(processed + 1);
            task.setSuccessCount(successCount);
            task.setFailedCount(failedCount);
            
            // 实时计算并更新平均准确率（基于实际confidence值）
            if (task.getProcessedImages() > 0) {
                double avgAccuracy = totalConfidence.doubleValue() / task.getProcessedImages();
                task.setAccuracy(BigDecimal.valueOf(avgAccuracy).setScale(2, BigDecimal.ROUND_HALF_UP));
                log.info("Task {} current average accuracy: {}%", taskId, task.getAccuracy());
            }
            
            // 如果任务完成，设置状态和处理耗时
            if (task.getProcessedImages() >= task.getTotalImages()) {
                task.setStatus("COMPLETED");
                log.info("Task {} completed with final accuracy: {}%", taskId, task.getAccuracy());
                
                // 计算处理耗时
                if (task.getCreatedAt() != null) {
                    LocalDateTime now = LocalDateTime.now();
                    long seconds = java.time.Duration.between(task.getCreatedAt(), now).getSeconds();
                    long minutes = seconds / 60;
                    long remainingSeconds = seconds % 60;
                    task.setProcessingTime(String.format("%d分%d秒", minutes, remainingSeconds));
                }
            }
            
            task.setUpdatedAt(LocalDateTime.now());
            taskRepository.save(task);

            // Send progress update with real-time accuracy
            messagingTemplate.convertAndSend("/topic/progress", Map.of(
                    "taskId", taskId,
                    "processed", task.getProcessedImages(),
                    "total", task.getTotalImages(),
                    "status", task.getStatus(),
                    "accuracy", task.getAccuracy() != null ? task.getAccuracy().doubleValue() : 0.0,
                    "successCount", task.getSuccessCount(),
                    "failedCount", task.getFailedCount()));

            log.info("Completed processing image {} for task {}", imageId, taskId);

        } catch (Exception e) {
            log.error("Error processing image {} for task {}: {}", imageId, taskId, e.getMessage(), e);
        }
    }
    
    /**
     * 从MinIO下载图片到临时文件
     */
    private String downloadImageFromMinio(String bucket, String objectName) throws Exception {
        // 创建临时目录
        Path tempDir = Paths.get("temp_images");
        if (!Files.exists(tempDir)) {
            Files.createDirectories(tempDir);
        }
        
        // 生成临时文件名
        String fileName = "temp_" + System.currentTimeMillis() + "_" + objectName.substring(objectName.lastIndexOf("/") + 1);
        Path tempFile = tempDir.resolve(fileName);
        
        // 从MinIO下载文件
        try (InputStream inputStream = minioService.getObject(objectName);
             FileOutputStream outputStream = new FileOutputStream(tempFile.toFile())) {
            
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }
        
        return tempFile.toString();
    }
    
    /**
     * 调用Python预测API
     */
    private PredictionResult callPythonPredictAPI(String imagePath, String modelName) {
        try {
            String url = pythonApiUrl + "/api/predict";
            
            // 准备multipart请求
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new org.springframework.core.io.FileSystemResource(imagePath));
            
            // 如果指定了模型名称，添加到请求参数中
            if (modelName != null && !modelName.trim().isEmpty()) {
                body.add("model_name", modelName.trim());
                log.info("Calling Python API with model: {}", modelName);
            } else {
                log.info("Calling Python API without specific model (will use latest)");
            }
            
            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            
            // 发送请求
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                // 解析响应
                ObjectMapper mapper = new ObjectMapper();
                JsonNode jsonNode = mapper.readTree(response.getBody());
                
                log.info("Python API response: {}", response.getBody());
                
                // 检查是否有result字段，然后查找其中的predictions
                JsonNode resultNode = jsonNode.get("result");
                if (resultNode != null && resultNode.has("predictions") && 
                    resultNode.get("predictions").isArray() && resultNode.get("predictions").size() > 0) {
                    
                    JsonNode firstPrediction = resultNode.get("predictions").get(0);
                    
                    if (firstPrediction.has("confidence") && firstPrediction.has("class_name")) {
                        double confidence = firstPrediction.get("confidence").asDouble();
                        String className = firstPrediction.get("class_name").asText();
                        
                        log.info("Parsed prediction - class: {}, confidence: {}", className, confidence);
                        
                        // 根据类别名称映射到categoryId
                        Long categoryId = mapClassNameToCategoryId(className);
                        
                        return new PredictionResult(true, categoryId, confidence);
                    }
                } else {
                    log.warn("No valid predictions found in response: {}", response.getBody());
                }
            }
            
            log.warn("Python API returned no valid predictions for image: {}", imagePath);
            return new PredictionResult(false, null, 0.0);
            
        } catch (Exception e) {
            log.error("Error calling Python predict API: {}", e.getMessage(), e);
            return new PredictionResult(false, null, 0.0);
        }
    }
    
    /**
     * 将类别名称映射到categoryId
     */
    private Long mapClassNameToCategoryId(String className) {
        // 根据数据库中的实际类别映射关系来实现
        // 数据库类别: 1-食品类, 2-糖果类, 3-文具类, 4-洗漱用品, 5-饮料类, 7-其他类
        if (className == null || className.trim().isEmpty()) {
            return 7L; // 默认为其他类
        }
        
        String lowerClassName = className.toLowerCase().trim();
        
        // 食品类相关
        if (lowerClassName.contains("食品") || lowerClassName.contains("food") || 
            lowerClassName.contains("面包") || lowerClassName.contains("饼干") ||
            lowerClassName.contains("零食") || lowerClassName.contains("snack")) {
            return 1L;
        }
        
        // 糖果类相关
        if (lowerClassName.contains("糖果") || lowerClassName.contains("candy") ||
            lowerClassName.contains("巧克力") || lowerClassName.contains("chocolate") ||
            lowerClassName.contains("糖") || lowerClassName.contains("sweet")) {
            return 2L;
        }
        
        // 文具类相关
        if (lowerClassName.contains("文具") || lowerClassName.contains("stationery") ||
            lowerClassName.contains("笔") || lowerClassName.contains("pen") ||
            lowerClassName.contains("纸") || lowerClassName.contains("paper") ||
            lowerClassName.contains("本") || lowerClassName.contains("notebook")) {
            return 3L;
        }
        
        // 洗漱用品相关
        if (lowerClassName.contains("洗漱") || lowerClassName.contains("toiletries") ||
            lowerClassName.contains("牙膏") || lowerClassName.contains("toothpaste") ||
            lowerClassName.contains("洗发") || lowerClassName.contains("shampoo") ||
            lowerClassName.contains("肥皂") || lowerClassName.contains("soap")) {
            return 4L;
        }
        
        // 饮料类相关
        if (lowerClassName.contains("饮料") || lowerClassName.contains("beverage") ||
            lowerClassName.contains("水") || lowerClassName.contains("water") ||
            lowerClassName.contains("茶") || lowerClassName.contains("tea") ||
            lowerClassName.contains("咖啡") || lowerClassName.contains("coffee") ||
            lowerClassName.contains("juice") || lowerClassName.contains("果汁")) {
            return 5L;
        }
        
        // 默认为其他类
        return 7L;
    }
    
    /**
     * 清理临时文件
     */
    private void cleanupTempFile(String filePath) {
        try {
            if (filePath != null) {
                Files.deleteIfExists(Paths.get(filePath));
            }
        } catch (Exception e) {
            log.warn("Failed to cleanup temp file {}: {}", filePath, e.getMessage());
        }
    }

    private String extractObjectName(String fileUrl) {
        // Extract object name from MinIO URL
        // Format: http://host:port/bucket/object_name
        log.info("Extracting object name from URL: {}", fileUrl);
        String[] parts = fileUrl.split("/");
        if (parts.length >= 5) {
            // URL format: http://host:port/bucket/object_name
            String objectName = parts[parts.length - 1];
            log.info("Extracted object name: {}", objectName);
            return objectName;
        } else if (parts.length >= 2) {
            // Fallback to original logic
            String objectName = parts[parts.length - 1];
            log.warn("URL format not as expected, but extracted object name: {}", objectName);
            return objectName;
        }
        log.error("Failed to extract object name from URL: {}", fileUrl);
        return fileUrl;
    }
}
