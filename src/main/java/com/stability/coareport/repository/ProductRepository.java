package com.stability.coareport.repository;

import com.stability.coareport.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findByProductCodeAndBatchNo(String productCode, String batchNo);

    Optional<Product> findByProductName(String productName);

    List<Product> findByProductCode(String productCode);

    List<Product> findByStatus(String status);

    List<Product> findByCreatedBy(String createdBy);

    boolean existsByProductCodeAndBatchNo(String productCode, String batchNo);

    boolean existsByProductCodeAndBatchNoAndStorageCondition(String productCode, String batchNo, String storageCondition);

    Optional<Product> findByProductCodeAndBatchNoAndStorageCondition(String productCode, String batchNo, String storageCondition);
}
