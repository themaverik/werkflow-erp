package com.werkflow.business.common.idempotency.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CachedResponse {

    private String body;
    private Integer statusCode;
    private Map<String, String> headers;

    public CachedResponse(String body, Integer statusCode) {
        this.body = body;
        this.statusCode = statusCode;
        this.headers = new HashMap<>();
    }
}
