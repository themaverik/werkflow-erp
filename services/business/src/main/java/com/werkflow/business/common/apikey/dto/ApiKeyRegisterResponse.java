package com.werkflow.business.common.apikey.dto;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class ApiKeyRegisterResponse {
    private UUID id;
    private String name;
    private String tenantId;
    private OffsetDateTime createdAt;
}
