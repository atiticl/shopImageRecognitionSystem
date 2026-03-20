package com.shopimage.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopimage.entity.ClassificationTask;
import com.shopimage.entity.ProductImage;
import com.shopimage.repository.ClassificationTaskRepository;
import com.shopimage.repository.ProductImageRepository;
import com.shopimage.service.MessageProducer;
import com.shopimage.service.MinioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Component
@RequiredArgsConstructor
@Slf4j
public class BatchProcessConsumer {
    private final ClassificationTaskRepository taskRepository;
    private final ProductImageRepository imageRepository;
    private final MinioService minioService;
    private final MessageProducer messageProducer;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @RabbitListener(queues = "batch.queue")
    public void processBatchMessage(String body) throws Exception {
        Map<String, Object> payload = objectMapper.readValue(body, Map.class);
        Long taskId = Long.valueOf(payload.get("taskId").toString());
        String zipObjectName = payload.get("zipObjectName").toString();

        log.info("开始处理批量任务 {} 的压缩包: {}", taskId, zipObjectName);

        try {
            ClassificationTask task = taskRepository.findById(taskId).orElseThrow();
            task.setStatus("PROCESSING");
            taskRepository.save(task);

            // 从MinIO下载压缩包
            InputStream zipStream = minioService.getObject(zipObjectName);
            
            // 解压并处理图片
            List<ProductImage> extractedImages = extractAndUploadImages(zipStream, taskId);
            
            // 更新任务的总图片数
            task.setTotalImages(extractedImages.size());
            task.setUpdatedAt(LocalDateTime.now());
            taskRepository.save(task);

            // 为每张图片发送分类消息
            for (ProductImage image : extractedImages) {
                Map<String, Object> classifyMessage = new HashMap<>();
                classifyMessage.put("taskId", taskId);
                classifyMessage.put("imageId", image.getId());
                messageProducer.send(classifyMessage, taskId);
            }

            log.info("批量任务 {} 解压完成，共提取 {} 张图片", taskId, extractedImages.size());

        } catch (Exception e) {
            log.error("处理批量任务 {} 失败: {}", taskId, e.getMessage(), e);
            
            // 更新任务状态为失败
            ClassificationTask task = taskRepository.findById(taskId).orElse(null);
            if (task != null) {
                task.setStatus("FAILED");
                task.setUpdatedAt(LocalDateTime.now());
                taskRepository.save(task);
            }
        }
    }

    private List<ProductImage> extractAndUploadImages(InputStream zipStream, Long taskId) throws Exception {
        List<ProductImage> images = new ArrayList<>();
        
        try (ZipInputStream zis = new ZipInputStream(zipStream)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.isDirectory() && isImageFile(entry.getName())) {
                    // 读取图片数据
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        baos.write(buffer, 0, len);
                    }
                    
                    // 生成新的文件名
                    String originalName = entry.getName();
                    String normalizedName = originalName.replace("\\", "/");
                    String shortName = normalizedName.substring(normalizedName.lastIndexOf("/") + 1);
                    String fileName = "task-" + taskId + "/" + System.currentTimeMillis() + "_" + 
                            shortName;
                    String objectName = "test-images/" + fileName;
                    
                    // 上传到MinIO
                    ByteArrayInputStream imageStream = new ByteArrayInputStream(baos.toByteArray());
                    String fileUrl = minioService.upload(objectName, imageStream, baos.size(), getContentType(originalName));
                    
                    // 保存图片记录
                    ProductImage image = ProductImage.builder()
                            .objectName(objectName)
                            .fileUrl(fileUrl)
                            .userId(1L)
                            .bucket(minioService.getBucket())
                            .status("UPLOADED")
                            .uploadedAt(LocalDateTime.now())
                            .build();
                    
                    images.add(imageRepository.save(image));
                    log.debug("提取并上传图片: {} -> {}", originalName, fileUrl);
                }
                zis.closeEntry();
            }
        }
        
        return images;
    }

    private boolean isImageFile(String fileName) {
        String lowerName = fileName.toLowerCase();
        return lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") || 
               lowerName.endsWith(".png") || lowerName.endsWith(".gif") ||
               lowerName.endsWith(".bmp") || lowerName.endsWith(".webp");
    }

    private String getContentType(String fileName) {
        String lowerName = fileName.toLowerCase();
        if (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (lowerName.endsWith(".png")) {
            return "image/png";
        } else if (lowerName.endsWith(".gif")) {
            return "image/gif";
        } else if (lowerName.endsWith(".bmp")) {
            return "image/bmp";
        } else if (lowerName.endsWith(".webp")) {
            return "image/webp";
        }
        return "application/octet-stream";
    }
}
