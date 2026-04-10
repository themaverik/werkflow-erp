package com.werkflow.business.procurement.dto;

import com.werkflow.business.procurement.entity.PurchaseRequest.Priority;
import com.werkflow.business.procurement.entity.PurchaseRequest.PrStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(
    description = "Purchase request response with complete details and status",
    example = "{\"id\": 501, \"prNumber\": \"PR-2026-0001\", \"requestingDeptId\": 5, \"requesterUserId\": 10, \"requestDate\": \"2026-04-01\", \"requiredByDate\": \"2026-04-15\", \"priority\": \"HIGH\", \"totalAmount\": 500, \"status\": \"APPROVED\", \"justification\": \"Office supplies\", \"lineItems\": [{\"id\": 601, \"itemId\": 1, \"description\": \"Printer paper\", \"quantity\": 10, \"unitPrice\": 50, \"totalAmount\": 500}], \"createdAt\": \"2026-04-01T10:00:00Z\", \"updatedAt\": \"2026-04-05T14:00:00Z\"}"
)
public class PurchaseRequestResponse {
    private Long id;
    private String prNumber;
    private Long requestingDeptId;
    private Long requesterUserId;
    private LocalDate requestDate;
    private LocalDate requiredByDate;
    private Priority priority;
    private BigDecimal totalAmount;
    private PrStatus status;
    private Long approvedByUserId;
    private LocalDateTime approvedDate;
    private String processInstanceId;
    private String rejectionReason;
    private String justification;
    private String notes;
    private List<PrLineItemResponse> lineItems;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Schema(example = "Jane Smith")
    private String createdByDisplayName;

    @Schema(example = "John Doe")
    private String updatedByDisplayName;
}
