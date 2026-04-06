package com.werkflow.business.procurement.service;

import com.werkflow.business.common.context.TenantContext;
import com.werkflow.business.procurement.dto.ReceiptLineItemRequest;
import com.werkflow.business.procurement.dto.ReceiptLineItemResponse;
import com.werkflow.business.procurement.entity.PoLineItem;
import com.werkflow.business.procurement.entity.Receipt;
import com.werkflow.business.procurement.entity.ReceiptLineItem;
import com.werkflow.business.procurement.repository.PoLineItemRepository;
import com.werkflow.business.procurement.repository.ReceiptLineItemRepository;
import com.werkflow.business.procurement.repository.ReceiptRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for ReceiptLineItem operations.
 * All queries are tenant-scoped via TenantContext.
 * Parent Receipt and linked PoLineItem are validated to belong to the same tenant.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ReceiptLineItemService {

    private final ReceiptLineItemRepository lineItemRepository;
    private final ReceiptRepository receiptRepository;
    private final PoLineItemRepository poLineItemRepository;
    private final TenantContext tenantContext;

    private String getTenantId() {
        return tenantContext.getTenantId();
    }

    public List<ReceiptLineItemResponse> getAllLineItems() {
        String tenantId = getTenantId();
        log.debug("Fetching all receipt line items for tenant: {}", tenantId);
        return lineItemRepository.findByTenantId(tenantId).stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    public ReceiptLineItemResponse getLineItemById(Long id) {
        String tenantId = getTenantId();
        log.debug("Fetching receipt line item by id: {} for tenant: {}", id, tenantId);
        ReceiptLineItem item = lineItemRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("ReceiptLineItem not found with id: " + id));
        if (!item.getTenantId().equals(tenantId)) {
            throw new AccessDeniedException("Not authorized to access this ReceiptLineItem");
        }
        return toResponse(item);
    }

    public List<ReceiptLineItemResponse> getLineItemsByReceiptId(Long receiptId) {
        String tenantId = getTenantId();
        log.debug("Fetching receipt line items for receipt: {} in tenant: {}", receiptId, tenantId);
        // Validate receipt belongs to tenant
        receiptRepository.findById(receiptId)
            .filter(r -> r.getTenantId().equals(tenantId))
            .orElseThrow(() -> new EntityNotFoundException("Receipt not found with id: " + receiptId));
        return lineItemRepository.findByReceiptIdAndTenantId(receiptId, tenantId).stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    @Transactional
    public ReceiptLineItemResponse createLineItem(Long receiptId, ReceiptLineItemRequest request) {
        String tenantId = getTenantId();
        log.info("Creating receipt line item for receipt: {} in tenant: {}", receiptId, tenantId);

        // Validate parent receipt belongs to same tenant
        Receipt receipt = receiptRepository.findById(receiptId)
            .orElseThrow(() -> new EntityNotFoundException("Receipt not found with id: " + receiptId));
        if (!receipt.getTenantId().equals(tenantId)) {
            throw new AccessDeniedException("Receipt does not belong to the current tenant");
        }

        // Validate PoLineItem belongs to same tenant (cross-domain FK validation)
        PoLineItem poLineItem = poLineItemRepository.findById(request.getPoLineItemId())
            .orElseThrow(() -> new EntityNotFoundException(
                "PoLineItem not found with id: " + request.getPoLineItemId()));
        if (!poLineItem.getTenantId().equals(tenantId)) {
            throw new AccessDeniedException("PoLineItem does not belong to the current tenant");
        }

        ReceiptLineItem item = ReceiptLineItem.builder()
            .tenantId(tenantId)
            .receipt(receipt)
            .poLineItem(poLineItem)
            .receivedQuantity(request.getReceivedQuantity())
            .acceptedQuantity(request.getAcceptedQuantity() != null ? request.getAcceptedQuantity() : 0)
            .rejectedQuantity(request.getRejectedQuantity() != null ? request.getRejectedQuantity() : 0)
            .condition(request.getCondition() != null ? request.getCondition() : ReceiptLineItem.ItemCondition.GOOD)
            .notes(request.getNotes())
            .build();

        ReceiptLineItem saved = lineItemRepository.save(item);
        log.info("Receipt line item created with id: {} for tenant: {}", saved.getId(), tenantId);
        return toResponse(saved);
    }

    @Transactional
    public ReceiptLineItemResponse updateLineItem(Long id, ReceiptLineItemRequest request) {
        String tenantId = getTenantId();
        log.info("Updating receipt line item: {} for tenant: {}", id, tenantId);

        ReceiptLineItem item = lineItemRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("ReceiptLineItem not found with id: " + id));
        if (!item.getTenantId().equals(tenantId)) {
            throw new AccessDeniedException("Not authorized to update this ReceiptLineItem");
        }

        item.setReceivedQuantity(request.getReceivedQuantity());
        item.setAcceptedQuantity(request.getAcceptedQuantity() != null ? request.getAcceptedQuantity() : 0);
        item.setRejectedQuantity(request.getRejectedQuantity() != null ? request.getRejectedQuantity() : 0);
        if (request.getCondition() != null) {
            item.setCondition(request.getCondition());
        }
        item.setNotes(request.getNotes());

        ReceiptLineItem updated = lineItemRepository.save(item);
        log.info("Receipt line item updated: {} for tenant: {}", id, tenantId);
        return toResponse(updated);
    }

    @Transactional
    public void deleteLineItem(Long id) {
        String tenantId = getTenantId();
        log.info("Deleting receipt line item: {} for tenant: {}", id, tenantId);

        ReceiptLineItem item = lineItemRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("ReceiptLineItem not found with id: " + id));
        if (!item.getTenantId().equals(tenantId)) {
            throw new AccessDeniedException("Not authorized to delete this ReceiptLineItem");
        }

        lineItemRepository.delete(item);
        log.info("Receipt line item deleted: {} for tenant: {}", id, tenantId);
    }

    private ReceiptLineItemResponse toResponse(ReceiptLineItem item) {
        return ReceiptLineItemResponse.builder()
            .id(item.getId())
            .receiptId(item.getReceipt().getId())
            .poLineItemId(item.getPoLineItem().getId())
            .itemDescription(item.getPoLineItem().getDescription())
            .receivedQuantity(item.getReceivedQuantity())
            .acceptedQuantity(item.getAcceptedQuantity())
            .rejectedQuantity(item.getRejectedQuantity())
            .condition(item.getCondition())
            .notes(item.getNotes())
            .createdAt(item.getCreatedAt())
            .updatedAt(item.getUpdatedAt())
            .build();
    }
}
