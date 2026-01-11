package com.stability.coareport.service;

import com.stability.coareport.dto.CertificateOfAnalysisDto;
import com.stability.coareport.entity.CertificateOfAnalysis;
import com.stability.coareport.repository.CertificateOfAnalysisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CertificateOfAnalysisService {

    private final CertificateOfAnalysisRepository coaRepository;

    @Transactional
    public CertificateOfAnalysisDto createCertificate(CertificateOfAnalysisDto dto) {
        CertificateOfAnalysis coa = convertToEntity(dto);

        // Trim all string fields
        trimStringFields(coa);

        CertificateOfAnalysis saved = coaRepository.save(coa);
        return convertToDto(saved);
    }

    @Transactional
    public CertificateOfAnalysisDto updateCertificate(Long id, CertificateOfAnalysisDto dto) {
        CertificateOfAnalysis existing = coaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Certificate not found with id: " + id));

        updateEntityFromDto(existing, dto);

        // Trim all string fields
        trimStringFields(existing);

        CertificateOfAnalysis updated = coaRepository.save(existing);
        return convertToDto(updated);
    }

    @Transactional(readOnly = true)
    public CertificateOfAnalysisDto getCertificateById(Long id) {
        CertificateOfAnalysis coa = coaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Certificate not found with id: " + id));
        return convertToDto(coa);
    }

    @Transactional(readOnly = true)
    public List<CertificateOfAnalysisDto> getAllCertificates() {
        return coaRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CertificateOfAnalysisDto> getCertificatesByProductName(String productName) {
        return coaRepository.findByProductNameOrderByCreatedAtDesc(productName).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CertificateOfAnalysisDto> getCertificatesByBatchNo(String batchNo) {
        return coaRepository.findByBatchNoOrderByCreatedAtDesc(batchNo).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteCertificate(Long id) {
        if (!coaRepository.existsById(id)) {
            throw new RuntimeException("Certificate not found with id: " + id);
        }
        coaRepository.deleteById(id);
    }

    private CertificateOfAnalysis convertToEntity(CertificateOfAnalysisDto dto) {
        CertificateOfAnalysis coa = new CertificateOfAnalysis();
        coa.setId(dto.getId());
        coa.setProductName(dto.getProductName());
        coa.setProductCode(dto.getProductCode());
        coa.setSpecificationId(dto.getSpecificationId());
        coa.setStorageCondition(dto.getStorageCondition());
        coa.setSampleOrientation(dto.getSampleOrientation());
        coa.setBatchNo(dto.getBatchNo());
        coa.setBatchSize(dto.getBatchSize());
        coa.setMfgDate(dto.getMfgDate());
        coa.setSchedulePeriod(dto.getSchedulePeriod());
        coa.setPackingType(dto.getPackingType());
        coa.setArNo(dto.getArNo());
        coa.setProtocolId(dto.getProtocolId());
        coa.setExpDate(dto.getExpDate());
        coa.setScheduleDate(dto.getScheduleDate());
        coa.setPackSize(dto.getPackSize());
        coa.setStpNumber(dto.getStpNumber());
        coa.setCompanyName(dto.getCompanyName());
        coa.setBranchName(dto.getBranchName());
        coa.setHdpeCapDetails(dto.getHdpeCapDetails());
        coa.setLdpeNozzleDetails(dto.getLdpeNozzleDetails());
        coa.setLdpeBottleDetails(dto.getLdpeBottleDetails());
        return coa;
    }

    private void updateEntityFromDto(CertificateOfAnalysis coa, CertificateOfAnalysisDto dto) {
        coa.setProductName(dto.getProductName());
        coa.setProductCode(dto.getProductCode());
        coa.setSpecificationId(dto.getSpecificationId());
        coa.setStorageCondition(dto.getStorageCondition());
        coa.setSampleOrientation(dto.getSampleOrientation());
        coa.setBatchNo(dto.getBatchNo());
        coa.setBatchSize(dto.getBatchSize());
        coa.setMfgDate(dto.getMfgDate());
        coa.setSchedulePeriod(dto.getSchedulePeriod());
        coa.setPackingType(dto.getPackingType());
        coa.setArNo(dto.getArNo());
        coa.setProtocolId(dto.getProtocolId());
        coa.setExpDate(dto.getExpDate());
        coa.setScheduleDate(dto.getScheduleDate());
        coa.setPackSize(dto.getPackSize());
        coa.setStpNumber(dto.getStpNumber());
        coa.setCompanyName(dto.getCompanyName());
        coa.setBranchName(dto.getBranchName());
        coa.setHdpeCapDetails(dto.getHdpeCapDetails());
        coa.setLdpeNozzleDetails(dto.getLdpeNozzleDetails());
        coa.setLdpeBottleDetails(dto.getLdpeBottleDetails());
    }

    private CertificateOfAnalysisDto convertToDto(CertificateOfAnalysis coa) {
        CertificateOfAnalysisDto dto = new CertificateOfAnalysisDto();
        dto.setId(coa.getId());
        dto.setProductName(coa.getProductName());
        dto.setProductCode(coa.getProductCode());
        dto.setSpecificationId(coa.getSpecificationId());
        dto.setStorageCondition(coa.getStorageCondition());
        dto.setSampleOrientation(coa.getSampleOrientation());
        dto.setBatchNo(coa.getBatchNo());
        dto.setBatchSize(coa.getBatchSize());
        dto.setMfgDate(coa.getMfgDate());
        dto.setSchedulePeriod(coa.getSchedulePeriod());
        dto.setPackingType(coa.getPackingType());
        dto.setArNo(coa.getArNo());
        dto.setProtocolId(coa.getProtocolId());
        dto.setExpDate(coa.getExpDate());
        dto.setScheduleDate(coa.getScheduleDate());
        dto.setPackSize(coa.getPackSize());
        dto.setStpNumber(coa.getStpNumber());
        dto.setCompanyName(coa.getCompanyName());
        dto.setBranchName(coa.getBranchName());
        dto.setHdpeCapDetails(coa.getHdpeCapDetails());
        dto.setLdpeNozzleDetails(coa.getLdpeNozzleDetails());
        dto.setLdpeBottleDetails(coa.getLdpeBottleDetails());
        dto.setCreatedAt(coa.getCreatedAt());
        dto.setUpdatedAt(coa.getUpdatedAt());
        dto.setCreatedBy(coa.getCreatedBy());
        dto.setUpdatedBy(coa.getUpdatedBy());
        return dto;
    }

    private void trimStringFields(CertificateOfAnalysis coa) {
        if (coa.getProductName() != null) coa.setProductName(coa.getProductName().trim());
        if (coa.getProductCode() != null) coa.setProductCode(coa.getProductCode().trim());
        if (coa.getSpecificationId() != null) coa.setSpecificationId(coa.getSpecificationId().trim());
        if (coa.getStorageCondition() != null) coa.setStorageCondition(coa.getStorageCondition().trim());
        if (coa.getSampleOrientation() != null) coa.setSampleOrientation(coa.getSampleOrientation().trim());
        if (coa.getBatchNo() != null) coa.setBatchNo(coa.getBatchNo().trim());
        if (coa.getBatchSize() != null) coa.setBatchSize(coa.getBatchSize().trim());
        if (coa.getSchedulePeriod() != null) coa.setSchedulePeriod(coa.getSchedulePeriod().trim());
        if (coa.getPackingType() != null) coa.setPackingType(coa.getPackingType().trim());
        if (coa.getArNo() != null) coa.setArNo(coa.getArNo().trim());
        if (coa.getProtocolId() != null) coa.setProtocolId(coa.getProtocolId().trim());
        if (coa.getPackSize() != null) coa.setPackSize(coa.getPackSize().trim());
        if (coa.getStpNumber() != null) coa.setStpNumber(coa.getStpNumber().trim());
        if (coa.getCompanyName() != null) coa.setCompanyName(coa.getCompanyName().trim());
        if (coa.getBranchName() != null) coa.setBranchName(coa.getBranchName().trim());
        if (coa.getHdpeCapDetails() != null) coa.setHdpeCapDetails(coa.getHdpeCapDetails().trim());
        if (coa.getLdpeNozzleDetails() != null) coa.setLdpeNozzleDetails(coa.getLdpeNozzleDetails().trim());
        if (coa.getLdpeBottleDetails() != null) coa.setLdpeBottleDetails(coa.getLdpeBottleDetails().trim());
    }
}
