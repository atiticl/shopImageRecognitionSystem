package com.shopimage.repository;

import com.shopimage.entity.ClassificationTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ClassificationTaskRepository extends JpaRepository<ClassificationTask, Long>, JpaSpecificationExecutor<ClassificationTask> {
}