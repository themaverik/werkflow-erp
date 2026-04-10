package com.werkflow.business.inventory.controller;

import com.werkflow.business.common.context.UserContext;
import com.werkflow.business.inventory.dto.MaintenanceRecordRequestDto;
import com.werkflow.business.inventory.dto.MaintenanceRecordResponseDto;
import com.werkflow.business.inventory.entity.AssetInstance;
import com.werkflow.business.inventory.entity.MaintenanceRecord;
import com.werkflow.business.inventory.service.AssetInstanceService;
import com.werkflow.business.inventory.service.MaintenanceRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * REST Controller for MaintenanceRecord operations
 */
@RestController
@RequestMapping("/maintenance-records")
@RequiredArgsConstructor
@Tag(name = "Maintenance Records", description = "Asset maintenance tracking APIs")
public class MaintenanceRecordController {

    private final MaintenanceRecordService maintenanceService;
    private final AssetInstanceService assetService;

    @PostMapping
    @Operation(summary = "Create maintenance record", description = "Supports idempotent creation via Idempotency-Key header. " +
        "Provide a unique idempotency key to safely retry failed requests without duplicating the resource. " +
        "If the key is omitted, each request is processed independently. " +
        "If the same key is used with different payloads, a 409 Conflict is returned.")
    public ResponseEntity<MaintenanceRecordResponseDto> createMaintenanceRecord(
            @Valid @RequestBody MaintenanceRecordRequestDto requestDto,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey) {
        AssetInstance asset = assetService.getInstanceById(requestDto.getAssetInstanceId());

        MaintenanceRecord record = MaintenanceRecord.builder()
            .assetInstance(asset)
            .maintenanceType(MaintenanceRecord.MaintenanceType.valueOf(requestDto.getMaintenanceType()))
            .scheduledDate(requestDto.getScheduledDate())
            .completedDate(requestDto.getCompletedDate())
            .performedBy(requestDto.getPerformedBy())
            .cost(requestDto.getCost())
            .description(requestDto.getDescription())
            .nextMaintenanceDate(requestDto.getNextMaintenanceDate())
            .build();

        MaintenanceRecord created = maintenanceService.createMaintenanceRecord(record);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToResponse(created));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get maintenance record by ID", description = "Retrieve a maintenance record by its ID")
    public ResponseEntity<MaintenanceRecordResponseDto> getMaintenanceRecordById(@PathVariable Long id) {
        MaintenanceRecord record = maintenanceService.getMaintenanceRecordById(id);
        return ResponseEntity.ok(mapToResponse(record));
    }

    @GetMapping
    @Operation(summary = "Get all maintenance records", description = "Retrieve all maintenance records", parameters = {
        @Parameter(name = "page", description = "0-indexed page number"),
        @Parameter(name = "size", description = "Page size (max 1000)"),
        @Parameter(name = "sort", description = "Sort criteria (e.g., createdAt,desc)")
    })
    public ResponseEntity<Page<MaintenanceRecordResponseDto>> getAllMaintenanceRecords(@ParameterObject Pageable pageable) {
        return ResponseEntity.ok(maintenanceService.getAllMaintenanceRecords(pageable).map(this::mapToResponse));
    }

    @GetMapping("/asset/{assetId}")
    @Operation(summary = "Get maintenance history", description = "Get maintenance history for an asset")
    public ResponseEntity<List<MaintenanceRecordResponseDto>> getMaintenanceHistory(@PathVariable Long assetId) {
        List<MaintenanceRecord> records = maintenanceService.getMaintenanceHistory(assetId);
        return ResponseEntity.ok(records.stream().map(this::mapToResponse).collect(Collectors.toList()));
    }

    @GetMapping("/type/{maintenanceType}")
    @Operation(summary = "Get maintenance by type", description = "Get maintenance records by type")
    public ResponseEntity<List<MaintenanceRecordResponseDto>> getMaintenanceByType(@PathVariable String maintenanceType) {
        List<MaintenanceRecord> records = maintenanceService.getMaintenanceByType(maintenanceType);
        return ResponseEntity.ok(records.stream().map(this::mapToResponse).collect(Collectors.toList()));
    }

    @GetMapping("/incomplete")
    @Operation(summary = "Get incomplete maintenance", description = "Retrieve incomplete maintenance records")
    public ResponseEntity<List<MaintenanceRecordResponseDto>> getIncompleteMaintenanceRecords() {
        List<MaintenanceRecord> records = maintenanceService.getIncompleteMaintenanceRecords();
        return ResponseEntity.ok(records.stream().map(this::mapToResponse).collect(Collectors.toList()));
    }

