package com.stability.coareport.controller;

import com.stability.coareport.dto.PageResponse;
import com.stability.coareport.dto.ProductDto;
import com.stability.coareport.dto.ProductRegistrationRequest;
import com.stability.coareport.service.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
public class ProductManagementController {

    private static final Logger logger = LoggerFactory.getLogger(ProductManagementController.class);

    @Autowired
    private ProductService productService;

    @PostMapping("/register")
    public ResponseEntity<?> registerProduct(
            @RequestBody ProductRegistrationRequest request,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            logger.info("Product registration request from user: {}", username);

            ProductDto product = productService.registerProduct(request, username);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Product registered successfully");
            response.put("product", product);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error registering product", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @GetMapping
    public ResponseEntity<PageResponse<ProductDto>> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection) {
        try {
            PageResponse<ProductDto> products = productService.getAllProductsPaginated(page, size, sortBy, sortDirection);
            return ResponseEntity.ok(products);
        } catch (Exception e) {
            logger.error("Error fetching products", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getProductById(@PathVariable Long id) {
        try {
            ProductDto product = productService.getProductById(id);
            return ResponseEntity.ok(product);
        } catch (Exception e) {
            logger.error("Error fetching product", e);
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    @GetMapping("/code/{productCode}")
    public ResponseEntity<List<ProductDto>> getProductsByCode(@PathVariable String productCode) {
        try {
            List<ProductDto> products = productService.getProductsByCode(productCode);
            return ResponseEntity.ok(products);
        } catch (Exception e) {
            logger.error("Error fetching products by code", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<ProductDto>> getProductsByStatus(@PathVariable String status) {
        try {
            List<ProductDto> products = productService.getProductsByStatus(status);
            return ResponseEntity.ok(products);
        } catch (Exception e) {
            logger.error("Error fetching products by status", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateProduct(
            @PathVariable Long id,
            @RequestBody ProductRegistrationRequest request,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            logger.info("Product update request for ID {} from user: {}", id, username);

            ProductDto product = productService.updateProduct(id, request, username);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Product updated successfully");
            response.put("product", product);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error updating product", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateProductStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> statusUpdate) {
        try {
            String status = statusUpdate.get("status");
            productService.updateProductStatus(id, status);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Product status updated successfully");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error updating product status", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProduct(@PathVariable Long id) {
        try {
            productService.deleteProduct(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Product deleted successfully");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error deleting product", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }
}
