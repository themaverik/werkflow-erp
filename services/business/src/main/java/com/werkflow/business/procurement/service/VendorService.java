package com.werkflow.business.procurement.service;

import com.werkflow.business.common.context.TenantContext;
import com.werkflow.business.procurement.dto.VendorRequest;
import com.werkflow.business.procurement.dto.VendorResponse;
import com.werkflow.business.procurement.entity.Vendor;
import com.werkflow.business.procurement.repository.VendorRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for Vendor operations.
 * All queries are tenant-scoped via TenantContext.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class VendorService {

    private final VendorRepository vendorRepository;
    private final TenantContext tenantContext;

    private String getTenantId() {
        return tenantContext.getTenantId();
    }

    public Page<VendorResponse> getAllVendors(Pageable pageable) {
        String tenantId = getTenantId();
        log.debug("Fetching all vendors for tenant: {}", tenantId);
        return vendorRepository.findByTenantId(tenantId, pageable)
            .map(this::toResponse);
    }

    public VendorResponse getVendorById(Long id) {
        String tenantId = getTenantId();
        log.debug("Fetching vendor by id: {} for tenant: {}", id, tenantId);
        Vendor vendor = vendorRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Vendor not found with id: " + id));
        if (!vendor.getTenantId().equals(tenantId)) {
            throw new AccessDeniedException("Not authorized to access this Vendor");
        }
        return toResponse(vendor);
    }

    @Transactional
    public VendorResponse createVendor(VendorRequest request) {
        String tenantId = getTenantId();
        log.info("Creating vendor: {} for tenant: {}", request.getName(), tenantId);

        Vendor vendor = Vendor.builder()
            .tenantId(tenantId)
            .name(request.getName())
            .contactPerson(request.getContactPerson())
            .email(request.getEmail())
            .phone(request.getPhone())
            .address(request.getAddress())
            .taxId(request.getTaxId())
            .paymentTerms(request.getPaymentTerms())
            .status(request.getStatus() != null ? request.getStatus() : Vendor.VendorStatus.ACTIVE)
            .rating(request.getRating())
            .notes(request.getNotes())
            .metadata(request.getMetadata())
            .build();

        Vendor saved = vendorRepository.save(vendor);
        log.info("Vendor created with id: {} for tenant: {}", saved.getId(), tenantId);
        return toResponse(saved);
    }

    @Transactional
    public VendorResponse updateVendor(Long id, VendorRequest request) {
        String tenantId = getTenantId();
        log.info("Updating vendor: {} for tenant: {}", id, tenantId);

        Vendor vendor = vendorRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Vendor not found with id: " + id));
        if (!vendor.getTenantId().equals(tenantId)) {
            throw new AccessDeniedException("Not authorized to update this Vendor");
        }

        vendor.setName(request.getName());
        vendor.setContactPerson(request.getContactPerson());
        vendor.setEmail(request.getEmail());
        vendor.setPhone(request.getPhone());
        vendor.setAddress(request.getAddress());
        vendor.setTaxId(request.getTaxId());
        vendor.setPaymentTerms(request.getPaymentTerms());
        if (request.getStatus() != null) {
            vendor.setStatus(request.getStatus());
        }
        vendor.setRating(request.getRating());
        vendor.setNotes(request.getNotes());
        vendor.setMetadata(request.getMetadata());

        Vendor updated = vendorRepository.save(vendor);
        log.info("Vendor updated: {} for tenant: {}", id, tenantId);
        return toResponse(updated);
    }

    @Transactional
    public void deleteVendor(Long id) {
        String tenantId = getTenantId();
        log.info("Deleting vendor: {} for tenant: {}", id, tenantId);

        Vendor vendor = vendorRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Vendor not found with id: " + id));
        if (!vendor.getTenantId().equals(tenantId)) {
            throw new AccessDeniedException("Not authorized to delete this Vendor");
        }

        vendorRepository.delete(vendor);
        log.info("Vendor deleted: {} for tenant: {}", id, tenantId);
    }

    private VendorResponse toResponse(Vendor vendor) {
        return VendorResponse.builder()
            .id(vendor.getId())
            .name(vendor.getName())
            .contactPerson(vendor.getContactPerson())
            .email(vendor.getEmail())
            .phone(vendor.getPhone())
            .address(vendor.getAddress())
            .taxId(vendor.getTaxId())
            .paymentTerms(vendor.getPaymentTerms())
            .status(vendor.getStatus())
            .rating(vendor.getRating())
            .notes(vendor.getNotes())
            .metadata(vendor.getMetadata())
            .createdAt(vendor.getCreatedAt())
            .updatedAt(vendor.getUpdatedAt())
            .build();
    }
}
