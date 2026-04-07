package com.werkflow.business.procurement.service;

import com.werkflow.business.common.context.TenantContext;
import com.werkflow.business.common.validator.CrossDomainValidator;
import com.werkflow.business.procurement.dto.PrLineItemRequest;
import com.werkflow.business.procurement.dto.PrLineItemResponse;
import com.werkflow.business.procurement.dto.PurchaseRequestRequest;
import com.werkflow.business.procurement.dto.PurchaseRequestResponse;
import com.werkflow.business.procurement.entity.PrLineItem;
import com.werkflow.business.procurement.entity.PurchaseRequest;
import com.werkflow.business.procurement.repository.PrLineItemRepository;
import com.werkflow.business.procurement.repository.PurchaseRequestRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for PurchaseRequest operations.
 * All queries are tenant-scoped via TenantContext.
 *
 * <p>Cross-domain FK validation is performed for {@code requestingDeptId} via
 * {@link com.werkflow.business.common.validator.CrossDomainValidator}.
 * Validation is tenant-scoped: the department must exist and belong to the current tenant.
 * {@code requesterUserId} and vendor/budget IDs on line items are not yet validated.
 *
 * <p>Tenant isolation IS enforced: read, update, and delete operations verify
 * that the requested PurchaseRequest belongs to the current tenant.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PurchaseRequestService {

    private final PurchaseRequestRepository prRepository;
    private final PrLineItemRepository lineItemRepository;
    private final TenantContext tenantContext;
    private final CrossDomainValidator validator;

    private String getTenantId() {
        return tenantContext.getTenantId();
    }

    public List<PurchaseRequestResponse> getAllPurchaseRequests() {
        String tenantId = getTenantId();
        log.debug("Fetching all purchase requests for tenant: {}", tenantId);
        return prRepository.findByTenantId(tenantId).stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    public PurchaseRequestResponse getPurchaseRequestById(Long id) {
        String tenantId = getTenantId();
        log.debug("Fetching purchase request by id: {} for tenant: {}", id, tenantId);
        PurchaseRequest pr = prRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Purchase request not found with id: " + id));
        if (!pr.getTenantId().equals(tenantId)) {
            throw new AccessDeniedException("Not authorized to access this PurchaseRequest");
        }
        return toResponse(pr);
    }

    @Transactional
    public PurchaseRequestResponse createPurchaseRequest(PurchaseRequestRequest request) {
        String tenantId = getTenantId();
        log.info("Creating purchase request for tenant: {}", tenantId);

        validator.validateDepartmentExists(request.getRequestingDeptId(), tenantId);

        PurchaseRequest pr = PurchaseRequest.builder()
            .tenantId(tenantId)
            .prNumber("PR-" + System.currentTimeMillis())
            .requestingDeptId(request.getRequestingDeptId())
            .requesterUserId(request.getRequesterUserId())
            .requestDate(request.getRequestDate())
            .requiredByDate(request.getRequiredByDate())
            .priority(request.getPriority() != null ? request.getPriority() : PurchaseRequest.Priority.MEDIUM)
            .totalAmount(BigDecimal.ZERO)
            .status(PurchaseRequest.PrStatus.DRAFT)
            .justification(request.getJustification())
            .notes(request.getNotes())
            .processInstanceId(request.getProcessInstanceId())
            .build();

        PurchaseRequest saved = prRepository.save(pr);

        if (request.getLineItems() != null && !request.getLineItems().isEmpty()) {
            for (PrLineItemRequest itemRequest : request.getLineItems()) {
                PrLineItem lineItem = buildLineItem(itemRequest, saved, tenantId);
                lineItemRepository.save(lineItem);
            }
        }

        log.info("Purchase request created with id: {} for tenant: {}", saved.getId(), tenantId);
        return toResponse(saved);
    }

    @Transactional
    public PurchaseRequestResponse updatePurchaseRequest(Long id, PurchaseRequestRequest request) {
        String tenantId = getTenantId();
        log.info("Updating purchase request: {} for tenant: {}", id, tenantId);

        PurchaseRequest pr = prRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Purchase request not found with id: " + id));
        if (!pr.getTenantId().equals(tenantId)) {
            throw new AccessDeniedException("Not authorized to update this PurchaseRequest");
        }

        if (request.getRequestingDeptId() != null && !request.getRequestingDeptId().equals(pr.getRequestingDeptId())) {
            validator.validateDepartmentExists(request.getRequestingDeptId(), tenantId);
        }

        pr.setRequestingDeptId(request.getRequestingDeptId());
        pr.setRequesterUserId(request.getRequesterUserId());
        pr.setRequestDate(request.getRequestDate());
        pr.setRequiredByDate(request.getRequiredByDate());
        if (request.getPriority() != null) {
            pr.setPriority(request.getPriority());
        }
        pr.setJustification(request.getJustification());
        pr.setNotes(request.getNotes());

        PurchaseRequest updated = prRepository.save(pr);
        log.info("Purchase request updated: {} for tenant: {}", id, tenantId);
        return toResponse(updated);
    }

    @Transactional
    public void deletePurchaseRequest(Long id) {
        String tenantId = getTenantId();
        log.info("Deleting purchase request: {} for tenant: {}", id, tenantId);

        PurchaseRequest pr = prRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Purchase request not found with id: " + id));
        if (!pr.getTenantId().equals(tenantId)) {
            throw new AccessDeniedException("Not authorized to delete this PurchaseRequest");
        }

        prRepository.delete(pr);
        log.info("Purchase request deleted: {} for tenant: {}", id, tenantId);
    }

    public List<PrLineItemResponse> getLineItemsByPurchaseRequestId(Long purchaseRequestId) {
        String tenantId = getTenantId();
        log.debug("Fetching line items for PR: {} in tenant: {}", purchaseRequestId, tenantId);
        // Validate PR belongs to tenant before returning its line items
        prRepository.findById(purchaseRequestId)
            .filter(pr -> pr.getTenantId().equals(tenantId))
            .orElseThrow(() -> new EntityNotFoundException("Purchase request not found with id: " + purchaseRequestId));
        return lineItemRepository.findByPurchaseRequestIdAndTenantId(purchaseRequestId, tenantId).stream()
            .map(this::lineItemToResponse)
            .collect(Collectors.toList());
    }

    private PrLineItem buildLineItem(PrLineItemRequest req, PurchaseRequest pr, String tenantId) {
        BigDecimal unitPrice = req.getEstimatedUnitPrice() != null ? req.getEstimatedUnitPrice() : BigDecimal.ZERO;
        BigDecimal totalPrice = unitPrice.multiply(BigDecimal.valueOf(req.getQuantity()));
        return PrLineItem.builder()
            .tenantId(tenantId)
            .purchaseRequest(pr)
            .lineNumber(req.getLineNumber())
            .description(req.getDescription())
            .itemDescription(req.getDescription())
            .quantity(req.getQuantity())
            .unitOfMeasure(req.getUnitOfMeasure())
            .estimatedUnitPrice(unitPrice)
            .estimatedTotalAmount(totalPrice)
            .totalPrice(totalPrice)
            .budgetCategoryId(req.getBudgetCategoryId())
            .specifications(req.getSpecifications())
            .build();
    }

    private PurchaseRequestResponse toResponse(PurchaseRequest pr) {
        String tenantId = getTenantId();
        List<PrLineItemResponse> lineItems = lineItemRepository
            .findByPurchaseRequestIdAndTenantId(pr.getId(), tenantId).stream()
            .map(this::lineItemToResponse)
            .collect(Collectors.toList());

        return PurchaseRequestResponse.builder()
            .id(pr.getId())
            .prNumber(pr.getPrNumber())
            .requestingDeptId(pr.getRequestingDeptId())
            .requesterUserId(pr.getRequesterUserId())
            .requestDate(pr.getRequestDate())
            .requiredByDate(pr.getRequiredByDate())
            .priority(pr.getPriority())
            .totalAmount(pr.getTotalAmount())
            .status(pr.getStatus())
            .approvedByUserId(pr.getApprovedByUserId())
            .approvedDate(pr.getApprovedDate())
            .processInstanceId(pr.getProcessInstanceId())
            .rejectionReason(pr.getRejectionReason())
            .justification(pr.getJustification())
            .notes(pr.getNotes())
            .lineItems(lineItems)
            .createdAt(pr.getCreatedAt())
            .updatedAt(pr.getUpdatedAt())
            .build();
    }

    private PrLineItemResponse lineItemToResponse(PrLineItem item) {
        return PrLineItemResponse.builder()
            .id(item.getId())
            .purchaseRequestId(item.getPurchaseRequest().getId())
            .lineNumber(item.getLineNumber())
            .description(item.getDescription())
            .quantity(item.getQuantity())
            .unitOfMeasure(item.getUnitOfMeasure())
            .estimatedUnitPrice(item.getEstimatedUnitPrice())
            .totalPrice(item.getTotalPrice())
            .budgetCategoryId(item.getBudgetCategoryId())
            .specifications(item.getSpecifications())
            .createdAt(item.getCreatedAt())
            .updatedAt(item.getUpdatedAt())
            .build();
    }
}
