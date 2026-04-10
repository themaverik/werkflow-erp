package com.werkflow.business.common.meta;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnumValueDTO {
    private String value;
    private String label;
    private String description;
}
