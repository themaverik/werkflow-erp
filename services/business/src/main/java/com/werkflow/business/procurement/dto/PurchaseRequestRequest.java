package com.werkflow.business.procurement.dto;

import com.werkflow.business.procurement.entity.PurchaseRequest.Priority;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(
    description = "Purchase request with line items",
    example = "{\"requestingDeptId\": 5, \"requesterUserId\": 10, \"requestDate\": \"2026-04-01\", \"requiredByDate\": \"2026-04-15\", \"priority\": \"HIGH\", \"justification\": \"Office supplies replenishment\", \"notes\": \"For marketing team\", \"lineItems\": [{\"itemId\": 1, \"description\": \"Printer paper\", \"quantity\": 10, \"unitPrice\": 50, \"totalAmount\": 500}]}"
)
public class PurchaseRequestRequest {
    @NotNull(message = "Requesting department ID is required")
    private Long requestingDeptId;

    @NotNull(message = "Requester user ID is required")
    private Long requesterUserId;

    @NotNull(message = "Request date is required")
    private LocalDate requestDate;

    private LocalDate requiredByDate;

    private Priority priority;

    @Size(max = 2000)
    private String justification;

    @Size(max = 1000)
    private String notes;

    @Valid
    private List<PrLineItemRequest> lineItems;

    private String processInstanceId;
}
