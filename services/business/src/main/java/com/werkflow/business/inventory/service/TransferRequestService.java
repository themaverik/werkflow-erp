package com.werkflow.business.inventory.service;

import com.werkflow.business.common.context.TenantContext;
import com.werkflow.business.inventory.entity.AssetInstance;
import com.werkflow.business.inventory.entity.TransferRequest;
import com.werkflow.business.inventory.repository.AssetInstanceRepository;
import com.werkflow.business.inventory.repository.TransferRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for TransferRequest operations (Order batching and inter-department transfers).
 * All queries are tenant-scoped via TenantContext.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class TransferRequestService {

    private final TransferRequestRepository transferRepository;
    private final AssetInstanceRepository assetRepository;
    private final TenantContext tenantContext;

    private String getTenantId() {
        return tenantContext.getTenantId();
    }

    /**
     * Create a new transfer request
     */
    @Transactional
    public TransferRequest createTransferRequest(TransferRequest request) {
        String tenantId = getTenantId();
        log.info("Creating new transfer request for asset: {} for tenant: {}",
            request.getAssetInstance().getAssetTag(), tenantId);

        // Validate that the asset instance belongs to the same tenant
        AssetInstance asset = assetRepository.findById(request.getAssetInstance().getId())
            .orElseThrow(() -> new EntityNotFoundException("Asset instance not found"));
        if (!asset.getTenantId().equals(tenantId)) {
            throw new AccessDeniedException("AssetInstance does not belong to the current tenant");
        }

        if (request.getFromDeptId() == null || request.getToDeptId() == null) {
            throw new IllegalArgumentException("From and To department IDs are required");
        }

        request.setTenantId(tenantId);
        request.setInitiatedDate(LocalDateTime.now());

        TransferRequest saved = transferRepository.save(request);
        log.info("Transfer request created with id: {} for tenant: {}", saved.getId(), tenantId);
        return saved;
    }

    /**
     * Get transfer request by ID
     */
    public TransferRequest getTransferRequestById(Long id) {
        String tenantId = getTenantId();
        TransferRequest request = transferRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Transfer request not found with id: " + id));
        if (!request.getTenantId().equals(tenantId)) {
            throw new AccessDeniedException("Not authorized to access this TransferRequest");
        }
        return request;
    }

    /**
     * Get transfer requests by asset
     */
    public List<TransferRequest> getTransfersByAsset(Long assetId) {
        String tenantId = getTenantId();
        AssetInstance asset = assetRepository.findById(assetId)
            .orElseThrow(() -> new EntityNotFoundException("Asset instance not found with id: " + assetId));
        if (!asset.getTenantId().equals(tenantId)) {
            throw new AccessDeniedException("AssetInstance does not belong to the current tenant");
        }

        return transferRepository.findByTenantIdAndAssetInstance(tenantId, asset);
    }

    /**
     * Get transfer requests by status
     */
    public List<TransferRequest> getTransfersByStatus(String status) {
        String tenantId = getTenantId();
        return transferRepository.findByTenantIdAndStatus(tenantId, status);
    }

    /**
     * Get pending transfer requests
     */
    public List<TransferRequest> getPendingTransfers() {
        String tenantId = getTenantId();
        return transferRepository.findPendingRequestsForTenant(tenantId);
    }

    /**
     * Get transfers from department
     */
    public List<TransferRequest> getTransfersFromDepartment(Long deptId) {
        String tenantId = getTenantId();
        return transferRepository.findByTenantIdAndFromDeptId(tenantId, deptId);
    }

    /**
     * Get transfers to department
     */
    public List<TransferRequest> getTransfersToDepartment(Long deptId) {
        String tenantId = getTenantId();
        return transferRepository.findByTenantIdAndToDeptId(tenantId, deptId);
    }

    /**
     * Get active inter-department transfers
     */
    public List<TransferRequest> getActiveInterDepartmentTransfers() {
        String tenantId = getTenantId();
        return transferRepository.findActiveInterDepartmentTransfersForTenant(tenantId);
    }

    /**
     * Get active loan requests
     */
    public List<TransferRequest> getActiveLoanRequests() {
        String tenantId = getTenantId();
        return transferRepository.findActiveLoanRequestsForTenant(tenantId);
    }

    /**
     * Get overdue loans
     */
    public List<TransferRequest> getOverdueLoans() {
        String tenantId = getTenantId();
        return transferRepository.findOverdueLoansForTenant(tenantId, LocalDateTime.now());
    }

    /**
     * Approve transfer request
     */
    @Transactional
    public TransferRequest approveTransfer(Long id, Long approverUserId) {
        String tenantId = getTenantId();
        log.info("Approving transfer request with id: {} for tenant: {}", id, tenantId);

        TransferRequest request = transferRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Transfer request not found with id: " + id));
        if (!request.getTenantId().equals(tenantId)) {
            throw new AccessDeniedException("Not authorized to approve this TransferRequest");
        }

        if (!request.getStatus().equals(TransferRequest.TransferStatus.PENDING)) {
            throw new IllegalStateException("Transfer request cannot be approved in status: " + request.getStatus());
        }

        request.setStatus(TransferRequest.TransferStatus.APPROVED);
        request.setApprovedByUserId(approverUserId);
        request.setApprovedDate(LocalDateTime.now());

        return transferRepository.save(request);
    }

    /**
     * Reject transfer request
     */
    @Transactional
    public TransferRequest rejectTransfer(Long id, String rejectionReason) {
        String tenantId = getTenantId();
        log.info("Rejecting transfer request with id: {} for tenant: {}", id, tenantId);

        TransferRequest request = transferRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Transfer request not found with id: " + id));
        if (!request.getTenantId().equals(tenantId)) {
            throw new AccessDeniedException("Not authorized to reject this TransferRequest");
        }

        if (!request.getStatus().equals(TransferRequest.TransferStatus.PENDING)) {
            throw new IllegalStateException("Transfer request cannot be rejected in status: " + request.getStatus());
        }

        request.setStatus(TransferRequest.TransferStatus.REJECTED);
        request.setRejectionReason(rejectionReason);

        return transferRepository.save(request);
    }

    /**
     * Complete transfer request
     */
    @Transactional
    public TransferRequest completeTransfer(Long id) {
        String tenantId = getTenantId();
        log.info("Completing transfer request with id: {} for tenant: {}", id, tenantId);

        TransferRequest request = transferRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Transfer request not found with id: " + id));
        if (!request.getTenantId().equals(tenantId)) {
            throw new AccessDeniedException("Not authorized to complete this TransferRequest");
        }

        if (!request.getStatus().equals(TransferRequest.TransferStatus.APPROVED)) {
            throw new IllegalStateException("Only approved transfers can be completed");
        }

        request.setStatus(TransferRequest.TransferStatus.COMPLETED);
        request.setCompletedDate(LocalDateTime.now());

        return transferRepository.save(request);
    }

    /**
     * Update transfer request
     */
    @Transactional
    public TransferRequest updateTransferRequest(Long id, TransferRequest requestDetails) {
        String tenantId = getTenantId();
        log.info("Updating transfer request with id: {} for tenant: {}", id, tenantId);

        TransferRequest request = transferRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Transfer request not found with id: " + id));
        if (!request.getTenantId().equals(tenantId)) {
            throw new AccessDeniedException("Not authorized to update this TransferRequest");
        }

        if (!request.getStatus().equals(TransferRequest.TransferStatus.PENDING)) {
            throw new IllegalStateException("Only pending transfers can be updated");
        }

        request.setTransferReason(requestDetails.getTransferReason());
        request.setExpectedReturnDate(requestDetails.getExpectedReturnDate());
        if (requestDetails.getRejectionReason() != null) {
            request.setRejectionReason(requestDetails.getRejectionReason());
        }

        return transferRepository.save(request);
    }

    /**
     * Get all transfer requests
     */
    public List<TransferRequest> getAllTransfers() {
        String tenantId = getTenantId();
        return transferRepository.findByTenantId(tenantId);
    }

    /**
     * Search transfer requests
     */
    public List<TransferRequest> searchTransfers(String searchTerm) {
        String tenantId = getTenantId();
        return transferRepository.searchTransfersForTenant(tenantId, searchTerm);
    }
}
