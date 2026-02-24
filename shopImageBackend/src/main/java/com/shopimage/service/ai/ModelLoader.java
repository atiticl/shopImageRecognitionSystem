package com.shopimage.service.ai;

import io.minio.MinioClient;
import io.minio.GetObjectArgs;
import com.shopimage.entity.Model;
import com.shopimage.repository.ModelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ModelLoader {
    private final MinioClient minioClient;
    private final ModelRepository modelRepository;
    @Value("${minio.bucket}")
    private String bucket;
    
    private static final String MODEL_CACHE_DIR = "models";
    
    private String extractObjectName(String fileUrl) {
        if (fileUrl == null || fileUrl.trim().isEmpty()) {
            return fileUrl;
        }

        // Expected format: minio://models/resnet50.onnx
        if (fileUrl.startsWith("minio://")) {
            return fileUrl.substring("minio://".length());
        }

        // Handle HTTP URLs (including presigned URLs)
        if (fileUrl.startsWith("http://") || fileUrl.startsWith("https://")) {
            try {
                // Remove URL parameters (?之后的部分)
                String urlWithoutParams = fileUrl.split("\\?")[0];
                
                // Extract object name from URL
                // URL format: http://host:port/bucket/object_name
                String[] parts = urlWithoutParams.split("/");
                if (parts.length >= 5) {
                    // Join parts after bucket to form object name
                    StringBuilder objectName = new StringBuilder();
                    for (int i = 4; i < parts.length; i++) {
                        if (i > 4) {
                            objectName.append("/");
                        }
                        objectName.append(parts[i]);
                    }
                    log.info("Extracted object name from HTTP URL: {} -> {}", fileUrl, objectName.toString());
                    return objectName.toString();
                }
            } catch (Exception e) {
                log.warn("Failed to parse HTTP URL, using original: {}", fileUrl, e);
            }
        }

        // Fallback to original format if URL is not in expected format
        log.warn("Model file URL is not in expected format: {}", fileUrl);
        return fileUrl;
    }

    public String loadModel(Long modelId) throws Exception {
        // 如果modelId为null，表示没有可用的模型
        if (modelId == null) {
            throw new RuntimeException("No model available for classification. Please upload and activate a model first.");
        }
        
        // Create cache directory if not exists
        Path cacheDir = Paths.get(MODEL_CACHE_DIR);
        if (!Files.exists(cacheDir)) {
            Files.createDirectories(cacheDir);
        }
        
        String modelPath = MODEL_CACHE_DIR + "/model_" + modelId;
        File modelFile = new File(modelPath);
        
        // If model already cached, return path
        if (modelFile.exists()) {
            log.info("Model {} already cached at {}", modelId, modelPath);
            return modelPath;
        }
        
        // Download model from MinIO
        // First, check if model exists in database
        try {
            // Get model details from database
            Optional<Model> modelOpt = modelRepository.findById(modelId);
            if (modelOpt.isEmpty()) {
                log.error("Model with id {} not found in database", modelId);
                throw new RuntimeException("Model not found in database");
            }
            Model model = modelOpt.get();
            log.info("Found model in database: {} (version: {})", model.getName(), model.getVersion());
            log.info("Model file URL: {}", model.getFileUrl());

            // Extract object name from model file URL
            String fileUrl = model.getFileUrl();
            String objectName = extractObjectName(fileUrl);
            log.info("Attempting to download model from bucket: {}, objectName: {}", bucket, objectName);

            try (InputStream modelStream = minioClient.getObject(
                    GetObjectArgs.builder()
                        .bucket(bucket)
                        .object(objectName)
                        .build());
                 FileOutputStream fos = new FileOutputStream(modelFile)) {
                
                log.info("Successfully downloaded model from MinIO");
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = modelStream.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                }
            }
        } catch (Exception e) {
            log.error("Failed to download model from MinIO: {}", e.getMessage());
            throw e;
        }
        
        log.info("Model {} downloaded and cached at {}", modelId, modelPath);
        return modelPath;
    }
}
