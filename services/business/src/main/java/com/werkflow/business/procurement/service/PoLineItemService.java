package com.werkflow.business.procurement.service;

import com.werkflow.business.common.context.TenantContext;
import com.werkflow.business.procurement.dto.PoLineItemRequest;
import com.werkflow.business.procurement.dto.PoLineItemResponse;
import com.werkflow.business.procurement.entity.PoLineItem;
import com.werkflow.business.procurement.entity.PurchaseOrder;
import com.werkflow.business.procurement.repository.PoLineItemRepository;
import com.werkflow.business.procurement.repository.PurchaseOrderRepository;
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
 * Service for PoLineItem operations.
 * All queries are tenant-scoped via TenantContext.
 * The parent PurchaseOrder is validated to belong to the same tenant.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PoLineItemService {

    private final PoLineItemRepository lineItemRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final TenantContext tenantContext;

    private String getTenantId() {
        return tenantContext.getTenantId();
    }

    public List<PoLineItemResponse> getAllLineItems() {
        String tenantId = getTenantId();
        log.debug("Fetching all PO line items for tenant: {}", tenantId);
        return lineItemRepository.findByTenantId(tenantId).stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    public PoLineItemResponse getLineItemById(Long id) {
        String tenantId = getTenantId();
        log.debug("Fetching PO line item by id: {} for tenant: {}", id, tenantId);
        PoLineItem item = lineItemRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("PoLineItem not found with id: " + id));
        if (!item.getTenantId().equals(tenantId)) {
            throw new AccessDeniedException("Not authorized to access this PoLineItem");
        }
        return toResponse(item);
    }

    public List<PoLineItemResponse> getLineItemsByPurchaseOrderId(Long purchaseOrderId) {
        String tenantId = getTenantId();
        log.debug("Fetching PO line items for PO: {} in tenant: {}", purchaseOrderId, tenantId);
        // Validate PO belongs to tenant
        purchaseOrderRepository.findById(purchaseOrderId)
            .filter(po -> po.getTenantId().equals(tenantId))
            .orElseThrow(() -> new EntityNotFoundException(
                "Purchase order not found with id: " + purchaseOrderId));
        return lineItemRepository.findByPurchaseOrderIdAndTenantId(purchaseOrderId, tenantId).stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    @Transactional
    public PoLineItemResponse createLineItem(Long purchaseOrderId, PoLineItemRequest request) {
        String tenantId = getTenantId();
        log.info("Creating PO line item for PO: {} in tenant: {}", purchaseOrderId, tenantId);

        // Validate parent PO belongs to same tenant
        PurchaseOrder po = purchaseOrderRepository.findById(purchaseOrderId)
            .orElseThrow(() -> new EntityNotFoundException(
                "Purchase order not found with id: " + purchaseOrderId));
        if (!po.getTenantId().equals(tenantId)) {
            throw new AccessDeniedException("PurchaseOrder does not belong to the current tenant");
        }

        BigDecimal unitPrice = request.getUnitPrice() != null ? request.getUnitPrice() : BigDecimal.ZERO;
        int qty = request.getOrderedQuantity() != null ? request.getOrderedQuantity() : 0;
        BigDecimal totalPrice = unitPrice.multiply(BigDecimal.valueOf(qty));

        PoLineItem item = PoLineItem.builder()
            .tenantId(tenantId)
            .purchaseOrder(po)
            .lineNumber(request.getLineNumber())
            .description(request.getDescription())
            .itemDescription(request.getDescription())
            .orderedQuantity(qty)
            .quantity(qty)
            .unitOfMeasure(request.getUnitOfMeasure())
            .unitPrice(unitPrice)
            .totalAmount(totalPrice)
            .totalPrice(totalPrice)
            .specifications(request.getSpecifications())
            .build();

        PoLineItem saved = lineItemRepository.save(item);
        log.info("PO line item created with id: {} for tenant: {}", saved.getId(), tenantId);
        return toResponse(saved);
    }

    @Transactional
    public PoLineItemResponse updateLineItem(Long id, PoLineItemRequest request) {
        String tenantId = getTenantId();
        log.info("Updating PO line item: {} for tenant: {}", id, tenantId);

        PoLineItem item = lineItemRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("PoLineItem not found with id: " + id));
        if (!item.getTenantId().equals(tenantId)) {
            throw new AccessDeniedException("Not authorized to update this PoLineItem");
        }

        BigDecimal unitPrice = request.getUnitPrice() != null ? request.getUnitPrice() : BigDecimal.ZERO;
        int qty = request.getOrderedQuantity() != null ? request.getOrderedQuantity() : 0;
        BigDecimal totalPrice = unitPrice.multiply(BigDecimal.valueOf(qty));

        item.setLineNumber(request.getLineNumber());
        item.setDescription(request.getDescription());
        item.setItemDescription(request.getDescription());
        item.setOrderedQuantity(qty);
        item.setQuantity(qty);
        item.setUnitOfMeasure(request.getUnitOfMeasure());
        item.setUnitPrice(unitPrice);
        item.setTotalAmount(totalPrice);
        item.setTotalPrice(totalPrice);
        item.setSpecifications(request.getSpecifications());

        PoLineItem updated = lineItemRepository.save(item);
        log.info("PO line item updated: {} for tenant: {}", id, tenantId);
        return toResponse(updated);
    }

    @Transactional
    public void deleteLineItem(Long id) {
        String tenantId = getTenantId();
        log.info("Deleting PO line item: {} for tenant: {}", id, tenantId);

        PoLineItem item = lineItemRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("PoLineItem not found with id: " + id));
        if (!item.getTenantId().equals(tenantId)) {
            throw new AccessDeniedException("Not authorized to delete this PoLineItem");
        }

        lineItemRepository.delete(item);
        log.info("PO line item deleted: {} for tenant: {}", id, tenantId);
    }

    private PoLineItemResponse toResponse(PoLineItem item) {
        return PoLineItemResponse.builder()
            .id(item.getId())
            .purchaseOrderId(item.getPurchaseOrder().getId())
            .prLineItemId(item.getPrLineItem() != null ? item.getPrLineItem().getId() : null)
            .lineNumber(item.getLineNumber())
            .description(item.getDescription())
            .orderedQuantity(item.getOrderedQuantity())
            .receivedQuantity(item.getReceivedQuantity())
            .unitOfMeasure(item.getUnitOfMeasure())
            .unitPrice(item.getUnitPrice())
            .totalPrice(item.getTotalPrice())
            .specifications(item.getSpecifications())
            .createdAt(item.getCreatedAt())
            .updatedAt(item.getUpdatedAt())
            .build();
    }
}
