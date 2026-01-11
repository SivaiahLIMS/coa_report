package com.stability.coareport.service;

import com.stability.coareport.dto.PageResponse;
import com.stability.coareport.dto.ProductDto;
import com.stability.coareport.dto.ProductRegistrationRequest;
import com.stability.coareport.entity.Product;
import com.stability.coareport.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private static final Logger logger = LoggerFactory.getLogger(ProductService.class);

    @Autowired
    private ProductRepository productRepository;

    @Transactional
    public ProductDto registerProduct(ProductRegistrationRequest request, String username) {
        logger.info("Registering product: {} - Batch: {} - Storage: {}",
                request.getProductCode(), request.getBatchNo(), request.getStorageCondition());

        if (request.getStorageCondition() == null || request.getStorageCondition().trim().isEmpty()) {
            throw new RuntimeException("Storage condition is required");
        }

        if (productRepository.existsByProductCodeAndBatchNoAndStorageCondition(
                request.getProductCode(), request.getBatchNo(), request.getStorageCondition())) {
            throw new RuntimeException("Product with this code, batch number, and storage condition already exists");
        }

        if (request.getMfgDate() != null && request.getExpDate() != null) {
            if (request.getMfgDate().isAfter(request.getExpDate()) ||
                    request.getMfgDate().isEqual(request.getExpDate())) {
                throw new RuntimeException("Manufacturing date must be before expiry date");
            }
        }

        Product product = new Product();
        product.setProductName(request.getProductName());
        product.setProductCode(request.getProductCode());
        product.setBatchNo(request.getBatchNo());
        product.setArNo(request.getArNo());
        product.setDrugProductMfgLocation(request.getDrugProductMfgLocation());
        product.setDrugSubstanceMfgLocation(request.getDrugSubstanceMfgLocation());
        product.setDrugSubstanceBatchNo(request.getDrugSubstanceBatchNo());
        product.setSpecificationId(request.getSpecificationId());
        product.setBatchSize(request.getBatchSize());
        product.setProtocolId(request.getProtocolId());
        product.setStpNo(request.getStpNo());
        product.setMfgDate(request.getMfgDate());
        product.setExpDate(request.getExpDate());
        product.setStorageCondition(request.getStorageCondition());
        product.setScheduleDate(request.getScheduleDate());
        product.setSampleOrientation(request.getSampleOrientation());
        product.setPackageType(request.getPackageType());
        product.setPackSize(request.getPackSize());
        product.setHdpeCapDetails(request.getHdpeCapDetails());
        product.setLdpeNozzleDetails(request.getLdpeNozzleDetails());
        product.setLdpeBottleDetails(request.getLdpeBottleDetails());
        product.setMarket(request.getMarket());
        product.setStatus("Active");
        product.setCreatedBy(username);

        Product saved = productRepository.save(product);
        logger.info("Product registered successfully with ID: {}", saved.getId());

        return mapToDto(saved);
    }

    public List<ProductDto> getAllProducts() {
        return productRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public PageResponse<ProductDto> getAllProductsPaginated(int page, int size, String sortBy, String sortDirection) {
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDirection)
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;

        PageRequest pageRequest;
        if (sortBy != null && !sortBy.isEmpty()) {
            pageRequest = PageRequest.of(page, size, Sort.by(direction, sortBy));
        } else {
            pageRequest = PageRequest.of(page, size, Sort.by(direction, "id"));
        }

        Page<Product> productPage = productRepository.findAll(pageRequest);
        Page<ProductDto> dtoPage = productPage.map(this::mapToDto);
        return PageResponse.fromSpringPage(dtoPage);
    }

    public List<ProductDto> getProductsByStatus(String status) {
        return productRepository.findByStatus(status).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public ProductDto getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
        return mapToDto(product);
    }

    public List<ProductDto> getProductsByCode(String productCode) {
        return productRepository.findByProductCode(productCode).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public ProductDto updateProduct(Long id, ProductRegistrationRequest request, String username) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));

        if (request.getMfgDate() != null && request.getExpDate() != null) {
            if (request.getMfgDate().isAfter(request.getExpDate()) ||
                    request.getMfgDate().isEqual(request.getExpDate())) {
                throw new RuntimeException("Manufacturing date must be before expiry date");
            }
        }

        if (request.getProductName() != null && !request.getProductName().equals(product.getProductName())) {
            throw new RuntimeException("Product name cannot be changed. Current name: " + product.getProductName());
        }
        product.setArNo(request.getArNo());
        product.setDrugProductMfgLocation(request.getDrugProductMfgLocation());
        product.setDrugSubstanceMfgLocation(request.getDrugSubstanceMfgLocation());
        product.setDrugSubstanceBatchNo(request.getDrugSubstanceBatchNo());
        product.setSpecificationId(request.getSpecificationId());
        product.setBatchSize(request.getBatchSize());
        product.setProtocolId(request.getProtocolId());
        product.setStpNo(request.getStpNo());
        product.setMfgDate(request.getMfgDate());
        product.setExpDate(request.getExpDate());
        product.setStorageCondition(request.getStorageCondition());
        product.setScheduleDate(request.getScheduleDate());
        product.setSampleOrientation(request.getSampleOrientation());
        product.setPackageType(request.getPackageType());
        product.setPackSize(request.getPackSize());
        product.setHdpeCapDetails(request.getHdpeCapDetails());
        product.setLdpeNozzleDetails(request.getLdpeNozzleDetails());
        product.setLdpeBottleDetails(request.getLdpeBottleDetails());
        product.setMarket(request.getMarket());

        Product updated = productRepository.save(product);
        logger.info("Product updated successfully: {}", id);

        return mapToDto(updated);
    }

    @Transactional
    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new RuntimeException("Product not found with id: " + id);
        }
        productRepository.deleteById(id);
        logger.info("Product deleted: {}", id);
    }

    @Transactional
    public void updateProductStatus(Long id, String status) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
        product.setStatus(status);
        productRepository.save(product);
        logger.info("Product status updated to {}: {}", status, id);
    }

    private ProductDto mapToDto(Product product) {
        ProductDto dto = new ProductDto();
        dto.setId(product.getId());
        dto.setProductName(product.getProductName());
        dto.setProductCode(product.getProductCode());
        dto.setBatchNo(product.getBatchNo());
        dto.setArNo(product.getArNo());
        dto.setDrugProductMfgLocation(product.getDrugProductMfgLocation());
        dto.setDrugSubstanceMfgLocation(product.getDrugSubstanceMfgLocation());
        dto.setDrugSubstanceBatchNo(product.getDrugSubstanceBatchNo());
        dto.setSpecificationId(product.getSpecificationId());
        dto.setBatchSize(product.getBatchSize());
        dto.setProtocolId(product.getProtocolId());
        dto.setStpNo(product.getStpNo());
        dto.setMfgDate(product.getMfgDate());
        dto.setExpDate(product.getExpDate());
        dto.setStorageCondition(product.getStorageCondition());
        dto.setScheduleDate(product.getScheduleDate());
        dto.setSampleOrientation(product.getSampleOrientation());
        dto.setPackageType(product.getPackageType());
        dto.setPackSize(product.getPackSize());
        dto.setHdpeCapDetails(product.getHdpeCapDetails());
        dto.setLdpeNozzleDetails(product.getLdpeNozzleDetails());
        dto.setLdpeBottleDetails(product.getLdpeBottleDetails());
        dto.setMarket(product.getMarket());
        dto.setStatus(product.getStatus());
        dto.setCreatedBy(product.getCreatedBy());
        dto.setCreatedAt(product.getCreatedAt());
        dto.setUpdatedAt(product.getUpdatedAt());
        return dto;
    }
}
