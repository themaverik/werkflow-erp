package com.werkflow.business.inventory.dto;

import com.werkflow.business.inventory.entity.TransferRequest.TransferStatus;
import com.werkflow.business.inventory.entity.TransferRequest.TransferType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(
    description = "Asset transfer request response with complete transfer details and status",
    example = "{\"id\": 2001, \"assetInstanceId\": 1001, \"assetTag\": \"ASSET-001\", \"assetName\": \"Laptop\", \"fromDeptId\": 5, \"toDeptId\": 8, \"transferType\": \"PERMANENT\", \"transferReason\": \"Department reorganization\", \"status\": \"COMPLETED\", \"initiatedByUserId\": 20, \"initiatedDate\": \"2026-04-01T10:00:00Z\", \"approvedByUserId\": 15, \"approvedDate\": \"2026-04-02T14:00:00Z\", \"completedDate\": \"2026-04-03T09:00:00Z\", \"createdAt\": \"2026-04-01T10:00:00Z\", \"updatedAt\": \"2026-04-03T09:00:00Z\"}"
)
public class TransferRequestResponse {

    private Long id;
    private Long assetInstanceId;
    private String assetTag;
    private String assetName;
    private Long fromDeptId;
    private Long fromUserId;
    private Long toDeptId;
    private Long toUserId;
    private TransferType transferType;
    private String transferReason;
    private LocalDate expectedReturnDate;
    private Long initiatedByUserId;
    private LocalDateTime initiatedDate;
    private Long approvedByUserId;
    private LocalDateTime approvedDate;
    private LocalDateTime completedDate;
    private TransferStatus status;
    private String processInstanceId;
    private String rejectionReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
