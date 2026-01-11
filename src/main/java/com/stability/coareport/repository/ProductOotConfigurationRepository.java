package com.stability.coareport.repository;

import com.stability.coareport.entity.ProductOotConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductOotConfigurationRepository extends JpaRepository<ProductOotConfiguration, Long> {

    List<ProductOotConfiguration> findByProductIdAndIsActiveTrue(Long productId);

    List<ProductOotConfiguration> findByProductId(Long productId);

    Optional<ProductOotConfiguration> findByProductIdAndTestName(Long productId, String testName);

    List<ProductOotConfiguration> findByApprovalStatus(String approvalStatus);

    boolean existsByProductIdAndTestName(Long productId, String testName);
}
