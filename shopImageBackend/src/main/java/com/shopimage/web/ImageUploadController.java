package com.shopimage.web;

import com.shopimage.category.Category;
import com.shopimage.category.CategoryRepository;
import com.shopimage.common.api.ApiResponse;
import com.shopimage.entity.Model;
import com.shopimage.entity.SystemLog;
import com.shopimage.repository.ModelRepository;
import com.shopimage.service.ai.InferenceService;
import com.shopimage.service.ai.ModelLoader;
import com.shopimage.service.ai.PreprocessService;
import com.shopimage.utils.LogUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
@Slf4j
public class ImageUploadController {
    private final ModelRepository modelRepository;
    private final PreprocessService preprocessService;
    private final ModelLoader modelLoader;
    private final InferenceService inferenceService;
    private final CategoryRepository categoryRepository;

    @PostMapping("/classify")
    public ApiResponse<Map<String, Object>> uploadAndClassify(@RequestParam("file") MultipartFile file) {
        try {
            // 记录图片上传开始
            LogUtils.logInfo(SystemLog.Module.IMAGE, "开始处理图片分类: " + file.getOriginalFilename() + ", 大小: " + file.getSize(), getCurrentUsername());
            
            // 验证文件
            if (file.isEmpty()) {
                LogUtils.logWarn(SystemLog.Module.IMAGE, "用户上传了空文件", getCurrentUsername());
                return ApiResponse.error("请选择要上传的图片");
            }
            
            // 验证文件类型
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                LogUtils.logWarn(SystemLog.Module.IMAGE, "用户上传了非图片文件: " + contentType, getCurrentUsername());
                return ApiResponse.error("只支持图片文件");
            }
            
            // 验证图片格式
            BufferedImage image;
            try {
                image = ImageIO.read(new ByteArrayInputStream(file.getBytes()));
                if (image == null) {
                    LogUtils.logWarn(SystemLog.Module.IMAGE, "无效的图片格式: " + file.getOriginalFilename(), getCurrentUsername());
                    return ApiResponse.error("无效的图片格式");
                }
            } catch (IOException e) {
                LogUtils.logError(SystemLog.Module.IMAGE, "图片读取失败 - 文件: " + file.getOriginalFilename() + ", 错误: " + e.getMessage(), getCurrentUsername());
                return ApiResponse.error("图片读取失败: " + e.getMessage());
            }
            
            // 获取活跃模型
            String modelPath;
            Model model = null;
            try {
                model = modelRepository.findFirstByStatusOrderByUploadedAtDesc(Model.ModelStatus.ACTIVE);
                if (model != null) {
                    modelPath = modelLoader.loadModel(model.getId());
                    LogUtils.logInfo(SystemLog.Module.IMAGE, "使用模型: " + model.getName() + " (ID: " + model.getId() + ")", getCurrentUsername());
                } else {
                    log.warn("No active model found");
                    LogUtils.logError(SystemLog.Module.SYSTEM, "没有可用的激活模型，请先上传并激活一个模型", getCurrentUsername());
                    return ApiResponse.error("没有可用的模型进行分类，请先上传并激活一个模型");
                }
            } catch (Exception e) {
                log.error("Error loading model: {}", e.getMessage());
                LogUtils.logError(SystemLog.Module.SYSTEM, "模型加载失败，错误: " + e.getMessage(), getCurrentUsername());
                return ApiResponse.error("模型加载失败: " + e.getMessage());
            }
            
            // 预处理图片
            float[] inputData = preprocessService.preprocessImageFromBytes(file.getBytes());
            
            // 执行推理
            List<InferenceService.Prediction> predictions = inferenceService.infer(modelPath, inputData, 3);
            
            // 构建响应
            Map<String, Object> response = new HashMap<>();
            response.put("predictions", predictions);
            response.put("modelInfo", model != null ? Map.of(
                "id", model.getId(),
                "name", model.getName(),
                "version", model.getVersion()
            ) : Map.of("name", "resnet50.onnx", "source", "root_directory"));
            response.put("imageInfo", Map.of(
                "filename", file.getOriginalFilename(),
                "size", file.getSize(),
                "contentType", contentType
            ));
            
            // 记录分类成功
            String topPrediction = predictions.isEmpty() ? "无预测结果" : 
                (predictions.get(0).categoryId != null ? getCategoryName(predictions.get(0).categoryId) : "未知类别");
            LogUtils.logImageClassification(getCurrentUsername(), file.getOriginalFilename(), topPrediction, true);
            
            log.info("Successfully classified image: {}", file.getOriginalFilename());
            return ApiResponse.ok(response);
            
        } catch (Exception e) {
            log.error("Image classification failed: {}", e.getMessage(), e);
            LogUtils.logError(SystemLog.Module.IMAGE, "图片分类失败 - 文件: " + file.getOriginalFilename() + ", 错误: " + e.getMessage(), getCurrentUsername());
            return ApiResponse.error("图片分类失败: " + e.getMessage());
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

    /**
     * 根据类别ID获取类别名称
     */
    private String getCategoryName(Long categoryId) {
        try {
            Optional<Category> category = categoryRepository.findById(categoryId);
            return category.map(Category::getName).orElse("未知类别");
        } catch (Exception e) {
            log.debug("获取类别名称失败: {}", e.getMessage());
            return "未知类别";
        }
    }
}