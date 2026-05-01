package com.werkflow.business.common.identity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Request DTO for creating or updating a custody mapping.
 */
public record CustodyMappingRequest(

    @NotBlank(message = "custodyOwner is required")
    String custodyOwner,

    @NotEmpty(message = "candidateGroups must contain at least one group")
    List<@NotBlank(message = "candidate group name must not be blank") String> candidateGroups
) {}
