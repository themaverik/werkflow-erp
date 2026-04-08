package com.werkflow.business.inventory.dto;

import com.werkflow.business.inventory.entity.AssetInstance.AssetCondition;
import com.werkflow.business.inventory.entity.AssetInstance.AssetStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(
    description = "Asset instance response with complete asset details",
    example = "{\"id\": 1001, \"assetDefinitionId\": 50, \"assetDefinitionName\": \"Laptop\", \"assetTag\": \"ASSET-001\", \"serialNumber\": \"SN123456\", \"purchaseDate\": \"2025-01-15\", \"purchaseCost\": 1500.00, \"warrantyExpiryDate\": \"2027-01-15\", \"condition\": \"GOOD\", \"status\": \"IN_USE\", \"currentLocation\": \"Office 101\", \"notes\": \"Dell XPS 15\", \"createdAt\": \"2025-01-15T10:00:00Z\", \"updatedAt\": \"2026-04-01T10:00:00Z\"}"
)
public class AssetInstanceResponse {

    private Long id;
    private Long assetDefinitionId;
    private String assetDefinitionName;
    private String assetTag;
    private String serialNumber;
    private LocalDate purchaseDate;
    private BigDecimal purchaseCost;
    private LocalDate warrantyExpiryDate;
    private AssetCondition condition;
    private AssetStatus status;
    private String currentLocation;
    private String notes;
    private Map<String, Object> metadata;
    private CustodyRecordResponse currentCustody;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
