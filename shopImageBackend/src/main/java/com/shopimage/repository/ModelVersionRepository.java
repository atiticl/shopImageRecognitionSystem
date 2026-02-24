package com.shopimage.repository;

import com.shopimage.entity.ModelVersion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ModelVersionRepository extends JpaRepository<ModelVersion, Long> {
    
    /**
     * 根据模型ID查找所有版本，按创建时间倒序
     */
    List<ModelVersion> findByModelIdOrderByCreatedAtDesc(Long modelId);
    
    /**
     * 根据模型ID查找所有版本，按创建时间倒序（分页）
     */
    Page<ModelVersion> findByModelIdOrderByCreatedAtDesc(Long modelId, Pageable pageable);
    
    /**
     * 根据模型ID查找所有版本
     */
    List<ModelVersion> findByModelId(Long modelId);
    
    /**
     * 根据模型ID和是否为当前版本查找
     */
    List<ModelVersion> findByModelIdAndIsCurrent(Long modelId, Boolean isCurrent);
    
    /**
     * 根据模型ID查找当前版本
     */
    @Query("SELECT mv FROM ModelVersion mv WHERE mv.model.id = :modelId AND mv.isCurrent = true")
    Optional<ModelVersion> findCurrentVersionByModelId(@Param("modelId") Long modelId);
    
    /**
     * 根据版本号和模型ID查找版本
     */
    Optional<ModelVersion> findByVersionNumberAndModelId(String versionNumber, Long modelId);
    
    /**
     * 根据模型ID统计版本数量
     */
    long countByModelId(Long modelId);
    
    /**
     * 查找指定模型的最新版本
     */
    @Query("SELECT mv FROM ModelVersion mv WHERE mv.model.id = :modelId ORDER BY mv.createdAt DESC")
    List<ModelVersion> findLatestVersionsByModelId(@Param("modelId") Long modelId);
    
    /**
     * 根据准确率范围查找版本
     */
    @Query("SELECT mv FROM ModelVersion mv WHERE mv.accuracy BETWEEN :minAccuracy AND :maxAccuracy")
    List<ModelVersion> findByAccuracyBetween(@Param("minAccuracy") Double minAccuracy, @Param("maxAccuracy") Double maxAccuracy);
    
    /**
     * 查找有文件的版本
     */
    @Query("SELECT mv FROM ModelVersion mv WHERE mv.fileUrl IS NOT NULL AND mv.fileUrl != ''")
    List<ModelVersion> findVersionsWithFiles();
    
    /**
     * 删除指定模型的所有版本
     */
    void deleteByModelId(Long modelId);
}