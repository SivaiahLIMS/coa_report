package com.stability.coareport.controller;

import com.stability.coareport.dto.CompanyRegistrationRequest;
import com.stability.coareport.entity.Branch;
import com.stability.coareport.entity.Company;
import com.stability.coareport.repository.BranchRepository;
import com.stability.coareport.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/companies")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CompanyController {

    private final CompanyRepository companyRepository;
    private final BranchRepository branchRepository;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'BRANCH_ADMIN', 'MANAGER', 'USER')")
    public ResponseEntity<List<Company>> getAllCompanies() {
        List<Company> companies = companyRepository.findByActiveTrue();
        return ResponseEntity.ok(companies);
    }

    @GetMapping("/{companyId}/branches")
    @PreAuthorize("hasAnyRole('ADMIN', 'BRANCH_ADMIN', 'MANAGER', 'USER')")
    public ResponseEntity<List<Branch>> getBranchesByCompany(@PathVariable Long companyId) {
        List<Branch> branches = branchRepository.findByCompanyIdAndActiveTrue(companyId);
        return ResponseEntity.ok(branches);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> registerCompany(@RequestBody CompanyRegistrationRequest request) {
        try {
            if (companyRepository.findByName(request.getName()).isPresent()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Company name already exists"));
            }

            Company company = new Company();
            company.setName(request.getName());
            company.setCode(request.getCode());
            company.setAddress(request.getAddress());
            company.setContactEmail(request.getContactEmail());
            company.setContactPhone(request.getContactPhone());
            company.setActive(true);

            Company savedCompany = companyRepository.save(company);
            return ResponseEntity.ok(savedCompany);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'BRANCH_ADMIN')")
    public ResponseEntity<?> getCompanyById(@PathVariable Long id) {
        return companyRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateCompany(@PathVariable Long id, @RequestBody CompanyRegistrationRequest request) {
        try {
            Company company = companyRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Company not found"));

            if (request.getName() != null) company.setName(request.getName());
            if (request.getCode() != null) company.setCode(request.getCode());
            if (request.getAddress() != null) company.setAddress(request.getAddress());
            if (request.getContactEmail() != null) company.setContactEmail(request.getContactEmail());
            if (request.getContactPhone() != null) company.setContactPhone(request.getContactPhone());

            Company updatedCompany = companyRepository.save(company);
            return ResponseEntity.ok(updatedCompany);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteCompany(@PathVariable Long id) {
        try {
            Company company = companyRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Company not found"));
            company.setActive(false);
            companyRepository.save(company);
            return ResponseEntity.ok(Map.of("message", "Company deactivated successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
