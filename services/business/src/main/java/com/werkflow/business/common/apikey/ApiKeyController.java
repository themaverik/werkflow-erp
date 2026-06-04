package com.werkflow.business.common.apikey;

import com.werkflow.business.common.apikey.dto.ApiKeyGenerateRequest;
import com.werkflow.business.common.apikey.dto.ApiKeyGenerateResponse;
import com.werkflow.business.common.apikey.dto.ApiKeyRegisterRequest;
import com.werkflow.business.common.apikey.dto.ApiKeyRegisterResponse;
import com.werkflow.business.common.apikey.entity.ApiKey;
import com.werkflow.business.common.apikey.repository.ApiKeyRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

@RestController
@RequestMapping("/api-keys")
@RequiredArgsConstructor
public class ApiKeyController {

    private final ApiKeyRepository apiKeyRepository;

    /**
     * Registers a pre-hashed API key for the given tenant.
     * Called by admin service during connector registration — the raw key never leaves the caller.
     * tenantId is provided explicitly in the body (service-to-service callers may not carry a tenant JWT claim).
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'ENGINE_SERVICE')")
    public ResponseEntity<ApiKeyRegisterResponse> register(@Valid @RequestBody ApiKeyRegisterRequest request) {
        String tenantId = request.getTenantId();

        if (apiKeyRepository.findByKeyHashAndActiveTrue(request.getKeyHash()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "API key with this hash already exists");
        }

        ApiKey saved = apiKeyRepository.save(ApiKey.of(request.getKeyHash(), tenantId, request.getName()));

        return ResponseEntity.status(HttpStatus.CREATED).body(
            ApiKeyRegisterResponse.builder()
                .id(saved.getId())
                .name(saved.getName())
                .tenantId(tenantId)
                .createdAt(saved.getCreatedAt())
                .build()
        );
    }

    /**
     * Generates a cryptographically secure API key, stores only its SHA-256 hash,
     * and returns the raw key exactly once. Called by admin-service during connector
     * setup — the raw key goes directly to OpenBao without the browser ever seeing it.
     */
    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'ENGINE_SERVICE')")
    public ResponseEntity<ApiKeyGenerateResponse> generate(@Valid @RequestBody ApiKeyGenerateRequest request) {
        String rawKey = generateRawKey();
        String keyHash = sha256Hex(rawKey);

        if (apiKeyRepository.findByKeyHashAndActiveTrue(keyHash).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Generated key hash already exists — retry");
        }

        ApiKey saved = apiKeyRepository.save(ApiKey.of(keyHash, request.getTenantId(), request.getName()));

        return ResponseEntity.status(HttpStatus.CREATED).body(
            ApiKeyGenerateResponse.builder()
                .rawKey(rawKey)
                .id(saved.getId())
                .name(saved.getName())
                .tenantId(saved.getTenantId())
                .createdAt(saved.getCreatedAt())
                .build()
        );
    }

    private static String generateRawKey() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        StringBuilder sb = new StringBuilder(64);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(64);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
