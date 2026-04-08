package com.werkflow.business.procurement.service;

import com.werkflow.business.common.context.TenantContext;
import com.werkflow.business.common.sequence.NumberGenerationService;
import com.werkflow.business.procurement.dto.PoLineItemRequest;
import com.werkflow.business.procurement.dto.PoLineItemResponse;
import com.werkflow.business.procurement.dto.PurchaseOrderRequest;
import com.werkflow.business.procurement.dto.PurchaseOrderResponse;
import com.werkflow.business.procurement.entity.PoLineItem;
import com.werkflow.business.procurement.entity.PurchaseOrder;
import com.werkflow.business.procurement.entity.PurchaseRequest;
import com.werkflow.business.procurement.entity.Vendor;
import com.werkflow.business.procurement.repository.PoLineItemRepository;
import com.werkflow.business.procurement.repository.PurchaseOrderRepository;
import com.werkflow.business.procurement.repository.PurchaseRequestRepository;
import com.werkflow.business.procurement.repository.VendorRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for PurchaseOrder operations.
 * All queries are tenant-scoped via TenantContext.
 * Cross-domain FK references (Vendor, PurchaseRequest) are validated against the same tenant.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PurchaseOrderService {

    private final PurchaseOrderRepository poRepository;
    private final VendorRepository vendorRepository;
    private final PurchaseRequestRepository purchaseRequestRepository;
    private final PoLineItemRepository lineItemRepository;
    private final TenantContext tenantContext;
    private final NumberGenerationService numberGenerationService;

    private String getTenantId() {
        return tenantContext.getTenantId();
    }

    public Page<PurchaseOrderResponse> getAllPurchaseOrders(Pageable pageable) {
        String tenantId = getTenantId();
        log.debug("Fetching all purchase orders for tenant: {}", tenantId);
        return poRepository.findByTenantId(tenantId, pageable)
            .map(this::toResponse);
    }

    public PurchaseOrderResponse getPurchaseOrderById(Long id) {
        String tenantId = getTenantId();
        log.debug("Fetching purchase order by id: {} for tenant: {}", id, tenantId);
        PurchaseOrder po = poRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Purchase order not found with id: " + id));
        if (!po.getTenantId().equals(tenantId)) {
            throw new AccessDeniedException("Not authorized to access this PurchaseOrder");
        }
        return toResponse(po);
    }

    @Transactional
    public PurchaseOrderResponse createPurchaseOrder(PurchaseOrderRequest request) {
        String tenantId = getTenantId();
        log.info("Creating purchase order for tenant: {}", tenantId);

        // Validate vendor belongs to same tenant (cross-domain FK validation)
        Vendor vendor = vendorRepository.findById(request.getVendorId())
            .orElseThrow(() -> new EntityNotFoundException("Vendor not found with id: " + request.getVendorId()));
        if (!vendor.getTenantId().equals(tenantId)) {
            throw new AccessDeniedException("Vendor does not belong to the current tenant");
        }

        // Validate purchase request belongs to same tenant (if provided)
        PurchaseRequest purchaseRequest = null;
        if (request.getPurchaseRequestId() != null) {
            purchaseRequest = purchaseRequestRepository.findById(request.getPurchaseRequestId())
                .orElseThrow(() -> new EntityNotFoundException(
                    "Purchase request not found with id: " + request.getPurchaseRequestId()));
            if (!purchaseRequest.getTenantId().equals(tenantId)) {
                throw new AccessDeniedException("PurchaseRequest does not belong to the current tenant");
            }
        }

        PurchaseOrder po = PurchaseOrder.builder()
            .tenantId(tenantId)
            .poNumber(numberGenerationService.generatePoNumber(tenantId))
            .vendor(vendor)
            .purchaseRequest(purchaseRequest)
            .orderDate(request.getOrderDate())
            .expectedDeliveryDate(request.getExpectedDeliveryDate())
            .totalAmount(BigDecimal.ZERO)
            .grandTotal(BigDecimal.ZERO)
            .status(PurchaseOrder.PoStatus.DRAFT)
            .createdByUserId(request.getCreatedByUserId())
            .deliveryAddress(request.getDeliveryAddress())
            .paymentTerms(request.getPaymentTerms())
            .notes(request.getNotes())
            .processInstanceId(request.getProcessInstanceId())
            .build();

        PurchaseOrder saved = poRepository.save(po);

        if (request.getLineItems() != null && !request.getLineItems().isEmpty()) {
            for (PoLineItemRequest itemRequest : request.getLineItems()) {
                PoLineItem lineItem = buildLineItem(itemRequest, saved, tenantId);
                lineItemRepository.save(lineItem);
            }
        }

        log.info("Purchase order created with id: {} for tenant: {}", saved.getId(), tenantId);
        return toResponse(saved);
    }

    @Transactional
    public PurchaseOrderResponse updatePurchaseOrder(Long id, PurchaseOrderRequest request) {
        String tenantId = getTenantId();
        log.info("Updating purchase order: {} for tenant: {}", id, tenantId);

        PurchaseOrder po = poRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Purchase order not found with id: " + id));
        if (!po.getTenantId().equals(tenantId)) {
            throw new AccessDeniedException("Not authorized to update this PurchaseOrder");
        }

        // Validate vendor belongs to same tenant
        Vendor vendor = vendorRepository.findById(request.getVendorId())
            .orElseThrow(() -> new EntityNotFoundException("Vendor not found with id: " + request.getVendorId()));
        if (!vendor.getTenantId().equals(tenantId)) {
            throw new AccessDeniedException("Vendor does not belong to the current tenant");
        }

        po.setVendor(vendor);
        po.setOrderDate(request.getOrderDate());
        po.setExpectedDeliveryDate(request.getExpectedDeliveryDate());
        po.setCreatedByUserId(request.getCreatedByUserId());
        po.setDeliveryAddress(request.getDeliveryAddress());
        po.setPaymentTerms(request.getPaymentTerms());
        po.setNotes(request.getNotes());

        PurchaseOrder updated = poRepository.save(po);
        log.info("Purchase order updated: {} for tenant: {}", id, tenantId);
        return toResponse(updated);
    }

    @Transactional
    public void deletePurchaseOrder(Long id) {
        String tenantId = getTenantId();
        log.info("Deleting purchase order: {} for tenant: {}", id, tenantId);

        PurchaseOrder po = poRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Purchase order not found with id: " + id));
        if (!po.getTenantId().equals(tenantId)) {
            throw new AccessDeniedException("Not authorized to delete this PurchaseOrder");
        }

        poRepository.delete(po);
        log.info("Purchase order deleted: {} for tenant: {}", id, tenantId);
    }

    public List<PoLineItemResponse> getLineItemsByPurchaseOrderId(Long purchaseOrderId) {
        String tenantId = getTenantId();
        log.debug("Fetching line items for PO: {} in tenant: {}", purchaseOrderId, tenantId);
        // Validate PO belongs to tenant before returning its line items
        poRepository.findById(purchaseOrderId)
            .filter(po -> po.getTenantId().equals(tenantId))
            .orElseThrow(() -> new EntityNotFoundException("Purchase order not found with id: " + purchaseOrderId));
        return lineItemRepository.findByPurchaseOrderIdAndTenantId(purchaseOrderId, tenantId).stream()
            .map(this::lineItemToResponse)
            .collect(Collectors.toList());
    }

    private PoLineItem buildLineItem(PoLineItemRequest req, PurchaseOrder po, String tenantId) {
        BigDecimal unitPrice = req.getUnitPrice() != null ? req.getUnitPrice() : BigDecimal.ZERO;
        int qty = req.getOrderedQuantity() != null ? req.getOrderedQuantity() : 0;
        BigDecimal totalPrice = unitPrice.multiply(BigDecimal.valueOf(qty));
        return PoLineItem.builder()
            .tenantId(tenantId)
            .purchaseOrder(po)
            .lineNumber(req.getLineNumber())
            .description(req.getDescription())
            .itemDescription(req.getDescription())
            .orderedQuantity(qty)
            .quantity(qty)
            .unitOfMeasure(req.getUnitOfMeasure())
            .unitPrice(unitPrice)
            .totalAmount(totalPrice)
            .totalPrice(totalPrice)
            .specifications(req.getSpecifications())
            .build();
    }

    private PurchaseOrderResponse toResponse(PurchaseOrder po) {
        String tenantId = getTenantId();
        List<PoLineItemResponse> lineItems = lineItemRepository
            .findByPurchaseOrderIdAndTenantId(po.getId(), tenantId).stream()
            .map(this::lineItemToResponse)
            .collect(Collectors.toList());

        return PurchaseOrderResponse.builder()
            .id(po.getId())
            .poNumber(po.getPoNumber())
            .purchaseRequestId(po.getPurchaseRequest() != null ? po.getPurchaseRequest().getId() : null)
            .vendorId(po.getVendor().getId())
            .vendorName(po.getVendor().getName())
            .orderDate(po.getOrderDate())
            .expectedDeliveryDate(po.getExpectedDeliveryDate())
            .totalAmount(po.getTotalAmount())
            .status(po.getStatus())
            .createdByUserId(po.getCreatedByUserId())
            .deliveryAddress(po.getDeliveryAddress())
            .paymentTerms(po.getPaymentTerms())
            .notes(po.getNotes())
            .processInstanceId(po.getProcessInstanceId())
            .lineItems(lineItems)
            .createdAt(po.getCreatedAt())
            .updatedAt(po.getUpdatedAt())
            .build();
    }

    private PoLineItemResponse lineItemToResponse(PoLineItem item) {
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
