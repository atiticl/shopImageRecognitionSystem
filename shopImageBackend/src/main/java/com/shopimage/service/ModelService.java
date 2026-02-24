package com.shopimage.service;

import com.shopimage.dto.ModelDTO;
import com.shopimage.dto.ModelVersionDTO;
import com.shopimage.entity.Model;
import com.shopimage.entity.ModelVersion;
import com.shopimage.repository.ModelRepository;
import com.shopimage.repository.ModelVersionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ModelService {
    
    private final ModelRepository modelRepository;
    private final ModelVersionRepository modelVersionRepository;
    private final MinioService minioService;

    /**
     * 分页获取模型列表
     */
    public Page<ModelDTO> getAllModels(Pageable pageable, String status, String type, String keyword) {
        Specification<Model> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            if (status != null && !status.isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("status"), Model.ModelStatus.valueOf(status)));
            }
            
            if (type != null && !type.isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("type"), Model.ModelType.valueOf(type)));
            }
            
            if (keyword != null && !keyword.isEmpty()) {
                Predicate namePredicate = criteriaBuilder.like(root.get("name"), "%" + keyword + "%");
                Predicate descPredicate = criteriaBuilder.like(root.get("description"), "%" + keyword + "%");
                predicates.add(criteriaBuilder.or(namePredicate, descPredicate));
            }
            
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
        
        return modelRepository.findAll(spec, pageable).map(ModelDTO::fromEntity);
    }

    /**
     * 分页查询模型
     */
    public Page<Model> findAll(Pageable pageable) {
        return modelRepository.findAll(pageable);
    }

    /**
     * 分页查询模型（带过滤条件）
     */
    public Page<Model> getModels(int page, int size, String sortBy, String sortDir, String name, String status, String type) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        
        // 构建查询条件
        Specification<Model> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            // 按名称搜索
            if (name != null && !name.trim().isEmpty()) {
                predicates.add(criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("name")), 
                    "%" + name.trim().toLowerCase() + "%"
                ));
            }
            
            // 按状态过滤
            if (status != null && !status.trim().isEmpty()) {
                try {
                    Model.ModelStatus modelStatus = Model.ModelStatus.valueOf(status.trim().toUpperCase());
                    predicates.add(criteriaBuilder.equal(root.get("status"), modelStatus));
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid status value: {}", status);
                }
            }
            
            // 按类型过滤
            if (type != null && !type.trim().isEmpty()) {
                try {
                    Model.ModelType modelType = Model.ModelType.valueOf(type.trim().toUpperCase());
                    predicates.add(criteriaBuilder.equal(root.get("type"), modelType));
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid type value: {}", type);
                }
            }
            
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
        
        return modelRepository.findAll(spec, pageable);
    }

    /**
     * 根据ID获取模型详情
     */
    public ModelDTO getModelById(Long id) {
        Model model = modelRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("模型不存在: " + id));
        return ModelDTO.fromEntity(model);
    }

    /**
     * 创建新模型
     */
    @Transactional
    public ModelDTO createModel(ModelDTO modelDTO) {
        Model model = modelDTO.toEntity();
        model.setCreateTime(LocalDateTime.now());
        model.setUpdateTime(LocalDateTime.now());
        
        if (model.getStatus() == null) {
            model.setStatus(Model.ModelStatus.INACTIVE);
        }
        
        model = modelRepository.save(model);
        return ModelDTO.fromEntity(model);
    }

    /**
     * 更新模型信息
     */
    public Model updateModel(Long id, String name, String version, String type, String framework, String description, Double threshold) {
        Model model = modelRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("模型不存在"));
        
        model.setName(name);
        model.setVersion(version);
        model.setType(Model.ModelType.valueOf(type));
        model.setFramework(framework);
        model.setDescription(description);
        model.setThreshold(BigDecimal.valueOf(threshold));
        model.setUpdateTime(LocalDateTime.now());
        
        return modelRepository.save(model);
    }

    /**
     * 上传模型文件
     */
    @Transactional
    public void uploadModelFile(Long id, MultipartFile file) {
        Model model = modelRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("模型不存在: " + id));
        
        try {
            String objectName = "models/" + model.getName() + "_" + System.currentTimeMillis() + "_" + file.getOriginalFilename();
            String fileUrl = minioService.upload(objectName, file);
            
            model.setFileUrl("minio://" + objectName);
            model.setFileSize(file.getSize());
            model.setUpdateTime(LocalDateTime.now());
            
            modelRepository.save(model);
            
        } catch (Exception e) {
            throw new RuntimeException("文件上传失败: " + e.getMessage(), e);
        }
    }

    /**
     * 激活模型
     */
    @Transactional
    public void activateModel(Long id) {
        Model model = modelRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("模型不存在: " + id));
        
        // 停用其他所有激活的模型
        List<Model> activeModels = modelRepository.findByStatus(Model.ModelStatus.ACTIVE);
        for (Model activeModel : activeModels) {
            activeModel.setStatus(Model.ModelStatus.INACTIVE);
            activeModel.setUpdateTime(LocalDateTime.now());
        }
        modelRepository.saveAll(activeModels);
        
        // 激活当前模型
        model.setStatus(Model.ModelStatus.ACTIVE);
        model.setUpdateTime(LocalDateTime.now());
        modelRepository.save(model);
    }

    /**
     * 停用模型
     */
    @Transactional
    public void deactivateModel(Long id) {
        Model model = modelRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("模型不存在: " + id));
        
        model.setStatus(Model.ModelStatus.INACTIVE);
        model.setUpdateTime(LocalDateTime.now());
        modelRepository.save(model);
    }

    /**
     * 测试模型
     */
    public Map<String, Object> testModel(Long id, MultipartFile image) {
        Model model = modelRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("模型不存在: " + id));
        
        if (model.getStatus() != Model.ModelStatus.ACTIVE) {
            throw new RuntimeException("模型未激活，无法进行测试");
        }
        
        try {
            // 1. 上传测试图片到临时位置
            String testImagePath = "test-images/" + System.currentTimeMillis() + "_" + image.getOriginalFilename();
            String imageUrl = minioService.upload(testImagePath, image);
            
            // 2. 调用训练后端的预测接口
            Map<String, Object> inferenceResult = callTrainBackendPredict(image);
            
            // 3. 构建返回结果
            Map<String, Object> result = new HashMap<>();
            result.put("modelId", id);
            result.put("modelName", model.getName());
            result.put("testImageName", image.getOriginalFilename());
            result.put("testImageUrl", imageUrl);
            result.put("prediction", inferenceResult.get("prediction"));
            result.put("confidence", inferenceResult.get("confidence"));
            result.put("predictions", inferenceResult.get("predictions"));
            result.put("testTime", LocalDateTime.now());
            result.put("inferenceTime", inferenceResult.get("inferenceTime"));
            
            log.info("模型测试完成: 模型={}, 图片={}, 预测结果={}", 
                    model.getName(), image.getOriginalFilename(), inferenceResult.get("prediction"));
            
            return result;
            
        } catch (Exception e) {
            log.error("模型测试失败: 模型={}, 图片={}, 错误={}", 
                    model.getName(), image.getOriginalFilename(), e.getMessage());
            throw new RuntimeException("模型测试失败: " + e.getMessage());
        }
    }
    
    /**
     * 调用训练后端的预测接口
     */
    private Map<String, Object> callTrainBackendPredict(MultipartFile image) {
        try {
            // 训练后端的预测接口地址
            String trainBackendUrl = "http://localhost:5000/api/predict";
            
            // 创建multipart请求
            org.springframework.util.LinkedMultiValueMap<String, Object> parts = 
                new org.springframework.util.LinkedMultiValueMap<>();
            parts.add("file", new org.springframework.core.io.ByteArrayResource(image.getBytes()) {
                @Override
                public String getFilename() {
                    return image.getOriginalFilename();
                }
            });
            
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.MULTIPART_FORM_DATA);
            
            org.springframework.http.HttpEntity<org.springframework.util.LinkedMultiValueMap<String, Object>> requestEntity = 
                new org.springframework.http.HttpEntity<>(parts, headers);
            
            // 调用训练后端API
            org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
            org.springframework.http.ResponseEntity<Map> response = restTemplate.postForEntity(
                trainBackendUrl, requestEntity, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                
                // 解析训练后端的响应格式
                Map<String, Object> result = new HashMap<>();
                result.put("prediction", responseBody.get("predicted_class"));
                result.put("confidence", responseBody.get("confidence"));
                result.put("predictions", responseBody.get("all_predictions"));
                result.put("inferenceTime", responseBody.get("inference_time"));
                
                return result;
            } else {
                throw new RuntimeException("训练后端预测失败: " + response.getStatusCode());
            }
            
        } catch (Exception e) {
            log.error("调用训练后端预测接口失败: {}", e.getMessage());
            throw new RuntimeException("调用训练后端预测接口失败: " + e.getMessage());
        }
    }

    /**
     * 删除模型
     */
    @Transactional
    public void deleteModel(Long id) {
        Model model = modelRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("模型不存在: " + id));
        
        if (model.getStatus() == Model.ModelStatus.ACTIVE) {
            throw new RuntimeException("不能删除正在使用的模型，请先激活其他模型");
        }
        
        // 删除模型文件
        if (model.getFileUrl() != null) {
            deleteModelFile(model.getFileUrl());
        }
        
        // 删除模型记录
        modelRepository.delete(model);
        log.info("模型删除成功: {}", model.getName());
    }

    /**
     * 删除模型文件
     */
    public void deleteModelFile(String filePath) {
        try {
            String objectName = extractObjectName(filePath);
            log.info("提取的对象名称: {}", objectName);
            minioService.deleteFile(objectName);
            log.info("模型文件删除成功: {}", filePath);
        } catch (Exception e) {
            log.error("删除模型文件失败: {}", filePath, e);
            throw new RuntimeException("删除模型文件失败: " + e.getMessage());
        }
    }

    /**
     * 从文件路径中提取对象名称
     */
    private String extractObjectName(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new IllegalArgumentException("文件路径不能为空");
        }

        // 处理 minio:// 格式的URL
        if (filePath.startsWith("minio://")) {
            return filePath.substring("minio://".length());
        }

        // 处理完整的HTTP URL（包含预签名参数）
        if (filePath.startsWith("http://") || filePath.startsWith("https://")) {
            try {
                // 移除URL参数（?之后的部分）
                String urlWithoutParams = filePath.split("\\?")[0];
                
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
                    return objectName.toString();
                }
            } catch (Exception e) {
                log.warn("解析HTTP URL失败，使用fallback方法: {}", filePath);
            }
        }

        // Fallback: 简单地从最后一个/之后提取
        return filePath.substring(filePath.lastIndexOf("/") + 1);
    }

    /**
     * 获取模型版本列表
     */
    public List<ModelVersionDTO> getModelVersions(Long modelId) {
        List<ModelVersion> versions = modelVersionRepository.findByModelIdOrderByCreatedAtDesc(modelId);
        return versions.stream()
                .map(ModelVersionDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 获取模型版本列表（分页）
     */
    public Page<ModelVersionDTO> getModelVersions(Long modelId, int page, int size) {
        Pageable pageable = Pageable.ofSize(size).withPage(page);
        Page<ModelVersion> versions = modelVersionRepository.findByModelIdOrderByCreatedAtDesc(modelId, pageable);
        return versions.map(ModelVersionDTO::fromEntity);
    }

    /**
     * 创建新版本
     */
    @Transactional
    public ModelVersionDTO createModelVersion(Long modelId, ModelVersionDTO versionDTO) {
        Model model = modelRepository.findById(modelId)
                .orElseThrow(() -> new RuntimeException("模型不存在: " + modelId));
        
        ModelVersion version = versionDTO.toEntity();
        version.setModel(model);
        version.setCreatedAt(LocalDateTime.now());
        version.setUpdatedAt(LocalDateTime.now());
        
        // 如果设置为当前版本，则将其他版本设置为非当前
        if (Boolean.TRUE.equals(version.getIsCurrent())) {
            List<ModelVersion> currentVersions = modelVersionRepository.findByModelIdAndIsCurrent(modelId, true);
            for (ModelVersion currentVersion : currentVersions) {
                currentVersion.setIsCurrent(false);
                currentVersion.setUpdatedAt(LocalDateTime.now());
            }
            modelVersionRepository.saveAll(currentVersions);
        }
        
        version = modelVersionRepository.save(version);
        return ModelVersionDTO.fromEntity(version);
    }

    /**
     * 切换到指定版本
     */
    @Transactional
    public void switchToVersion(Long modelId, Long versionId) {
        ModelVersion targetVersion = modelVersionRepository.findById(versionId)
                .orElseThrow(() -> new RuntimeException("版本不存在: " + versionId));
        
        if (!targetVersion.getModel().getId().equals(modelId)) {
            throw new RuntimeException("版本不属于指定模型");
        }
        
        // 将所有版本设置为非当前
        List<ModelVersion> allVersions = modelVersionRepository.findByModelId(modelId);
        for (ModelVersion version : allVersions) {
            version.setIsCurrent(false);
            version.setUpdatedAt(LocalDateTime.now());
        }
        
        // 设置目标版本为当前版本
        targetVersion.setIsCurrent(true);
        targetVersion.setUpdatedAt(LocalDateTime.now());
        
        modelVersionRepository.saveAll(allVersions);
    }
}