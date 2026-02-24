package com.shopimage.repository;

import com.shopimage.entity.Model;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ModelRepository extends JpaRepository<Model, Long>, JpaSpecificationExecutor<Model> {
    
    /**
     * 根据状态查找模型
     */
    List<Model> findByStatus(Model.ModelStatus status);
    
    /**
     * 根据状态统计模型数量
     */
    long countByStatus(Model.ModelStatus status);
    
    /**
     * 根据类型统计模型数量
     */
    long countByType(Model.ModelType type);
    
    /**
     * 根据名称查找模型
     */
    Optional<Model> findByName(String name);
    
    /**
     * 根据名称和版本查找模型
     */
    Optional<Model> findByNameAndVersion(String name, String version);
    
    /**
     * 查找激活的模型
     */
    @Query("SELECT m FROM Model m WHERE m.status = 'ACTIVE' ORDER BY m.createTime DESC")
    List<Model> findActiveModels();
    
    /**
     * 查找第一个激活的模型
     */
    default Optional<Model> findActiveModel() {
        List<Model> activeModels = findActiveModels();
        return activeModels.isEmpty() ? Optional.empty() : Optional.of(activeModels.get(0));
    }
    
    /**
     * 根据状态查找第一个模型
     */
    @Query("SELECT m FROM Model m WHERE m.status = :status ORDER BY m.createTime DESC")
    Model findFirstByStatusOrderByUploadedAtDesc(@Param("status") Model.ModelStatus status);
}
