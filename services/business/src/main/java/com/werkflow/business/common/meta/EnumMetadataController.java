package com.werkflow.business.common.meta;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for exposing metadata about enums across the application.
 * Provides endpoints for API clients to discover available enum values and their descriptions.
 */
@RestController
@RequestMapping("/api/v1/meta")
@RequiredArgsConstructor
@Tag(name = "Metadata", description = "Metadata endpoints for application enums and configurations")
public class EnumMetadataController {

    private final EnumMetadataService enumMetadataService;

    /**
     * Get metadata for all enums in the application.
     * Returns comprehensive information about all available enums including their values,
     * labels, and descriptions for UI/client development.
     *
     * @return ResponseEntity containing EnumMetadataResponseDTO with all enums
     */
    @GetMapping("/enums")
    @Operation(
        summary = "Get all enum metadata",
        description = "Returns metadata for all enums in the application including values, labels, and descriptions"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved enum metadata",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = EnumMetadataResponseDTO.class)
            )
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error"
        )
    })
    public ResponseEntity<EnumMetadataResponseDTO> getAllEnums() {
        EnumMetadataResponseDTO response = enumMetadataService.getAllEnums();
        return ResponseEntity.ok(response);
    }
}
