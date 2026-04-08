package com.werkflow.business.procurement.service;

import com.werkflow.business.common.context.TenantContext;
import com.werkflow.business.common.sequence.NumberGenerationService;
import com.werkflow.business.procurement.dto.ReceiptLineItemRequest;
import com.werkflow.business.procurement.dto.ReceiptLineItemResponse;
import com.werkflow.business.procurement.dto.ReceiptRequest;
import com.werkflow.business.procurement.dto.ReceiptResponse;
import com.werkflow.business.procurement.entity.PoLineItem;
import com.werkflow.business.procurement.entity.PurchaseOrder;
import com.werkflow.business.procurement.entity.Receipt;
import com.werkflow.business.procurement.entity.ReceiptLineItem;
import com.werkflow.business.procurement.repository.PoLineItemRepository;
import com.werkflow.business.procurement.repository.PurchaseOrderRepository;
import com.werkflow.business.procurement.repository.ReceiptLineItemRepository;
import com.werkflow.business.procurement.repository.ReceiptRepository;
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
 * Service for Receipt operations.
 * All queries are tenant-scoped via TenantContext.
 * Cross-domain FK references (PurchaseOrder, PoLineItem) are validated against the same tenant.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ReceiptService {

    private final ReceiptRepository receiptRepository;
    private final PurchaseOrderRepository poRepository;
    private final PoLineItemRepository poLineItemRepository;
    private final ReceiptLineItemRepository lineItemRepository;
    private final TenantContext tenantContext;
    private final NumberGenerationService numberGenerationService;

    private String getTenantId() {
        return tenantContext.getTenantId();
    }

    public Page<ReceiptResponse> getAllReceipts(Pageable pageable) {
        String tenantId = getTenantId();
        log.debug("Fetching all receipts for tenant: {}", tenantId);
        return receiptRepository.findByTenantId(tenantId, pageable)
            .map(this::toResponse);
    }

    public ReceiptResponse getReceiptById(Long id) {
        String tenantId = getTenantId();
        log.debug("Fetching receipt by id: {} for tenant: {}", id, tenantId);
        Receipt receipt = receiptRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Receipt not found with id: " + id));
        if (!receipt.getTenantId().equals(tenantId)) {
            throw new AccessDeniedException("Not authorized to access this Receipt");
        }
        return toResponse(receipt);
    }

    @Transactional
    public ReceiptResponse createReceipt(ReceiptRequest request) {
        String tenantId = getTenantId();
        log.info("Creating receipt for tenant: {}", tenantId);

        // Validate PO belongs to same tenant (cross-domain FK validation)
        PurchaseOrder po = poRepository.findById(request.getPurchaseOrderId())
            .orElseThrow(() -> new EntityNotFoundException(
                "Purchase order not found with id: " + request.getPurchaseOrderId()));
        if (!po.getTenantId().equals(tenantId)) {
            throw new AccessDeniedException("PurchaseOrder does not belong to the current tenant");
        }

        Receipt receipt = Receipt.builder()
            .tenantId(tenantId)
            .receiptNumber(numberGenerationService.generateGrnNumber(tenantId))
            .purchaseOrder(po)
            .receiptDate(request.getReceiptDate())
            .receivedByUserId(request.getReceivedByUserId())
            .status(Receipt.ReceiptStatus.DRAFT)
            .notes(request.getNotes())
            .discrepancyNotes(request.getDiscrepancyNotes())
            .build();

        Receipt saved = receiptRepository.save(receipt);

        if (request.getLineItems() != null && !request.getLineItems().isEmpty()) {
            for (ReceiptLineItemRequest itemRequest : request.getLineItems()) {
                // Validate PoLineItem belongs to same tenant before linking
                PoLineItem poLineItem = poLineItemRepository.findById(itemRequest.getPoLineItemId())
                    .orElseThrow(() -> new EntityNotFoundException(
                        "PoLineItem not found with id: " + itemRequest.getPoLineItemId()));
                if (!poLineItem.getTenantId().equals(tenantId)) {
                    throw new AccessDeniedException("PoLineItem does not belong to the current tenant");
                }

                ReceiptLineItem lineItem = ReceiptLineItem.builder()
                    .tenantId(tenantId)
                    .receipt(saved)
                    .poLineItem(poLineItem)
                    .receivedQuantity(itemRequest.getReceivedQuantity())
                    .acceptedQuantity(itemRequest.getAcceptedQuantity() != null ? itemRequest.getAcceptedQuantity() : 0)
                    .rejectedQuantity(itemRequest.getRejectedQuantity() != null ? itemRequest.getRejectedQuantity() : 0)
                    .condition(itemRequest.getCondition() != null
                        ? itemRequest.getCondition()
                        : ReceiptLineItem.ItemCondition.GOOD)
                    .notes(itemRequest.getNotes())
                    .build();
                lineItemRepository.save(lineItem);
            }
        }

        log.info("Receipt created with id: {} for tenant: {}", saved.getId(), tenantId);
        return toResponse(saved);
    }

    @Transactional
    public ReceiptResponse updateReceipt(Long id, ReceiptRequest request) {
        String tenantId = getTenantId();
        log.info("Updating receipt: {} for tenant: {}", id, tenantId);

        Receipt receipt = receiptRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Receipt not found with id: " + id));
        if (!receipt.getTenantId().equals(tenantId)) {
            throw new AccessDeniedException("Not authorized to update this Receipt");
        }

        receipt.setReceiptDate(request.getReceiptDate());
        receipt.setReceivedByUserId(request.getReceivedByUserId());
        receipt.setNotes(request.getNotes());
        receipt.setDiscrepancyNotes(request.getDiscrepancyNotes());

        Receipt updated = receiptRepository.save(receipt);
        log.info("Receipt updated: {} for tenant: {}", id, tenantId);
        return toResponse(updated);
    }

    @Transactional
    public void deleteReceipt(Long id) {
        String tenantId = getTenantId();
        log.info("Deleting receipt: {} for tenant: {}", id, tenantId);

        Receipt receipt = receiptRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Receipt not found with id: " + id));
        if (!receipt.getTenantId().equals(tenantId)) {
            throw new AccessDeniedException("Not authorized to delete this Receipt");
        }

        receiptRepository.delete(receipt);
        log.info("Receipt deleted: {} for tenant: {}", id, tenantId);
    }

    public List<ReceiptLineItemResponse> getLineItemsByReceiptId(Long receiptId) {
        String tenantId = getTenantId();
        log.debug("Fetching line items for receipt: {} in tenant: {}", receiptId, tenantId);
        // Validate receipt belongs to tenant before returning its line items
        receiptRepository.findById(receiptId)
            .filter(r -> r.getTenantId().equals(tenantId))
            .orElseThrow(() -> new EntityNotFoundException("Receipt not found with id: " + receiptId));
        return lineItemRepository.findByReceiptIdAndTenantId(receiptId, tenantId).stream()
            .map(this::lineItemToResponse)
            .collect(Collectors.toList());
    }

    private ReceiptResponse toResponse(Receipt receipt) {
        String tenantId = getTenantId();
        List<ReceiptLineItemResponse> lineItems = lineItemRepository
            .findByReceiptIdAndTenantId(receipt.getId(), tenantId).stream()
            .map(this::lineItemToResponse)
            .collect(Collectors.toList());

        return ReceiptResponse.builder()
            .id(receipt.getId())
            .receiptNumber(receipt.getReceiptNumber())
            .purchaseOrderId(receipt.getPurchaseOrder().getId())
            .poNumber(receipt.getPurchaseOrder().getPoNumber())
            .receiptDate(receipt.getReceiptDate())
            .receivedByUserId(receipt.getReceivedByUserId())
            .status(receipt.getStatus())
            .notes(receipt.getNotes())
            .discrepancyNotes(receipt.getDiscrepancyNotes())
            .lineItems(lineItems)
            .createdAt(receipt.getCreatedAt())
            .updatedAt(receipt.getUpdatedAt())
            .build();
    }

    private ReceiptLineItemResponse lineItemToResponse(ReceiptLineItem item) {
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