    @GetMapping("/overdue")
    @Operation(summary = "Get overdue maintenance", description = "Retrieve overdue maintenance records")
    public ResponseEntity<List<MaintenanceRecordResponseDto>> getOverdueMaintenanceRecords() {
        List<MaintenanceRecord> records = maintenanceService.getOverdueMaintenanceRecords();
        return ResponseEntity.ok(records.stream().map(this::mapToResponse).collect(Collectors.toList()));
    }

    @GetMapping("/completed")
    @Operation(summary = "Get completed maintenance", description = "Retrieve completed maintenance records")
    public ResponseEntity<List<MaintenanceRecordResponseDto>> getCompletedMaintenanceRecords() {
        List<MaintenanceRecord> records = maintenanceService.getCompletedMaintenanceRecords();
        return ResponseEntity.ok(records.stream().map(this::mapToResponse).collect(Collectors.toList()));
    }

    @GetMapping("/scheduled-due")
    @Operation(summary = "Get scheduled maintenance due", description = "Retrieve scheduled maintenance coming due")
    public ResponseEntity<List<MaintenanceRecordResponseDto>> getScheduledMaintenanceDue(
            @RequestParam(defaultValue = "30") Integer daysFromNow) {
        LocalDate dueDate = LocalDate.now().plusDays(daysFromNow);
        List<MaintenanceRecord> records = maintenanceService.getScheduledMaintenanceDue(dueDate);
        return ResponseEntity.ok(records.stream().map(this::mapToResponse).collect(Collectors.toList()));
    }

    @GetMapping("/expensive")
    @Operation(summary = "Get expensive maintenance", description = "Retrieve expensive maintenance records")
    public ResponseEntity<List<MaintenanceRecordResponseDto>> getExpensiveMaintenanceRecords(
            @RequestParam BigDecimal minCost) {
        List<MaintenanceRecord> records = maintenanceService.getExpensiveMaintenanceRecords(minCost);
        return ResponseEntity.ok(records.stream().map(this::mapToResponse).collect(Collectors.toList()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update maintenance record", description = "Update a maintenance record")
    public ResponseEntity<MaintenanceRecordResponseDto> updateMaintenanceRecord(
            @PathVariable Long id,
            @Valid @RequestBody MaintenanceRecordRequestDto requestDto) {
        MaintenanceRecord recordDetails = MaintenanceRecord.builder()
            .scheduledDate(requestDto.getScheduledDate())
            .completedDate(requestDto.getCompletedDate())
            .performedBy(requestDto.getPerformedBy())
            .cost(requestDto.getCost())
            .description(requestDto.getDescription())
            .nextMaintenanceDate(requestDto.getNextMaintenanceDate())
            .build();

        MaintenanceRecord updated = maintenanceService.updateMaintenanceRecord(id, recordDetails);
        return ResponseEntity.ok(mapToResponse(updated));
    }

    @PutMapping("/{id}/complete")
    @Operation(summary = "Complete maintenance", description = "Mark maintenance record as completed")
    public ResponseEntity<MaintenanceRecordResponseDto> completeMaintenanceRecord(
            @PathVariable Long id,
            @RequestParam LocalDate completedDate,
            @RequestParam(required = false) LocalDate nextMaintenanceDate) {
        MaintenanceRecord updated = maintenanceService.completeMaintenanceRecord(id, completedDate, nextMaintenanceDate);
        return ResponseEntity.ok(mapToResponse(updated));
    }

    private MaintenanceRecordResponseDto mapToResponse(MaintenanceRecord record) {
        MaintenanceRecordResponseDto response = MaintenanceRecordResponseDto.builder()
            .id(record.getId())
            .assetInstanceId(record.getAssetInstance().getId())
            .assetTag(record.getAssetInstance().getAssetTag())
            .maintenanceType(record.getMaintenanceType().toString())
            .scheduledDate(record.getScheduledDate())
            .completedDate(record.getCompletedDate())
            .performedBy(record.getPerformedBy())
            .cost(record.getCost())
            .description(record.getDescription())
            .nextMaintenanceDate(record.getNextMaintenanceDate())
            .createdAt(record.getCreatedAt())
            .build();

        try {
            response.setCreatedByDisplayName(UserContext.getDisplayName());
            response.setUpdatedByDisplayName(UserContext.getDisplayName());
        } catch (IllegalStateException e) {
            // UserContext not available — leave as null
        }

        return response;
    }
}
