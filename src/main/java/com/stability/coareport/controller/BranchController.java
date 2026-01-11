package com.stability.coareport.controller;

import com.stability.coareport.dto.BranchRegistrationRequest;
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
@RequestMapping("/api/branches")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class BranchController {

    private final BranchRepository branchRepository;
    private final CompanyRepository companyRepository;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'BRANCH_ADMIN')")
    public ResponseEntity<List<Branch>> getAllBranches() {
        List<Branch> branches = branchRepository.findByActiveTrue();
        return ResponseEntity.ok(branches);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> registerBranch(@RequestBody BranchRegistrationRequest request) {
        try {
            Company company = companyRepository.findById(request.getCompanyId())
                    .orElseThrow(() -> new RuntimeException("Company not found"));

            Branch branch = new Branch();
            branch.setName(request.getName());
            branch.setCode(request.getCode());
            branch.setLocation(request.getLocation());
            branch.setContactEmail(request.getContactEmail());
            branch.setContactPhone(request.getContactPhone());
            branch.setCompany(company);
            branch.setActive(true);

            Branch savedBranch = branchRepository.save(branch);
            return ResponseEntity.ok(savedBranch);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'BRANCH_ADMIN')")
    public ResponseEntity<?> getBranchById(@PathVariable Long id) {
        return branchRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'BRANCH_ADMIN')")
    public ResponseEntity<?> updateBranch(@PathVariable Long id, @RequestBody BranchRegistrationRequest request) {
        try {
            Branch branch = branchRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Branch not found"));

            if (request.getName() != null) branch.setName(request.getName());
            if (request.getCode() != null) branch.setCode(request.getCode());
            if (request.getLocation() != null) branch.setLocation(request.getLocation());
            if (request.getContactEmail() != null) branch.setContactEmail(request.getContactEmail());
            if (request.getContactPhone() != null) branch.setContactPhone(request.getContactPhone());

            Branch updatedBranch = branchRepository.save(branch);
            return ResponseEntity.ok(updatedBranch);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteBranch(@PathVariable Long id) {
        try {
            Branch branch = branchRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Branch not found"));
            branch.setActive(false);
            branchRepository.save(branch);
            return ResponseEntity.ok(Map.of("message", "Branch deactivated successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
