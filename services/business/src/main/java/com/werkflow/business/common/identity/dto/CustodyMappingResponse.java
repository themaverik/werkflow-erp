package com.werkflow.business.common.identity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * API response DTO for custody mapping resources.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustodyMappingResponse {

    private Long id;
    private String tenantId;
    private String custodyOwner;
    private List<String> candidateGroups;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
