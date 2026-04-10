package com.werkflow.business.inventory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO for MaintenanceRecord response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaintenanceRecordResponseDto {

    private Long id;

    private Long assetInstanceId;

    private String assetTag;

    private String maintenanceType;

    private LocalDate scheduledDate;

    private LocalDate completedDate;

    private String performedBy;

    private BigDecimal cost;

    private String description;

    private LocalDate nextMaintenanceDate;

    private LocalDateTime createdAt;

    @Schema(example = "Jane Smith")
    private String createdByDisplayName;

    @Schema(example = "John Doe")
    private String updatedByDisplayName;
}
