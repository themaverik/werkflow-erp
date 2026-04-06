package com.werkflow.business.procurement.service;

import com.werkflow.business.common.context.TenantContext;
import com.werkflow.business.procurement.dto.PrLineItemRequest;
import com.werkflow.business.procurement.dto.PrLineItemResponse;
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
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for PrLineItem operations.
 * All queries are tenant-scoped via TenantContext.
 * The parent PurchaseRequest is validated to belong to the same tenant.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PrLineItemService {

    private final PrLineItemRepository lineItemRepository;
    private final PurchaseRequestRepository purchaseRequestRepository;
    private final TenantContext tenantContext;

    private String getTenantId() {
        return tenantContext.getTenantId();
    }

    public List<PrLineItemResponse> getAllLineItems() {
        String tenantId = getTenantId();
        log.debug("Fetching all PR line items for tenant: {}", tenantId);
        return lineItemRepository.findByTenantId(tenantId).stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    public PrLineItemResponse getLineItemById(Long id) {
        String tenantId = getTenantId();
        log.debug("Fetching PR line item by id: {} for tenant: {}", id, tenantId);
        PrLineItem item = lineItemRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("PrLineItem not found with id: " + id));
        if (!item.getTenantId().equals(tenantId)) {
            throw new AccessDeniedException("Not authorized to access this PrLineItem");
        }
        return toResponse(item);
    }

    public List<PrLineItemResponse> getLineItemsByPurchaseRequestId(Long purchaseRequestId) {
        String tenantId = getTenantId();
        log.debug("Fetching PR line items for PR: {} in tenant: {}", purchaseRequestId, tenantId);
        // Validate PR belongs to tenant
        purchaseRequestRepository.findById(purchaseRequestId)
            .filter(pr -> pr.getTenantId().equals(tenantId))
            .orElseThrow(() -> new EntityNotFoundException(
                "Purchase request not found with id: " + purchaseRequestId));
        return lineItemRepository.findByPurchaseRequestIdAndTenantId(purchaseRequestId, tenantId).stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    @Transactional
    public PrLineItemResponse createLineItem(Long purchaseRequestId, PrLineItemRequest request) {
        String tenantId = getTenantId();
        log.info("Creating PR line item for PR: {} in tenant: {}", purchaseRequestId, tenantId);

        // Validate parent PR belongs to same tenant
        PurchaseRequest pr = purchaseRequestRepository.findById(purchaseRequestId)
            .orElseThrow(() -> new EntityNotFoundException(
                "Purchase request not found with id: " + purchaseRequestId));
        if (!pr.getTenantId().equals(tenantId)) {
            throw new AccessDeniedException("PurchaseRequest does not belong to the current tenant");
        }

        BigDecimal unitPrice = request.getEstimatedUnitPrice() != null
            ? request.getEstimatedUnitPrice() : BigDecimal.ZERO;
        BigDecimal totalPrice = unitPrice.multiply(BigDecimal.valueOf(request.getQuantity()));

        PrLineItem item = PrLineItem.builder()
            .tenantId(tenantId)
            .purchaseRequest(pr)
            .lineNumber(request.getLineNumber())
            .description(request.getDescription())
            .itemDescription(request.getDescription())
            .quantity(request.getQuantity())
            .unitOfMeasure(request.getUnitOfMeasure())
            .estimatedUnitPrice(unitPrice)
            .estimatedTotalAmount(totalPrice)
            .totalPrice(totalPrice)
            .budgetCategoryId(request.getBudgetCategoryId())
            .specifications(request.getSpecifications())
            .build();

        PrLineItem saved = lineItemRepository.save(item);
        log.info("PR line item created with id: {} for tenant: {}", saved.getId(), tenantId);
        return toResponse(saved);
    }

    @Transactional
    public PrLineItemResponse updateLineItem(Long id, PrLineItemRequest request) {
        String tenantId = getTenantId();
        log.info("Updating PR line item: {} for tenant: {}", id, tenantId);

        PrLineItem item = lineItemRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("PrLineItem not found with id: " + id));
        if (!item.getTenantId().equals(tenantId)) {
            throw new AccessDeniedException("Not authorized to update this PrLineItem");
        }

        BigDecimal unitPrice = request.getEstimatedUnitPrice() != null
            ? request.getEstimatedUnitPrice() : BigDecimal.ZERO;
        BigDecimal totalPrice = unitPrice.multiply(BigDecimal.valueOf(request.getQuantity()));

        item.setLineNumber(request.getLineNumber());
        item.setDescription(request.getDescription());
        item.setItemDescription(request.getDescription());
        item.setQuantity(request.getQuantity());
        item.setUnitOfMeasure(request.getUnitOfMeasure());
        item.setEstimatedUnitPrice(unitPrice);
        item.setEstimatedTotalAmount(totalPrice);
        item.setTotalPrice(totalPrice);
        item.setBudgetCategoryId(request.getBudgetCategoryId());
        item.setSpecifications(request.getSpecifications());

        PrLineItem updated = lineItemRepository.save(item);
        log.info("PR line item updated: {} for tenant: {}", id, tenantId);
        return toResponse(updated);
    }

    @Transactional
    public void deleteLineItem(Long id) {
        String tenantId = getTenantId();
        log.info("Deleting PR line item: {} for tenant: {}", id, tenantId);

        PrLineItem item = lineItemRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("PrLineItem not found with id: " + id));
        if (!item.getTenantId().equals(tenantId)) {
            throw new AccessDeniedException("Not authorized to delete this PrLineItem");
        }

        lineItemRepository.delete(item);
        log.info("PR line item deleted: {} for tenant: {}", id, tenantId);
    }

    private PrLineItemResponse toResponse(PrLineItem item) {
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
