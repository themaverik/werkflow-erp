package com.werkflow.business.common.apikey.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ApiKeyRegisterRequest {

    @NotBlank @Size(max = 64)
    @Pattern(regexp = "[0-9a-f]{64}", message = "keyHash must be a 64-character lowercase hex SHA-256 digest")
    private String keyHash;

    @NotBlank @Size(max = 100)
    private String name;

    /** Explicit tenant scoping for service-to-service calls where JWT carries no tenant claim. */
    @NotBlank @Size(max = 100)
    private String tenantId;
}
