package com.werkflow.business.common.apikey.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ApiKeyGenerateRequest {

    @NotBlank @Size(max = 100)
    private String name;

    @NotBlank @Size(max = 100)
    private String tenantId;
}
