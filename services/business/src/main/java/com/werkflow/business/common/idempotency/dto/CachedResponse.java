package com.werkflow.business.common.idempotency.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@Builder
public class CachedResponse {

    private String body;
    private Integer statusCode;

    @Builder.Default
    private Map<String, String> headers = new HashMap<>();
}
