package com.shopimage.web;

import com.shopimage.common.api.ApiResponse;
import com.shopimage.entity.Model;
import com.shopimage.entity.ProductImage;
import com.shopimage.repository.ModelRepository;
import com.shopimage.repository.ProductImageRepository;
import com.shopimage.service.ai.InferenceService;
import com.shopimage.service.ai.ModelLoader;
import com.shopimage.service.ai.PreprocessService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/classify")
@RequiredArgsConstructor
@Slf4j
public class ClassifyController {
    private final ProductImageRepository imageRepository;
    private final ModelRepository modelRepository;
    private final PreprocessService preprocessService;
    private final ModelLoader modelLoader;
    private final InferenceService inferenceService;

    @GetMapping("/one")
    public ApiResponse<Map<String, Object>> classifyOne(@RequestParam("imageId") Long imageId) {
        try {
            // Get image info
            ProductImage image = imageRepository.findById(imageId).orElseThrow();
            
            // Get active model
            Model model;
            try {
                model = modelRepository.findFirstByStatusOrderByUploadedAtDesc(Model.ModelStatus.ACTIVE);
                if (model == null) {
                    return ApiResponse.error("No active model found");
                }
            } catch (Exception e) {
                log.error("Error retrieving active model: {}", e.getMessage());
                // Fallback to the original method with manual limiting
                Optional<Model> activeModelOpt = modelRepository.findActiveModel();
                if (activeModelOpt.isEmpty()) {
                    return ApiResponse.error("No active model found");
                }
                model = activeModelOpt.get();
            }
            
            // Load model
            String modelPath = modelLoader.loadModel(model.getId());
            
            // Extract object name from file URL
//            String objectName = extractObjectName(image.getFileUrl());
            
            // Preprocess image
            float[] inputData = preprocessService.preprocessImage(image.getBucket(), image.getObjectName());
            
            // Run inference
            List<InferenceService.Prediction> predictions = inferenceService.infer(modelPath, inputData, 5);
            
            // Build response
            Map<String, Object> result = new HashMap<>();
            result.put("imageId", imageId);
            result.put("modelId", model.getId());
            result.put("modelName", model.getName());
            result.put("threshold", model.getThreshold());
            result.put("predictions", predictions);
            result.put("meetsThreshold", !predictions.isEmpty() && 
                predictions.get(0).confidence >= model.getThreshold().doubleValue());
            
            return ApiResponse.ok(result);
            
        } catch (Exception e) {
            log.error("Error classifying image {}: {}", imageId, e.getMessage(), e);
            return ApiResponse.error("Classification failed: " + e.getMessage());
        }
    }
    
    private String extractObjectName(String fileUrl) {
        String[] parts = fileUrl.split("/");
        if (parts.length >= 2) {
            return parts[parts.length - 1];
        }
        return fileUrl;
    }
}
